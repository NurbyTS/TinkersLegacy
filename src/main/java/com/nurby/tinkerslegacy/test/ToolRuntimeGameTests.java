package com.nurby.tinkerslegacy.test;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import com.nurby.tinkerslegacy.TLConfig;
import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.event.ToolLevelingEvents;
import com.nurby.tinkerslegacy.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.library.tool.ToolLeveling;
import com.nurby.tinkerslegacy.library.tool.ToolModifierEntry;
import com.nurby.tinkerslegacy.library.tool.ToolModifiers;
import com.nurby.tinkerslegacy.library.tool.ToolProgress;
import com.nurby.tinkerslegacy.library.tool.ToolState;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLTools;

import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static com.nurby.tinkerslegacy.test.ToolAssemblyGameTests.id;
import static com.nurby.tinkerslegacy.test.ToolAssemblyGameTests.tool;

@GameTestHolder(TinkersLegacy.MODID)
@PrefixGameTestTemplate(false)
public final class ToolRuntimeGameTests {

        // Progression
        @GameTest(template = "empty")
        public static void thresholdsCarryXpAndRespectPromotionCaps(GameTestHelper helper) {
                helper.assertValueEqual(ToolLeveling.threshold(500, 2.0, 0), 500L, "Initial requirement");
                helper.assertValueEqual(ToolLeveling.threshold(500, 2.0, 1), 1000L, "Second requirement");
                helper.assertValueEqual(ToolLeveling.threshold(500, 2.0, 2), 2000L, "Third requirement");
                helper.assertValueEqual(ToolLeveling.advance(ToolProgress.EMPTY, 499, 500, 2.0, -1),
                                new ToolProgress(0, 499, 0), "No early promotion");
                helper.assertValueEqual(ToolLeveling.advance(ToolProgress.EMPTY, 500, 500, 2.0, -1),
                                new ToolProgress(1, 0, 1), "Exact threshold earns one slot");
                helper.assertValueEqual(ToolLeveling.advance(ToolProgress.EMPTY, 1601, 500, 2.0, 0),
                                new ToolProgress(2, 101, 2), "Multiple promotions retain overflow XP");
                ToolProgress capped = ToolLeveling.advance(ToolProgress.EMPTY, 3600, 500, 2.0, 2);
                helper.assertValueEqual(capped, new ToolProgress(2, 2100, 2), "Cap counts earned promotions");
                helper.assertValueEqual(ToolLeveling.advance(capped, 100, 500, 2.0, 2), capped, "Capped state is retained");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void hugeXpAndNearOneMultipliersStayBounded(GameTestHelper helper) {
                ToolProgress fixed = ToolLeveling.advance(ToolProgress.EMPTY, Long.MAX_VALUE, 1, 1.0, 0);
                helper.assertValueEqual(fixed.promotions(), Integer.MAX_VALUE, "Levels saturate");
                helper.assertValueEqual(fixed.earnedSlots(), Integer.MAX_VALUE, "Slots saturate");
                helper.assertValueEqual(fixed.xp(), Long.MAX_VALUE - Integer.MAX_VALUE, "Fixed requirement overflow is retained");
                helper.assertValueEqual(ToolLeveling.advance(ToolProgress.EMPTY, Long.MAX_VALUE, 1, Math.nextUp(1.0), 0),
                                fixed, "Equal rounded requirements are processed in a bounded batch");
                helper.assertValueEqual(ToolLeveling.threshold(500, 2.0, Integer.MAX_VALUE), Long.MAX_VALUE, "Requirements saturate");
                ToolProgress saturated = ToolLeveling.advance(new ToolProgress(0, Long.MAX_VALUE - 5, 0), 100, 500, 2.0, 1);
                helper.assertValueEqual(saturated.xp(), Long.MAX_VALUE - 500, "XP addition saturates safely");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void progressCodecRetainsLongXpAndRejectsNegativeXp(GameTestHelper helper) {
                ToolProgress original = new ToolProgress(42, Long.MAX_VALUE, 43);
                var encoded = ToolProgress.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
                helper.assertValueEqual(ToolProgress.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow(), original, "Long XP round trip");
                encoded.getAsJsonObject().addProperty("xp", -1);
                helper.assertTrue(ToolProgress.CODEC.parse(JsonOps.INSTANCE, encoded).error().isPresent(), "Negative XP is rejected");
                helper.succeed();
        }

        // Modifiers
        @GameTest(template = "empty")
        public static void modifierSlotsAndIngredientCapsAreEnforced(GameTestHelper helper) {
                ItemStack original = tool(TLTools.Type.PICKAXE, id("wood"));
                original.set(TLDataComponents.STARTING_MODIFIERS, 1);
                ItemStack haste = ToolModifiers.applyIngredient(original, new ItemStack(Items.REDSTONE));
                helper.assertTrue(!haste.isEmpty(), "One redstone starts Haste");
                helper.assertValueEqual(ToolState.freeModifiers(haste), 0, "Starting a level spends one slot");
                helper.assertValueEqual(ToolState.freeModifiers(original), 1, "Preview preserves original slots");
                haste.set(TLDataComponents.MODIFIERS, List.of(new ToolModifierEntry(ToolModifiers.HASTE, 49)));
                helper.assertTrue(ToolModifiers.applyIngredient(haste, new ItemStack(Items.REDSTONE_BLOCK)).isEmpty(),
                                "A block cannot cross into an unaffordable level");
                haste = ToolModifiers.applyIngredient(haste, new ItemStack(Items.REDSTONE));
                helper.assertTrue(!haste.isEmpty(), "Completing an existing level needs no new slot");
                helper.assertTrue(ToolModifiers.applyIngredient(haste, new ItemStack(Items.REDSTONE)).isEmpty(), "Next level needs a slot");
                haste.set(TLDataComponents.STARTING_MODIFIERS, 5);
                haste.set(TLDataComponents.MODIFIERS, List.of(new ToolModifierEntry(ToolModifiers.HASTE, 249)));
                helper.assertTrue(ToolModifiers.applyIngredient(haste, new ItemStack(Items.REDSTONE_BLOCK)).isEmpty(), "No partial block consumption");
                haste = ToolModifiers.applyIngredient(haste, new ItemStack(Items.REDSTONE));
                helper.assertValueEqual(haste.get(TLDataComponents.MODIFIERS).getFirst().points(), 250, "Five Haste levels");
                helper.assertTrue(ToolModifiers.applyIngredient(haste, new ItemStack(Items.REDSTONE)).isEmpty(), "Haste cap");
                ItemStack sharp = original.copy();
                sharp.set(TLDataComponents.STARTING_MODIFIERS, 5);
                sharp.set(TLDataComponents.MODIFIERS, List.of(new ToolModifierEntry(ToolModifiers.SHARPNESS, 359)));
                helper.assertTrue(ToolModifiers.applyIngredient(sharp, new ItemStack(Items.QUARTZ_BLOCK)).isEmpty(), "Quartz block boundary");
                sharp = ToolModifiers.applyIngredient(sharp, new ItemStack(Items.QUARTZ));
                helper.assertValueEqual(sharp.get(TLDataComponents.MODIFIERS).getFirst().points(), 360, "Five Sharpness levels");
                helper.assertTrue(ToolModifiers.applyIngredient(sharp, new ItemStack(Items.QUARTZ)).isEmpty(), "Sharpness cap");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void recalculationPreservesModifierOrderAndSavedProgress(GameTestHelper helper) {
                ItemStack first = tool(TLTools.Type.PICKAXE, id("wood"));
                first.set(TLDataComponents.MODIFIERS, List.of(new ToolModifierEntry(ToolModifiers.DIAMOND, 1),
                                new ToolModifierEntry(ToolModifiers.HASTE, 50)));
                ItemStack reversed = first.copy();
                reversed.set(TLDataComponents.MODIFIERS, List.of(new ToolModifierEntry(ToolModifiers.HASTE, 50),
                                new ToolModifierEntry(ToolModifiers.DIAMOND, 1)));
                DynamicTool item = (DynamicTool) first.getItem();
                item.calculateStats(first);
                item.calculateStats(reversed);
                helper.assertTrue(!first.get(TLDataComponents.MODIFIED_STATS).get("mining_speed")
                                .equals(reversed.get(TLDataComponents.MODIFIED_STATS).get("mining_speed")), "Acquisition order affects Haste");
                ToolState.initialize(first, true);
                UUID identity = first.get(TLDataComponents.TOOL_ID);
                ToolProgress progress = new ToolProgress(2, 73, 2);
                first.set(TLDataComponents.PROGRESS, progress);
                first.setDamageValue(10);
                first.set(DataComponents.CUSTOM_NAME, Component.literal("Old faithful"));
                item.calculateStats(first);
                ItemStack once = first.copy();
                item.calculateStats(first);
                helper.assertTrue(ItemStack.matches(first, once), "Rebuilding is idempotent");
                helper.assertValueEqual(first.get(TLDataComponents.TOOL_ID), identity, "Identity remains stable");
                helper.assertValueEqual(first.get(TLDataComponents.PROGRESS), progress, "XP and earned slots remain");
                helper.assertValueEqual(first.getDamageValue(), 10, "Absolute damage remains");
                helper.assertValueEqual(first.getHoverName().getString(), "Old faithful", "Custom name remains");
                helper.assertTrue(ToolModifiers.applyIngredient(first, new ItemStack(Items.DIAMOND)).isEmpty(), "Diamond applies once");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void hasteUsesTheCorrectToolCategories(GameTestHelper helper) {
                for (TLTools.Type type : TLTools.Type.values()) {
                        ItemStack original = tool(type, id(type == TLTools.Type.BOLT ? "iron" : "wood"));
                        ItemStack haste = ToolModifiers.applyIngredient(original, new ItemStack(Items.REDSTONE));
                        if (TLTools.tool(type) instanceof com.nurby.tinkerslegacy.tools.ProjectileToolDefinition) {
                                helper.assertTrue(haste.isEmpty(), "Haste cannot modify standalone ammunition");
                                continue;
                        }
                        var before = original.get(TLDataComponents.MODIFIED_STATS);
                        var after = haste.get(TLDataComponents.MODIFIED_STATS);
                        helper.assertValueEqual(after.get("mining_speed").doubleValue() > before.get("mining_speed").doubleValue(),
                                        TLTools.tool(type).isHarvestTool(), type + " mining effect");
                        helper.assertValueEqual(after.get("attack_speed").doubleValue() > before.get("attack_speed").doubleValue(),
                                        TLTools.tool(type).isWeapon(), type + " attack effect");
                }
                helper.succeed();
        }

        // Durability
        @GameTest(template = "empty")
        public static void vanillaFinalDurabilityDamageDoesNotDestroyTheTool(GameTestHelper helper) {
                ItemStack stack = tool(TLTools.Type.PICKAXE, id("wood"));
                int maximum = stack.getMaxDamage();
                stack.setDamageValue(maximum - 1);
                AtomicBoolean destroyed = new AtomicBoolean();
                stack.hurtAndBreak(1, helper.getLevel(), (ServerPlayer) null, item -> destroyed.set(true));
                helper.assertValueEqual(stack.getCount(), 1, "Vanilla hurtAndBreak retains the actual stack");
                helper.assertTrue(!destroyed.get(), "Vanilla destruction callback does not run");
                helper.assertTrue(ToolState.broken(stack), "Final point marks the tool broken");
                helper.assertValueEqual(ToolState.damage(stack), maximum, "Logical damage reaches maximum");
                helper.assertValueEqual(stack.getDamageValue(), maximum - 1, "Vanilla damage stays below destruction");
                helper.assertValueEqual(stack.getBarWidth(), 0, "Broken durability bar is empty");
                helper.assertValueEqual(stack.getDestroySpeed(Blocks.STONE.defaultBlockState()), 0.0F, "Broken mining is disabled");
                stack.hurtAndBreak(100, helper.getLevel(), (ServerPlayer) null, item -> destroyed.set(true));
                helper.assertValueEqual(stack.getCount(), 1, "Repeated damage cannot destroy broken tools");
                ToolState.repair(stack, 1);
                helper.assertTrue(!ToolState.broken(stack), "One repaired point makes the tool usable");
                helper.assertValueEqual(ToolState.damage(stack), maximum - 1, "Repair restores one logical point");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void upgradesDoNotRepairBrokenToolsOrReplaceTheirDamage(GameTestHelper helper) {
                ItemStack broken = tool(TLTools.Type.PICKAXE, id("wood"));
                int maximum = broken.getMaxDamage();
                ToolState.hurt(broken, maximum);
                ItemStack upgraded = ToolModifiers.applyIngredient(broken, new ItemStack(Items.DIAMOND));
                helper.assertTrue(ToolState.broken(upgraded), "An upgrade does not repair a tool");
                helper.assertValueEqual(upgraded.getMaxDamage(), maximum + 500, "Diamond durability bonus");
                helper.assertValueEqual(ToolState.damage(upgraded), maximum, "Upgrade retains prior broken damage");
                ToolState.repair(upgraded, 1);
                helper.assertTrue(!ToolState.broken(upgraded), "Explicit repair restores use");
                helper.assertValueEqual(ToolState.damage(upgraded), maximum - 1, "Repair uses original damage rather than new maximum");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void miningWearUsesEffectivenessAndRetainsTheLeafException(GameTestHelper helper) {
                try (TestPlayer test = player(helper)) {
                        ServerPlayer player = test.player();
                        ItemStack pickaxe = tool(TLTools.Type.PICKAXE, id("wood"));
                        // Effectiveness is a matching rule, even when the material's speed is below one.
                        pickaxe.set(DataComponents.TOOL, new Tool(
                                        List.of(Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_PICKAXE, 0.5F)), 1.0F, 1));
                        player.setItemInHand(InteractionHand.MAIN_HAND, pickaxe);
                        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
                        helper.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
                        player.gameMode.destroyBlock(pos);
                        helper.assertValueEqual(ToolState.damage(pickaxe), 1, "Slow effective mining costs one point");
                        helper.getLevel().setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
                        player.gameMode.destroyBlock(pos);
                        helper.assertValueEqual(ToolState.damage(pickaxe), 3, "Ineffective mining costs two points");
                        helper.getLevel().setBlockAndUpdate(pos.below(), Blocks.DIRT.defaultBlockState());
                        helper.getLevel().setBlockAndUpdate(pos, Blocks.SHORT_GRASS.defaultBlockState());
                        player.gameMode.destroyBlock(pos);
                        helper.assertValueEqual(ToolState.damage(pickaxe), 5, "Zero-hardness blocks retain classic wear");

                        ItemStack hatchet = tool(TLTools.Type.HATCHET, id("wood"));
                        player.setItemInHand(InteractionHand.MAIN_HAND, hatchet);
                        helper.getLevel().setBlockAndUpdate(pos, Blocks.OAK_LEAVES.defaultBlockState());
                        player.gameMode.destroyBlock(pos);
                        helper.assertTrue(helper.getLevel().getBlockState(pos).isAir(), "Hatchet removed leaves");
                        helper.assertValueEqual(ToolState.damage(hatchet), 0, "Hatchets do not spend durability on leaves");
                        player.setGameMode(GameType.CREATIVE);
                        helper.getLevel().setBlockAndUpdate(pos, Blocks.OAK_LOG.defaultBlockState());
                        player.gameMode.destroyBlock(pos);
                        helper.assertValueEqual(ToolState.damage(hatchet), 0, "Creative mining is free");
                }
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void playerAttacksUseOutgoingDamageAndWeaponCategoriesForWear(GameTestHelper helper) {
                try (TestPlayer test = player(helper)) {
                        ServerPlayer player = test.player();
                        player.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(35.0D);
                        player.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(1024.0D);
                        player.setOnGround(true);
                        for (TLTools.Type type : TLTools.Type.values()) {
                                ItemStack stack = tool(type, id(type == TLTools.Type.BOLT ? "iron" : "wood"));
                                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                                LivingEntity target = helper.spawnWithNoFreeWill(EntityType.PIG, 1, 1, 1);
                                target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0D);
                                target.setHealth(200.0F);
                                player.attack(target);
                                helper.assertTrue(target.getHealth() < 200.0F, type + " actually hit the target");
                                if (type == TLTools.Type.PICKAXE) {
                                        helper.assertTrue(Math.abs(200 - target.getHealth() - 32.55F) < 0.001F,
                                                        "The original fifteen-point cutoff reduces thirty-five damage to 32.55 before charging wear");
                                }
                                helper.assertValueEqual(ToolState.damage(stack), TLTools.tool(type) instanceof com.nurby.tinkerslegacy.tools.ProjectileToolDefinition
                                                ? 0 : TLTools.tool(type).isWeapon() ? 3 : 6,
                                                type + " pays floor(max(1, outgoing damage after cutoff / 10) times its category multiplier)");
                                target.discard();
                        }

                        ItemStack sword = tool(TLTools.Type.BROADSWORD, id("wood"));
                        player.setItemInHand(InteractionHand.MAIN_HAND, sword);
                        LivingEntity absorbed = helper.spawnWithNoFreeWill(EntityType.PIG, 1, 1, 1);
                        absorbed.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(100.0D);
                        absorbed.setAbsorptionAmount(100.0F);
                        player.attack(absorbed);
                        helper.assertValueEqual(absorbed.getHealth(), 10.0F, "Absorption prevents actual health damage");
                        helper.assertValueEqual(ToolState.damage(sword), 3, "Absorption does not reduce outgoing-damage wear");
                        absorbed.discard();
                        LivingEntity overkill = helper.spawnWithNoFreeWill(EntityType.PIG, 1, 1, 1);
                        overkill.setHealth(1.0F);
                        player.attack(overkill);
                        helper.assertValueEqual(ToolState.damage(sword), 6, "Overkill still uses the attack's outgoing damage");
                        overkill.discard();
                }
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void failedCreativeAndBrokenAttacksDoNotSpendDurability(GameTestHelper helper) {
                try (TestPlayer test = player(helper)) {
                        ServerPlayer player = test.player();
                        player.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(35.0D);
                        player.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(1024.0D);
                        player.setOnGround(true);
                        ItemStack stack = tool(TLTools.Type.PICKAXE, id("wood"));
                        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                        LivingEntity target = helper.spawnWithNoFreeWill(EntityType.PIG, 1, 1, 1);
                        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0D);
                        target.setHealth(200.0F);
                        target.setInvulnerable(true);
                        player.attack(target);
                        helper.assertValueEqual(ToolState.damage(stack), 0, "Failed attack spends no durability");
                        target.setInvulnerable(false);
                        player.attack(target);
                        helper.assertValueEqual(ToolState.damage(stack), 6, "Successful attack after failure charges once using damage after cutoff");
                        player.setGameMode(GameType.CREATIVE);
                        target.invulnerableTime = 0;
                        player.attack(target);
                        helper.assertValueEqual(ToolState.damage(stack), 6, "Creative attack spends no durability");
                        player.setGameMode(GameType.SURVIVAL);
                        ToolState.hurt(stack, stack.getMaxDamage());
                        float health = target.getHealth();
                        target.invulnerableTime = 0;
                        player.attack(target);
                        helper.assertValueEqual(target.getHealth(), health, "Broken tools cannot attack");
                        helper.assertValueEqual(stack.getCount(), 1, "Broken attack retains the item");
                        target.discard();
                }
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void previewIdentityAndPersistentStateRemainSeparate(GameTestHelper helper) {
                ItemStack stack = tool(TLTools.Type.PICKAXE, id("wood"));
                ToolState.initialize(stack, false);
                helper.assertTrue(!stack.has(TLDataComponents.TOOL_ID), "Preview never assigns identity");
                ToolState.initialize(stack, true);
                UUID identity = stack.get(TLDataComponents.TOOL_ID);
                ToolState.initialize(stack, true);
                helper.assertValueEqual(stack.get(TLDataComponents.TOOL_ID), identity, "Real identity is assigned only once");
                stack.set(TLDataComponents.PROGRESS, new ToolProgress(3, 321, 3));
                stack.set(TLDataComponents.MODIFIERS, List.of(new ToolModifierEntry(ToolModifiers.HASTE, 23)));
                stack.set(TLDataComponents.REPAIR_COUNT, 7);
                ToolState.hurt(stack, stack.getMaxDamage());
                var saved = stack.save(helper.getLevel().registryAccess());
                ItemStack restored = ItemStack.parse(helper.getLevel().registryAccess(), saved).orElseThrow();
                helper.assertTrue(restored.is(stack.getItem()), "Item identity survives saving");
                helper.assertValueEqual(restored.getCount(), stack.getCount(), "Saved stack count");
                helper.assertValueEqual(restored.get(TLDataComponents.MATERIALS), stack.get(TLDataComponents.MATERIALS), "Saved materials");
                helper.assertValueEqual(restored.get(TLDataComponents.TOOL_ID), identity, "Saved tool identity");
                helper.assertValueEqual(restored.get(TLDataComponents.PROGRESS), stack.get(TLDataComponents.PROGRESS), "Saved progress");
                helper.assertValueEqual(restored.get(TLDataComponents.MODIFIERS), stack.get(TLDataComponents.MODIFIERS), "Saved modifier order and progress");
                helper.assertValueEqual(restored.get(TLDataComponents.REPAIR_COUNT), 7, "Saved repair count");
                helper.assertValueEqual(restored.get(TLDataComponents.STARTING_MODIFIERS), stack.get(TLDataComponents.STARTING_MODIFIERS), "Saved starting allowance");
                helper.assertTrue(ToolState.broken(restored), "Saved broken state");
                helper.assertValueEqual(ToolState.damage(restored), ToolState.damage(stack), "Saved absolute broken damage");
                helper.assertValueEqual(restored.getMaxDamage(), stack.getMaxDamage(), "Saved maximum durability");
                // The existing stats codec deliberately normalizes all Number values to doubles.
                for (var entry : stack.get(TLDataComponents.MODIFIED_STATS).entrySet()) {
                        helper.assertValueEqual(restored.get(TLDataComponents.MODIFIED_STATS).get(entry.getKey()).doubleValue(),
                                        entry.getValue().doubleValue(), "Saved stat " + entry.getKey());
                }
                helper.succeed();
        }

        // Server Actions
        @GameTest(template = "empty")
        public static void disabledLevelingAndCreativeUseRetainProgress(GameTestHelper helper) {
                try (TestPlayer test = player(helper)) {
                        ItemStack stack = tool(TLTools.Type.PICKAXE, id("wood"));
                        ToolProgress previous = new ToolProgress(2, 17, 2);
                        stack.set(TLDataComponents.PROGRESS, previous);
                        boolean enabled = TLConfig.ENABLE_LEVELING.get();
                        try {
                                TLConfig.ENABLE_LEVELING.set(false);
                                ToolLeveling.award(stack, test.player(), 10000);
                                helper.assertValueEqual(stack.get(TLDataComponents.PROGRESS), previous, "Disabled leveling preserves progress");
                                helper.assertTrue(!stack.get(TLDataComponents.LEVELING_ENABLED), "Disabled flag synchronizes");
                        } finally {
                                TLConfig.ENABLE_LEVELING.set(enabled);
                        }
                        test.player().setGameMode(GameType.CREATIVE);
                        stack.hurtAndBreak(1000, test.player(), EquipmentSlot.MAINHAND);
                        ToolLeveling.award(stack, test.player(), 1000);
                        helper.assertValueEqual(stack.getDamageValue(), 0, "Creative players spend no durability");
                        helper.assertValueEqual(stack.get(TLDataComponents.PROGRESS), previous, "Creative actions earn no XP");
                }
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void miningXpRequiresActualUncanceledRemoval(GameTestHelper helper) {
                try (TestPlayer test = player(helper)) {
                        ServerPlayer player = test.player();
                        ItemStack stack = tool(TLTools.Type.PICKAXE, id("wood"));
                        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
                        helper.getLevel().setBlockAndUpdate(pos, Blocks.DIAMOND_ORE.defaultBlockState());
                        helper.assertTrue(!stack.isCorrectToolForDrops(Blocks.DIAMOND_ORE.defaultBlockState()), "Wood cannot harvest diamond drops");
                        player.gameMode.destroyBlock(pos);
                        helper.assertTrue(helper.getLevel().getBlockState(pos).isAir(), "Ore was actually removed");
                        helper.assertValueEqual(xp(stack), 1L, "Effective mining earns XP independent of drop tier");
                        helper.assertValueEqual(ToolState.damage(stack), 1, "Effective mining costs one point");
                        helper.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
                        Consumer<BlockEvent.BreakEvent> cancel = event -> {
                                if (event.getPlayer() == player) event.setCanceled(true);
                        };
                        NeoForge.EVENT_BUS.addListener(cancel);
                        try {
                                player.gameMode.destroyBlock(pos);
                                helper.assertValueEqual(xp(stack), 1L, "Canceled break earns no XP");
                                helper.assertTrue(helper.getLevel().getBlockState(pos).is(Blocks.STONE), "Canceled block remains");
                                helper.assertValueEqual(ToolState.damage(stack), 1, "Canceled mining spends no durability");
                        } finally {
                                NeoForge.EVENT_BUS.unregister(cancel);
                        }
                        helper.getLevel().setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
                        player.gameMode.destroyBlock(pos);
                        helper.assertValueEqual(xp(stack), 1L, "Ineffective mining earns no XP");
                        helper.assertValueEqual(ToolState.damage(stack), 3, "Ineffective mining costs two points");
                        helper.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
                        stack.setDamageValue(stack.getMaxDamage() - 1);
                        player.gameMode.destroyBlock(pos);
                        helper.assertTrue(ToolState.broken(stack), "Final successful action breaks the tool");
                        helper.assertValueEqual(xp(stack), 2L, "Final successful action earns XP");
                        helper.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
                        player.gameMode.destroyBlock(pos);
                        helper.assertValueEqual(xp(stack), 2L, "Already broken tools earn no XP");
                }
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void rightClickXpRequiresCommittedTransformation(GameTestHelper helper) {
                try (TestPlayer test = player(helper)) {
                        ServerPlayer player = test.player();
                        ItemStack shovel = tool(TLTools.Type.SHOVEL, id("wood"));
                        player.setItemInHand(InteractionHand.MAIN_HAND, shovel);
                        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
                        helper.getLevel().setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
                        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
                        player.gameMode.useItemOn(player, helper.getLevel(), shovel, InteractionHand.MAIN_HAND, hit);
                        helper.assertTrue(helper.getLevel().getBlockState(pos).is(Blocks.DIRT_PATH), "Shovel creates path");
                        helper.assertValueEqual(xp(shovel), 1L, "Committed path awards one XP");
                        player.gameMode.useItemOn(player, helper.getLevel(), shovel, InteractionHand.MAIN_HAND, hit);
                        helper.assertValueEqual(xp(shovel), 1L, "Unchanged action earns no XP");
                }
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void combatCreditExcludesAbsorptionAndSurvivesHotbarChanges(GameTestHelper helper) {
                try (TestPlayer test = player(helper)) {
                        ServerPlayer player = test.player();
                        ItemStack stack = tool(TLTools.Type.BROADSWORD, id("wood"));
                        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                        LivingEntity target = helper.spawnWithNoFreeWill(EntityType.PIG, 1, 1, 1);
                        target.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(3);
                        target.setAbsorptionAmount(3);
                        float before = target.getHealth();
                        target.hurt(player.damageSources().playerAttack(player), 5);
                        helper.assertValueEqual(before - target.getHealth(), 2.0F, "Only actual health loss counts");
                        helper.assertValueEqual(xp(stack), 0L, "Surviving target defers XP");
                        player.getInventory().selected = 1;
                        target.invulnerableTime = 0;
                        target.hurt(target.damageSources().generic(), 1000);
                        flushDeaths(helper);
                        helper.assertValueEqual(xp(stack), 2L, "XP follows tool identity after switching slots");
                        target.discard();
                }
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void combatCreditIncludesBothRapierHitsButExcludesOverkill(GameTestHelper helper) {
                try (TestPlayer test = player(helper)) {
                        ServerPlayer player = test.player();
                        ItemStack stack = tool(TLTools.Type.RAPIER, id("wood"));
                        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                        LivingEntity target = helper.spawnWithNoFreeWill(EntityType.PIG, 1, 1, 1);
                        TLTools.tool(TLTools.Type.RAPIER).dealDamage(player, target, player.damageSources().playerAttack(player), 6);
                        helper.assertValueEqual(target.getHealth(), 4.0F, "Both rapier damage calls applied");
                        target.invulnerableTime = 0;
                        target.hurt(target.damageSources().generic(), 1000);
                        flushDeaths(helper);
                        helper.assertValueEqual(xp(stack), 6L, "Both rapier calls receive credit");
                        target.discard();
                        LivingEntity overkill = helper.spawnWithNoFreeWill(EntityType.PIG, 2, 1, 1);
                        overkill.hurt(player.damageSources().playerAttack(player), 1000);
                        flushDeaths(helper);
                        helper.assertValueEqual(xp(stack), 16L, "Overkill credit is capped at ten health");
                        overkill.discard();
                }
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void canceledDeathsDoNotPayCombatCredit(GameTestHelper helper) {
                try (TestPlayer test = player(helper)) {
                        ServerPlayer player = test.player();
                        ItemStack stack = tool(TLTools.Type.BROADSWORD, id("wood"));
                        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                        LivingEntity target = helper.spawnWithNoFreeWill(EntityType.PIG, 1, 1, 1);
                        Consumer<LivingDeathEvent> cancel = event -> {
                                if (event.getEntity() == target) {
                                        event.setCanceled(true);
                                        target.setHealth(1);
                                }
                        };
                        // Registered after the runtime listener to exercise a later cancellation.
                        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, LivingDeathEvent.class, cancel);
                        try {
                                target.hurt(player.damageSources().playerAttack(player), 1000);
                                flushDeaths(helper);
                                helper.assertValueEqual(xp(stack), 0L, "Canceled death pays no XP");
                        } finally {
                                NeoForge.EVENT_BUS.unregister(cancel);
                                target.discard();
                        }
                }
                helper.succeed();
        }

        // Helpers
        private static long xp(ItemStack stack) {
                return stack.getOrDefault(TLDataComponents.PROGRESS, ToolProgress.EMPTY).xp();
        }

        private static void flushDeaths(GameTestHelper helper) {
                ToolLevelingEvents.onServerTick(new ServerTickEvent.Post(() -> true, helper.getLevel().getServer()));
        }

        private static TestPlayer player(GameTestHelper helper) {
                CommonListenerCookie cookie = CommonListenerCookie.createInitial(
                                new GameProfile(UUID.randomUUID(), "tool-test-player"), false);
                ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                                cookie.gameProfile(), cookie.clientInformation());
                Connection connection = new Connection(PacketFlow.SERVERBOUND);
                EmbeddedChannel channel = new EmbeddedChannel(connection);
                helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
                player.setGameMode(GameType.SURVIVAL);
                BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
                player.moveTo(pos.getX(), pos.getY(), pos.getZ(), 0, 0);
                return new TestPlayer(player, channel);
        }

        private record TestPlayer(ServerPlayer player, EmbeddedChannel channel) implements AutoCloseable {

                @Override
                public void close() {
                        player.server.getPlayerList().remove(player);
                        player.discard();
                        channel.finishAndReleaseAll();
                }
        }
}
