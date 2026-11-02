package com.nurby.tinkerslegacy.mixin;

import com.nurby.tinkerslegacy.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.event.ToolCombatAbilities;
import com.nurby.tinkerslegacy.tools.ScytheDefinition;
import com.nurby.tinkerslegacy.library.tool.ToolState;
import com.nurby.tinkerslegacy.library.tool.traits.ToolTraits;
import com.nurby.tinkerslegacy.library.tool.modifier.ModifierEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerAttackMixin {

        @Unique
        private ItemStack tinkerslegacy$wearStack = ItemStack.EMPTY;
        @Unique
        private float tinkerslegacy$wearDamage;

        // Attack State
        @Inject(method = "attack", at = @At("HEAD"))
        private void tinkerslegacy$beginAttack(Entity target, CallbackInfo callback) {
                tinkerslegacy$clearWear();
        }

        @Inject(method = "attack", at = @At("RETURN"))
        private void tinkerslegacy$finishAttack(Entity target, CallbackInfo callback) {
                tinkerslegacy$clearWear();
        }

        @Unique
        private void tinkerslegacy$clearWear() {
                tinkerslegacy$wearStack = ItemStack.EMPTY;
                tinkerslegacy$wearDamage = 0.0F;
        }

        // Damage
        @ModifyVariable(
                        method = "attack",
                        at = @At(value = "STORE", ordinal = 0),
                        ordinal = 0
        )
        private float tinkerslegacy$modifyBaseDamage(float damage, Entity target) {
                Player player = (Player) (Object) this;
                ItemStack weapon = player.getWeaponItem();
                if (!(target instanceof LivingEntity living) || !(weapon.getItem() instanceof DynamicTool tool)
                                || tool.getDefinition() instanceof com.nurby.tinkerslegacy.tools.ProjectileToolDefinition
                                || ToolState.broken(weapon)) return damage;
                // Vanilla applies cooldown and critical multipliers after this base damage value.
                return ModifierEffects.modifyDamage(weapon, player, living,
                                ToolTraits.attackDamage(weapon, player, living, damage));
        }

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
                Player player = (Player) (Object) this;
                ItemStack weapon = player.getWeaponItem();

                if (weapon.getItem() instanceof DynamicTool tool) {
                        if (tool.getDefinition() instanceof com.nurby.tinkerslegacy.tools.ProjectileToolDefinition) {
                                return target.hurt(source, damage);
                        }
                        if (ToolState.broken(weapon)) return false;
                        damage = ToolCombatAbilities.finishDamage(weapon, damage, ToolCombatAbilities.attackStrength(player));
                        boolean successful = ToolCombatAbilities.hit(player, weapon, target, source, damage);
                        if (successful) {
                                if (tool.getDefinition() instanceof ScytheDefinition) {
                                        // A broken primary hit must stop the remaining area attack immediately.
                                        tool.hurtOnHit(weapon, player, damage);
                                } else {
                                        tinkerslegacy$wearStack = weapon;
                                        tinkerslegacy$wearDamage = damage;
                                }
                        }
                        if (tool.getDefinition() instanceof ScytheDefinition && ToolCombatAbilities.attackStrength(player) > 0.9F) {
                                successful |= ToolCombatAbilities.sweep(player, target, source);
                        }
                        return successful;
                }

                return target.hurt(source, damage);
        }

        @Redirect(
                        method = "attack",
                        at = @At(
                                        value = "INVOKE",
                                        target = "Lnet/minecraft/world/item/ItemStack;postHurtEnemy(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/player/Player;)V"
                        )
        )
        private void tinkerslegacy$applyWear(ItemStack weapon, LivingEntity target, Player player) {
                ItemStack captured = tinkerslegacy$wearStack;
                float damage = tinkerslegacy$wearDamage;
                tinkerslegacy$clearWear();

                if (weapon.getItem() instanceof DynamicTool tool) {
                        // Secondary scythe hits pay their own wear; only a successful primary attack pays here.
                        if (weapon == captured) tool.hurtOnHit(weapon, player, damage);
                } else {
                        weapon.postHurtEnemy(target, player);
                }
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
                Player player = (Player) (Object) this;
                ItemStack weapon = player.getWeaponItem();

                if (weapon.getItem() instanceof DynamicTool tool) {
                        strength *= tool.getDefinition().knockback();
                }

                target.knockback(strength, x, z);
        }
}
