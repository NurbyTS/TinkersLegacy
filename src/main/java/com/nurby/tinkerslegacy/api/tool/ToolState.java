package com.nurby.tinkerslegacy.api.tool;

import com.nurby.tinkerslegacy.TLConfig;
import com.nurby.tinkerslegacy.api.tool.modifier.ModifierEffects;
import com.nurby.tinkerslegacy.api.tool.traits.ToolTraits;
import com.nurby.tinkerslegacy.content.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public final class ToolState {
        public static final int DATA_VERSION = 3;

        private ToolState() {
        }

        // Initialization

        public static void initialize(ItemStack stack, boolean assignIdentity) {
                if (!(stack.getItem() instanceof DynamicTool)) return;
                if (!stack.has(TLDataComponents.PROGRESS)) stack.set(TLDataComponents.PROGRESS, ToolProgress.EMPTY);
                if (!stack.has(TLDataComponents.MODIFIERS)) stack.set(TLDataComponents.MODIFIERS, List.of());
                if (!stack.has(TLDataComponents.STARTING_MODIFIERS)) {
                        stack.set(TLDataComponents.STARTING_MODIFIERS, TLConfig.NEW_TOOL_MIN_MODIFIERS.getAsInt());
                }
                if (!stack.has(TLDataComponents.BROKEN)) {
                        stack.set(TLDataComponents.BROKEN,
                                        stack.getMaxDamage() > 0 && stack.getDamageValue() >= stack.getMaxDamage());
                }
                if (broken(stack) && !stack.has(TLDataComponents.BROKEN_DAMAGE)) {
                        stack.set(TLDataComponents.BROKEN_DAMAGE, Math.max(stack.getMaxDamage(), stack.getDamageValue()));
                }
                if (!stack.has(TLDataComponents.REPAIR_COUNT)) stack.set(TLDataComponents.REPAIR_COUNT, 0);
                if (stack.getMaxDamage() > 0) {
                        stack.set(DataComponents.DAMAGE, Math.min(stack.getDamageValue(), stack.getMaxDamage() - 1));
                }
                if (assignIdentity && !stack.has(TLDataComponents.TOOL_ID)) {
                        stack.set(TLDataComponents.TOOL_ID, UUID.randomUUID());
                }
        }

        // Getters

        public static boolean broken(ItemStack stack) {
                return stack.getOrDefault(TLDataComponents.BROKEN, false);
        }

        public static int damage(ItemStack stack) {
                return broken(stack) ? stack.getOrDefault(TLDataComponents.BROKEN_DAMAGE, stack.getMaxDamage())
                                : stack.getDamageValue();
        }

        public static int modifierCapacity(ItemStack stack) {
                long available = (long) stack.getOrDefault(TLDataComponents.STARTING_MODIFIERS, 3)
                                + stack.getOrDefault(TLDataComponents.PROGRESS, ToolProgress.EMPTY).earnedSlots()
                                + ToolTraits.bonusSlots(stack) + ToolModifiers.bonusSlots(stack);
                return (int) Math.clamp(available, 0, Integer.MAX_VALUE);
        }

        public static int usedModifiers(ItemStack stack) {
                long used = 0;
                for (ToolModifierEntry entry : stack.getOrDefault(TLDataComponents.MODIFIERS, List.<ToolModifierEntry>of())) {
                        ToolModifiers.Definition modifier = ToolModifiers.get(entry.id());
                        // Unknown extensions reserve their slot until the extension is available again.
                        used += modifier == null ? 1 : modifier.slots(entry.points());
                }
                return (int) Math.clamp(used, 0, Integer.MAX_VALUE);
        }

        public static int availableModifiers(ItemStack stack) {
                return modifierCapacity(stack) - usedModifiers(stack);
        }

        public static int freeModifiers(ItemStack stack) {
                return Math.max(0, availableModifiers(stack));
        }

        // Durability

        public static void hurt(ItemStack stack, int amount) {
                hurt(stack, amount, null);
        }

        public static void hurt(ItemStack stack, int amount, @Nullable LivingEntity entity) {
                if (entity != null && (entity.level().isClientSide || entity.hasInfiniteMaterials())) return;
                if (amount <= 0 || broken(stack) || stack.getMaxDamage() <= 0) return;
                amount = ModifierEffects.adjustDamage(stack, amount, ToolTraits.damage(stack, amount, entity), entity);
                if (amount <= 0 || stack.has(DataComponents.UNBREAKABLE)) return;
                long damaged = (long) stack.getDamageValue() + amount;
                if (damaged >= stack.getMaxDamage()) {
                        stack.set(TLDataComponents.BROKEN, true);
                        stack.set(TLDataComponents.BROKEN_DAMAGE, stack.getMaxDamage());
                }
                // ItemStack checks its destruction threshold even when damageItem returns zero.
                stack.setDamageValue((int) Math.min(damaged, stack.getMaxDamage() - 1L));
        }

        public static void repair(ItemStack stack, int amount) {
                if (amount <= 0 || damage(stack) <= 0) return;
                initialize(stack, false);
                int remaining = Math.max(0, damage(stack) - amount);
                boolean stillBroken = remaining >= stack.getMaxDamage();
                stack.set(TLDataComponents.BROKEN, stillBroken);
                if (stillBroken) stack.set(TLDataComponents.BROKEN_DAMAGE, remaining);
                else stack.remove(TLDataComponents.BROKEN_DAMAGE);
                stack.setDamageValue(Math.max(0, Math.min(remaining, stack.getMaxDamage() - 1)));
                int count = stack.getOrDefault(TLDataComponents.REPAIR_COUNT, 0);
                stack.set(TLDataComponents.REPAIR_COUNT, count == Integer.MAX_VALUE ? count : count + 1);
        }
}
