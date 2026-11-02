package com.nurby.tinkerslegacy.api.trait;

import com.nurby.tinkerslegacy.content.entity.TLToolProjectile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public interface TraitDefinition {
    // Configuration & Validation
    default int color() {
        return 0xFFFFFF;
    }

    default int maxLevel() {
        return Integer.MAX_VALUE;
    }

    default boolean isExcluded(List<TraitInstance> currentTraits) {
        return false;
    }

    default boolean meetsRequirements(List<TraitInstance> currentTraits) {
        return true;
    }

    // Stats
    default int bonusSlots(int level, int amount) {
        return 0;
    }

    default void applyStats(ItemStack stack, int level, int amount, Map<String, Number> stats) {
    }

    default void finishStats(ItemStack stack, int level, int amount, Map<String, Number> stats) {
    }

    // Combat & Usage Hooks
    default int damage(ItemStack stack, int level, int amount, int original, int damageAmount, @Nullable LivingEntity entity) {
        return damageAmount;
    }

    default int heal(ItemStack stack, int level, int amount, int original, int healAmount) {
        return healAmount;
    }

    default float attackDamage(ItemStack stack, int level, int amount, LivingEntity attacker, LivingEntity target, float original, float attackAmount) {
        return attackAmount;
    }

    default void beforeHit(ItemStack stack, int level, int amount, LivingEntity attacker, LivingEntity target) {
    }

    default void afterHit(ItemStack stack, int level, int amount, LivingEntity attacker, LivingEntity target, float damage) {
    }

    default void afterBlockBreak(ItemStack stack, int level, int amount, ServerLevel serverLevel, BlockState state, BlockPos pos, LivingEntity entity) {
    }

    default void inventoryTick(ItemStack stack, int level, int amount, Level worldLevel, Entity entity, boolean selected) {
    }

    default void applyEnchantments(ItemStack stack, int level, int amount, HolderLookup.Provider access) {
    }

    // Projectiles
    default int projectileCount(ItemStack stack, int level, int amount, RandomSource random, int current) {
        return current;
    }

    default float projectileInaccuracy(ItemStack stack, int level, int amount, float inaccuracy) {
        return inaccuracy;
    }

    default void projectileLaunch(TLToolProjectile projectile, int level, int amount) {
    }

    default void projectileHitBlock(TLToolProjectile projectile, int level, int amount) {
    }

    // Events
    default float miningSpeed(ItemStack stack, int level, int amount, Player player, BlockState state, BlockPos pos, float original, float speed) {
        return speed;
    }

    default void blockDrops(ItemStack stack, int level, int amount, BlockDropsEvent event) {
    }

    default void experienceDrop(ItemStack stack, int level, int amount, LivingExperienceDropEvent event) {
    }

    default void attributes(ItemStack stack, int level, int amount, ItemAttributeModifierEvent event) {
    }

    default void hurt(ItemStack stack, int level, int amount, LivingIncomingDamageEvent event) {
    }

    default void shieldBlock(ItemStack stack, int level, int amount, LivingShieldBlockEvent event) {
    }
}