package com.nurby.tinkerslegacy.mixin;

import com.nurby.tinkerslegacy.library.tool.ToolVanillaProjectiles;
import com.nurby.tinkerslegacy.library.tool.ToolState;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Predicate;

@Mixin(ProjectileWeaponItem.class)
public abstract class ProjectileWeaponItemMixin {
        // Selection
        @Inject(method = {"getAllSupportedProjectiles(Lnet/minecraft/world/item/ItemStack;)Ljava/util/function/Predicate;",
                        "getSupportedHeldProjectiles(Lnet/minecraft/world/item/ItemStack;)Ljava/util/function/Predicate;"},
                        at = @At("RETURN"), cancellable = true)
        private void tinkerslegacy$ammunition(ItemStack weapon, CallbackInfoReturnable<Predicate<ItemStack>> callback) {
                if (ToolVanillaProjectiles.weapon(weapon)) {
                        callback.setReturnValue(callback.getReturnValue().or(ToolVanillaProjectiles::available));
                }
        }

        // Shooting
        @Inject(method = "draw", at = @At("HEAD"))
        private static void tinkerslegacy$identifyAmmunition(ItemStack weapon, ItemStack ammunition, LivingEntity shooter,
                        CallbackInfoReturnable<List<ItemStack>> callback) {
                // Vanilla copies ammunition before Multishot; every copied shot must retain its source identity.
                if (!shooter.level().isClientSide && ToolVanillaProjectiles.weapon(weapon) && ToolVanillaProjectiles.available(ammunition)) {
                        ToolState.initialize(ammunition, true);
                }
        }

        @Inject(method = "draw", at = @At("RETURN"), cancellable = true)
        private static void tinkerslegacy$splitAmmunition(ItemStack weapon, ItemStack ammunition, LivingEntity shooter,
                        CallbackInfoReturnable<List<ItemStack>> callback) {
                if (ToolVanillaProjectiles.weapon(weapon)) callback.setReturnValue(ToolVanillaProjectiles.split(callback.getReturnValue(), shooter));
        }

        @Inject(method = "useAmmo", at = @At("HEAD"), cancellable = true)
        private static void tinkerslegacy$useAmmunition(ItemStack weapon, ItemStack ammunition, LivingEntity shooter,
                        boolean extra, CallbackInfoReturnable<ItemStack> callback) {
                if (ToolVanillaProjectiles.weapon(weapon) && ToolVanillaProjectiles.ammunition(ammunition)) {
                        callback.setReturnValue(ToolVanillaProjectiles.use(weapon, ammunition, shooter, extra));
                }
        }

        @Inject(method = "createProjectile", at = @At("HEAD"), cancellable = true)
        private void tinkerslegacy$createProjectile(Level level, LivingEntity shooter, ItemStack weapon, ItemStack ammunition,
                        boolean critical, CallbackInfoReturnable<Projectile> callback) {
                if (ToolVanillaProjectiles.weapon(weapon) && ToolVanillaProjectiles.ammunition(ammunition)) {
                        callback.setReturnValue(ToolVanillaProjectiles.create(level, shooter, weapon, ammunition, critical));
                }
        }
}
