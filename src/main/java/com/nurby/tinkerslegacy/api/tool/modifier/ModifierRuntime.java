package com.nurby.tinkerslegacy.api.tool.modifier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface ModifierRuntime {
        default int durability(ItemStack stack, int points, int amount, LivingEntity entity) { return amount; }
        default float damage(ItemStack stack, int points, LivingEntity attacker, LivingEntity target, float amount) { return amount; }
        default void afterHit(ItemStack stack, int points, LivingEntity attacker, LivingEntity target, float damage) { }
        default void inventoryTick(ItemStack stack, int points, Level level, Entity entity, boolean selected) { }
        default void enchantments(ItemStack stack, int points, HolderLookup.Provider registries) { }
        default void afterBlockBreak(ItemStack stack, int points, ServerLevel level, BlockPos pos) { }
}
