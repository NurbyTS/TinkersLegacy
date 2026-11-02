package com.nurby.tinkerslegacy.test;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.entity.TLToolProjectile;
import com.nurby.tinkerslegacy.event.ToolCombatAbilities;
import com.nurby.tinkerslegacy.event.ToolLevelingEvents;
import com.nurby.tinkerslegacy.library.tool.ToolLeveling;
import com.nurby.tinkerslegacy.library.tool.ToolProgress;
import com.nurby.tinkerslegacy.library.tool.ToolState;
import com.nurby.tinkerslegacy.registry.TLAttachments;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLTools;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.nurby.tinkerslegacy.test.ToolAssemblyGameTests.id;
import static com.nurby.tinkerslegacy.test.ToolAssemblyGameTests.tool;

@GameTestHolder(TinkersLegacy.MODID)
@PrefixGameTestTemplate(false)
public final class LevelingParityGameTests {
        // Progression
        @GameTest(template = "empty")
        public static void fractionalRequirementsRoundEachPromotionWithoutUnboundedLoops(GameTestHelper helper) {
                helper.assertValueEqual(ToolLeveling.threshold(10, 1.25, 4), 22L, "10, 12, 15, 18, 22 use original integer steps");
                helper.assertValueEqual(ToolLeveling.threshold(3, 1.5, 3), 9L, "Per-step rounding differs from a single power");
                helper.assertValueEqual(ToolLeveling.threshold(1, Math.nextUp(1.0), Integer.MAX_VALUE), 1L,
                                "An unchanged rounded threshold remains bounded");
                helper.assertTrue(ToolLeveling.threshold(Integer.MAX_VALUE, 1.000000001, Integer.MAX_VALUE) > Integer.MAX_VALUE,
                                "Tiny nonzero increments are batched without billions of iterations");
                helper.assertValueEqual(ToolLeveling.advance(ToolProgress.EMPTY, 56, 10, 1.25, 0),
                                new ToolProgress(4, 1, 4), "Promotion loop retains XP under rounded requirements");
                helper.succeed();
        }

        // Persistence
        @GameTest(template = "empty")
        public static void victimSaveLoadRetainsUnpaidCreditAndPaysOnlyOnce(GameTestHelper helper) {
                ServerPlayer player = player(helper);
                try {
                        ItemStack sword = tool(TLTools.Type.BROADSWORD, id("wood"));
                        player.setItemInHand(InteractionHand.MAIN_HAND, sword);
                        LivingEntity target = helper.spawnWithNoFreeWill(EntityType.PIG, 1, 2, 1);
                        target.hurt(player.damageSources().playerAttack(player), 3);
                        helper.assertValueEqual(xp(sword), 0L, "Surviving victim defers melee XP");
                        helper.assertValueEqual(target.getData(TLAttachments.COMBAT_CREDIT).entries().size(), 1,
                                        "Credit is stored on the victim");
                        CompoundTag saved = target.saveWithoutId(new CompoundTag());
                        target.discard();
                        LivingEntity restored = EntityType.PIG.create(helper.getLevel());
                        restored.load(saved);
                        helper.assertValueEqual(restored.getData(TLAttachments.COMBAT_CREDIT).entries().getFirst().damage(), 3.0,
                                        "Entity serialization preserves exact unpaid credit");
                        player.getInventory().setItem(4, sword);
                        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
                        restored.invulnerableTime = 0;
                        restored.hurt(restored.damageSources().generic(), 1000);
                        flush(helper);
                        helper.assertValueEqual(xp(sword), 3L, "Reloaded victim pays the same tool after a slot switch");
                        flush(helper);
                        helper.assertValueEqual(xp(sword), 3L, "Consumed credit cannot pay twice");
                        helper.assertTrue(!restored.hasData(TLAttachments.COMBAT_CREDIT), "Paid ledger is removed");
                        restored.discard();
                } finally {
                        helper.getLevel().getServer().getPlayerList().remove(player);
                }
                helper.succeed();
        }

        // Launchers
        @GameTest(template = "empty")
        public static void launcherEarnsDrawTimeXpOnHitBeforeTheVictimDies(GameTestHelper helper) {
                ServerPlayer player = player(helper);
                try {
                        ItemStack bow = tool(TLTools.Type.SHORTBOW, id("wood"));
                        ItemStack arrows = tool(TLTools.Type.ARROW, id("wood"));
                        ToolState.initialize(bow, true);
                        ToolState.initialize(arrows, true);
                        player.getInventory().setItem(4, bow);
                        player.getInventory().setItem(5, arrows);
                        LivingEntity target = target(helper);
                        new Shot(helper, player, arrows, bow, 3, 1).hit(target);
                        helper.assertTrue(target.isAlive(), "The qualifying hit is not a kill");
                        helper.assertValueEqual(xp(bow), 3L, "Wood shortbow earns ceil(5 * 12 / 20) immediately");
                        helper.assertValueEqual(xp(arrows), 0L, "Ammunition retains damage-on-death XP");
                        target.invulnerableTime = 0;
                        new Shot(helper, player, arrows, bow, 3, 0.4).hit(target);
                        helper.assertValueEqual(xp(bow), 3L, "An impact at the original speed cutoff earns no launcher XP");
                        target.invulnerableTime = 0;
                        target.hurt(target.damageSources().generic(), 1000);
                        flush(helper);
                        helper.assertValueEqual(xp(bow), 3L, "Death does not pay the launcher again");
                        helper.assertValueEqual(xp(arrows), 6L, "Both successful ammunition hits keep health damage credit");
                        target.discard();
                } finally {
                        helper.getLevel().getServer().getPlayerList().remove(player);
                }
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void splitBoltDamageRewardsTheLauncherOnceAndCreativeHitsEarnNothing(GameTestHelper helper) {
                ServerPlayer player = player(helper);
                try {
                        ItemStack bow = tool(TLTools.Type.CROSSBOW, id("iron"));
                        ItemStack bolts = tool(TLTools.Type.BOLT, id("iron"));
                        ToolState.initialize(bow, true);
                        ToolState.initialize(bolts, true);
                        player.getInventory().setItem(4, bow);
                        player.getInventory().setItem(5, bolts);
                        LivingEntity target = target(helper);
                        new Shot(helper, player, bolts, bow, 4, 1).hit(target);
                        helper.assertValueEqual(xp(bow), 23L, "Iron crossbow grants ceil(45 / (4 * .5)) once for both halves");
                        player.setGameMode(GameType.CREATIVE);
                        target.invulnerableTime = 0;
                        new Shot(helper, player, bolts, bow, 4, 1).hit(target);
                        helper.assertValueEqual(xp(bow), 23L, "Creative hits do not earn launcher XP");
                        target.discard();
                } finally {
                        helper.getLevel().getServer().getPlayerList().remove(player);
                }
                helper.succeed();
        }

        // Defense
        @GameTest(template = "empty")
        public static void blockingRewardsFinalEventsAndRejectsLaterCancellation(GameTestHelper helper) {
                ServerPlayer player = player(helper);
                try {
                        // ServerPlayer.tick expires login immunity; doTick below advances actual item use.
                        for (int tick = 0; tick < 60; tick++) player.tick();
                        ItemStack sign = tool(TLTools.Type.BATTLESIGN, id("iron"));
                        player.setItemInHand(InteractionHand.MAIN_HAND, sign);
                        player.startUsingItem(InteractionHand.MAIN_HAND);
                        for (int tick = 0; tick < 6; tick++) player.doTick();
                        helper.assertTrue(ToolCombatAbilities.blocking(player), "The real sign is actively blocking");
                        LivingEntity attacker = target(helper);
                        attacker.setPos(player.getX(), player.getY(), player.getZ() + 2);
                        player.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(20);
                        player.setAbsorptionAmount(20);
                        float health = player.getHealth();
                        player.hurt(player.damageSources().mobAttack(attacker), 10);
                        flush(helper);
                        helper.assertValueEqual(player.getHealth(), health, "Absorption prevents the remaining health damage");
                        helper.assertTrue(ToolState.damage(sign) > 0, "The actual shield pipeline consumes sign durability");
                        helper.assertValueEqual(xp(sign), 10L, "A confirmed block rewards incoming damage even after absorption reduces new damage to zero");
                        attacker.discard();
                        var canceled = new LivingShieldBlockEvent(player, new DamageContainer(player.damageSources().mobAttack(player), 10), true);
                        NeoForge.EVENT_BUS.post(canceled);
                        canceled.setCanceled(true);
                        flush(helper);
                        helper.assertValueEqual(xp(sign), 10L, "Late cancellation prevents XP");
                        Arrow arrow = new Arrow(helper.getLevel(), player, new ItemStack(Items.ARROW), null);
                        arrow.setOwner(player);
                        var reflected = new ProjectileImpactEvent(arrow, new EntityHitResult(player));
                        reflected.setCanceled(true);
                        ToolLevelingEvents.reflected(player, sign, reflected, 0);
                        flush(helper);
                        helper.assertValueEqual(xp(sign), 11L, "Confirmed reflection grants at least one XP");
                        arrow.discard();
                } finally {
                        helper.getLevel().getServer().getPlayerList().remove(player);
                }
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void creativeLaunchEligibilitySurvivesReloadAndModeChanges(GameTestHelper helper) {
                ServerPlayer player = player(helper);
                try {
                        ItemStack bow = tool(TLTools.Type.SHORTBOW, id("wood"));
                        ItemStack arrows = tool(TLTools.Type.ARROW, id("wood"));
                        ToolState.initialize(bow, true);
                        ToolState.initialize(arrows, true);
                        player.getInventory().setItem(4, bow);
                        player.getInventory().setItem(5, arrows);
                        player.setGameMode(GameType.CREATIVE);
                        Shot creative = new Shot(helper, player, arrows, bow, 3, 1);
                        helper.assertTrue(!creative.canEarnXp(), "Creative use records ineligible XP at launch");
                        CompoundTag saved = creative.saveWithoutId(new CompoundTag());
                        creative.discard();
                        player.setGameMode(GameType.SURVIVAL);
                        Shot restored = new Shot(helper, player, arrows, bow, 3, 1);
                        restored.load(saved);
                        helper.assertTrue(!restored.canEarnXp(), "Chunk reload retains the original creative launch flag");
                        LivingEntity target = target(helper);
                        restored.hit(target);
                        helper.assertTrue(target.getHealth() < 100, "The restored projectile still lands a real damaging hit");
                        helper.assertValueEqual(xp(bow), 0L, "Switching to Survival cannot turn a creative shot into launcher XP");
                        target.invulnerableTime = 0;
                        target.hurt(target.damageSources().generic(), 1000);
                        flush(helper);
                        helper.assertValueEqual(xp(arrows), 0L, "Creative-origin ammunition also earns no death credit");
                        restored.discard();
                        target.discard();
                } finally {
                        helper.getLevel().getServer().getPlayerList().remove(player);
                }
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void creativeVanillaAmmoCannotEarnLauncherXpAfterModeChange(GameTestHelper helper) {
                ServerPlayer player = player(helper);
                try {
                        ItemStack bow = tool(TLTools.Type.SHORTBOW, id("wood"));
                        player.setItemInHand(InteractionHand.MAIN_HAND, bow);
                        player.getInventory().setItem(1, new ItemStack(Items.ARROW));
                        player.setGameMode(GameType.CREATIVE);
                        var definition = (com.nurby.tinkerslegacy.tools.RangedToolDefinition) TLTools.tool(TLTools.Type.SHORTBOW);
                        com.nurby.tinkerslegacy.library.tool.ToolRangedActions.release(definition, bow, helper.getLevel(), player, 0);
                        Arrow arrow = helper.getLevel().getEntitiesOfClass(Arrow.class, player.getBoundingBox().inflate(4))
                                        .stream().filter(entity -> entity.getOwner() == player).findFirst().orElseThrow();
                        helper.assertValueEqual(arrow.getPersistentData().getLong("tinkerslegacy:launcher_xp"), 0L,
                                        "The real vanilla-ammo launch path stores zero creative reward");
                        player.setGameMode(GameType.SURVIVAL);
                        LivingEntity target = target(helper);
                        target.hurt(player.damageSources().arrow(arrow, player), 3);
                        helper.assertValueEqual(xp(bow), 0L, "Creative vanilla ammunition stays ineligible after switching modes");
                        arrow.discard();
                        target.discard();
                } finally {
                        helper.getLevel().getServer().getPlayerList().remove(player);
                }
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void secondaryArrowEffectsDoNotRepeatLauncherHitRewards(GameTestHelper helper) {
                ServerPlayer player = player(helper);
                try {
                        ItemStack bow = tool(TLTools.Type.SHORTBOW, id("wood"));
                        ToolState.initialize(bow, true);
                        player.getInventory().setItem(4, bow);
                        Arrow arrow = new Arrow(helper.getLevel(), player, new ItemStack(Items.ARROW), null);
                        arrow.getPersistentData().putUUID("tinkerslegacy:launcher_id", bow.get(TLDataComponents.TOOL_ID));
                        arrow.getPersistentData().putLong("tinkerslegacy:launcher_xp", 3);
                        arrow.setDeltaMovement(0, 0, 1);
                        LivingEntity target = target(helper);
                        target.hurt(player.damageSources().indirectMagic(arrow, player), 3);
                        helper.assertValueEqual(xp(bow), 0L, "An extra effect using the same arrow source is not another projectile impact");
                        target.invulnerableTime = 0;
                        target.hurt(player.damageSources().arrow(arrow, player), 3);
                        helper.assertValueEqual(xp(bow), 3L, "The actual qualifying projectile damage awards one hit");
                        arrow.discard();
                        target.discard();
                } finally {
                        helper.getLevel().getServer().getPlayerList().remove(player);
                }
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void reflectingCustomAmmoUsesItsImpactDamage(GameTestHelper helper) {
                ServerPlayer player = player(helper);
                try {
                        ItemStack sign = tool(TLTools.Type.BATTLESIGN, id("iron"));
                        player.setItemInHand(InteractionHand.MAIN_HAND, sign);
                        player.startUsingItem(InteractionHand.MAIN_HAND);
                        for (int tick = 0; tick < 6; tick++) player.doTick();
                        Shot projectile = new Shot(helper, player, tool(TLTools.Type.ARROW, id("wood")), ItemStack.EMPTY, 10, 1);
                        projectile.setDeltaMovement(player.getLookAngle().scale(-1));
                        helper.getLevel().addFreshEntity(projectile);
                        var event = new ProjectileImpactEvent(projectile, new EntityHitResult(player));
                        NeoForge.EVENT_BUS.post(event);
                        helper.assertTrue(event.isCanceled(), "The real Battlesign reflection path blocks this incoming shot");
                        flush(helper);
                        helper.assertValueEqual(xp(sign), 10L, "Reflection credits custom launch damage instead of AbstractArrow's default base damage");
                        helper.assertValueEqual(ToolState.damage(sign), 10, "Reflection wear uses the same incoming custom damage");
                        projectile.discard();
                } finally {
                        helper.getLevel().getServer().getPlayerList().remove(player);
                }
                helper.succeed();
        }

        // Fixtures
        private static long xp(ItemStack stack) {
                return stack.getOrDefault(TLDataComponents.PROGRESS, ToolProgress.EMPTY).xp();
        }

        private static void flush(GameTestHelper helper) {
                ToolLevelingEvents.onServerTick(new ServerTickEvent.Post(() -> true, helper.getLevel().getServer()));
        }

        private static ServerPlayer player(GameTestHelper helper) {
                ServerPlayer player = helper.makeMockServerPlayerInLevel();
                player.setGameMode(GameType.SURVIVAL);
                player.moveTo(helper.absolutePos(new BlockPos(2, 2, 2)), 0, 0);
                return player;
        }

        private static LivingEntity target(GameTestHelper helper) {
                LivingEntity target = helper.spawnWithNoFreeWill(EntityType.PIG, 1, 2, 1);
                target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
                target.setHealth(100);
                return target;
        }

        private static final class Shot extends TLToolProjectile {
                private Shot(GameTestHelper helper, ServerPlayer player, ItemStack ammo, ItemStack launcher, float damage, double speed) {
                        super(helper.getLevel(), player, ammo, launcher, damage, false);
                        setDeltaMovement(0, 0, speed);
                }

                private void hit(Entity target) {
                        onHitEntity(new EntityHitResult(target));
                }
        }
}
