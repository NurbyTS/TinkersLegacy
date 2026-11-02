package com.nurby.tinkerslegacy.test;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.entity.TLToolProjectile;
import com.nurby.tinkerslegacy.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.library.tool.ToolRangedActions;
import com.nurby.tinkerslegacy.library.tool.ToolState;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLItems;
import com.nurby.tinkerslegacy.registry.TLMaterials;
import com.nurby.tinkerslegacy.registry.TLTools;
import com.nurby.tinkerslegacy.tools.RangedToolDefinition;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ArrowLooseEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.function.Consumer;

@GameTestHolder(TinkersLegacy.MODID)
@PrefixGameTestTemplate(false)
public final class VanillaRangedGameTests {
        // Launchers
        @GameTest(template = "empty")
        public static void classicCrossbowCompletesWhileHeldAndFiresAfterRelease(GameTestHelper helper) {
                ServerPlayer player = player(helper);
                ItemStack crossbow = tool(TLTools.Type.CROSSBOW);
                ItemStack bolts = tool(TLTools.Type.BOLT);
                player.setItemInHand(InteractionHand.MAIN_HAND, crossbow);
                player.getInventory().setItem(1, bolts);
                RangedToolDefinition definition = (RangedToolDefinition) ((DynamicTool) crossbow.getItem()).getDefinition();
                int charge = (int) Math.ceil(definition.drawTime() / crossbow.get(TLDataComponents.MODIFIED_STATS).get("draw_speed").doubleValue());
                int original = ToolRangedActions.ammo(bolts);
                crossbow.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                helper.assertTrue(player.isUsingItem(), "A crossbow starts and retains the held-use session");
                crossbow.onUseTick(helper.getLevel(), player, 72000 - charge - 1);
                helper.assertTrue(ToolRangedActions.isLoaded(crossbow), "Completing the draw loads while the button is held");
                helper.assertTrue(player.isUsingItem(), "Charging does not release and immediately auto-fire");
                helper.assertValueEqual(ToolRangedActions.ammo(bolts), original, "Classic loading consumes no ammunition");
                crossbow.releaseUsing(helper.getLevel(), player, 72000 - charge - 1);
                player.stopUsingItem();
                crossbow.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                helper.assertValueEqual(projectiles(helper, player).size(), 1, "The next click fires one projectile");
                helper.assertTrue(!ToolRangedActions.isLoaded(crossbow), "Firing clears charge");
                helper.assertValueEqual(ToolRangedActions.ammo(bolts), original - 1, "The shot costs one bolt");
                finish(helper, player);
        }

        @GameTest(template = "empty")
        public static void canceledClassicCrossbowShotRetainsChargeAndAmmo(GameTestHelper helper) {
                ServerPlayer player = player(helper);
                ItemStack crossbow = tool(TLTools.Type.CROSSBOW);
                ItemStack bolts = tool(TLTools.Type.BOLT);
                player.setItemInHand(InteractionHand.MAIN_HAND, crossbow);
                player.getInventory().setItem(1, bolts);
                crossbow.releaseUsing(helper.getLevel(), player, 0);
                Consumer<ArrowLooseEvent> listener = event -> {
                        if (event.getEntity() == player) event.setCanceled(true);
                };
                NeoForge.EVENT_BUS.addListener(listener);
                try {
                        crossbow.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                        helper.assertTrue(ToolRangedActions.isLoaded(crossbow), "A canceled shot retains charge");
                        helper.assertValueEqual(ToolRangedActions.ammo(bolts), ToolRangedActions.maxAmmo(bolts), "Canceled firing retains ammo");
                        helper.assertValueEqual(ToolState.damage(crossbow), 0, "Canceled firing retains durability");
                        helper.assertTrue(projectiles(helper, player).isEmpty(), "Canceled firing spawns no projectile");
                } finally {
                        NeoForge.EVENT_BUS.unregister(listener);
                }
                finish(helper, player);
        }

        // Vanilla Weapons
        @GameTest(template = "empty")
        public static void vanillaBowFiresBothCustomAmmoKindsAndPreservesComponents(GameTestHelper helper) {
                ServerPlayer player = player(helper);
                for (TLTools.Type type : List.of(TLTools.Type.ARROW, TLTools.Type.BOLT)) {
                        ItemStack bow = new ItemStack(Items.BOW);
                        ItemStack ammunition = tool(type);
                        player.setItemInHand(InteractionHand.MAIN_HAND, bow);
                        player.getInventory().setItem(1, ammunition);
                        int before = ToolRangedActions.ammo(ammunition);
                        helper.assertTrue(player.getProjectile(bow) == ammunition, "The vanilla predicate finds custom ammunition");
                        bow.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                        bow.releaseUsing(helper.getLevel(), player, 72000 - 20);
                        player.stopUsingItem();
                        List<TLToolProjectile> fired = projectiles(helper, player);
                        helper.assertValueEqual(fired.size(), 1, "The vanilla bow creates a material projectile");
                        helper.assertValueEqual(fired.getFirst().getItem().get(TLDataComponents.MATERIALS), ammunition.get(TLDataComponents.MATERIALS),
                                        "Fired ammunition preserves material layers");
                        helper.assertValueEqual(fired.getFirst().getItem().get(TLDataComponents.TOOL_ID), ammunition.get(TLDataComponents.TOOL_ID),
                                        "Fired ammunition retains identity for recovery and XP");
                        helper.assertValueEqual(ToolRangedActions.ammo(ammunition), before - 1, "A shot spends one ammunition unit");
                        helper.assertValueEqual(ammunition.getCount(), 1, "Vanilla draw does not split away the ammo tool");
                        helper.assertValueEqual(bow.getDamageValue(), 1, "Vanilla launcher wears once");
                        fired.forEach(TLToolProjectile::discard);
                }
                finish(helper, player);
        }

        @GameTest(template = "empty")
        public static void vanillaCrossbowLoadsLastShotAndRecoveryRestoresIt(GameTestHelper helper) {
                ServerPlayer player = player(helper);
                for (TLTools.Type type : List.of(TLTools.Type.ARROW, TLTools.Type.BOLT)) {
                        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
                        ItemStack ammunition = tool(type);
                        while (ToolRangedActions.ammo(ammunition) > 1) ToolRangedActions.consumeAmmo(ammunition);
                        player.setItemInHand(InteractionHand.MAIN_HAND, crossbow);
                        player.getInventory().setItem(1, ammunition);
                        crossbow.releaseUsing(helper.getLevel(), player, 0);
                        helper.assertTrue(ToolState.broken(ammunition), "Vanilla loading spends the final ammo unit without removing the item");
                        helper.assertValueEqual(ammunition.getCount(), 1, "The empty ammo tool is retained");
                        var loaded = crossbow.getOrDefault(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);
                        helper.assertValueEqual(loaded.getItems().size(), 1, "The paid final shot is stored in the vanilla crossbow");
                        crossbow.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                        TLToolProjectile fired = projectiles(helper, player).getFirst();
                        helper.assertTrue(!CrossbowItem.isCharged(crossbow), "Vanilla firing consumes the loaded snapshot");
                        helper.assertValueEqual(ToolRangedActions.ammo(ammunition), 0, "Vanilla firing does not consume ammunition a second time");
                        CompoundTag saved = new CompoundTag();
                        fired.addAdditionalSaveData(saved);
                        RecoverableProjectile restored = new RecoverableProjectile(helper.getLevel(), player, loaded.getItems().getFirst());
                        restored.readAdditionalSaveData(saved);
                        helper.assertTrue(restored.recover(player), "The saved projectile restores its matching empty ammo tool");
                        helper.assertValueEqual(ToolRangedActions.ammo(ammunition), 1, "Recovery restores exactly one shot");
                        helper.assertTrue(!restored.recover(player), "Recovery cannot be repeated");
                        fired.discard();
                }
                finish(helper, player);
        }

        @GameTest(template = "empty")
        public static void multishotCostsOneAmmoAndOnlyOneProjectileIsRecoverable(GameTestHelper helper) {
                ServerPlayer player = player(helper);
                ItemStack crossbow = new ItemStack(Items.CROSSBOW);
                crossbow.enchant(helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.MULTISHOT), 1);
                ItemStack bolts = tool(TLTools.Type.BOLT);
                int before = ToolRangedActions.ammo(bolts);
                player.setItemInHand(InteractionHand.MAIN_HAND, crossbow);
                player.getInventory().setItem(1, bolts);
                crossbow.releaseUsing(helper.getLevel(), player, 0);
                crossbow.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                List<TLToolProjectile> fired = projectiles(helper, player);
                helper.assertValueEqual(fired.size(), 3, "Vanilla Multishot creates three custom projectiles");
                helper.assertValueEqual(ToolRangedActions.ammo(bolts), before - 1, "Multishot charges only one ammunition unit");
                int recoverable = 0;
                for (TLToolProjectile projectile : fired) {
                        helper.assertValueEqual(projectile.getItem().get(TLDataComponents.TOOL_ID), bolts.get(TLDataComponents.TOOL_ID),
                                        "Every Multishot projectile credits its real ammunition tool");
                        CompoundTag saved = new CompoundTag();
                        projectile.addAdditionalSaveData(saved);
                        if (saved.getBoolean("Recoverable")) recoverable++;
                }
                helper.assertValueEqual(recoverable, 1, "Only the paid projectile can restore ammunition");
                finish(helper, player);
        }

        @GameTest(template = "empty")
        public static void splittingCombinesWithMultishotWithoutSpendingOrDuplicatingAmmo(GameTestHelper helper) {
                ServerPlayer player = player(helper);
                ItemStack crossbow = new ItemStack(Items.CROSSBOW);
                crossbow.enchant(helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.MULTISHOT), 1);
                ItemStack arrows = tool(TLTools.Type.ARROW);
                arrows.set(TLDataComponents.MATERIALS, arrows.get(TLDataComponents.MATERIALS).stream().map(layer ->
                                layer.statType().equals(com.nurby.tinkerslegacy.registry.ToolStatTypes.ARROW_SHAFT.getId())
                                                ? new com.nurby.tinkerslegacy.library.material.layer.MaterialLayer(layer.statType(), TLMaterials.BONE.getId()) : layer).toList());
                ((DynamicTool) arrows.getItem()).calculateStats(arrows);
                int before = ToolRangedActions.ammo(arrows);
                player.setItemInHand(InteractionHand.MAIN_HAND, crossbow);
                player.getInventory().setItem(1, arrows);
                player.getRandom().setSeed(4096);
                crossbow.releaseUsing(helper.getLevel(), player, 0);
                var loaded = crossbow.getOrDefault(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY).getItems();
                helper.assertValueEqual(loaded.size(), 4, "A successful Splitting roll adds a shot to vanilla Multishot");
                helper.assertValueEqual(ToolRangedActions.ammo(arrows), before - 1, "Combining effects still spends one shot");
                helper.assertValueEqual(loaded.stream().filter(stack -> !stack.has(DataComponents.INTANGIBLE_PROJECTILE)).count(), 1L,
                                "Only the paid shot is recoverable");
                for (ItemStack snapshot : loaded) {
                        helper.assertValueEqual(snapshot.get(TLDataComponents.TOOL_ID), arrows.get(TLDataComponents.TOOL_ID),
                                        "Split and Multishot snapshots share the original ammunition identity");
                }
                finish(helper, player);
        }

        @GameTest(template = "empty")
        public static void creativeAmmoIsFreeAndInfinityDoesNotDuplicateCustomAmmo(GameTestHelper helper) {
                ServerPlayer player = player(helper);
                ItemStack bow = new ItemStack(Items.BOW);
                bow.enchant(helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.INFINITY), 1);
                ItemStack arrows = tool(TLTools.Type.ARROW);
                player.setItemInHand(InteractionHand.MAIN_HAND, bow);
                player.getInventory().setItem(1, arrows);
                int before = ToolRangedActions.ammo(arrows);
                bow.releaseUsing(helper.getLevel(), player, 72000 - 20);
                helper.assertValueEqual(ToolRangedActions.ammo(arrows), before - 1, "Vanilla Infinity is restricted to ordinary arrows");
                projectiles(helper, player).forEach(TLToolProjectile::discard);
                player.setGameMode(GameType.CREATIVE);
                bow.releaseUsing(helper.getLevel(), player, 72000 - 20);
                helper.assertValueEqual(ToolRangedActions.ammo(arrows), before - 1, "Creative custom ammunition is free");
                CompoundTag saved = new CompoundTag();
                projectiles(helper, player).getFirst().addAdditionalSaveData(saved);
                helper.assertTrue(!saved.getBoolean("Recoverable"), "A free creative projectile cannot restore survival ammunition");
                finish(helper, player);
        }

        @GameTest(template = "empty")
        public static void emptyAmmoAndShurikenDoNotHideUsableVanillaArrows(GameTestHelper helper) {
                ServerPlayer player = player(helper);
                ItemStack bow = new ItemStack(Items.BOW);
                ItemStack arrows = tool(TLTools.Type.ARROW);
                while (ToolRangedActions.consumeAmmo(arrows)) {
                }
                player.setItemInHand(InteractionHand.MAIN_HAND, bow);
                player.getInventory().setItem(1, arrows);
                player.getInventory().setItem(2, tool(TLTools.Type.SHURIKEN));
                helper.assertTrue(player.getProjectile(bow).isEmpty(), "Empty ammunition and shuriken are not valid bow projectiles");
                ItemStack vanilla = new ItemStack(Items.ARROW);
                player.getInventory().setItem(3, vanilla);
                helper.assertTrue(player.getProjectile(bow) == vanilla, "Existing vanilla ammunition selection remains intact");
                finish(helper, player);
        }

        @GameTest(template = "empty")
        public static void vanillaLaunchRetainsMaterialMotionWithoutReducingItsDamage(GameTestHelper helper) {
                ServerPlayer player = player(helper);
                ItemStack bow = new ItemStack(Items.BOW);
                ItemStack arrows = tool(TLTools.Type.ARROW);
                arrows.set(TLDataComponents.MATERIALS, arrows.get(TLDataComponents.MATERIALS).stream().map(layer ->
                                layer.statType().equals(com.nurby.tinkerslegacy.registry.ToolStatTypes.ARROW_SHAFT.getId())
                                                ? new com.nurby.tinkerslegacy.library.material.layer.MaterialLayer(layer.statType(), TLMaterials.ENDROD.getId()) : layer).toList());
                ((DynamicTool) arrows.getItem()).calculateStats(arrows);
                player.setItemInHand(InteractionHand.MAIN_HAND, bow);
                player.getInventory().setItem(1, arrows);
                bow.releaseUsing(helper.getLevel(), player, 72000 - 20);
                TLToolProjectile projectile = projectiles(helper, player).getFirst();
                helper.assertTrue(projectile.getDeltaMovement().length() < 0.5, "Endspeed changes the launched movement after vanilla assigns velocity");
                CompoundTag saved = new CompoundTag();
                projectile.addAdditionalSaveData(saved);
                helper.assertValueEqual(saved.getFloat("VanillaLaunchVelocity"), 3.0F,
                                "Damage retains full bow draw power before the trait slows each accelerated collision step");
                finish(helper, player);
        }

        // Projectiles
        @GameTest(template = "empty")
        public static void embeddedShurikenStopsItsAnimationAndRetainsItAcrossReload(GameTestHelper helper) {
                ServerPlayer player = player(helper);
                ItemStack shuriken = tool(TLTools.Type.SHURIKEN);
                RecoverableProjectile projectile = new RecoverableProjectile(helper.getLevel(), player, shuriken);
                projectile.tick();
                helper.assertValueEqual(projectile.flightTicks(), 1, "An airborne projectile advances its animation");
                BlockPos block = helper.absolutePos(new BlockPos(1, 1, 1));
                helper.getLevel().setBlockAndUpdate(block, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                projectile.moveTo(block.getX() + 0.5, block.getY() + 0.5, block.getZ() + 0.5);
                projectile.embed(block);
                helper.assertTrue(projectile.isEmbedded(), "The grounded state is synchronized immediately on impact");
                int frozen = projectile.flightTicks();
                projectile.tick();
                projectile.tick();
                helper.assertValueEqual(projectile.flightTicks(), frozen, "A grounded shuriken does not keep spinning");
                CompoundTag saved = new CompoundTag();
                projectile.addAdditionalSaveData(saved);
                RecoverableProjectile restored = new RecoverableProjectile(helper.getLevel(), player, shuriken);
                restored.readAdditionalSaveData(saved);
                helper.assertTrue(restored.isEmbedded(), "A loaded grounded projectile remains grounded for rendering");
                helper.assertValueEqual(restored.flightTicks(), frozen, "Reload preserves the resting spin angle");
                finish(helper, player);
        }

        @GameTest(template = "empty")
        public static void flamingProjectileCooksLethalDropsAndRestoresFireOnRejectedHits(GameTestHelper helper) {
                ServerPlayer player = player(helper);
                ItemStack arrows = tool(TLTools.Type.ARROW);
                var pig = helper.spawn(net.minecraft.world.entity.EntityType.PIG, new BlockPos(1, 2, 1));
                pig.setHealth(1);
                RecoverableProjectile lethal = new RecoverableProjectile(helper.getLevel(), player, arrows);
                lethal.igniteForSeconds(30);
                lethal.hit(pig);
                helper.assertTrue(!pig.isAlive(), "The test projectile kills its target");
                var drops = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, pig.getBoundingBox().inflate(2));
                helper.assertTrue(drops.stream().anyMatch(drop -> drop.getItem().is(Items.COOKED_PORKCHOP)),
                                "Flame is applied before the lethal hit so death loot is cooked");
                helper.assertTrue(drops.stream().noneMatch(drop -> drop.getItem().is(Items.PORKCHOP)), "A lethal flaming hit does not drop raw pork");
                var protectedPig = helper.spawn(net.minecraft.world.entity.EntityType.PIG, new BlockPos(2, 2, 1));
                protectedPig.setInvulnerable(true);
                protectedPig.setRemainingFireTicks(40);
                RecoverableProjectile rejected = new RecoverableProjectile(helper.getLevel(), player, arrows);
                rejected.igniteForSeconds(30);
                rejected.hit(protectedPig);
                helper.assertValueEqual(protectedPig.getRemainingFireTicks(), 40, "A rejected hit restores the original fire duration");
                protectedPig.discard();
                drops.forEach(net.minecraft.world.entity.item.ItemEntity::discard);
                finish(helper, player);
        }

        // Helpers
        private static ItemStack tool(TLTools.Type type) {
                DynamicTool item = TLItems.Tools.item(type);
                ItemStack stack = new ItemStack(item);
                stack.set(TLDataComponents.MATERIALS, item.getDefinition().defaultMaterials(TLMaterials.IRON.get()));
                item.calculateStats(stack);
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

        private static void finish(GameTestHelper helper, ServerPlayer player) {
                projectiles(helper, player).forEach(TLToolProjectile::discard);
                helper.getLevel().getServer().getPlayerList().remove(player);
                helper.succeed();
        }

        private static final class RecoverableProjectile extends TLToolProjectile {
                private RecoverableProjectile(Level level, Player player, ItemStack ammunition) {
                        super(level, player, ammunition, ItemStack.EMPTY, 4, true);
                }

                private boolean recover(Player player) {
                        return tryPickup(player);
                }

                private void hit(net.minecraft.world.entity.Entity target) {
                        onHitEntity(new net.minecraft.world.phys.EntityHitResult(target));
                }

                private void embed(BlockPos position) {
                        onHitBlock(new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(position),
                                        net.minecraft.core.Direction.NORTH, position, false));
                }
        }
}
