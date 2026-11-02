package com.nurby.tinkerslegacy.test;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.entity.TLToolProjectile;
import com.nurby.tinkerslegacy.event.ToolLevelingEvents;
import com.nurby.tinkerslegacy.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.library.tool.ToolModifiers;
import com.nurby.tinkerslegacy.library.tool.ToolProgress;
import com.nurby.tinkerslegacy.library.tool.ToolRangedActions;
import com.nurby.tinkerslegacy.library.tool.ToolState;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLItems;
import com.nurby.tinkerslegacy.registry.TLMaterials;
import com.nurby.tinkerslegacy.registry.TLToolEntities;
import com.nurby.tinkerslegacy.registry.TLTools;
import com.nurby.tinkerslegacy.tools.RangedToolDefinition;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ArrowLooseEvent;
import net.neoforged.neoforge.event.entity.player.ArrowNockEvent;
import java.util.function.Consumer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(TinkersLegacy.MODID)
@PrefixGameTestTemplate(false)
public final class RangedToolGameTests {

        // Ammunition
        @GameTest(template = "empty")
        public static void ammoUsesTenDurabilityAndRecoveryPreservesProgress(GameTestHelper helper) {
                ItemStack arrows = ranged(TLTools.Type.ARROW);
                int maximum = ToolRangedActions.maxAmmo(arrows);
                helper.assertTrue(maximum > 0, "Arrow materials provide ammunition");
                ToolState.initialize(arrows, true);
                var identity = arrows.get(TLDataComponents.TOOL_ID);
                ToolProgress progress = new ToolProgress(2, 19, 2);
                arrows.set(TLDataComponents.PROGRESS, progress);
                for (int shot = 0; shot < maximum; shot++) {
                        helper.assertTrue(ToolRangedActions.consumeAmmo(arrows), "Each available shot can be spent");
                }
                helper.assertTrue(ToolState.broken(arrows), "The final shot leaves an empty retained ammo tool");
                helper.assertValueEqual(arrows.getCount(), 1, "An empty ammo tool is not destroyed");
                helper.assertTrue(!ToolRangedActions.consumeAmmo(arrows), "Empty ammunition cannot be fired");
                helper.assertTrue(ToolRangedActions.restoreAmmo(arrows), "Pickup restores one shot");
                helper.assertValueEqual(ToolRangedActions.ammo(arrows), 1, "Pickup adds precisely one ammunition unit");
                helper.assertValueEqual(arrows.get(TLDataComponents.PROGRESS), progress, "Pickup retains XP");
                helper.assertValueEqual(arrows.get(TLDataComponents.TOOL_ID), identity, "Pickup retains identity");
                helper.assertValueEqual(arrows.get(TLDataComponents.REPAIR_COUNT), 0, "Pickup does not add repair fatigue");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void projectilePickupRequiresMatchingAmmoAndCannotDuplicateIt(GameTestHelper helper) {
                ServerPlayer player = player(helper);
                ItemStack arrows = ranged(TLTools.Type.ARROW);
                ToolState.initialize(arrows, true);
                TestProjectile projectile = new TestProjectile(helper.getLevel(), player, arrows, ItemStack.EMPTY, 3, true);
                helper.assertTrue(!projectile.recover(player), "Pickup waits when there is no matching ammo tool");
                player.getInventory().setItem(0, arrows);
                helper.assertTrue(!projectile.recover(player), "A full ammo tool cannot receive an extra shot");
                ToolRangedActions.consumeAmmo(arrows);
                ToolRangedActions.consumeAmmo(arrows);
                int before = ToolRangedActions.ammo(arrows);
                helper.assertTrue(projectile.recover(player), "Matching damaged ammo receives the recovered shot");
                helper.assertValueEqual(ToolRangedActions.ammo(arrows), before + 1, "Only one shot is recovered");
                helper.assertTrue(!projectile.recover(player), "The same projectile cannot be recovered twice");
                finish(helper, player);
        }

        // Launchers
        @GameTest(template = "empty")
        public static void crossbowLoadingConsumesNothingAndFiringConsumesExactlyOnce(GameTestHelper helper) {
                ServerPlayer player = player(helper);
                ItemStack crossbow = ranged(TLTools.Type.CROSSBOW);
                ItemStack bolts = ranged(TLTools.Type.BOLT);
                player.setItemInHand(InteractionHand.MAIN_HAND, crossbow);
                player.getInventory().setItem(1, bolts);
                RangedToolDefinition definition = (RangedToolDefinition) ((DynamicTool) crossbow.getItem()).getDefinition();
                int ammunition = ToolRangedActions.ammo(bolts);
                ToolRangedActions.release(definition, crossbow, helper.getLevel(), player, 72000 - 4);
                helper.assertTrue(!ToolRangedActions.isLoaded(crossbow), "Releasing an incomplete charge does nothing");
                ToolRangedActions.release(definition, crossbow, helper.getLevel(), player, 0);
                helper.assertTrue(ToolRangedActions.isLoaded(crossbow), "A full charge persists on the stack");
                helper.assertValueEqual(ToolRangedActions.ammo(bolts), ammunition, "Loading retains the bolt");
                ((DynamicTool) crossbow.getItem()).calculateStats(crossbow);
                helper.assertTrue(ToolRangedActions.isLoaded(crossbow), "Recalculation retains crossbow charge");
                ToolRangedActions.use(definition, helper.getLevel(), player, InteractionHand.MAIN_HAND);
                helper.assertTrue(!ToolRangedActions.isLoaded(crossbow), "Firing clears the charge");
                helper.assertValueEqual(ToolRangedActions.ammo(bolts), ammunition - 1, "Firing spends one bolt");
                helper.assertValueEqual(ToolState.damage(crossbow), 1, "Firing wears the launcher once");
                ToolRangedActions.use(definition, helper.getLevel(), player, InteractionHand.MAIN_HAND);
                helper.assertValueEqual(ToolRangedActions.ammo(bolts), ammunition - 1, "Starting another charge does not fire twice");
                finish(helper, player);
        }

        @GameTest(template = "empty")
        public static void shortbowReleaseAndCreativeShotsRespectAmmo(GameTestHelper helper) {
                ServerPlayer player = player(helper);
                ItemStack bow = ranged(TLTools.Type.SHORTBOW);
                ItemStack arrows = ranged(TLTools.Type.ARROW);
                player.setItemInHand(InteractionHand.MAIN_HAND, bow);
                player.getInventory().setItem(1, arrows);
                RangedToolDefinition definition = (RangedToolDefinition) ((DynamicTool) bow.getItem()).getDefinition();
                int original = ToolRangedActions.ammo(arrows);
                ToolRangedActions.release(definition, bow, helper.getLevel(), player, 72000 - 4);
                helper.assertValueEqual(ToolRangedActions.ammo(arrows), original, "Sub-five-tick draws spend no ammo");
                ToolRangedActions.release(definition, bow, helper.getLevel(), player, 0);
                helper.assertValueEqual(ToolRangedActions.ammo(arrows), original - 1, "A completed shot spends one arrow");
                helper.assertValueEqual(ToolState.damage(bow), 1, "The launcher wears once");
                player.setGameMode(GameType.CREATIVE);
                ToolRangedActions.release(definition, bow, helper.getLevel(), player, 0);
                helper.assertValueEqual(ToolRangedActions.ammo(arrows), original - 1, "Creative arrows are free");
                helper.assertValueEqual(ToolState.damage(bow), 1, "Creative launchers retain durability");
                finish(helper, player);
        }

        @GameTest(template = "empty")
        public static void hasteImprovesDrawSpeedAndRejectsAmmunition(GameTestHelper helper) {
                ItemStack bow = ranged(TLTools.Type.SHORTBOW);
                ItemStack haste = ToolModifiers.applyIngredient(bow, new ItemStack(Items.REDSTONE));
                double before = bow.get(TLDataComponents.MODIFIED_STATS).get("draw_speed").doubleValue();
                double after = haste.get(TLDataComponents.MODIFIED_STATS).get("draw_speed").doubleValue();
                helper.assertTrue(Math.abs(after - before * 1.002) < 0.00001, "Redstone adds original bow draw speed");
                helper.assertTrue(ToolModifiers.applyIngredient(ranged(TLTools.Type.ARROW), new ItemStack(Items.REDSTONE)).isEmpty(),
                                "Projectile tools cannot spend modifiers on ineffective Haste");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void canceledDrawAndReleaseSpendNeitherAmmoNorDurability(GameTestHelper helper) {
                ServerPlayer player = player(helper);
                ItemStack bow = ranged(TLTools.Type.SHORTBOW);
                ItemStack arrows = ranged(TLTools.Type.ARROW);
                player.setItemInHand(InteractionHand.MAIN_HAND, bow);
                player.getInventory().setItem(1, arrows);
                RangedToolDefinition definition = (RangedToolDefinition) ((DynamicTool) bow.getItem()).getDefinition();
                Consumer<ArrowNockEvent> nock = event -> {
                        if (event.getEntity() == player) event.setCanceled(true);
                };
                Consumer<ArrowLooseEvent> loose = event -> {
                        if (event.getEntity() == player) event.setCanceled(true);
                };
                NeoForge.EVENT_BUS.addListener(nock);
                NeoForge.EVENT_BUS.addListener(loose);
                try {
                        var result = ToolRangedActions.use(definition, helper.getLevel(), player, InteractionHand.MAIN_HAND);
                        helper.assertTrue(!result.getResult().consumesAction(), "Canceled nocking does not begin the shot");
                        helper.assertTrue(!player.isUsingItem(), "Canceled nocking leaves the player idle");
                        ToolRangedActions.release(definition, bow, helper.getLevel(), player, 0);
                        helper.assertValueEqual(ToolRangedActions.ammo(arrows), ToolRangedActions.maxAmmo(arrows), "Canceled release spends no ammunition");
                        helper.assertValueEqual(ToolState.damage(bow), 0, "Canceled release spends no durability");
                } finally {
                        NeoForge.EVENT_BUS.unregister(nock);
                        NeoForge.EVENT_BUS.unregister(loose);
                }
                finish(helper, player);
        }

        // Projectiles
        @GameTest(template = "empty")
        public static void projectilePersistenceRetainsMaterialsDamageAndCreditIds(GameTestHelper helper) {
                ServerPlayer player = player(helper);
                ItemStack arrows = ranged(TLTools.Type.ARROW);
                ItemStack bow = ranged(TLTools.Type.SHORTBOW);
                ToolState.initialize(arrows, true);
                ToolState.initialize(bow, true);
                TLToolProjectile original = new TLToolProjectile(helper.getLevel(), player, arrows, bow, 7.5F, true);
                CompoundTag saved = new CompoundTag();
                original.addAdditionalSaveData(saved);
                TLToolProjectile restored = new TLToolProjectile(TLToolEntities.PROJECTILE.get(), helper.getLevel());
                restored.readAdditionalSaveData(saved);
                helper.assertValueEqual(restored.contributingTools(), original.contributingTools(), "Ammo and launcher IDs survive reload");
                helper.assertTrue(ItemStack.matches(restored.getItem(), original.getItem()), "Material appearance survives reload");
                helper.assertValueEqual(restored.impactDamage(), 7.5F, "Impact damage survives reload");
                finish(helper, player);
        }

        @GameTest(template = "empty")
        public static void projectileKillsCreditActualHealthDamageAfterToolSwitch(GameTestHelper helper) {
                ServerPlayer player = player(helper);
                ItemStack arrows = ranged(TLTools.Type.ARROW);
                ItemStack bow = ranged(TLTools.Type.SHORTBOW);
                ToolState.initialize(arrows, true);
                ToolState.initialize(bow, true);
                player.getInventory().setItem(4, arrows);
                player.getInventory().setItem(5, bow);
                LivingEntity target = helper.spawnWithNoFreeWill(EntityType.PIG, 1, 1, 1);
                target.setHealth(10);
                target.setAbsorptionAmount(4);
                TestProjectile projectile = new TestProjectile(helper.getLevel(), player, arrows, bow, 30, true);
                projectile.setDeltaMovement(0, 0, 1);
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
                projectile.hit(target);
                ToolLevelingEvents.onServerTick(new ServerTickEvent.Post(() -> true, helper.getLevel().getServer()));
                helper.assertValueEqual(arrows.get(TLDataComponents.PROGRESS).xp(), 10L, "Ammo gains health damage only, excluding absorption and overkill");
                helper.assertValueEqual(bow.get(TLDataComponents.PROGRESS).xp(), 6L, "Iron launcher earns draw-time XP once per qualifying hit after the hotbar switch");
                finish(helper, player);
        }

        @GameTest(template = "empty")
        public static void boltsBypassArmorForHalfTheirDamage(GameTestHelper helper) {
                ServerPlayer player = player(helper);
                ItemStack arrow = ranged(TLTools.Type.ARROW);
                ItemStack bolt = ranged(TLTools.Type.BOLT);
                LivingEntity normal = helper.spawnWithNoFreeWill(EntityType.PIG, 1, 1, 1);
                LivingEntity pierced = helper.spawnWithNoFreeWill(EntityType.PIG, 2, 1, 1);
                for (LivingEntity target : new LivingEntity[] { normal, pierced }) {
                        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
                        target.setHealth(100);
                        target.getAttribute(Attributes.ARMOR).setBaseValue(20);
                }
                new TestProjectile(helper.getLevel(), player, arrow, ItemStack.EMPTY, 10, true).hit(normal);
                new TestProjectile(helper.getLevel(), player, bolt, ItemStack.EMPTY, 10, true).hit(pierced);
                helper.assertTrue(pierced.getHealth() < normal.getHealth(), "Half-bypassing bolts defeat more armor than ordinary arrows");
                helper.assertTrue(pierced.getHealth() >= 90, "Armor bypass does not double total damage");
                finish(helper, player);
        }

        @GameTest(template = "empty")
        public static void vanillaArrowsRetainLauncherCreditWhenThePlayerSwitchesTools(GameTestHelper helper) {
                ServerPlayer player = player(helper);
                ItemStack bow = ranged(TLTools.Type.SHORTBOW);
                player.setItemInHand(InteractionHand.MAIN_HAND, bow);
                player.getInventory().setItem(1, new ItemStack(Items.ARROW, 2));
                RangedToolDefinition definition = (RangedToolDefinition) ((DynamicTool) bow.getItem()).getDefinition();
                ToolRangedActions.release(definition, bow, helper.getLevel(), player, 0);
                var arrows = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.projectile.Arrow.class,
                                player.getBoundingBox().inflate(2), arrow -> arrow.getOwner() == player);
                helper.assertValueEqual(arrows.size(), 1, "The bow uses the native entity for vanilla arrows");
                player.getInventory().setItem(4, bow);
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
                LivingEntity target = helper.spawnWithNoFreeWill(EntityType.PIG, 1, 1, 1);
                target.setHealth(10);
                target.hurt(player.damageSources().arrow(arrows.getFirst(), player), 30);
                ToolLevelingEvents.onServerTick(new ServerTickEvent.Post(() -> true, helper.getLevel().getServer()));
                helper.assertValueEqual(bow.get(TLDataComponents.PROGRESS).xp(), 6L, "Vanilla arrows pay immediate draw-time launcher XP after a slot switch");
                helper.assertValueEqual(player.getInventory().getItem(1).getCount(), 1, "Native arrow firing consumes precisely one arrow");
                finish(helper, player);
        }

        @GameTest(template = "empty")
        public static void emptyAmmunitionCanPunchWithoutMeleeXpOrWear(GameTestHelper helper) {
                ServerPlayer player = player(helper);
                ItemStack arrows = ranged(TLTools.Type.ARROW);
                ToolState.hurt(arrows, arrows.getMaxDamage());
                int damage = ToolState.damage(arrows);
                player.setItemInHand(InteractionHand.MAIN_HAND, arrows);
                LivingEntity target = helper.spawnWithNoFreeWill(EntityType.PIG, 1, 1, 1);
                target.setHealth(1);
                player.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(1024);
                player.attack(target);
                helper.assertTrue(target.getHealth() < 1, "Empty ammunition is still an ordinary item for punching");
                ToolLevelingEvents.onServerTick(new ServerTickEvent.Post(() -> true, helper.getLevel().getServer()));
                helper.assertValueEqual(arrows.get(TLDataComponents.PROGRESS).xp(), 0L, "Punching cannot level projectile ammunition");
                helper.assertValueEqual(ToolState.damage(arrows), damage, "Punching does not spend ammunition");
                helper.assertValueEqual(arrows.getDestroySpeed(net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState()), 1.0F,
                                "Empty ammunition mines at normal bare-hand speed");
                finish(helper, player);
        }

        @GameTest(template = "empty")
        public static void savedAndSynchronizedStatsRetainExactComponentEquality(GameTestHelper helper) {
                ItemStack arrows = ranged(TLTools.Type.ARROW);
                var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),
                                helper.getLevel().registryAccess());
                try {
                        for (var type : java.util.List.of(TLDataComponents.BASE_STATS, TLDataComponents.MODIFIED_STATS)) {
                                var original = arrows.get(type);
                                var encoded = TLDataComponents.TOOL_STATS_CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, original).getOrThrow();
                                var decoded = TLDataComponents.TOOL_STATS_CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, encoded).getOrThrow();
                                helper.assertValueEqual(decoded, original, "Saving retains the exact stat value representation");
                                TLDataComponents.TOOL_STATS_STREAM_CODEC.encode(buffer, original);
                                helper.assertValueEqual(TLDataComponents.TOOL_STATS_STREAM_CODEC.decode(buffer), original,
                                                "Client synchronization retains the exact stat value representation");
                        }
                } finally {
                        buffer.release();
                }
                helper.succeed();
        }

        // Helpers
        private static ItemStack ranged(TLTools.Type type) {
                DynamicTool item = TLItems.Tools.item(type);
                ItemStack stack = new ItemStack(item);
                stack.set(TLDataComponents.MATERIALS, item.getDefinition().defaultMaterials(TLMaterials.IRON.get()));
                item.calculateStats(stack);
                return stack;
        }

        private static ServerPlayer player(GameTestHelper helper) {
                ServerPlayer player = helper.makeMockServerPlayerInLevel();
                player.setGameMode(GameType.SURVIVAL);
                player.moveTo(helper.absolutePos(new net.minecraft.core.BlockPos(2, 2, 2)), 0, 0);
                return player;
        }

        private static void finish(GameTestHelper helper, ServerPlayer player) {
                helper.getLevel().getServer().getPlayerList().remove(player);
                helper.succeed();
        }

        private static final class TestProjectile extends TLToolProjectile {
                private TestProjectile(Level level, Player player, ItemStack ammunition, ItemStack launcher, float damage, boolean recoverable) {
                        super(level, player, ammunition, launcher, damage, recoverable);
                }

                private boolean recover(Player player) {
                        return tryPickup(player);
                }

                private void hit(Entity entity) {
                        onHitEntity(new EntityHitResult(entity));
                }
        }
}
