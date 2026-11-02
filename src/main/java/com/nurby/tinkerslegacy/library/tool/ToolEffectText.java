package com.nurby.tinkerslegacy.library.tool;

import com.nurby.tinkerslegacy.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.library.tool.traits.ToolTraits;
import com.nurby.tinkerslegacy.tools.CleaverDefinition;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public final class ToolEffectText {

        private ToolEffectText() {
        }

        // Modifiers

        public static MutableComponent modifierName(ResourceLocation id) {
                MutableComponent name = Component.translatable("modifier." + id.getNamespace() + "."
                                + (ToolModifiers.isFortify(id) ? "fortify" : id.getPath()));
                if (ToolModifiers.isFortify(id)) {
                        ResourceLocation material = ResourceLocation.tryParse(id.getPath().substring(8).replaceFirst("/", ":"));
                        appendMaterial(name, material);
                }
                return name.withStyle(style -> style.withColor(ToolModifiers.color(ItemStack.EMPTY, id)));
        }

        public static MutableComponent modifierName(ItemStack stack, ResourceLocation id) {
                int level = ToolModifiers.displayLevel(stack, id);
                MutableComponent name = modifierName(id);
                if (id.equals(ToolModifiers.REINFORCED) && level >= 5) {
                        name = Component.translatable("modifier.tinkerslegacy.reinforced.unbreakable");
                } else if ((id.equals(ToolModifiers.HASTE) || id.equals(ToolModifiers.SHARPNESS)) && level > 1) {
                        name = Component.translatable("modifier." + id.getNamespace() + "." + id.getPath() + "." + level);
                } else if (level > 1 || id.equals(ToolModifiers.LUCK) && level > 0) {
                        name.append(" ").append(level <= 10 ? Component.translatable("enchantment.level." + level)
                                        : Component.literal(Integer.toString(level)));
                }
                if (id.equals(ToolModifiers.EMBOSSMENT)) {
                        var materials = stack.get(TLDataComponents.EMBOSSMENT);
                        if (materials != null && !materials.isEmpty()) appendMaterial(name, materials.getFirst().material());
                }
                return name.withStyle(style -> style.withColor(ToolModifiers.color(stack, id)));
        }

        public static Component modifierLine(ItemStack stack, ToolModifierEntry entry) {
                ToolModifiers.Definition definition = ToolModifiers.get(entry.id());
                MutableComponent name = modifierName(stack, entry.id());
                // A completed level keeps its own maximum until the next ingredient starts another level.
                int maximum = definition == null ? 0 : definition.nextThreshold(Math.max(0, entry.points() - 1));
                return definition == null || definition.pointsPerLevel() == 1 ? name
                                : Component.translatable("tinkerslegacy.tool.modifier_progress", name,
                                                entry.points(), maximum)
                                                .withStyle(name.getStyle());
        }

        public static List<Component> modifierNames(ItemStack stack, boolean detailed) {
                List<Component> names = new ArrayList<>();
                boolean cleaver = stack.getItem() instanceof DynamicTool tool && tool.getDefinition() instanceof CleaverDefinition;
                if (cleaver) names.add(modifierName(stack, ToolModifiers.BEHEADING));
                for (ToolModifierEntry entry : stack.getOrDefault(TLDataComponents.MODIFIERS, List.<ToolModifierEntry>of())) {
                        if (entry.id().equals(ToolModifiers.CREATIVE) || ToolModifiers.get(entry.id()) == null
                                        || cleaver && entry.id().equals(ToolModifiers.BEHEADING)) continue;
                        names.add(detailed ? modifierLine(stack, entry) : modifierName(stack, entry.id()));
                }
                return List.copyOf(names);
        }

        private static void appendMaterial(MutableComponent name, ResourceLocation material) {
                if (material != null && TLRegistries.MATERIALS.containsKey(material)) {
                        name.append(" (").append(Component.translatable(material.getNamespace() + ".material." + material.getPath()))
                                        .append(")");
                }
        }

        // Traits

        public static MutableComponent traitName(ResourceLocation id) {
                return Component.translatable("trait." + id.getNamespace() + "." + id.getPath())
                                .withStyle(style -> style.withColor(ToolTraits.color(ItemStack.EMPTY, id)));
        }

        public static List<Component> traitNames(ItemStack stack, List<ResourceLocation> traits) {
                List<Component> names = new ArrayList<>();
                HashSet<ResourceLocation> shown = new HashSet<>();
                for (ResourceLocation trait : traits) {
                        ResourceLocation displayId = ToolTraits.displayId(trait);
                        if (!shown.add(displayId)) continue;
                        MutableComponent name = traitName(displayId);
                        if (!displayId.equals(trait)) {
                                int level = traits.stream().distinct().filter(id -> ToolTraits.displayId(id).equals(displayId))
                                                .mapToInt(id -> !displayId.getPath().equals("writable") && id.getPath().endsWith("2") ? 2 : 1).sum();
                                if (level > 1) name.append(" ").append(Component.translatable("enchantment.level." + level));
                        }
                        names.add(name.withStyle(style -> style.withColor(ToolTraits.color(stack, trait))));
                }
                return List.copyOf(names);
        }
}
