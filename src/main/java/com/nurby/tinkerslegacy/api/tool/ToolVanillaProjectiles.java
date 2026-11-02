package com.nurby.tinkerslegacy.api.tool;

import com.nurby.tinkerslegacy.api.tool.traits.ToolTraits;
import com.nurby.tinkerslegacy.content.entity.TLToolProjectile;
import com.nurby.tinkerslegacy.content.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.content.tools.ProjectileToolDefinition;
import com.nurby.tinkerslegacy.util.TinkersUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public final class ToolVanillaProjectiles {
        private ToolVanillaProjectiles() {
        }

        // Getters
        public static boolean weapon(ItemStack stack) {
                return stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem;
        }

        public static boolean ammunition(ItemStack stack) {
                return stack.getItem() instanceof DynamicTool tool
                                && tool.getDefinition() instanceof ProjectileToolDefinition definition
                                && definition.kind() != ProjectileToolDefinition.Kind.SHURIKEN;
        }

        public static boolean available(ItemStack stack) {
                return ammunition(stack) && ToolRangedActions.ammo(stack) > 0;
        }

        // Ammunition
        public static ItemStack use(ItemStack weapon, ItemStack ammunition, LivingEntity shooter, boolean extra) {
                if (!available(ammunition)) return ItemStack.EMPTY;
                int cost = !extra && !shooter.hasInfiniteMaterials() && shooter.level() instanceof ServerLevel level
                                ? EnchantmentHelper.processAmmoUse(level, weapon, ammunition, 1) : 0;
                if (cost > ToolRangedActions.ammo(ammunition)) return ItemStack.EMPTY;
                if (!shooter.level().isClientSide) ToolState.initialize(ammunition, true);
                ItemStack projectile = ammunition.copyWithCount(1);
                boolean consumed = false;
                for (int shot = 0; shot < cost; shot++) consumed |= ToolRangedActions.consumeAmmo(ammunition, shooter);
                if (consumed) {
                        projectile.remove(DataComponents.INTANGIBLE_PROJECTILE);
                } else {
                        // Free shots, including Reinforced saves, cannot restore paid ammunition.
                        projectile.set(DataComponents.INTANGIBLE_PROJECTILE, Unit.INSTANCE);
                }
                return projectile;
        }

        public static List<ItemStack> split(List<ItemStack> projectiles, LivingEntity shooter) {
                if (shooter.level().isClientSide || projectiles.isEmpty()) return projectiles;
                ItemStack ammunition = projectiles.getFirst();
                if (!ammunition(ammunition) || ToolTraits.projectileCount(ammunition, shooter.getRandom()) < 2) return projectiles;
                List<ItemStack> result = new ArrayList<>(projectiles);
                ItemStack extra = ammunition.copyWithCount(1);
                extra.set(DataComponents.INTANGIBLE_PROJECTILE, Unit.INSTANCE);
                result.add(extra);
                return result;
        }

        public static TLToolProjectile create(Level level, LivingEntity shooter, ItemStack weapon, ItemStack ammunition, boolean critical) {
                boolean recoverable = !shooter.hasInfiniteMaterials() && !ammunition.has(DataComponents.INTANGIBLE_PROJECTILE);
                TLToolProjectile projectile = new TLToolProjectile(level, shooter, ammunition, weapon,
                                (float) TinkersUtils.getActualDamage(ammunition), recoverable);
                projectile.setCritArrow(critical);
                return projectile;
        }
}
