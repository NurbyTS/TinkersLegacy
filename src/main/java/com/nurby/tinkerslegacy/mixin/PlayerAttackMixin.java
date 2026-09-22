package com.nurby.tinkerslegacy.mixin;

import com.nurby.tinkerslegacy.item.dynamic.DynamicTool;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Player.class)
public abstract class PlayerAttackMixin {
    @Redirect(
            method = "attack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
                    ordinal = 0
            )
    )
    private boolean tinkerslegacy$dealDamage(
            Entity target,
            DamageSource source,
            float damage
    ) {
        System.out.println("HOOKED");

        Player player = (Player) (Object) this;

        if (player.getWeaponItem().getItem() instanceof DynamicTool tool) {
            return tool.getDefinition().dealDamage(
                    player,
                    target,
                    source,
                    damage
            );
        }

        return target.hurt(source, damage);
    }

    @Redirect(
            method = "attack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V"
            )
    )
    private void tinkerslegacy$modifyKnockback(
            LivingEntity target,
            double strength,
            double x,
            double z
    ) {
        Player player = (Player)(Object)this;
        ItemStack weapon = player.getWeaponItem();

        if (weapon.getItem() instanceof DynamicTool tool) {
            strength *= tool.getDefinition().knockback();
        }

        target.knockback(strength, x, z);
    }
}