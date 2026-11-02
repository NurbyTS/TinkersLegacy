package com.nurby.tinkerslegacy.api.tool;

import com.nurby.tinkerslegacy.api.tool.traits.ToolTraits;
import com.nurby.tinkerslegacy.content.entity.TLToolProjectile;
import com.nurby.tinkerslegacy.content.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.content.tools.ProjectileToolDefinition;
import com.nurby.tinkerslegacy.content.tools.RangedToolDefinition;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLItems;
import com.nurby.tinkerslegacy.registry.TLMaterials;
import com.nurby.tinkerslegacy.registry.tools.TLTools;
import com.nurby.tinkerslegacy.util.TinkersUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class ToolRangedActions {
        public static final int DURABILITY_PER_AMMO = 10;

        private ToolRangedActions() {
        }

        // Getters
        public static int ammo(ItemStack stack) {
                return ToolState.broken(stack) ? 0 : Math.max(0, stack.getMaxDamage() - ToolState.damage(stack)) / DURABILITY_PER_AMMO;
        }

        public static int maxAmmo(ItemStack stack) {
                return Math.max(0, stack.getMaxDamage()) / DURABILITY_PER_AMMO;
        }

        public static boolean isLoaded(ItemStack stack) {
                return !stack.getOrDefault(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY).isEmpty();
        }

        public static ItemStack findAmmo(RangedToolDefinition definition, Player player) {
                for (InteractionHand hand : InteractionHand.values()) {
                        ItemStack stack = player.getItemInHand(hand);
                        if (matchesAmmo(definition, stack)) return stack;
                }
                for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                        ItemStack stack = player.getInventory().getItem(slot);
                        if (matchesAmmo(definition, stack)) return stack;
                }
                return ItemStack.EMPTY;
        }

        private static boolean matchesAmmo(RangedToolDefinition definition, ItemStack stack) {
                if (stack.isEmpty()) return false;
                if (stack.getItem() instanceof DynamicTool item && item.getDefinition() instanceof ProjectileToolDefinition projectile) {
                        return projectile.kind() == (definition.kind() == RangedToolDefinition.Kind.CROSSBOW
                                        ? ProjectileToolDefinition.Kind.BOLT : ProjectileToolDefinition.Kind.ARROW) && ammo(stack) > 0;
                }
                return definition.kind() != RangedToolDefinition.Kind.CROSSBOW && stack.getItem() instanceof ArrowItem;
        }

        // Launchers
        public static InteractionResultHolder<ItemStack> use(
                        RangedToolDefinition definition, Level level, Player player, InteractionHand hand) {
                ItemStack stack = player.getItemInHand(hand);
                if (ToolState.broken(stack)) {
                        return InteractionResultHolder.fail(stack);
                }
                ItemStack ammunition = findAmmo(definition, player);
                boolean loaded = definition.kind() == RangedToolDefinition.Kind.CROSSBOW && isLoaded(stack);
                if (!loaded) {
                        InteractionResultHolder<ItemStack> event = EventHooks.onArrowNock(stack, level, player, hand, !ammunition.isEmpty());
                        if (event != null) return event;
                }
                if (ammunition.isEmpty() && !player.hasInfiniteMaterials()) {
                        return InteractionResultHolder.fail(stack);
                }
                if (loaded) {
                        int charge = level.isClientSide ? 72000 : EventHooks.onArrowLoose(stack, level, player, 72000, !ammunition.isEmpty());
                        if (charge < 5) return InteractionResultHolder.fail(stack);
                        if (!level.isClientSide && shoot(definition, stack, ammunition, level, player, definition.drawProgress(stack, charge))) {
                                stack.remove(DataComponents.CHARGED_PROJECTILES);
                        }
                } else {
                        player.startUsingItem(hand);
                }
                return InteractionResultHolder.consume(stack);
        }

        public static void tick(RangedToolDefinition definition, ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
                if (definition.kind() != RangedToolDefinition.Kind.CROSSBOW || level.isClientSide
                                || !(entity instanceof Player player) || ToolState.broken(stack) || isLoaded(stack)) return;
                int used = definition.getUseDuration(stack, entity) - timeLeft;
                if (definition.drawProgress(stack, used) < 1) return;
                ItemStack ammunition = findAmmo(definition, player);
                if (!ammunition.isEmpty() || player.hasInfiniteMaterials()) load(definition, stack, ammunition, level, player);
        }

        private static void load(RangedToolDefinition definition, ItemStack stack, ItemStack ammunition, Level level, Player player) {
                if (isLoaded(stack)) return;
                // Keep using the item until release; completing a charge must not immediately fire it.
                if (ammunition.getItem() instanceof DynamicTool) ToolState.initialize(ammunition, true);
                stack.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.of(
                                ammunition.isEmpty() ? creativeAmmo(definition) : ammunition.copyWithCount(1)));
                level.playSound(null, player.blockPosition(), SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.PLAYERS, 1, 1);
        }

        public static void release(RangedToolDefinition definition, ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
                if (level.isClientSide || !(entity instanceof Player player) || ToolState.broken(stack)) {
                        return;
                }
                int used = definition.getUseDuration(stack, entity) - timeLeft;
                ItemStack ammunition = findAmmo(definition, player);
                if (ammunition.isEmpty() && !player.hasInfiniteMaterials()) {
                        return;
                }
                if (definition.kind() != RangedToolDefinition.Kind.CROSSBOW) {
                        used = EventHooks.onArrowLoose(stack, level, player, used, !ammunition.isEmpty());
                        if (used < 5) return;
                }
                float progress = definition.drawProgress(stack, used);
                if (definition.kind() == RangedToolDefinition.Kind.CROSSBOW) {
                        if (progress >= 1) load(definition, stack, ammunition, level, player);
                } else if (used >= 5) {
                        shoot(definition, stack, ammunition, level, player, progress);
                }
        }

        public static ItemStack creativeAmmo(RangedToolDefinition definition) {
                if (definition.kind() != RangedToolDefinition.Kind.CROSSBOW) {
                        return new ItemStack(Items.ARROW);
                }
                DynamicTool item = TLItems.Tools.item(TLTools.Type.BOLT);
                ItemStack result = new ItemStack(item);
                result.set(TLDataComponents.MATERIALS, item.getDefinition().defaultMaterials(TLMaterials.IRON.get()));
                item.calculateStats(result);
                return result;
        }

        private static boolean shoot(RangedToolDefinition definition, ItemStack launcher, ItemStack ammunition,
                        Level level, Player player, float progress) {
                if (ammunition.isEmpty()) {
                        ammunition = creativeAmmo(definition);
                }
                float range = launcher.getOrDefault(TLDataComponents.MODIFIED_STATS, Map.<String, Number>of()).getOrDefault("range", 1).floatValue();
                float draw = Math.clamp(progress, 0, 1);
                float velocity = Math.min(1, (draw * draw + draw * 2) / 3) * draw * definition.projectileSpeed() * range;
                if (velocity <= 0 || !Float.isFinite(velocity)) {
                        return false;
                }
                ToolState.initialize(launcher, true);
                int count = ToolTraits.projectileCount(ammunition, level.random);
                AbstractArrow projectile;
                if (ammunition.getItem() instanceof DynamicTool item && item.getDefinition() instanceof ProjectileToolDefinition) {
                        if (ammo(ammunition) <= 0) {
                                return false;
                        }
                        ToolState.initialize(ammunition, true);
                        float damage = projectileDamage(definition, launcher, ammunition, draw * draw);
                        projectile = new TLToolProjectile(level, player, ammunition, launcher, damage, false);
                        float accuracy = ammunition.getOrDefault(TLDataComponents.MODIFIED_STATS, Map.<String, Number>of()).getOrDefault("accuracy", 1).floatValue();
                        projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0,
                                        velocity, ToolTraits.projectileInaccuracy(ammunition, definition.inaccuracy() + (1 / Math.max(accuracy, 0.01F) - 1) * velocity / 2) + (count > 1 ? 3 : 0));
                        ToolTraits.projectileLaunch((TLToolProjectile) projectile);
                } else if (ammunition.getItem() instanceof ArrowItem arrow) {
                        projectile = arrow.createArrow(level, ammunition.copyWithCount(1), player, launcher);
                        projectile.getPersistentData().putUUID("tinkerslegacy:launcher_id", launcher.get(TLDataComponents.TOOL_ID));
                        projectile.getPersistentData().putLong("tinkerslegacy:launcher_xp",
                                        player.hasInfiniteMaterials() ? 0 : ToolLeveling.launcherXp(launcher));
                        projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, velocity, definition.inaccuracy());
                        projectile.setCritArrow(draw >= 1);
                        if (player.hasInfiniteMaterials()) {
                                projectile.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                        }
                } else {
                        return false;
                }
                if (!level.addFreshEntity(projectile)) {
                        return false;
                }
                if (count > 1 && projectile instanceof TLToolProjectile) {
                        TLToolProjectile extra = new TLToolProjectile(level, player, ammunition, launcher,
                                        projectileDamage(definition, launcher, ammunition, draw * draw), false);
                        float accuracy = ammunition.getOrDefault(TLDataComponents.MODIFIED_STATS, Map.<String, Number>of()).getOrDefault("accuracy", 1).floatValue();
                        extra.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, velocity,
                                        ToolTraits.projectileInaccuracy(ammunition, definition.inaccuracy() + (1 / Math.max(accuracy, 0.01F) - 1) * velocity / 2) + 3);
                        ToolTraits.projectileLaunch(extra);
                        level.addFreshEntity(extra);
                }
                if (!player.hasInfiniteMaterials()) {
                        boolean consumed = consumeAmmo(ammunition, player);
                        if (projectile instanceof TLToolProjectile toolProjectile) toolProjectile.setRecoverable(consumed);
                        launcher.hurtAndBreak(1, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
                }
                level.playSound(null, player.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1,
                                1 / (level.random.nextFloat() * 0.4F + 1.2F) + velocity * 0.5F);
                player.awardStat(Stats.ITEM_USED.get(launcher.getItem()));
                return true;
        }

        public static float projectileDamage(RangedToolDefinition definition, ItemStack launcher, ItemStack ammunition, float power) {
                double bonus = launcher.getOrDefault(TLDataComponents.MODIFIED_STATS, Map.<String, Number>of()).getOrDefault("bonus_damage", 0).doubleValue();
                ToolDefinition projectile = ((DynamicTool) ammunition.getItem()).getDefinition();
                double attack = ammunition.getOrDefault(TLDataComponents.MODIFIED_STATS, Map.<String, Number>of()).getOrDefault("attack_damage", 0).doubleValue();
                double total = (projectile.baseDamage() + attack * projectile.damagePotential()
                                + definition.baseProjectileDamage() * power + bonus) * definition.projectileDamageModifier() * power;
                return (float) Math.max(0, total);
        }

        // Ammunition
        public static boolean consumeAmmo(ItemStack stack) {
                return consumeAmmo(stack, null);
        }

        public static boolean consumeAmmo(ItemStack stack, @Nullable LivingEntity shooter) {
                if (shooter != null && (shooter.level().isClientSide || shooter.hasInfiniteMaterials())) return false;
                if (!(stack.getItem() instanceof DynamicTool item) || !(item.getDefinition() instanceof ProjectileToolDefinition)) {
                        if (stack.isEmpty()) return false;
                        stack.shrink(1);
                        return true;
                }
                int before = ammo(stack);
                if (before <= 0) {
                        return false;
                }
                ToolState.hurt(stack, DURABILITY_PER_AMMO, shooter);
                if (ammo(stack) <= 0) {
                        stack.set(TLDataComponents.BROKEN, true);
                        stack.set(TLDataComponents.BROKEN_DAMAGE, stack.getMaxDamage());
                }
                // A prevented durability event fires normally but cannot create recoverable ammunition.
                return ammo(stack) < before;
        }

        public static boolean restoreAmmo(ItemStack stack) {
                if (!(stack.getItem() instanceof DynamicTool item) || !(item.getDefinition() instanceof ProjectileToolDefinition)
                                || ammo(stack) >= maxAmmo(stack)) {
                        return false;
                }
                int repairs = stack.getOrDefault(TLDataComponents.REPAIR_COUNT, 0);
                ToolState.repair(stack, DURABILITY_PER_AMMO);
                stack.set(TLDataComponents.REPAIR_COUNT, repairs);
                return true;
        }

        public static InteractionResultHolder<ItemStack> throwShuriken(ItemStack stack, Level level, Player player, InteractionHand hand) {
                if (ammo(stack) <= 0 || player.getCooldowns().isOnCooldown(stack.getItem())) {
                        return InteractionResultHolder.fail(stack);
                }
                if (!level.isClientSide) {
                        ToolState.initialize(stack, true);
                        TLToolProjectile projectile = new TLToolProjectile(level, player, stack, ItemStack.EMPTY,
                                        (float) TinkersUtils.getActualDamage(stack), false);
                        projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, 2.1F, 0);
                        ToolTraits.projectileLaunch(projectile);
                        if (!level.addFreshEntity(projectile)) {
                                return InteractionResultHolder.fail(stack);
                        }
                        if (!player.hasInfiniteMaterials()) {
                                projectile.setRecoverable(consumeAmmo(stack, player));
                        }
                        player.getCooldowns().addCooldown(stack.getItem(), 4);
                        player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
}
