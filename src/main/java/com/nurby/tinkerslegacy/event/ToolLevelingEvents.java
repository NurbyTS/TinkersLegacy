package com.nurby.tinkerslegacy.event;

import com.nurby.tinkerslegacy.TLConfig;
import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.entity.TLToolProjectile;
import com.nurby.tinkerslegacy.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.library.tool.ToolLeveling;
import com.nurby.tinkerslegacy.library.tool.ToolState;
import com.nurby.tinkerslegacy.registry.TLAttachments;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.tools.ProjectileToolDefinition;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = TinkersLegacy.MODID)
public final class ToolLevelingEvents {

        private static final Map<LivingEntity, ArrayDeque<DamageSnapshot>> PENDING = new WeakHashMap<>();
        private static final List<Runnable> DEFENSE = new ArrayList<>();
        private static final Map<LivingEntity, LivingDeathEvent> DEATHS = new HashMap<>();

        private ToolLevelingEvents() {
        }

        // Broken Tools

        @SubscribeEvent
        public static void onAttack(AttackEntityEvent event) {
                ItemStack stack = event.getEntity().getMainHandItem();
                if (stack.getItem() instanceof DynamicTool tool && !(tool.getDefinition() instanceof ProjectileToolDefinition)
                                && ToolState.broken(stack)) event.setCanceled(true);
        }

        @SubscribeEvent
        public static void onBreak(BlockEvent.BreakEvent event) {
                ItemStack stack = event.getPlayer().getMainHandItem();
                if (stack.getItem() instanceof DynamicTool tool && !(tool.getDefinition() instanceof ProjectileToolDefinition)
                                && ToolState.broken(stack)) event.setCanceled(true);
        }

        // Combat Credit

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void beforeDamage(LivingDamageEvent.Pre event) {
                if (event.getEntity().level().isClientSide) return;
                List<Contribution> contributions = List.of();
                if (event.getSource().getEntity() instanceof ServerPlayer player
                                && event.getSource().getDirectEntity() == player && !player.hasInfiniteMaterials()) {
                        ItemStack stack = player.getMainHandItem();
                        if (stack.getItem() instanceof DynamicTool tool && !(tool.getDefinition() instanceof ProjectileToolDefinition)
                                        && !ToolState.broken(stack)) {
                                ToolState.initialize(stack, true);
                                contributions = List.of(new Contribution(player.getUUID(), stack.get(TLDataComponents.TOOL_ID)));
                        }
                }
                if (event.getSource().getEntity() instanceof ServerPlayer player && !player.hasInfiniteMaterials()
                                && event.getSource().getDirectEntity() instanceof TLToolProjectile projectile && projectile.canEarnXp()) {
                        contributions = projectile.ammunitionIdentity() == null ? List.of()
                                        : List.of(new Contribution(player.getUUID(), projectile.ammunitionIdentity()));
                }
                if (!TLConfig.ENABLE_LEVELING.getAsBoolean()) contributions = List.of();
                PENDING.computeIfAbsent(event.getEntity(), entity -> new ArrayDeque<>())
                                .push(new DamageSnapshot(event.getEntity().getHealth(), contributions));
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void afterDamage(LivingDamageEvent.Post event) {
                if (event.getEntity().level().isClientSide) return;
                ArrayDeque<DamageSnapshot> pending = PENDING.get(event.getEntity());
                if (pending == null || pending.isEmpty()) return;
                DamageSnapshot snapshot = pending.pop();
                if (pending.isEmpty()) PENDING.remove(event.getEntity());
                if (event.getSource().is(DamageTypeTags.IS_PROJECTILE)
                                && event.getSource().getDirectEntity() instanceof AbstractArrow arrow && !(arrow instanceof TLToolProjectile)
                                && event.getSource().getEntity() instanceof ServerPlayer player
                                && arrow.getPersistentData().hasUUID("tinkerslegacy:launcher_id")) {
                        ToolLeveling.launcherHit(player, arrow.getPersistentData().getUUID("tinkerslegacy:launcher_id"),
                                        arrow.getPersistentData().getLong("tinkerslegacy:launcher_xp"), arrow.getDeltaMovement().length());
                }
                if (snapshot.contributions().isEmpty()) return;
                // Health is clamped to zero; comparing it avoids credit for overkill and absorption.
                double actual = Math.min(Math.max(0, event.getNewDamage()),
                                Math.max(0, snapshot.health() - event.getEntity().getHealth()));
                if (actual > 0 && Double.isFinite(actual)) {
                        for (Contribution contribution : snapshot.contributions()) {
                                var entity = event.getEntity();
                                entity.setData(TLAttachments.COMBAT_CREDIT, entity.getData(TLAttachments.COMBAT_CREDIT)
                                                .add(contribution.player(), contribution.tool(), actual));
                        }
                }
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void onDeath(LivingDeathEvent event) {
                if (event.getEntity().level().isClientSide) return;
                DEATHS.put(event.getEntity(), event);
        }

        @SubscribeEvent
        public static void onServerTick(ServerTickEvent.Post event) {
                List<Runnable> defense = List.copyOf(DEFENSE);
                DEFENSE.clear();
                defense.forEach(Runnable::run);
                for (var death : Map.copyOf(DEATHS).entrySet()) {
                        DEATHS.remove(death.getKey());
                        LivingEntity entity = death.getKey();
                        // A later death listener can cancel or revive the target after our callback.
                        if (death.getValue().isCanceled() || !entity.isDeadOrDying()) continue;
                        payContributions(entity);
                }
        }

        private static void payContributions(LivingEntity entity) {
                var contributions = entity.removeData(TLAttachments.COMBAT_CREDIT);
                PENDING.remove(entity);
                if (contributions == null) return;
                for (var entry : contributions.entries()) {
                        ServerPlayer player = entity.getServer().getPlayerList().getPlayer(entry.player());
                        if (player == null) continue;
                        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                                ItemStack stack = player.getInventory().getItem(slot);
                                if (stack.getItem() instanceof DynamicTool
                                                && entry.tool().equals(stack.get(TLDataComponents.TOOL_ID))) {
                                        ToolLeveling.award(stack, player, Math.round(entry.damage()));
                                        break;
                                }
                        }
                }
        }

        // Defensive Rewards
        @SubscribeEvent
        public static void shield(LivingShieldBlockEvent event) {
                if (!(event.getEntity() instanceof ServerPlayer player) || !ToolCombatAbilities.blocking(player)
                                || !event.getOriginalBlock()) return;
                var source = event.getDamageSource();
                if (source.is(DamageTypeTags.BYPASSES_SHIELD) || source.is(DamageTypeTags.IS_EXPLOSION)
                                || source.is(DamageTypeTags.IS_PROJECTILE) || source.is(DamageTypes.MAGIC)
                                || source.is(DamageTypes.INDIRECT_MAGIC)) return;
                ItemStack stack = player.getUseItem();
                int reward = Math.round(event.getOriginalBlockedDamage());
                // Read final cancellation and retained shield absorption; later armor can reduce new damage to zero.
                DEFENSE.add(() -> {
                        if (!event.isCanceled() && event.getBlocked()
                                        && (event.getDamageContainer().getBlockedDamage() > 0 || event.getBlockedDamage() > 0)) {
                                ToolLeveling.award(stack, player, reward);
                        }
                });
        }

        public static void reflected(Player player, ItemStack stack, ProjectileImpactEvent event, int damage) {
                if (!(player instanceof ServerPlayer)) return;
                DEFENSE.add(() -> {
                        if (event.isCanceled() && event.getProjectile().getOwner() == player) {
                                ToolLeveling.award(stack, player, Math.max(1, damage));
                        }
                });
        }

        @SubscribeEvent
        public static void onStop(ServerStoppedEvent event) {
                PENDING.clear();
                DEFENSE.clear();
                DEATHS.clear();
        }

        private record Contribution(UUID player, UUID tool) {
        }

        private record DamageSnapshot(float health, List<Contribution> contributions) {
        }
}
