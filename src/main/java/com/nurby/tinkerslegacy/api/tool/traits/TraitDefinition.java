package com.nurby.tinkerslegacy.api.tool.traits;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.Map;

public interface TraitDefinition {
        default int color() { return 0xFFFFFF; }
        default int bonusSlots() { return 0; }
        default void applyStats(ItemStack stack, Map<String, Number> stats) { }
        default int damage(ItemStack stack, int original, int amount, @Nullable LivingEntity entity) { return amount; }
        default int heal(ItemStack stack, int original, int amount) { return amount; }
        default float attackDamage(ItemStack stack, LivingEntity attacker, LivingEntity target, float original, float amount) { return amount; }
        default void beforeHit(ItemStack stack, LivingEntity attacker, LivingEntity target) { }
        default void afterHit(ItemStack stack, LivingEntity attacker, LivingEntity target, float damage) { }
        default void afterBlockBreak(ItemStack stack, ServerLevel level, BlockState state, BlockPos pos, LivingEntity entity) { }
        default void inventoryTick(ItemStack stack, Level level, Entity entity, boolean selected) { }
}
