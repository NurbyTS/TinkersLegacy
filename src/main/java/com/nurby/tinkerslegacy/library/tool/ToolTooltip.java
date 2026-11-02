package com.nurby.tinkerslegacy.library.tool;

import com.nurby.tinkerslegacy.item.dynamic.DynamicPart;
import com.nurby.tinkerslegacy.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.library.material.layer.MaterialLayer;
import com.nurby.tinkerslegacy.library.tool.traits.ToolTraits;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLRegistries;
import com.nurby.tinkerslegacy.registry.ToolStatTypes;
import com.nurby.tinkerslegacy.tools.ProjectileToolDefinition;
import com.nurby.tinkerslegacy.tools.RangedToolDefinition;
import com.nurby.tinkerslegacy.util.TinkersUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public final class ToolTooltip {

        private ToolTooltip() {
        }

        // Views

        public static void addTool(ItemStack stack, List<Component> lines, boolean shift, boolean control, boolean attack) {
                if (!(stack.getItem() instanceof DynamicTool tool)) return;
                addLeveling(stack, lines);
                if (shift) {
                        addStats(stack, lines, false);
                } else if (control) {
                        addParts(stack, lines);
                } else {
                        if (ToolState.broken(stack)) {
                                lines.add(Component.translatable(tool.getDefinition() instanceof ProjectileToolDefinition
                                                ? "tinkerslegacy.tool.empty" : "tinkerslegacy.tool.broken")
                                                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
                        }
                        lines.addAll(ToolEffectText.traitNames(stack, ToolTraits.traits(stack)));
                        lines.addAll(ToolEffectText.modifierNames(stack, false));
                        lines.add(Component.empty());
                        lines.add(hint(false));
                        lines.add(hint(true));
                        if (attack) {
                                lines.add(Component.translatable("attribute.modifier.plus.0",
                                                ToolStatText.number(TinkersUtils.getActualDamage(stack)),
                                                Component.translatable("attribute.name.generic.attack_damage"))
                                                .withStyle(ChatFormatting.BLUE));
                        }
                }
        }

        public static void addPart(ItemStack stack, List<Component> lines, boolean shift) {
                if (!(stack.getItem() instanceof DynamicPart part)) return;
                List<MaterialLayer> layers = stack.getOrDefault(TLDataComponents.MATERIALS, List.of());
                if (part.definition().id().getPath().equals("sharpening_kit")) {
                        lines.add(Component.translatable("tinkerslegacy.tooltip.sharpening_kit.flint"));
                        lines.add(Component.translatable("tinkerslegacy.tooltip.sharpening_kit.upgrade"));
                        for (var layer : layers) {
                                if (!TLRegistries.MATERIALS.containsKey(layer.material())) continue;
                                var stats = TLRegistries.MATERIALS.get(layer.material()).stats().get(ToolStatTypes.HEAD.getId());
                                if (stats != null) {
                                        lines.add(ToolStatText.stat("harvest_level", stats.statMap().values().getOrDefault("harvest_level", 0)));
                                        break;
                                }
                        }
                        return;
                }
                addPartTraits(stack, layers, lines);
                if (shift) {
                        for (var layer : layers) {
                                List<Component> stats = ToolStatText.materialStats(layer);
                                if (stats.isEmpty()) continue;
                                lines.add(Component.empty());
                                lines.add(roleName(layer.statType()).withStyle(ChatFormatting.WHITE, ChatFormatting.UNDERLINE));
                                lines.addAll(stats);
                        }
                } else {
                        lines.add(Component.empty());
                        lines.add(hint(false));
                }
        }

        private static Component hint(boolean control) {
                MutableComponent key = Component.translatable("tinkerslegacy.tooltip." + (control ? "ctrl" : "shift"))
                                .withStyle(control ? ChatFormatting.AQUA : ChatFormatting.YELLOW);
                if (!control) key.withStyle(ChatFormatting.ITALIC);
                return Component.translatable("tinkerslegacy.tooltip." + (control ? "hold_more" : "hold_stats"), key)
                                .withStyle(ChatFormatting.GRAY);
        }

        // Leveling

        public static void addLeveling(ItemStack stack, List<Component> lines) {
                if (!stack.has(TLDataComponents.PROGRESS)) return;
                ToolProgress progress = stack.get(TLDataComponents.PROGRESS);
                float hue = 0.277777F * ((long) progress.promotions() + 1);
                hue -= (int) hue;
                Component name = ToolLeveling.levelName(progress).copy()
                                .withColor(Color.HSBtoRGB(hue, 0.75F, 0.8F) & 0xFFFFFF);
                lines.add(Component.translatable("tinkerslegacy.tool.level", name).withStyle(ChatFormatting.GRAY));
                if (stack.getOrDefault(TLDataComponents.LEVELING_ENABLED, false)
                                && !stack.getOrDefault(TLDataComponents.LEVEL_CAPPED, false)) {
                        lines.add(Component.translatable("tinkerslegacy.tool.xp",
                                        Component.literal(Long.toString(progress.xp())).withStyle(ChatFormatting.WHITE),
                                        Component.literal(Long.toString(stack.getOrDefault(TLDataComponents.NEXT_LEVEL_XP, 500L)))
                                                        .withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GRAY));
                }
        }

        // Stats

        public static void addStats(ItemStack stack, List<Component> lines, boolean detailed) {
                if (!(stack.getItem() instanceof DynamicTool tool)) return;
                var definition = tool.getDefinition();
                var values = stack.getOrDefault(TLDataComponents.MODIFIED_STATS, Map.<String, Number>of());
                if (definition instanceof ProjectileToolDefinition) {
                        lines.add(ToolStatText.ammo(stack, !detailed));
                        lines.add(ToolStatText.stat("attack_damage", TinkersUtils.getActualDamage(stack)));
                        lines.add(ToolStatText.stat("accuracy", values.getOrDefault("accuracy", 1)));
                } else {
                        lines.add(ToolStatText.durability(stack, !detailed));
                        if (definition.isHarvestTool()) {
                                for (String key : List.of("harvest_level", "harvest_level_pickaxe", "harvest_level_axe", "harvest_level_shovel")) {
                                        if (values.containsKey(key)) lines.add(ToolStatText.stat(key, values.get(key)));
                                }
                                lines.add(ToolStatText.stat("mining_speed", values.getOrDefault("mining_speed", 1)));
                        }
                        if (definition instanceof RangedToolDefinition launcher) {
                                double speed = Math.max(0.001, values.getOrDefault("draw_speed", 1).doubleValue());
                                lines.add(ToolStatText.stat("draw_speed", launcher.drawTime() / (20 * speed)));
                                lines.add(ToolStatText.stat("range", values.getOrDefault("range", 1)));
                                lines.add(ToolStatText.stat("bonus_damage", values.getOrDefault("bonus_damage", 0)));
                        }
                        lines.add(ToolStatText.stat("attack_damage", TinkersUtils.getActualDamage(stack)));
                }
                int free = ToolState.freeModifiers(stack);
                if (free > 0) lines.add(ToolStatText.stat("free_modifiers", free));
        }

        // Components

        public static void addParts(ItemStack stack, List<Component> lines) {
                if (!(stack.getItem() instanceof DynamicTool tool)) return;
                List<MaterialLayer> materials = stack.getOrDefault(TLDataComponents.MATERIALS, List.of());
                if (materials.size() < tool.getDefinition().parts().size()) return;
                int offset = 0;
                for (var requirement : tool.getDefinition().partRequirements()) {
                        List<MaterialLayer> layers = materials.subList(offset, offset + requirement.statTypes().size());
                        offset += requirement.statTypes().size();
                        ResourceLocation materialId = layers.getFirst().material();
                        int color = TLRegistries.MATERIALS.containsKey(materialId)
                                        ? TLRegistries.MATERIALS.get(materialId).color() : 0xFFFFFF;
                        lines.add(TinkersUtils.getMaterialPartName(requirement.part(), layers)
                                        .withColor(color).withStyle(ChatFormatting.UNDERLINE));
                        LinkedHashSet<ResourceLocation> traits = new LinkedHashSet<>();
                        ItemStack source = stack.copy();
                        source.set(TLDataComponents.MATERIALS, List.copyOf(layers));
                        source.remove(TLDataComponents.EMBOSSMENT);
                        for (var layer : layers) {
                                traits.addAll(ToolTraits.materialTraits(layer.material(), layer.statType()));
                                if (tool.getDefinition() instanceof ProjectileToolDefinition projectile
                                                && projectile.kind() != ProjectileToolDefinition.Kind.BOLT
                                                && layer.statType().equals(ToolStatTypes.HEAD.getId())) {
                                        traits.addAll(ToolTraits.materialTraits(layer.material(), ToolStatTypes.PROJECTILE.getId()));
                                }
                        }
                        boolean first = true;
                        for (var layer : layers) {
                                List<Component> stats = ToolStatText.materialStats(layer);
                                lines.addAll(stats);
                                if (first && !stats.isEmpty()) {
                                        // Component views show each source trait, not the completed tool's merged levels.
                                        for (var trait : traits) lines.addAll(ToolEffectText.traitNames(source, List.of(trait)));
                                        first = false;
                                }
                        }
                        lines.add(Component.empty());
                }
        }

        private static void addPartTraits(ItemStack stack, List<MaterialLayer> layers, List<Component> lines) {
                Map<List<ResourceLocation>, List<ResourceLocation>> groups = new LinkedHashMap<>();
                for (var layer : layers) {
                        List<ResourceLocation> traits = ToolTraits.materialTraits(layer.material(), layer.statType());
                        if (!traits.isEmpty()) groups.computeIfAbsent(traits, ignored -> new ArrayList<>()).add(layer.statType());
                }
                for (var group : groups.entrySet()) {
                        MutableComponent line = Component.empty();
                        if (groups.size() > 1) {
                                MutableComponent label = Component.empty();
                                for (var role : group.getValue()) {
                                        if (!label.getSiblings().isEmpty()) label.append(", ");
                                        label.append(roleName(role));
                                }
                                line.append(label.append(": ").withStyle(ChatFormatting.ITALIC));
                        }
                        List<Component> names = ToolEffectText.traitNames(stack, group.getKey());
                        for (int index = 0; index < names.size(); index++) {
                                if (index > 0) line.append(Component.literal(", ").withStyle(names.get(index).getStyle()));
                                line.append(names.get(index));
                        }
                        lines.add(line);
                }
        }

        private static MutableComponent roleName(ResourceLocation role) {
                return Component.translatable(role.getNamespace() + ".material_stat_type." + role.getPath());
        }
}
