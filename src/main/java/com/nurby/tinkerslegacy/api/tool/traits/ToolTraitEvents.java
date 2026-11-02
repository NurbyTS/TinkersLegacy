package com.nurby.tinkerslegacy.api.tool.traits;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.api.tool.ToolAreaActions;
import com.nurby.tinkerslegacy.api.tool.ToolState;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLTags;
import com.nurby.tinkerslegacy.util.TinkersUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

import java.util.Map;

@EventBusSubscriber(modid = TinkersLegacy.MODID)
public final class ToolTraitEvents {
        private ToolTraitEvents() {
        }

        // Mining Speed
        public static float aridiculousness(Level level, BlockPos pos) {
                var climate = level.getBiome(pos).value().getModifiedClimateSettings();
                float rain = level.isRaining() ? climate.downfall() / 2F : 0;
                return (float) (Math.pow(1.25, 3 * (0.5 + climate.temperature() - climate.downfall())) - 1.25) - rain;
        }

        @SubscribeEvent
        public static void miningSpeed(PlayerEvent.BreakSpeed event) {
                Player player = event.getEntity();
                ItemStack stack = player.getMainHandItem();
                if (ToolState.broken(stack)) return;
                BlockPos pos = event.getPosition().orElse(player.blockPosition());
                BlockState state = event.getState();
                float speed = event.getNewSpeed();
                float original = event.getOriginalSpeed();
                for (var trait : ToolTraits.traits(stack)) {
                        if (!trait.getNamespace().equals(TinkersLegacy.MODID)) continue;
                        switch (trait.getPath()) {
                                case "aquadynamic" -> {
                                        float coefficient = player.isInWater() ? 6.5F : 1;
                                        if (player.level().isRaining()) coefficient += player.level().getBiome(player.blockPosition())
                                                        .value().getModifiedClimateSettings().downfall() / 1.6F;
                                        speed += original * coefficient;
                                }
                                case "aridiculous" -> speed += original * aridiculousness(player.level(), pos) / 10F;
                                case "crumbling" -> {
                                        if (!state.requiresCorrectToolForDrops()) speed *= stack.getOrDefault(TLDataComponents.MODIFIED_STATS, Map.<String, Number>of())
                                                        .getOrDefault("mining_speed", 1).floatValue() * 0.5F;
                                }
                                case "depthdigger" -> speed += Math.max(0, 72 - pos.getY()) / 30F;
                                case "lightweight" -> speed *= 1.1F;
                                case "momentum" -> speed += original * ToolTraitEffects.level(player, ToolTraitEffects.MOMENTUM) / 80F;
                                case "stonebound" -> {
                                        if (ToolAreaActions.effective(stack, state)) speed += (float) Math.log(ToolState.damage(stack) / 72.0 + 1) * 2;
                                }
                                case "unnatural" -> {
                                        int required = state.is(TLTags.Blocks.NEEDS_COBALT_TOOL) ? 4 : state.is(BlockTags.NEEDS_DIAMOND_TOOL) ? 3
                                                        : state.is(BlockTags.NEEDS_IRON_TOOL) ? 2 : state.is(BlockTags.NEEDS_STONE_TOOL) ? 1 : 0;
                                        if (ToolAreaActions.effective(stack, state)) {
                                                var stats = stack.getOrDefault(TLDataComponents.MODIFIED_STATS, Map.<String, Number>of());
                                                int available = stats.getOrDefault("harvest_level", -1).intValue();
                                                if (state.is(BlockTags.MINEABLE_WITH_AXE)) available = Math.max(available,
                                                                stats.getOrDefault("harvest_level_axe", -1).intValue());
                                                if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)) available = Math.max(available,
                                                                stats.getOrDefault("harvest_level_shovel", -1).intValue());
                                                if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) available = Math.max(available,
                                                                stats.getOrDefault("harvest_level_pickaxe", -1).intValue());
                                                speed += Math.max(0, available - required);
                                        }
                                }
                                default -> { }
                        }
                }
                event.setNewSpeed(Math.max(0, speed));
        }

        // Drops
        @SubscribeEvent(priority = EventPriority.LOW)
        public static void drops(BlockDropsEvent event) {
                ItemStack tool = event.getTool();
                if (ToolTraits.has(tool, "established")) {
                        float roll = event.getLevel().random.nextFloat();
                        if (roll < 0.33F || event.getDroppedExperience() == 0 && roll < 0.03F) {
                                event.setDroppedExperience(event.getDroppedExperience() + 1);
                        }
                }
                if (!ToolTraits.has(tool, "autosmelt") || !ToolAreaActions.effective(tool, event.getState())) return;
                var level = event.getLevel();
                int fortune = tool.getEnchantmentLevel(level.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                                .getHolderOrThrow(Enchantments.FORTUNE));
                for (var drop : event.getDrops()) {
                        var recipe = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(drop.getItem()), level);
                        if (recipe.isEmpty()) continue;
                        ItemStack cooked = recipe.get().value().getResultItem(level.registryAccess()).copyWithCount(drop.getItem().getCount());
                        if (cooked.isEmpty()) continue;
                        if (fortune > 0 && com.nurby.tinkerslegacy.TLConfig.AUTOSMELT_FORTUNE_INTERACTION.get()) cooked.setCount(cooked.getCount() * level.random.nextInt(fortune + 1) + 1);
                        drop.setItem(cooked);
                        float xp = recipe.get().value().getExperience();
                        if (xp < 1 && level.random.nextFloat() < xp) xp += 1;
                        event.setDroppedExperience(event.getDroppedExperience() + (int) xp);
                }
        }

        @SubscribeEvent
        public static void experience(LivingExperienceDropEvent event) {
                Player player = event.getAttackingPlayer();
                if (player == null || !ToolTraits.has(player.getMainHandItem(), "established")) return;
                int xp = event.getDroppedExperience();
                event.setDroppedExperience(xp == 0 ? player.getRandom().nextFloat() < 0.03F ? 1 : 0
                                : 1 + Math.round(xp * 1.25F + player.getRandom().nextFloat() * 0.25F));
        }

        // Defenses
        @SubscribeEvent
        public static void attributes(ItemAttributeModifierEvent event) {
                if (ToolTraits.has(event.getItemStack(), "heavy")) {
                        event.addModifier(Attributes.KNOCKBACK_RESISTANCE,
                                        new AttributeModifier(ToolTraits.id("heavy"), 1, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.HAND);
                }
        }

        @SubscribeEvent(priority = EventPriority.LOW)
        public static void hurt(LivingIncomingDamageEvent event) {
                if (!(event.getEntity() instanceof Player player) || ToolTraits.secondary()) return;
                ItemStack stack = player.isBlocking() ? player.getUseItem() : player.getMainHandItem();
                if (ToolState.broken(stack)) return;
                if (ToolTraits.has(stack, "flammable")) {
                        if (player.isBlocking() && event.getSource().is(DamageTypeTags.IS_FIRE)) {
                                event.setCanceled(true);
                                stack.hurtAndBreak(3, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
                        }
                        if (event.getSource().getEntity() != null) event.getSource().getEntity().igniteForSeconds(3);
                }
                if (!player.isBlocking() && ToolTraits.has(stack, "spiky") && event.getSource().getEntity() instanceof LivingEntity attacker
                                && attacker != player && !event.getSource().is(DamageTypes.THORNS)) {
                        ToolTraits.thornDamage(attacker, player, (float) TinkersUtils.getActualDamage(stack) / 2);
                }
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void block(LivingShieldBlockEvent event) {
                LivingEntity player = event.getEntity();
                ItemStack stack = player.getUseItem();
                if (!event.getBlocked() || ToolState.broken(stack) || ToolTraits.secondary()) return;
                if (ToolTraits.has(stack, "stiff")) {
                        event.setBlockedDamage(Math.max(event.getBlockedDamage(), Math.min(event.getOriginalBlockedDamage() - 1,
                                        event.getBlockedDamage() + 1)));
                }
                if (ToolTraits.has(stack, "spiky") && event.getDamageSource().getEntity() instanceof LivingEntity attacker
                                && attacker != player && !event.getDamageSource().is(DamageTypes.THORNS)) {
                        ToolTraits.thornDamage(attacker, player, (float) TinkersUtils.getActualDamage(stack));
                }
        }

        @SubscribeEvent
        public static void teleport(EntityTeleportEvent.EnderEntity event) {
                if (event.getEntityLiving().hasEffect(ToolTraitEffects.ENDERFERENCE)) event.setCanceled(true);
        }
}
