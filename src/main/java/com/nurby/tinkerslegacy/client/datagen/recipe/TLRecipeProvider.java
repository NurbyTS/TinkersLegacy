package com.nurby.tinkerslegacy.client.datagen.recipe;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.registry.TLBlocks;
import com.nurby.tinkerslegacy.registry.TLItems;
import com.nurby.tinkerslegacy.registry.TLMetalFamilies;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.Tags;

import java.util.concurrent.CompletableFuture;

public class TLRecipeProvider extends RecipeProvider {
        public TLRecipeProvider(
                        PackOutput output,
                        CompletableFuture<HolderLookup.Provider> lookupProvider) {
                super(output, lookupProvider);
        }

        @Override
        protected void buildRecipes(RecipeOutput output) {
                stations(output);
                materials(output);
                slimyMaterials(output);
                metalFallbacks(output);
                TLParityRecipes.buildRecipes(output);

                for (var family : TLMetalFamilies.ALL) {
                        metal(output, family.nugget().get(), family.ingot().get(), family.storageBlock().get());
                }

                gem(
                                output,
                                TLItems.item(TLItems.Items.Gems.Type.SILKY_JEWEL),
                                TLBlocks.block(TLBlocks.StorageBlocks.Type.SILKY_JEWEL));

                for (var recipe : TLCookingRecipes.ALL) {
                        cooking(output, recipe);
                }
        }

        // Materials
        private void materials(RecipeOutput output) {
                var graveyard = TLBlocks.block(TLBlocks.IntermediaryBlocks.Type.GRAVEYARD_SOIL);
                var consecrated = TLBlocks.block(TLBlocks.IntermediaryBlocks.Type.CONSECRATED_SOIL);
                var lavawood = TLBlocks.block(TLBlocks.IntermediaryBlocks.Type.LAVAWOOD);
                var firewood = TLBlocks.block(TLBlocks.IntermediaryBlocks.Type.FIREWOOD);
                var reinforcement = TLItems.item(TLItems.Items.Miscellaneous.Type.REINFORCED_PLATE);

                ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, graveyard)
                                .requires(Items.DIRT).requires(Items.ROTTEN_FLESH).requires(Items.BONE_MEAL)
                                .unlockedBy("has_rotten_flesh", has(Items.ROTTEN_FLESH))
                                .save(output);
                SimpleCookingRecipeBuilder.smelting(Ingredient.of(graveyard), RecipeCategory.BUILDING_BLOCKS,
                                consecrated, 0.1F, 200)
                                .unlockedBy("has_graveyard_soil", has(graveyard))
                                .save(output, ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, "consecrated_soil"));

                // Original recipes used while the Smeltery is unavailable.
                ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, lavawood)
                                .pattern(" P ").pattern("PLP").pattern(" P ")
                                .define('P', ItemTags.PLANKS).define('L', Items.LAVA_BUCKET)
                                .unlockedBy("has_lava", has(Items.LAVA_BUCKET))
                                .save(output);
                ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, firewood)
                                .requires(lavawood).requires(Items.BLAZE_POWDER, 2)
                                .unlockedBy("has_lavawood", has(lavawood))
                                .save(output);
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, reinforcement)
                                .pattern("OOO").pattern("OGO").pattern("OOO")
                                .define('O', Items.OBSIDIAN).define('G', Items.GOLD_INGOT)
                                .unlockedBy("has_obsidian", has(Items.OBSIDIAN))
                                .save(output);
        }

        // Slime Materials
        private void slimyMaterials(RecipeOutput output) {
                var blue = TLItems.item(TLItems.Items.Slimeballs.Type.BLUE);
                var purple = TLItems.item(TLItems.Items.Slimeballs.Type.PURPLE);
                var magma = TLItems.item(TLItems.Items.Slimeballs.Type.MAGMA);
                slimyMud(output, TLBlocks.IntermediaryBlocks.Type.GREEN_SLIMY_MUD, Items.SLIME_BALL,
                                TLBlocks.SlimyBlocks.congealed(TLBlocks.SlimyBlocks.SlimeType.GREEN).get(),
                                TLItems.Items.Gems.Type.GREEN_SLIME_CRYSTAL);
                slimyMud(output, TLBlocks.IntermediaryBlocks.Type.BLUE_SLIMY_MUD, blue,
                                TLBlocks.SlimyBlocks.congealed(TLBlocks.SlimyBlocks.SlimeType.BLUE).get(),
                                TLItems.Items.Gems.Type.BLUE_SLIME_CRYSTAL);
                var magmaMud = TLBlocks.block(TLBlocks.IntermediaryBlocks.Type.MAGMA_SLIMY_MUD);
                ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, magmaMud)
                                .requires(magma, 2).requires(Items.MAGMA_CREAM, 2)
                                .requires(Items.SOUL_SAND).requires(Items.NETHERRACK)
                                .unlockedBy("has_magma_slime", has(magma)).save(output);
                slimeCrystal(output, magmaMud, TLItems.Items.Gems.Type.MAGMA_SLIME_CRYSTAL);

                // Original recipes used while slime islands are unavailable.
                ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, blue)
                                .requires(Items.SLIME_BALL).requires(Tags.Items.DYES_BLUE)
                                .unlockedBy("has_slimeball", has(Items.SLIME_BALL)).save(output);
                ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, purple)
                                .requires(Items.SLIME_BALL).requires(Tags.Items.DYES_PURPLE)
                                .unlockedBy("has_slimeball", has(Items.SLIME_BALL)).save(output);
                ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, magma)
                                .requires(Items.SLIME_BALL).requires(Tags.Items.DYES_ORANGE)
                                .unlockedBy("has_slimeball", has(Items.SLIME_BALL)).save(output);

                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TLItems.item(TLItems.Items.Miscellaneous.Type.EXPANDER_VERTICAL))
                                .pattern(" B ").pattern("ACA").pattern(" B ")
                                .define('A', Tags.Items.GEMS_LAPIS).define('B', Items.PISTON).define('C', purple)
                                .unlockedBy("has_purple_slime", has(purple)).save(output);
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TLItems.item(TLItems.Items.Miscellaneous.Type.EXPANDER_HORIZONTAL))
                                .pattern(" A ").pattern("BCB").pattern(" A ")
                                .define('A', Tags.Items.GEMS_LAPIS).define('B', Items.PISTON).define('C', purple)
                                .unlockedBy("has_purple_slime", has(purple)).save(output);
        }

        private void slimyMud(RecipeOutput output, TLBlocks.IntermediaryBlocks.Type type,
                        ItemLike slimeball, ItemLike congealed, TLItems.Items.Gems.Type crystal) {
                var mud = TLBlocks.block(type);
                ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, mud)
                                .requires(slimeball, 4).requires(Tags.Items.SANDS).requires(ItemTags.DIRT)
                                .group("tinkerslegacy:slimy_mud")
                                .unlockedBy("has_slimeball", has(slimeball)).save(output);
                ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, mud)
                                .requires(congealed).requires(Tags.Items.SANDS).requires(ItemTags.DIRT)
                                .group("tinkerslegacy:slimy_mud")
                                .unlockedBy("has_congealed_slime", has(congealed)).save(output, conversionId(mud, congealed));
                slimeCrystal(output, mud, crystal);
        }

        private void slimeCrystal(RecipeOutput output, ItemLike mud, TLItems.Items.Gems.Type crystal) {
                var result = TLItems.item(crystal);
                SimpleCookingRecipeBuilder.smelting(Ingredient.of(mud), RecipeCategory.MISC, result, 0.75F, 200)
                                .unlockedBy("has_slimy_mud", has(mud)).save(output, conversionId(result, mud));
        }

        // Metal Fallbacks
        private void metalFallbacks(RecipeOutput output) {
                var purple = TLItems.item(TLItems.Items.Slimeballs.Type.PURPLE);
                var knightslime = TLItems.item(TLItems.Items.Ingots.Type.KNIGHTSLIME);
                var manyullyn = TLItems.item(TLItems.Items.Ingots.Type.MANYULLYN);
                var nugget = TLItems.item(TLItems.Items.Nuggets.Type.MANYULLYN);
                var pigiron = TLItems.item(TLItems.Items.Ingots.Type.PIGIRON);
                ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, knightslime)
                                .requires(purple).requires(Tags.Items.INGOTS_IRON).requires(Tags.Items.STONES)
                                .group("tinkerslegacy:knightslime_ingot")
                                .unlockedBy("has_purple_slime", has(purple))
                                .save(output, ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, "knightslime_ingot_fallback"));
                ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, knightslime)
                                .requires(Items.SLIME_BALL).requires(Tags.Items.DYES_PURPLE)
                                .requires(Tags.Items.INGOTS_IRON).requires(Tags.Items.STONES)
                                .group("tinkerslegacy:knightslime_ingot")
                                .unlockedBy("has_slimeball", has(Items.SLIME_BALL))
                                .save(output, ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, "knightslime_ingot_fallback_without_islands"));
                ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, manyullyn)
                                .requires(TLItems.item(TLItems.Items.Ingots.Type.COBALT))
                                .requires(TLItems.item(TLItems.Items.Ingots.Type.ARDITE)).requires(Items.COAL_BLOCK)
                                .unlockedBy("has_cobalt", has(TLItems.item(TLItems.Items.Ingots.Type.COBALT)))
                                .save(output, ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, "manyullyn_ingot_fallback"));
                ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, nugget)
                                .requires(TLItems.item(TLItems.Items.Nuggets.Type.COBALT))
                                .requires(TLItems.item(TLItems.Items.Nuggets.Type.ARDITE)).requires(Items.COAL)
                                .unlockedBy("has_cobalt", has(TLItems.item(TLItems.Items.Nuggets.Type.COBALT)))
                                .save(output, ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, "manyullyn_nugget_fallback"));
                ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, pigiron, 3)
                                .requires(purple).requires(Ingredient.of(Tags.Items.INGOTS_IRON), 3)
                                .requires(Items.PORKCHOP, 3).requires(Tags.Items.GEMS_EMERALD)
                                .unlockedBy("has_purple_slime", has(purple))
                                .save(output, ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, "pigiron_ingot_fallback"));
        }

        // Workstations
        private void stations(RecipeOutput output) {
                var pattern = TLItems.item(TLItems.Items.IntermediaryItems.Type.BLANK_PATTERN);
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, pattern, 4)
                                .pattern("PS").pattern("SP")
                                .define('P', ItemTags.PLANKS).define('S', Items.STICK)
                                .unlockedBy("has_planks", has(ItemTags.PLANKS))
                                .save(output);

                station(output, TLBlocks.Workstations.Type.STENCIL_TABLE, Ingredient.of(ItemTags.PLANKS));
                station(output, TLBlocks.Workstations.Type.PART_BUILDER, Ingredient.of(ItemTags.LOGS));
                station(output, TLBlocks.Workstations.Type.TOOL_STATION, Ingredient.of(Items.CRAFTING_TABLE));

                forge(output, Items.IRON_BLOCK);
                forge(output, Items.GOLD_BLOCK);
                for (var family : TLMetalFamilies.ALL) {
                        forge(output, family.storageBlock().get());
                }

                ShapelessRecipeBuilder.shapeless(
                                RecipeCategory.DECORATIONS,
                                TLBlocks.block(TLBlocks.Workstations.Type.CRAFTING_STATION))
                                .requires(Items.CRAFTING_TABLE)
                                .unlockedBy("has_crafting_table", has(Items.CRAFTING_TABLE))
                                .save(output);
        }

        private void station(RecipeOutput output, TLBlocks.Workstations.Type type, Ingredient base) {
                var pattern = TLItems.item(TLItems.Items.IntermediaryItems.Type.BLANK_PATTERN);
                ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, TLBlocks.block(type))
                                .pattern("P").pattern("B")
                                .define('P', pattern).define('B', base)
                                .unlockedBy("has_pattern", has(pattern))
                                .save(type == TLBlocks.Workstations.Type.TOOL_STATION ? output : stationOutput(output, base));
        }

        private void forge(RecipeOutput output, ItemLike metal) {
                var station = TLBlocks.block(TLBlocks.Workstations.Type.TOOL_STATION);
                var forge = TLBlocks.block(TLBlocks.Workstations.Type.TOOL_FORGE);
                ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, forge)
                                .pattern("BBB").pattern("MTM").pattern("M M")
                                .define('B', TLBlocks.block(TLBlocks.SearedBlocks.Type.BRICKS))
                                .define('M', metal).define('T', station)
                                .unlockedBy("has_tool_station", has(station))
                                .save(stationOutput(output, Ingredient.of(metal)), conversionId(forge, metal));
        }

        private RecipeOutput stationOutput(RecipeOutput output, Ingredient appearance) {
                return new RecipeOutput() {
                        @Override
                        public net.minecraft.advancements.Advancement.Builder advancement() {
                                return output.advancement();
                        }

                        @Override
                        public void accept(ResourceLocation id, net.minecraft.world.item.crafting.Recipe<?> recipe,
                                        net.minecraft.advancements.AdvancementHolder advancement,
                                        net.neoforged.neoforge.common.conditions.ICondition... conditions) {
                                output.accept(id, new com.nurby.tinkerslegacy.recipe.StationRecipe(
                                                (net.minecraft.world.item.crafting.ShapedRecipe) recipe, appearance),
                                                advancement, conditions);
                        }
                };
        }

        // Cooking
        private void cooking(
                        RecipeOutput output,
                        TLCookingRecipes.CookingRecipe recipe) {
                ItemLike input = recipe.input().get();
                ItemLike result = recipe.result().get();
                String baseName = conversionId(result, input).getPath();

                for (var method : recipe.methods()) {
                        String name = method.recipeName() == null
                                        ? baseName + "_" + method.type().suffix()
                                        : method.recipeName();

                        method.type().builder(
                                        Ingredient.of(input),
                                        recipe.category(),
                                        result,
                                        recipe.experience(),
                                        method.cookingTime())
                                        .unlockedBy("has_ingredient", has(input))
                                        .save(output, ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, name));
                }
        }

        // Crafting
        private void metal(
                        RecipeOutput output,
                        ItemLike nugget,
                        ItemLike ingot,
                        ItemLike block) {
                packing(output, nugget, ingot, RecipeCategory.MISC);
                packing(output, ingot, block, RecipeCategory.BUILDING_BLOCKS);
        }

        private void gem(
                        RecipeOutput output,
                        ItemLike gem,
                        ItemLike block) {
                packing(output, gem, block, RecipeCategory.BUILDING_BLOCKS);
        }

        // Generates bidirectional recipes
        private void packing(
                        RecipeOutput output,
                        ItemLike small,
                        ItemLike packed,
                        RecipeCategory packedCategory) {
                ShapedRecipeBuilder.shaped(packedCategory, packed)
                                .pattern("###")
                                .pattern("###")
                                .pattern("###")
                                .define('#', small)
                                .unlockedBy("has_ingredient", has(small))
                                .save(output, conversionId(packed, small));

                ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, small, 9)
                                .requires(packed)
                                .unlockedBy("has_ingredient", has(packed))
                                .save(output, conversionId(small, packed));
        }

        private ResourceLocation conversionId(
                        ItemLike result,
                        ItemLike ingredient) {
                String resultName = BuiltInRegistries.ITEM.getKey(result.asItem()).getPath();
                String ingredientName = BuiltInRegistries.ITEM.getKey(ingredient.asItem()).getPath();

                return ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID,
                                resultName + "_from_" + ingredientName);
        }
}
