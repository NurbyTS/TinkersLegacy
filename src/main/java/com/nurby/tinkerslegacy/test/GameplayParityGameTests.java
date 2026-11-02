package com.nurby.tinkerslegacy.test;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.entity.TLToolProjectile;
import com.nurby.tinkerslegacy.event.ToolCombatAbilities;
import com.nurby.tinkerslegacy.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.library.material.layer.MaterialLayer;
import com.nurby.tinkerslegacy.library.tool.ToolAssembly;
import com.nurby.tinkerslegacy.library.tool.ToolModifiers;
import com.nurby.tinkerslegacy.library.tool.ToolProgress;
import com.nurby.tinkerslegacy.library.tool.ToolState;
import com.nurby.tinkerslegacy.library.tool.traits.ToolTraits;
import com.nurby.tinkerslegacy.library.tool.traits.ToolTraitData;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLTools;
import com.nurby.tinkerslegacy.registry.ToolStatTypes;

import io.netty.buffer.Unpooled;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.connection.ConnectionType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.nurby.tinkerslegacy.test.ToolAssemblyGameTests.id;
import static com.nurby.tinkerslegacy.test.ToolAssemblyGameTests.tool;

@GameTestHolder(TinkersLegacy.MODID)
@PrefixGameTestTemplate(false)
public final class GameplayParityGameTests {

        // Stats
        @GameTest(template = "empty")
        public static void basicToolsUseOriginalAverageAndHandleRounding(GameTestHelper helper) {
                helper.assertValueEqual(tool(TLTools.Type.PICKAXE, id("iron")).getMaxDamage(), 276,
                                "Iron's 204 head plus 50 extra, rounded after its 0.85 handle, then plus 60");
                ItemStack mattock = tool(TLTools.Type.MATTOCK, id("wood"));
                helper.assertValueEqual(mattock.getMaxDamage(), 60,
                                "The two wooden heads average to 35, then add 25 handle durability");
                near(helper, mattock.getDestroySpeed(Blocks.DIRT.defaultBlockState()), 1.9,
                                "Mattock applies its original 0.95 mining multiplier");
                ItemStack mixed = mattock("wood", "iron");
                helper.assertValueEqual(mixed.getMaxDamage(), 144,
                                "Mixed mattock head durability truncates its average before adding the wooden handle");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void miningMultipliersApplyAfterHasteAndDiamond(GameTestHelper helper) {
                for (var entry : Map.of(TLTools.Type.HAMMER, 4.99856084, TLTools.Type.EXCAVATOR, 3.49899259).entrySet()) {
                        ItemStack stack = tool(entry.getKey(), id("iron"));
                        ToolModifiers.setPoints(stack, ToolModifiers.HASTE, 50);
                        rebuild(stack);
                        near(helper, stack.get(TLDataComponents.BASE_STATS).get("mining_speed").doubleValue(), 6,
                                        "Original raw material mining speed remains separate from tool scaling");
                        near(helper, stack.get(TLDataComponents.MODIFIED_STATS).get("mining_speed").doubleValue(), entry.getValue(),
                                        "Fifty original redstone increments are scaled after their calculation");
                        var before = stack.get(TLDataComponents.MODIFIED_STATS);
                        rebuild(stack);
                        helper.assertValueEqual(stack.get(TLDataComponents.MODIFIED_STATS), before,
                                        "Rebuilding never applies the mining multiplier a second time");
                }
                ItemStack hammer = tool(TLTools.Type.HAMMER, id("iron"));
                ToolModifiers.setPoints(hammer, ToolModifiers.DIAMOND, 1);
                rebuild(hammer);
                near(helper, hammer.getDestroySpeed(Blocks.STONE.defaultBlockState()), 2.6,
                                "Diamond's raw +0.5 becomes +0.2 on the hammer");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void oldStatCachesRefreshWithoutLosingPersistentState(GameTestHelper helper) {
                ItemStack stack = tool(TLTools.Type.PICKAXE, id("iron"));
                ToolState.initialize(stack, true);
                var identity = stack.get(TLDataComponents.TOOL_ID);
                var progress = new ToolProgress(1, 37, 1);
                stack.set(TLDataComponents.PROGRESS, progress);
                stack.set(TLDataComponents.REPAIR_COUNT, 3);
                stack.set(TLDataComponents.TOOL_DATA_VERSION, 2);
                stack.set(DataComponents.MAX_DAMAGE, 275);
                stack.setDamageValue(42);
                var player = helper.makeMockPlayer(GameType.SURVIVAL);
                ((DynamicTool) stack.getItem()).inventoryTick(stack, helper.getLevel(), player, 0, true);
                helper.assertValueEqual(stack.getMaxDamage(), 276, "An old cached tool receives the corrected original stat");
                helper.assertValueEqual(stack.getDamageValue(), 42, "Recalculation preserves absolute wear");
                helper.assertValueEqual(stack.get(TLDataComponents.PROGRESS), progress, "Recalculation preserves earned progress");
                helper.assertValueEqual(stack.get(TLDataComponents.TOOL_ID), identity, "Recalculation preserves combat identity");
                helper.assertValueEqual(stack.get(TLDataComponents.REPAIR_COUNT), 3, "Recalculation preserves repair count");
                helper.succeed();
        }

        // Harvest Roles
        @GameTest(template = "empty")
        public static void mattockUsesEachHeadsTierAndSynchronizesItsRules(GameTestHelper helper) {
                Map<TagKey<Block>, List<Holder<Block>>> original = new LinkedHashMap<>();
                BuiltInRegistries.BLOCK.getTags().forEach(pair -> original.put(pair.getFirst(), pair.getSecond().stream().toList()));
                Map<TagKey<Block>, List<Holder<Block>>> tags = new LinkedHashMap<>(original);
                addBlock(tags, BlockTags.MINEABLE_WITH_AXE, Blocks.OBSIDIAN);
                addBlock(tags, BlockTags.MINEABLE_WITH_SHOVEL, Blocks.IRON_BLOCK);
                addBlock(tags, BlockTags.NEEDS_IRON_TOOL, Blocks.IRON_BLOCK);
                try {
                        // A scoped datapack fixture gives both head roles real nonzero harvest requirements.
                        BuiltInRegistries.BLOCK.bindTags(tags);
                        ItemStack preview = mattock("wood", "cobalt");
                        ItemStack originalPreview = preview.copy();
                        rebuild(preview);
                        helper.assertTrue(ItemStack.matches(preview, originalPreview),
                                        "Rebuilding the maintenance preview produces equal item components");
                        ItemStack strongShovel = roundTrip(preview, helper);
                        helper.assertTrue(ItemStack.matches(strongShovel, preview),
                                        "The creative packet preserves structurally equal tool rules");
                        helper.assertTrue(!strongShovel.get(DataComponents.TOOL).isCorrectForDrops(Blocks.OBSIDIAN.defaultBlockState()),
                                        "The synchronized vanilla tool component also rejects the weak axe head");
                        helper.assertTrue(!strongShovel.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()),
                                        "The strong shovel cannot authorize an insufficient axe head");
                        helper.assertTrue(strongShovel.isCorrectToolForDrops(Blocks.IRON_BLOCK.defaultBlockState()),
                                        "The shovel retains its own high-tier drops after a creative-slot packet");
                        ItemStack strongAxe = roundTrip(mattock("cobalt", "wood"), helper);
                        helper.assertTrue(strongAxe.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()),
                                        "The axe retains its own high-tier drops");
                        helper.assertTrue(!strongAxe.isCorrectToolForDrops(Blocks.IRON_BLOCK.defaultBlockState()),
                                        "The strong axe cannot authorize an insufficient shovel head");
                        ToolModifiers.setPoints(strongAxe, ToolModifiers.DIAMOND, 1);
                        rebuild(strongAxe);
                        helper.assertValueEqual(strongAxe.get(TLDataComponents.MODIFIED_STATS).get("harvest_level_shovel").intValue(), 1,
                                        "Diamond improves the wooden shovel by one tier independently of the cobalt axe");
                        helper.assertTrue(!strongAxe.isCorrectToolForDrops(Blocks.IRON_BLOCK.defaultBlockState()),
                                        "The improved shovel still cannot harvest the fixture's explicit iron-tier requirement");
                        helper.assertTrue(!strongAxe.get(DataComponents.TOOL).isCorrectForDrops(Blocks.IRON_BLOCK.defaultBlockState()),
                                        "The synchronized vanilla rules retain the improved shovel's separate limitation");
                } finally {
                        BuiltInRegistries.BLOCK.bindTags(original);
                }
                helper.succeed();
        }

        // Combat
        @GameTest(template = "empty")
        public static void cutoffFollowsCriticalAndPrecedesCooldown(GameTestHelper helper) {
                ItemStack stack = tool(TLTools.Type.PICKAXE, id("wood"));
                near(helper, ToolCombatAbilities.finishDamage(stack, 30, 1), 28.5,
                                "Raw twenty damage times a 1.5 critical is reduced at the fifteen-point cutoff");
                near(helper, ToolCombatAbilities.finishDamage(stack, 30 * 0.4F, 0.5F), 28.5 * 0.4,
                                "The original cutoff is evaluated before cooldown reduction");
                near(helper, ToolCombatAbilities.finishDamage(stack, 15, 1), 15,
                                "Damage at the cutoff remains unchanged");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void scytheCalculatesModifiersForEachTarget(GameTestHelper helper) {
                ServerPlayer player = helper.makeMockServerPlayerInLevel();
                try {
                        player.setGameMode(GameType.SURVIVAL);
                        player.setOnGround(true);
                        player.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(10);
                        player.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(1024);
                        player.setHealth(5);
                        ItemStack stack = tool(TLTools.Type.SCYTHE, id("wood"));
                        ToolModifiers.setPoints(stack, ToolModifiers.SMITE, 24);
                        ToolModifiers.setPoints(stack, ToolModifiers.FIERY, 15);
                        ToolModifiers.setPoints(stack, ToolModifiers.NECROTIC, 1);
                        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                        LivingEntity undead = target(helper, EntityType.HUSK, 1);
                        LivingEntity ordinary = target(helper, EntityType.COW, 2);
                        player.attack(undead);
                        near(helper, 200 - undead.getHealth(), 17.8,
                                        "Primary undead receives Smite, cutoff, and its Fiery hit");
                        near(helper, 200 - ordinary.getHealth(), 11,
                                        "Secondary ordinary target gets its own Fiery hit but never inherits Smite");
                        helper.assertTrue(ordinary.isOnFire(), "Secondary targets execute before-hit modifier callbacks");
                        near(helper, player.getHealth(), 7.88,
                                        "Necrotic executes independently for actual damage on both targets");
                        helper.assertValueEqual(ToolState.damage(stack), 2, "Each successful target pays wear once without recursive sweeps");
                } finally {
                        helper.getLevel().getServer().getPlayerList().remove(player);
                }
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void scytheKeepsOriginalChargeForSecondaryTraits(GameTestHelper helper) {
                ServerPlayer player = helper.makeMockServerPlayerInLevel();
                try {
                        player.setGameMode(GameType.SURVIVAL);
                        ItemStack stack = tool(TLTools.Type.SCYTHE, id("electrum"));
                        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                        player.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(4);
                        for (int tick = 0; tick < 40; tick++) player.doTick();
                        player.setOnGround(true);
                        stack.set(TLDataComponents.TRAIT_DATA, ToolTraitData.EMPTY);
                        LivingEntity primary = target(helper, EntityType.COW, 1);
                        LivingEntity secondary = target(helper, EntityType.COW, 2);
                        near(helper, player.getAttackStrengthScale(0.5F), 1, "The ordinary-speed scythe starts fully charged");
                        player.attack(primary);
                        helper.assertTrue(player.getAttackStrengthScale(0.5F) < 0.2F,
                                        "Vanilla resets the cooldown before the secondary target's attack event");
                        helper.assertTrue(secondary.getHealth() < 200, "The secondary target is struck by the real scythe sweep");
                        near(helper, ToolTraits.data(stack).get("charge"), 30,
                                        "Shocking gains fifteen charge on each target using the original full-strength attack");
                } finally {
                        helper.getLevel().getServer().getPlayerList().remove(player);
                }
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void spongeLauncherDoesNotGiveArrowsSqueaky(GameTestHelper helper) {
                Player player = helper.makeMockPlayer(GameType.SURVIVAL);
                ItemStack arrow = tool(TLTools.Type.ARROW, id("wood"));
                ItemStack bow = tool(TLTools.Type.SHORTBOW, id("sponge"));
                ImpactProjectile projectile = new ImpactProjectile(helper.getLevel(), player, arrow, bow);
                helper.assertTrue(ToolTraits.has(bow, "squeaky"), "The launcher fixture has Squeaky");
                helper.assertTrue(!ToolTraits.has(projectile.effectsStack(), "squeaky"), "Ordinary launcher traits are not ammunition hit traits");
                LivingEntity target = target(helper, EntityType.COW, 1);
                projectile.hit(target);
                near(helper, 200 - target.getHealth(), 10, "The actual impact still damages its target");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void ammunitionRetainsItsOwnSqueakyTrait(GameTestHelper helper) {
                Player player = helper.makeMockPlayer(GameType.SURVIVAL);
                ItemStack arrow = tool(TLTools.Type.ARROW, id("sponge"));
                ItemStack bow = tool(TLTools.Type.SHORTBOW, id("wood"));
                ImpactProjectile projectile = new ImpactProjectile(helper.getLevel(), player, arrow, bow);
                helper.assertTrue(ToolTraits.has(projectile.effectsStack(), "squeaky"), "Sponge ammunition keeps its actual material trait");
                LivingEntity target = target(helper, EntityType.COW, 1);
                projectile.hit(target);
                near(helper, target.getHealth(), 200, "Squeaky ammunition still suppresses its own impact damage");
                helper.succeed();
        }

        // Passive Repair
        @GameTest(template = "empty")
        public static void passiveHealingCannotReviveBrokenTools(GameTestHelper helper) {
                ItemStack stack = tool(TLTools.Type.PICKAXE, id("wood"));
                ToolState.hurt(stack, stack.getMaxDamage());
                helper.assertTrue(ToolState.broken(stack), "The wooden fixture is broken");
                int wear = ToolState.damage(stack);
                ToolTraits.passiveHeal(stack, 20);
                helper.assertTrue(ToolState.broken(stack), "Passive healing preserves the broken flag");
                helper.assertValueEqual(ToolState.damage(stack), wear, "Passive healing cannot erase broken wear");
                helper.assertValueEqual(stack.get(TLDataComponents.REPAIR_COUNT), 0, "Passive healing does not count as maintenance");
                ToolState.repair(stack, 2);
                helper.assertTrue(!ToolState.broken(stack), "Deliberate repair restores use");
                ToolTraits.passiveHeal(stack, 2);
                helper.assertValueEqual(ToolState.damage(stack), wear - 4, "Passive healing still works on usable tools");
                helper.assertValueEqual(stack.get(TLDataComponents.REPAIR_COUNT), 1, "Passive healing retains the active repair count");
                helper.succeed();
        }

        // Fixtures
        private static void near(GameTestHelper helper, double actual, double expected, String message) {
                helper.assertTrue(Math.abs(actual - expected) < 0.0001, message + ": expected " + expected + ", got " + actual);
        }

        private static void rebuild(ItemStack stack) {
                ((DynamicTool) stack.getItem()).calculateStats(stack);
        }

        private static ItemStack mattock(String axe, String shovel) {
                return ToolAssembly.create(TLTools.tool(TLTools.Type.MATTOCK), List.of(
                                new MaterialLayer(ToolStatTypes.HEAD.getId(), id(axe)),
                                new MaterialLayer(ToolStatTypes.HEAD.getId(), id(shovel)),
                                new MaterialLayer(ToolStatTypes.HANDLE.getId(), id("wood"))));
        }

        private static void addBlock(Map<TagKey<Block>, List<Holder<Block>>> tags, TagKey<Block> tag, Block block) {
                List<Holder<Block>> values = new ArrayList<>(tags.getOrDefault(tag, List.of()));
                values.add(block.builtInRegistryHolder());
                tags.put(tag, values);
        }

        private static ItemStack roundTrip(ItemStack stack, GameTestHelper helper) {
                RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess(), ConnectionType.NEOFORGE);
                try {
                        ServerboundSetCreativeModeSlotPacket.STREAM_CODEC.encode(buffer, new ServerboundSetCreativeModeSlotPacket(36, stack));
                        return ServerboundSetCreativeModeSlotPacket.STREAM_CODEC.decode(buffer).itemStack();
                } finally {
                        buffer.release();
                }
        }

        private static <T extends Mob> LivingEntity target(GameTestHelper helper, EntityType<T> type, int z) {
                LivingEntity target = helper.spawnWithNoFreeWill(type, 1, 1, z);
                target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
                target.getAttribute(Attributes.ARMOR).setBaseValue(0);
                target.setHealth(200);
                return target;
        }

        private static final class ImpactProjectile extends TLToolProjectile {
                private ImpactProjectile(Level level, Player player, ItemStack ammunition, ItemStack launcher) {
                        super(level, player, ammunition, launcher, 10, false);
                }

                private void hit(LivingEntity target) {
                        onHitEntity(new EntityHitResult(target));
                }
        }
}
