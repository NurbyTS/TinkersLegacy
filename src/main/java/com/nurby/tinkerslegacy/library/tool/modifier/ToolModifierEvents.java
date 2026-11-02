package com.nurby.tinkerslegacy.library.tool.modifier;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.library.tool.ToolModifiers;
import com.nurby.tinkerslegacy.library.tool.ToolState;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = TinkersLegacy.MODID)
public final class ToolModifierEvents {
        public static final String MOSS_PICKUP_VALUE = "tinkerslegacy:moss_pickup_value";
        private ToolModifierEvents() {
        }

        // Mining
        @SubscribeEvent(priority = EventPriority.LOW)
        public static void breakSpeed(PlayerEvent.BreakSpeed event) {
                ItemStack stack = event.getEntity().getMainHandItem();
                int blasting = ToolModifiers.level(stack, ToolModifiers.BLASTING);
                if (blasting <= 0 || ToolState.broken(stack) || event.getPosition().isEmpty()) return;
                float hardness = event.getState().getDestroySpeed(event.getEntity().level(), event.getPosition().get());
                if (hardness <= 0) return;
                float speed = stack.getOrDefault(TLDataComponents.MODIFIED_STATS, Map.<String, Number>of())
                                .getOrDefault("mining_speed", 1).floatValue() * hardness;
                speed /= blasting == 3 ? 1.1F : blasting == 2 ? 5 : 10;
                float weight = blasting / 3F;
                event.setNewSpeed(speed * weight + event.getOriginalSpeed() * (1 - weight));
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void drops(BlockDropsEvent event) {
                int blasting = ToolModifiers.level(event.getTool(), ToolModifiers.BLASTING);
                if (blasting <= 0) return;
                event.getDrops().removeIf(drop -> event.getLevel().random.nextFloat() < blasting / 3F);
        }

        // Mending Moss
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void pickupXp(PlayerXpEvent.PickupXp event) {
                int original = event.getOrb().value;
                for (ItemStack stack : List.of(event.getEntity().getMainHandItem(), event.getEntity().getOffhandItem())) {
                        event.getOrb().value -= ModifierEffects.storeMossXp(stack, event.getOrb().value);
                }
                if (event.getOrb().value != original) event.getOrb().getPersistentData().putInt(MOSS_PICKUP_VALUE, original);
        }

        @SubscribeEvent
        public static void awakenMoss(PlayerInteractEvent.RightClickBlock event) {
                ItemStack input = event.getItemStack();
                if (!BuiltInRegistries.ITEM.getKey(input.getItem()).equals(ToolModifiers.id("ball_of_moss"))
                                || event.getLevel().getBlockState(event.getPos()).getEnchantPowerBonus(event.getLevel(), event.getPos()) < 1) return;
                Player player = event.getEntity();
                if (player.experienceLevel < 10) {
                        if (!event.getLevel().isClientSide) player.displayClientMessage(
                                        Component.translatable("message.tinkerslegacy.mending_moss.not_enough_levels", 10), true);
                        return;
                }
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
                if (event.getLevel().isClientSide) return;
                input.shrink(1);
                player.onEnchantmentPerformed(ItemStack.EMPTY, 10);
                ItemStack result = new ItemStack(BuiltInRegistries.ITEM.get(ToolModifiers.id("mending_moss")));
                if (!player.getInventory().add(result)) player.drop(result, false);
                event.getLevel().playSound(null, event.getPos(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1, 1);
        }

        // Soulbound
        @SubscribeEvent(priority = EventPriority.HIGH)
        public static void death(LivingDropsEvent event) {
                if (!(event.getEntity() instanceof Player player)
                                || player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;
                var iterator = event.getDrops().iterator();
                while (iterator.hasNext()) {
                        ItemEntity drop = iterator.next();
                        if (!ToolModifiers.has(drop.getItem(), ToolModifiers.SOULBOUND)) continue;
                        ItemStack stack = drop.getItem();
                        if (player.getInventory().add(stack)) iterator.remove();
                }
        }

        @SubscribeEvent(priority = EventPriority.HIGH)
        public static void clone(PlayerEvent.Clone event) {
                if (!event.isWasDeath() || event.getEntity().level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;
                var inventory = event.getOriginal().getInventory();
                for (int index = 0; index < inventory.getContainerSize(); index++) {
                        ItemStack stack = inventory.getItem(index);
                        if (!ToolModifiers.has(stack, ToolModifiers.SOULBOUND)) continue;
                        inventory.setItem(index, ItemStack.EMPTY);
                        if (!event.getEntity().getInventory().add(stack)) event.getEntity().drop(stack, false);
                }
        }
}
