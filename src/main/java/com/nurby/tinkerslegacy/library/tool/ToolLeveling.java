package com.nurby.tinkerslegacy.library.tool;

import com.nurby.tinkerslegacy.TLConfig;
import com.nurby.tinkerslegacy.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.tools.RangedToolDefinition;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Set;
import java.util.UUID;

public final class ToolLeveling {

        private static final Set<Integer> NAMED_LEVELS = Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 19, 42, 66, 99);

        private ToolLeveling() {
        }

        // Progression

        public static long threshold(int base, double multiplier, int promotions) {
                if (base < 1 || !Double.isFinite(multiplier) || multiplier < 1 || promotions < 0) {
                        throw new IllegalArgumentException("Invalid leveling requirements");
                }
                BigDecimal decimal = BigDecimal.valueOf(multiplier);
                BigInteger divisor = BigInteger.TEN.pow(Math.max(0, decimal.scale()));
                BigInteger numerator = decimal.unscaledValue().multiply(BigInteger.TEN.pow(Math.max(0, -decimal.scale())));
                BigInteger increase = numerator.subtract(divisor);
                if (increase.signum() == 0 || promotions == 0) return base;
                BigInteger required = BigInteger.valueOf(base);
                BigInteger maximum = BigInteger.valueOf(Long.MAX_VALUE);
                long remaining = promotions;
                while (remaining > 0) {
                        BigInteger increment = required.multiply(increase).divide(divisor);
                        if (increment.signum() == 0) return required.longValue();
                        // Batch equal integer increments, preserving per-level truncation even near 1.0.
                        BigInteger boundary = increment.add(BigInteger.ONE).multiply(divisor)
                                        .add(increase).subtract(BigInteger.ONE).divide(increase);
                        long count = boundary.subtract(BigInteger.ONE).subtract(required).divide(increment)
                                        .add(BigInteger.ONE).max(BigInteger.ONE).min(BigInteger.valueOf(remaining)).longValue();
                        required = required.add(increment.multiply(BigInteger.valueOf(count)));
                        if (required.compareTo(maximum) >= 0) return Long.MAX_VALUE;
                        remaining -= count;
                }
                return required.longValue();
        }

        public static ToolProgress advance(ToolProgress progress, long amount, int base, double multiplier, int maximum) {
                if (base < 1 || !Double.isFinite(multiplier) || multiplier < 1) {
                        throw new IllegalArgumentException("Invalid leveling requirements");
                }
                if (amount <= 0 || maximum > 0 && progress.promotions() >= maximum) return progress;
                long xp = amount > Long.MAX_VALUE - progress.xp() ? Long.MAX_VALUE : progress.xp() + amount;
                int promotions = progress.promotions();
                int limit = maximum > 0 ? maximum : Integer.MAX_VALUE;
                long earned = progress.earnedSlots();
                if (multiplier == 1.0) {
                        int added = (int) Math.min((long) limit - promotions, xp / base);
                        promotions += added;
                        earned += added;
                        xp -= (long) added * base;
                } else {
                        while (promotions < limit) {
                                long required = threshold(base, multiplier, promotions);
                                if (xp < required) break;
                                int count = 1;
                                if (promotions < limit - 1 && threshold(base, multiplier, promotions + 1) == required) {
                                        int low = promotions + 1;
                                        int high = limit;
                                        // Batch equal rounded thresholds, including multipliers very close to one.
                                        while (low < high) {
                                                int middle = low + (high - low) / 2;
                                                if (threshold(base, multiplier, middle) == required) low = middle + 1;
                                                else high = middle;
                                        }
                                        count = (int) Math.min((long) low - promotions, xp / required);
                                }
                                xp -= required * count;
                                promotions += count;
                                earned += count;
                        }
                }
                return new ToolProgress(promotions, xp, (int) Math.min(earned, Integer.MAX_VALUE));
        }

        // Server State

        public static void synchronize(ItemStack stack) {
                if (!(stack.getItem() instanceof DynamicTool tool)) return;
                ToolProgress progress = stack.getOrDefault(TLDataComponents.PROGRESS, ToolProgress.EMPTY);
                int maximum = TLConfig.TOOL_MAXIMUM_LEVELS.getAsInt();
                stack.set(TLDataComponents.NEXT_LEVEL_XP,
                                threshold(baseXp(tool), TLConfig.TOOL_LEVEL_XP_MULTIPLIER.get(), progress.promotions()));
                stack.set(TLDataComponents.LEVELING_ENABLED, TLConfig.ENABLE_LEVELING.getAsBoolean());
                stack.set(TLDataComponents.LEVEL_CAPPED, maximum > 0 && progress.promotions() >= maximum);
        }

        public static void award(ItemStack stack, Player player, long amount) {
                if (!(player instanceof ServerPlayer serverPlayer) || player.hasInfiniteMaterials()
                                || !(stack.getItem() instanceof DynamicTool tool) || amount <= 0) return;
                ToolState.initialize(stack, true);
                synchronize(stack);
                if (!TLConfig.ENABLE_LEVELING.getAsBoolean()) return;
                ToolProgress previous = stack.get(TLDataComponents.PROGRESS);
                ToolProgress updated = advance(previous, amount, baseXp(tool),
                                TLConfig.TOOL_LEVEL_XP_MULTIPLIER.get(), TLConfig.TOOL_MAXIMUM_LEVELS.getAsInt());
                stack.set(TLDataComponents.PROGRESS, updated);
                synchronize(stack);
                if (updated.promotions() > previous.promotions()) {
                        tool.calculateStats(stack);
                        serverPlayer.displayClientMessage(Component.translatable("tinkerslegacy.tool.level_up",
                                        stack.getHoverName(), levelName(updated)), false);
                        serverPlayer.playNotifySound(SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.PLAYERS, 0.75F, 1.0F);
                }
        }

        // Projectile Rewards
        public static long launcherXp(ItemStack launcher) {
                if (!(launcher.getItem() instanceof DynamicTool tool)
                                || !(tool.getDefinition() instanceof RangedToolDefinition definition) || ToolState.broken(launcher)) return 0;
                double draw = launcher.getOrDefault(TLDataComponents.MODIFIED_STATS, java.util.Map.<String, Number>of())
                                .getOrDefault("draw_speed", 1).doubleValue();
                return Double.isFinite(draw) && draw > 0 ? Math.max(1, (long) Math.ceil(definition.drawTime() / (4 * draw))) : 0;
        }

        public static void launcherHit(Player player, UUID identity, long reward, double speed) {
                if (!(player instanceof ServerPlayer) || identity == null || reward <= 0 || !Double.isFinite(speed) || speed <= 0.4) return;
                for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                        ItemStack stack = player.getInventory().getItem(slot);
                        if (stack.getItem() instanceof DynamicTool tool && tool.getDefinition() instanceof RangedToolDefinition
                                        && identity.equals(stack.get(TLDataComponents.TOOL_ID))) {
                                award(stack, player, reward);
                                return;
                        }
                }
        }

        private static int baseXp(DynamicTool tool) {
                var configured = TLConfig.TOOL_BASE_XP.get(tool.getDefinition().id().getPath());
                return configured == null ? TLConfig.TOOL_DEFAULT_BASE_XP.getAsInt() : configured.getAsInt();
        }

        public static Component levelName(ToolProgress progress) {
                long level = (long) progress.promotions() + 1;
                if (level <= Integer.MAX_VALUE && NAMED_LEVELS.contains((int) level)) {
                        return Component.translatable("tinkerslegacy.tool.level." + level);
                }
                long pluses = level / 12;
                Component suffix = pluses <= 32 ? Component.literal("+".repeat((int) pluses))
                                : Component.translatable("tinkerslegacy.tool.level.repetitions", pluses);
                return Component.translatable("tinkerslegacy.tool.level." + level % 12).append(suffix);
        }
}
