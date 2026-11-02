package com.nurby.tinkerslegacy.test;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.library.tool.ToolModifiers;
import com.nurby.tinkerslegacy.library.tool.ToolState;
import com.nurby.tinkerslegacy.library.tool.traits.ToolTraitData;
import com.nurby.tinkerslegacy.library.tool.traits.ToolTraitEffects;
import com.nurby.tinkerslegacy.library.tool.traits.ToolTraits;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLTools;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.nurby.tinkerslegacy.test.ToolAssemblyGameTests.id;
import static com.nurby.tinkerslegacy.test.ToolAssemblyGameTests.tool;

@GameTestHolder(TinkersLegacy.MODID)
@PrefixGameTestTemplate(false)
public final class ToolModifierCombatGameTests {

        // Damage Ordering
        @GameTest(template = "empty")
        public static void criticalMultiplierIncludesMaterialAndPurchasedDamage(GameTestHelper helper) {
                ServerPlayer player = player(helper);
                try {
                        ItemStack stack = tool(TLTools.Type.BROADSWORD, id("manyullyn"));
                        ToolModifiers.setPoints(stack, ToolModifiers.SMITE, 24);
                        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                        player.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(1024);
                        player.setOnGround(false);
                        player.fallDistance = 1;
                        LivingEntity target = target(helper);
                        helper.assertTrue(ToolTraits.has(stack, "coldblooded"), "Material contributes the original full-health bonus");
                        helper.assertValueEqual(player.getAttackStrengthScale(0.5F), 1.0F, "Attack is fully charged");
                        player.attack(target);
                        // Four base damage, two Coldblooded, seven Smite, then critical and the original cutoff.
                        helper.assertTrue(Math.abs(200 - target.getHealth() - 19.05F) < 0.001F,
                                        "Critical damage includes both effect sources before the diminishing-return cutoff");
                } finally {
                        helper.getLevel().getServer().getPlayerList().remove(player);
                }
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void unchargedAttackScalesMaterialAndPurchasedDamage(GameTestHelper helper) {
                ServerPlayer player = player(helper);
                try {
                        ItemStack stack = tool(TLTools.Type.BROADSWORD, id("manyullyn"));
                        ToolModifiers.setPoints(stack, ToolModifiers.SMITE, 24);
                        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                        player.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(4);
                        player.setOnGround(true);
                        player.resetAttackStrengthTicker();
                        LivingEntity target = target(helper);
                        float cooldown = player.getAttackStrengthScale(0.5F);
                        helper.assertTrue(cooldown < 0.2F, "Attack begins with a depleted cooldown");
                        float expected = 13 * (0.2F + cooldown * cooldown * 0.8F);
                        player.attack(target);
                        helper.assertTrue(Math.abs(200 - target.getHealth() - expected) < 0.001F,
                                        "Cooldown reduces the entire material and purchased damage amount");
                } finally {
                        helper.getLevel().getServer().getPlayerList().remove(player);
                }
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void absorbedMeleeHitTriggersTraitsWithoutHealthDamageXp(GameTestHelper helper) {
                ServerPlayer player = player(helper);
                try {
                        ItemStack stack = tool(TLTools.Type.BROADSWORD, id("manyullyn"));
                        ToolState.initialize(stack, true);
                        ToolModifiers.setPoints(stack, ToolModifiers.WEBBED, 1);
                        ToolModifiers.setPoints(stack, ToolModifiers.SHULKING, 50);
                        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                        player.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(1024);
                        player.setOnGround(true);
                        LivingEntity target = target(helper);
                        target.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(100);
                        target.setAbsorptionAmount(100);
                        player.attack(target);
                        helper.assertValueEqual(target.getHealth(), 200.0F, "Absorption prevents all health damage");
                        helper.assertTrue(target.getAbsorptionAmount() < 100, "The melee attack succeeds against absorption");
                        helper.assertValueEqual(ToolTraitEffects.level(player, ToolTraitEffects.INSATIABLE), 1,
                                        "Successful absorbed hits still apply Insatiable");
                        helper.assertTrue(ToolState.damage(stack) > 0, "Successful absorbed hits still spend durability");
                        helper.assertTrue(target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && target.hasEffect(MobEffects.LEVITATION),
                                        "Webbed and Shulking apply even when absorption prevents health damage");
                        target.hurt(helper.getLevel().damageSources().genericKill(), Float.MAX_VALUE);
                        helper.assertValueEqual(stack.get(TLDataComponents.PROGRESS).xp(), 0L,
                                        "Absorbed damage never creates health-damage kill credit");
                } finally {
                        helper.getLevel().getServer().getPlayerList().remove(player);
                }
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void chargedShockingRunsBeforeTheMainMeleeHit(GameTestHelper helper) {
                ServerPlayer player = player(helper);
                try {
                        ItemStack stack = tool(TLTools.Type.BROADSWORD, id("electrum"));
                        stack.set(TLDataComponents.TRAIT_DATA, ToolTraitData.EMPTY.with("charge", 100));
                        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                        player.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(1024);
                        player.setOnGround(true);
                        LivingEntity target = target(helper);
                        player.attack(target);
                        helper.assertTrue(Math.abs(200 - target.getHealth() - 9) < 0.001F,
                                        "Five Shocking damage and four main-hit damage both land once");
                        helper.assertValueEqual(ToolTraits.data(stack).get("charge"), 0.0,
                                        "A charged successful melee attack consumes Shocking charge");
                } finally {
                        helper.getLevel().getServer().getPlayerList().remove(player);
                }
                helper.succeed();
        }

        // Fixtures
        private static ServerPlayer player(GameTestHelper helper) {
                ServerPlayer player = helper.makeMockServerPlayerInLevel();
                player.setGameMode(GameType.SURVIVAL);
                BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 3));
                player.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
                player.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(4);
                return player;
        }

        private static LivingEntity target(GameTestHelper helper) {
                LivingEntity target = helper.spawnWithNoFreeWill(EntityType.HUSK, 1, 2, 1);
                target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
                target.getAttribute(Attributes.ARMOR).setBaseValue(0);
                target.setHealth(200);
                return target;
        }
}
