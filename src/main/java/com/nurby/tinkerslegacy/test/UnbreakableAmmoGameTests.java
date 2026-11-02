package com.nurby.tinkerslegacy.test;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.entity.TLToolProjectile;
import com.nurby.tinkerslegacy.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.library.tool.ToolModifiers;
import com.nurby.tinkerslegacy.library.tool.ToolRangedActions;
import com.nurby.tinkerslegacy.library.tool.ToolState;
import com.nurby.tinkerslegacy.library.tool.ToolVanillaProjectiles;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLItems;
import com.nurby.tinkerslegacy.registry.TLMaterials;
import com.nurby.tinkerslegacy.registry.TLTools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@GameTestHolder(TinkersLegacy.MODID)
@PrefixGameTestTemplate(false)
public final class UnbreakableAmmoGameTests {
        // Ammunition
        @GameTest(template = "empty")
        public static void preventedAmmoWearReportsNoConsumption(GameTestHelper helper) {
                for (TLTools.Type type : List.of(TLTools.Type.ARROW, TLTools.Type.BOLT, TLTools.Type.SHURIKEN)) {
                        ItemStack ammunition = unbreakable(helper, type);
                        int before = ToolRangedActions.ammo(ammunition);
                        helper.assertTrue(!ToolRangedActions.consumeAmmo(ammunition), "A Reinforced save reports no recoverable shot");
                        helper.assertValueEqual(ToolRangedActions.ammo(ammunition), before, "Unbreakable retains available ammunition");
                        helper.assertValueEqual(ToolState.damage(ammunition), 20, "Firing does not erase earlier wear");
                        helper.assertTrue(!ToolState.broken(ammunition), "A prevented shot cannot empty the ammunition tool");
                }
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void partialReinforcedUsesShooterRandomAndOnlyPaidShotsCanBeRecovered(GameTestHelper helper) {
                ServerPlayer player = player(helper);
                try {
                        ItemStack launcher = new ItemStack(Items.BOW);
                        ItemStack ammunition = tool(TLTools.Type.ARROW);
                        ammunition.setDamageValue(20);
                        ToolModifiers.setPoints(ammunition, ToolModifiers.REINFORCED, 2);
                        ((DynamicTool) ammunition.getItem()).calculateStats(ammunition, helper.getLevel().registryAccess());
                        player.setItemInHand(InteractionHand.MAIN_HAND, launcher);
                        player.getInventory().setItem(1, ammunition);
                        player.getRandom().setSeed(4096);
                        ItemStack free = ToolVanillaProjectiles.use(launcher, ammunition, player, false);
                        helper.assertTrue(free.has(DataComponents.INTANGIBLE_PROJECTILE), "A seeded Reinforced II save marks the shot intangible");
                        TLToolProjectile freeProjectile = ToolVanillaProjectiles.create(helper.getLevel(), player, launcher, free, false);
                        verifyFreeShot(helper, player, ammunition, freeProjectile);
                        player.getRandom().setSeed(0);
                        ItemStack paid = ToolVanillaProjectiles.use(launcher, ammunition, player, false);
                        helper.assertTrue(!paid.has(DataComponents.INTANGIBLE_PROJECTILE), "A seeded failed Reinforced II roll preserves recovery");
                        helper.assertValueEqual(ToolState.damage(ammunition), 30, "The unprotected shot spends exactly ten durability");
                        TLToolProjectile paidProjectile = ToolVanillaProjectiles.create(helper.getLevel(), player, launcher, paid, false);
                        CompoundTag saved = new CompoundTag();
                        paidProjectile.addAdditionalSaveData(saved);
                        helper.assertTrue(saved.getBoolean("Recoverable"), "A paid shot remains recoverable");
                        RecoverableProjectile restored = new RecoverableProjectile(helper.getLevel(), player, ammunition);
                        restored.readAdditionalSaveData(saved);
                        helper.assertTrue(restored.recover(player), "The paid shot restores its spent ammunition");
                        helper.assertValueEqual(ToolState.damage(ammunition), 20, "Paid recovery restores precisely its own shot");
                        helper.assertTrue(!restored.recover(player), "A paid shot still cannot be recovered twice");
                } finally {
                        cleanup(helper, player);
                }
                helper.succeed();
        }

        // Launchers
        @GameTest(template = "empty")
        public static void tinkersLaunchersFireFreeShotsWithoutRecovery(GameTestHelper helper) {
                ServerPlayer player = player(helper);
                try {
                        for (TLTools.Type type : List.of(TLTools.Type.SHORTBOW, TLTools.Type.LONGBOW, TLTools.Type.CROSSBOW)) {
                                ItemStack launcher = unbreakable(helper, type);
                                ItemStack ammunition = unbreakable(helper, type == TLTools.Type.CROSSBOW ? TLTools.Type.BOLT : TLTools.Type.ARROW);
                                player.setItemInHand(InteractionHand.MAIN_HAND, launcher);
                                player.getInventory().setItem(1, ammunition);
                                launcher.releaseUsing(helper.getLevel(), player, 0);
                                if (type == TLTools.Type.CROSSBOW) launcher.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                                List<TLToolProjectile> fired = projectiles(helper, player);
                                helper.assertValueEqual(fired.size(), 1, "Unbreakable ammunition still launches normally");
                                verifyFreeShot(helper, player, ammunition, fired.getFirst());
                                helper.assertValueEqual(ToolState.damage(launcher), 20, "Unbreakable launchers retain their earlier wear");
                                fired.forEach(TLToolProjectile::discard);
                        }
                } finally {
                        cleanup(helper, player);
                }
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void unbreakableShurikenCannotRepairItselfByPickup(GameTestHelper helper) {
                ServerPlayer player = player(helper);
                try {
                        ItemStack shuriken = unbreakable(helper, TLTools.Type.SHURIKEN);
                        player.setItemInHand(InteractionHand.MAIN_HAND, shuriken);
                        helper.assertTrue(shuriken.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getResult().consumesAction(),
                                        "A free shuriken throw remains a successful action");
                        List<TLToolProjectile> fired = projectiles(helper, player);
                        helper.assertValueEqual(fired.size(), 1, "One free throw creates one shuriken");
                        verifyFreeShot(helper, player, shuriken, fired.getFirst());
                } finally {
                        cleanup(helper, player);
                }
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void vanillaLaunchersMarkFreeAmmoIntangibleIncludingMultishot(GameTestHelper helper) {
                ServerPlayer player = player(helper);
                try {
                        for (boolean crossbow : List.of(false, true)) {
                                for (TLTools.Type type : List.of(TLTools.Type.ARROW, TLTools.Type.BOLT)) {
                                        ItemStack launcher = new ItemStack(crossbow ? Items.CROSSBOW : Items.BOW);
                                        if (crossbow) {
                                                launcher.enchant(helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                                                                .getHolderOrThrow(Enchantments.MULTISHOT), 1);
                                        }
                                        ItemStack ammunition = unbreakable(helper, type);
                                        player.setItemInHand(InteractionHand.MAIN_HAND, launcher);
                                        player.getInventory().setItem(1, ammunition);
                                        launcher.releaseUsing(helper.getLevel(), player, crossbow ? 0 : 72000 - 20);
                                        if (crossbow) {
                                                List<ItemStack> loaded = launcher.getOrDefault(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY).getItems();
                                                helper.assertValueEqual(loaded.size(), 3, "Vanilla Multishot still loads three shots");
                                                helper.assertTrue(loaded.stream().allMatch(stack -> stack.has(DataComponents.INTANGIBLE_PROJECTILE)),
                                                                "Every saved Reinforced and Multishot shot is nonrecoverable");
                                                launcher.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                                        }
                                        List<TLToolProjectile> fired = projectiles(helper, player);
                                        helper.assertValueEqual(fired.size(), crossbow ? 3 : 1, "Free ammunition preserves vanilla projectile count");
                                        for (TLToolProjectile projectile : fired) verifyFreeShot(helper, player, ammunition, projectile);
                                        fired.forEach(TLToolProjectile::discard);
                                }
                        }
                } finally {
                        cleanup(helper, player);
                }
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void rejectedProjectileSpawnsNeverSpendAmmunition(GameTestHelper helper) {
                ServerPlayer player = player(helper);
                AtomicInteger rejected = new AtomicInteger();
                Consumer<EntityJoinLevelEvent> listener = event -> {
                        if (event.getEntity() instanceof TLToolProjectile projectile && projectile.getOwner() == player) {
                                rejected.incrementAndGet();
                                event.setCanceled(true);
                        }
                };
                NeoForge.EVENT_BUS.addListener(listener);
                try {
                        ItemStack launcher = tool(TLTools.Type.SHORTBOW);
                        ItemStack arrows = tool(TLTools.Type.ARROW);
                        player.setItemInHand(InteractionHand.MAIN_HAND, launcher);
                        player.getInventory().setItem(1, arrows);
                        launcher.releaseUsing(helper.getLevel(), player, 0);
                        helper.assertValueEqual(ToolState.damage(launcher), 0, "A rejected projectile spends no launcher durability");
                        helper.assertValueEqual(ToolState.damage(arrows), 0, "A rejected projectile spends no ammunition");
                        ItemStack shuriken = tool(TLTools.Type.SHURIKEN);
                        player.setItemInHand(InteractionHand.MAIN_HAND, shuriken);
                        helper.assertTrue(!shuriken.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getResult().consumesAction(),
                                        "A rejected shuriken spawn fails the action");
                        helper.assertValueEqual(ToolState.damage(shuriken), 0, "A rejected throw spends no ammunition");
                        helper.assertValueEqual(rejected.get(), 2, "Both actual spawn attempts reached the cancellation hook");
                        helper.assertTrue(projectiles(helper, player).isEmpty(), "No canceled projectile survives in the world");
                } finally {
                        NeoForge.EVENT_BUS.unregister(listener);
                        cleanup(helper, player);
                }
                helper.succeed();
        }

        // Helpers
        private static ItemStack tool(TLTools.Type type) {
                DynamicTool item = TLItems.Tools.item(type);
                ItemStack stack = new ItemStack(item);
                stack.set(TLDataComponents.MATERIALS, item.getDefinition().defaultMaterials(TLMaterials.IRON.get()));
                item.calculateStats(stack);
                return stack;
        }

        private static ItemStack unbreakable(GameTestHelper helper, TLTools.Type type) {
                ItemStack stack = tool(type);
                stack.setDamageValue(20);
                ToolModifiers.setPoints(stack, ToolModifiers.REINFORCED, 5);
                ((DynamicTool) stack.getItem()).calculateStats(stack, helper.getLevel().registryAccess());
                ToolState.initialize(stack, true);
                return stack;
        }

        private static ServerPlayer player(GameTestHelper helper) {
                ServerPlayer player = helper.makeMockServerPlayerInLevel();
                player.setGameMode(GameType.SURVIVAL);
                player.moveTo(helper.absolutePos(new BlockPos(2, 2, 2)), 0, 0);
                return player;
        }

        private static List<TLToolProjectile> projectiles(GameTestHelper helper, ServerPlayer player) {
                return helper.getLevel().getEntitiesOfClass(TLToolProjectile.class, player.getBoundingBox().inflate(4),
                                projectile -> projectile.getOwner() == player);
        }

        private static void verifyFreeShot(GameTestHelper helper, ServerPlayer player, ItemStack ammunition, TLToolProjectile projectile) {
                helper.assertValueEqual(ToolState.damage(ammunition), 20, "Free shots retain earlier ammunition wear");
                CompoundTag saved = new CompoundTag();
                projectile.addAdditionalSaveData(saved);
                helper.assertTrue(!saved.getBoolean("Recoverable"), "Prevented ammo wear cannot produce a recoverable projectile");
                RecoverableProjectile restored = new RecoverableProjectile(helper.getLevel(), player, ammunition);
                restored.readAdditionalSaveData(saved);
                helper.assertTrue(!restored.recover(player), "Reloading a free projectile cannot repair its source ammunition");
                helper.assertValueEqual(ToolState.damage(ammunition), 20, "Rejected pickup retains earlier ammunition wear");
        }

        private static void cleanup(GameTestHelper helper, ServerPlayer player) {
                projectiles(helper, player).forEach(TLToolProjectile::discard);
                helper.getLevel().getServer().getPlayerList().remove(player);
        }

        private static final class RecoverableProjectile extends TLToolProjectile {
                private RecoverableProjectile(Level level, Player player, ItemStack ammunition) {
                        super(level, player, ammunition, ItemStack.EMPTY, 4, true);
                }

                private boolean recover(Player player) {
                        return tryPickup(player);
                }
        }
}
