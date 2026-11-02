package com.nurby.tinkerslegacy.api.tool;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.registry.TLRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.awt.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.List;

public final class ToolStatText {
        private static final int DURABILITY_COLOR = amountColor(1);
        private static final Map<String, List<String>> MATERIAL_ORDER = Map.of(
                        "head", List.of("durability", "harvest_level", "mining_speed", "attack_damage"),
                        "handle", List.of("modifier", "durability"),
                        "extra", List.of("durability"),
                        "bow", List.of("draw_speed", "range", "bonus_damage"),
                        "bowstring", List.of("modifier"),
                        "arrow_shaft", List.of("modifier", "bonus_ammo"),
                        "fletching", List.of("modifier", "accuracy"));

        private ToolStatText() {
        }

        // Stats
        public static Component stat(String key, Number value) {
                Component formatted;
                if (key.equals("harvest_level") || key.startsWith("harvest_level_")) {
                        formatted = harvestLevel(value.intValue());
                } else if (key.equals("accuracy")) {
                        formatted = Component.literal(new DecimalFormat("#%",
                                        DecimalFormatSymbols.getInstance(Locale.US)).format(value.doubleValue()))
                                        .withColor(0xCDAACD);
                } else {
                        String amount = key.equals("free_modifiers")
                                        ? Integer.toString(value.intValue()) : number(value.doubleValue());
                        formatted = Component.literal(amount).withColor(switch (key) {
                                case "durability", "bonus_ammo" -> DURABILITY_COLOR;
                                case "mining_speed" -> 0x78A0CD;
                                case "attack_damage" -> 0xD76464;
                                case "modifier" -> 0xB9B95A;
                                case "draw_speed", "drawspeed" -> 0x808080;
                                case "range" -> 0x8CAFAF;
                                case "bonus_damage" -> 0x9B5041;
                                default -> ChatFormatting.GRAY.getColor();
                        });
                }
                return label(key).append(formatted);
        }

        public static Component durability(ItemStack stack, boolean textIfBroken) {
                if (textIfBroken && ToolState.broken(stack)) {
                        return label("durability").append(Component.translatable("tinkerslegacy.tool.broken")
                                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
                }
                int maximum = Math.max(0, stack.getMaxDamage());
                int remaining = ToolState.broken(stack) ? 0 : Math.max(0, maximum - ToolState.damage(stack));
                return ratio("durability", remaining, maximum);
        }

        public static Component ammo(ItemStack stack, boolean textIfEmpty) {
                if (textIfEmpty && ToolState.broken(stack)) {
                        return label("ammo").append(Component.translatable("tinkerslegacy.tool.empty")
                                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
                }
                return ratio("ammo", ToolRangedActions.ammo(stack), ToolRangedActions.maxAmmo(stack));
        }

        // Materials
        public static List<Component> materialStats(ResourceLocation materialId, ResourceLocation statType) {
            if (!TLRegistries.MATERIALS.containsKey(materialId)) {
                        return List.of();
                }
            var material = TLRegistries.MATERIALS.get(materialId);
            var stats = material.stats().get(statType);
                if (stats == null) {
                        return List.of();
                }
                Map<String, Number> values = stats.statMap().values();
            boolean builtin = statType.getNamespace().equals(TinkersLegacy.MODID);
            String role = statType.getPath();
                List<String> ordered = builtin ? MATERIAL_ORDER.getOrDefault(role, List.of()) : List.of();
                List<String> keys = new ArrayList<>(ordered);
                TreeSet<String> remaining = new TreeSet<>(values.keySet());
                remaining.removeAll(ordered);
                keys.addAll(remaining);

                List<Component> result = new ArrayList<>();
                for (String key : keys) {
                        Number value = values.get(key);
                        if (value == null) {
                                continue;
                        }
                        if (builtin && role.equals("bow") && key.equals("draw_speed")) {
                                // Material draw speed is displayed as its reciprocal, as in Tinkers 2.
                                value = 1.0F / value.floatValue();
                        }
                        result.add(stat(key, value));
                }
                return List.copyOf(result);
        }

        // Formatting
        public static String number(double value) {
                return new DecimalFormat("#,###,###.##", DecimalFormatSymbols.getInstance(Locale.US)).format(value);
        }

        private static MutableComponent label(String key) {
                return Component.translatable("tinkerslegacy.stat." + key)
                                .append(": ").withStyle(ChatFormatting.GRAY);
        }

        private static Component ratio(String key, int current, int maximum) {
                float fraction = maximum > 0 ? (float) current / maximum : 0;
                return label(key)
                                .append(Component.literal(number(current)).withColor(amountColor(fraction)))
                                .append(Component.literal("/").withStyle(ChatFormatting.GRAY))
                                .append(Component.literal(number(maximum)).withColor(DURABILITY_COLOR));
        }

        private static int amountColor(float fraction) {
                return Color.HSBtoRGB(Math.clamp(fraction / 3.0F, 0.01F, 0.5F), 0.65F, 0.8F) & 0xFFFFFF;
        }

        private static Component harvestLevel(int level) {
                int color = switch (level) {
                        case 0 -> 0x999999;
                        case 1 -> 0xCACACA;
                        case 2 -> 0x55FFFF;
                        case 3 -> 0x601CC4;
                        case 4 -> 0x2882D4;
                        default -> ChatFormatting.GRAY.getColor();
                };
                return level >= 0 && level <= 4
                                ? Component.translatable("tinkerslegacy.mininglevel." + level).withColor(color)
                                : Component.literal(Integer.toString(level)).withColor(color);
        }
}
