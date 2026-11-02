package com.nurby.tinkerslegacy.library.station;

import com.nurby.tinkerslegacy.TLConfig;
import com.nurby.tinkerslegacy.library.material.MaterialDefinition;
import com.nurby.tinkerslegacy.registry.TLItems;
import com.nurby.tinkerslegacy.library.tool.ToolRepair;
import com.nurby.tinkerslegacy.registry.TLRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import com.nurby.tinkerslegacy.registry.TLBlocks;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class MaterialInputs {
        public static final int UNIT = 144;
        public static final int SHARD = 72;
        public static final int FRAGMENT = 36;
        public static final int NUGGET = 16;
        private static final List<Entry> ENTRIES = new ArrayList<>();

        public record Entry(ResourceLocation material, Supplier<Ingredient> ingredient, int value, boolean craftable) {
                public Entry {
                        if (value <= 0) {
                                throw new IllegalArgumentException("Material value must be positive");
                        }
                }
        }

        public record Match(ResourceLocation material, int value, boolean craftable) {
        }

        static {
                // Natural Materials
                add("wood", () -> Ingredient.of(Items.STICK), SHARD, true);
                add("wood", () -> Ingredient.of(ItemTags.PLANKS), UNIT, true);
                add("wood", () -> Ingredient.of(ItemTags.LOGS), UNIT * 4, true);
                add("stone", () -> Ingredient.of(Items.STONE, Items.COBBLESTONE), UNIT, true);
                common("stone", "stones", UNIT, true);
                common("stone", "cobblestones", UNIT, true);
                add("flint", () -> Ingredient.of(Items.FLINT), UNIT, true);
                add("cactus", () -> Ingredient.of(Items.CACTUS), UNIT, true);
                add("bone", () -> Ingredient.of(Items.BONE), UNIT, true);
                add("bone", () -> Ingredient.of(Items.BONE_MEAL), FRAGMENT, true);
                add("obsidian", () -> Ingredient.of(Items.OBSIDIAN), UNIT, true);
                add("prismarine", () -> Ingredient.of(Items.PRISMARINE_SHARD), FRAGMENT, true);
                add("prismarine", () -> Ingredient.of(Items.PRISMARINE), UNIT, true);
                add("prismarine", () -> Ingredient.of(Items.PRISMARINE_BRICKS), FRAGMENT * 9, true);
                add("prismarine", () -> Ingredient.of(Items.DARK_PRISMARINE), UNIT * 2, true);
                add("netherrack", () -> Ingredient.of(Items.NETHERRACK), UNIT, true);
                add("endstone", () -> Ingredient.of(Items.END_STONE), UNIT, true);
                add("paper", () -> Ingredient.of(Items.PAPER), FRAGMENT, true);
                add("sponge", () -> Ingredient.of(Items.SPONGE), UNIT, true);
                add("firewood", () -> Ingredient.of(TLBlocks.block(TLBlocks.IntermediaryBlocks.Type.FIREWOOD)), UNIT, true);

                // Slime Crystals
                add("slime", () -> Ingredient.of(TLItems.item(TLItems.Items.Gems.Type.GREEN_SLIME_CRYSTAL)), UNIT, true);
                add("blueslime", () -> Ingredient.of(TLItems.item(TLItems.Items.Gems.Type.BLUE_SLIME_CRYSTAL)), UNIT, true);
                add("magmaslime", () -> Ingredient.of(TLItems.item(TLItems.Items.Gems.Type.MAGMA_SLIME_CRYSTAL)), UNIT, true);

                // Metals
                for (String metal : List.of("iron", "pigiron", "cobalt", "ardite", "manyullyn", "knightslime",
                                "copper", "bronze", "lead", "silver", "electrum", "steel")) {
                        boolean craftable = metal.equals("knightslime");
                        common(metal, "ingots/" + metal, UNIT, craftable);
                        common(metal, "nuggets/" + metal, NUGGET, craftable);
                        common(metal, "storage_blocks/" + metal, UNIT * 9, craftable);
                }
                add("iron", () -> Ingredient.of(Items.IRON_INGOT), UNIT, false);
                add("iron", () -> Ingredient.of(Items.IRON_NUGGET), NUGGET, false);
                add("iron", () -> Ingredient.of(Items.IRON_BLOCK), UNIT * 9, false);
                add("copper", () -> Ingredient.of(Items.COPPER_INGOT), UNIT, false);
                add("copper", () -> Ingredient.of(Items.COPPER_BLOCK), UNIT * 9, false);
                for (var metal : TLItems.Items.Ingots.Type.values()) {
                        if (metal == TLItems.Items.Ingots.Type.ALUBRASS) continue;
                        String name = metal.name().toLowerCase(java.util.Locale.ROOT);
                        boolean craftable = name.equals("knightslime");
                        add(name, () -> Ingredient.of(TLItems.item(metal)), UNIT, craftable);
                        add(name, () -> Ingredient.of(TLItems.item(TLItems.Items.Nuggets.Type.valueOf(metal.name()))), NUGGET, craftable);
                        add(name, () -> Ingredient.of(TLBlocks.block(TLBlocks.StorageBlocks.Type.valueOf(metal.name()))), UNIT * 9, craftable);
                }

                // Ranged Materials
                add("string", () -> Ingredient.of(Items.STRING), UNIT, true);
                add("vine", () -> Ingredient.of(Items.VINE), UNIT, true);
                local("slimevine_blue", "slime_vines/blue", UNIT);
                local("slimevine_purple", "slime_vines/purple", UNIT);
                add("blaze", () -> Ingredient.of(Items.BLAZE_ROD), UNIT, true);
                add("reed", () -> Ingredient.of(Items.SUGAR_CANE), UNIT, true);
                add("ice", () -> Ingredient.of(Items.PACKED_ICE), UNIT, true);
                add("endrod", () -> Ingredient.of(Items.END_ROD), UNIT, true);
                add("feather", () -> Ingredient.of(Items.FEATHER), UNIT, true);
                // Specific leaves precede the general vanilla leaves tag.
                local("slimeleaf_blue", "slime_leaves/blue", SHARD);
                local("slimeleaf_orange", "slime_leaves/orange", SHARD);
                local("slimeleaf_purple", "slime_leaves/purple", SHARD);
                add("leaf", () -> Ingredient.of(ItemTags.LEAVES), SHARD, true);

                // Legacy Repair
                add("diamond", () -> Ingredient.of(Items.DIAMOND), UNIT, false);
                add("diamond", () -> Ingredient.of(Items.DIAMOND_BLOCK), UNIT * 9, false);
        }

        private MaterialInputs() {
        }

        // Registration
        public static void add(Entry entry) {
                ENTRIES.add(entry);
        }

        private static void add(String material, Supplier<Ingredient> ingredient, int value, boolean craftable) {
                add(new Entry(ResourceLocation.fromNamespaceAndPath("tinkerslegacy", material), ingredient, value, craftable));
        }

        private static void common(String material, String path, int value, boolean craftable) {
                TagKey<Item> tag = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
                add(material, () -> Ingredient.of(tag), value, craftable);
        }

        private static void local(String material, String path, int value) {
                TagKey<Item> tag = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("tinkerslegacy", path));
                add(material, () -> Ingredient.of(tag), value, true);
        }

        // Getters
        public static boolean craftable(MaterialDefinition material) {
                return material.recipeInfo().craftable() || material.recipeInfo().castable() && allowsCastableMaterials();
        }

        public static boolean allowsCastableMaterials() {
                return TLConfig.PART_BUILDER_METAL_FALLBACK.get() || TLConfig.CRAFT_CASTABLE_MATERIALS.get();
        }

        public static Match match(ItemStack stack) {
                return match(stack, allowsCastableMaterials());
        }

        public static Match match(ItemStack stack, boolean allowCastable) {
                if (stack.isEmpty()) {
                        return null;
                }
                if (ToolRepair.isKit(stack)) {
                        ResourceLocation material = ToolRepair.kitMaterial(stack);
                        return material == null ? null : new Match(material, UNIT * 2, false);
                }
                if (stack.is(TLItems.Parts.MATERIAL_SHARD.get())) {
                        var material = TLItems.Parts.MATERIAL_SHARD.get().material(stack);
                        if (material == null || !material.hasShard()) {
                                return null;
                        }
                        boolean craftable = ENTRIES.stream().anyMatch(entry -> entry.material().equals(material.id())
                                        && (entry.craftable() && material.recipeInfo().craftable() || allowCastable && material.recipeInfo().castable()));
                        return new Match(material.id(), units(stack), craftable);
                }
                for (Entry entry : ENTRIES) {
                        if (entry.ingredient().get().test(stack)) {
                                var material = TLRegistries.MATERIALS.containsKey(entry.material())
                                                ? TLRegistries.MATERIALS.get(entry.material()) : null;
                                return material == null ? null : new Match(entry.material(), entry.value(),
                                                entry.craftable() && material.recipeInfo().craftable() || allowCastable && material.recipeInfo().castable());
                        }
                }
                return null;
        }

        public static int units(ItemStack stack) {
                return stack.getOrDefault(TLDataComponents.MATERIAL_UNITS, SHARD);
        }

        public static ItemStack remainder(ResourceLocation material, int value, ItemStack existing) {
                if (value <= 0) return existing.copy();
                var definition = TLRegistries.MATERIALS.get(material);
                if (definition == null || !definition.hasShard()) return ItemStack.EMPTY;
                int unit = SHARD;
                if (!existing.isEmpty()) {
                        if (!existing.is(TLItems.Parts.MATERIAL_SHARD.get())
                                        || !material.equals(existing.get(TLDataComponents.MATERIAL_ID))) return ItemStack.EMPTY;
                        unit = units(existing);
                        value += unit * existing.getCount();
                }
                // Preserve fractions such as prismarine/bonemeal and odd nugget remainders exactly.
                int a = unit;
                int b = value;
                while (b != 0) {
                        int next = a % b;
                        a = b;
                        b = next;
                }
                ItemStack result = TLItems.Parts.MATERIAL_SHARD.get().withMaterial(material, value / a, a);
                return result.getCount() <= result.getMaxStackSize() ? result : ItemStack.EMPTY;
        }

        public static ItemStack shard(ResourceLocation material, int count) {
                var definition = TLRegistries.MATERIALS.containsKey(material)
                                ? TLRegistries.MATERIALS.get(material) : null;
                return definition != null && definition.hasShard() && count > 0
                                ? TLItems.Parts.MATERIAL_SHARD.get().withMaterial(material, count) : ItemStack.EMPTY;
        }
}
