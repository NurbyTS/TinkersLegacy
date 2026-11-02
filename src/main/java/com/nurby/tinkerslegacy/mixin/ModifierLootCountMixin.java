package com.nurby.tinkerslegacy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.nurby.tinkerslegacy.library.tool.modifier.ModifierEffects;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.EnchantedCountIncreaseFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EnchantedCountIncreaseFunction.class)
public abstract class ModifierLootCountMixin {
        @WrapOperation(method = "run", at = @At(value = "INVOKE",
                        target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getEnchantmentLevel(Lnet/minecraft/core/Holder;Lnet/minecraft/world/entity/LivingEntity;)I"))
        private int tinkerslegacy$projectileLuck(Holder<Enchantment> enchantment, LivingEntity entity,
                        Operation<Integer> original, @Local(argsOnly = true) LootContext context) {
                return ModifierEffects.looting(context, enchantment, original.call(enchantment, entity));
        }
}
