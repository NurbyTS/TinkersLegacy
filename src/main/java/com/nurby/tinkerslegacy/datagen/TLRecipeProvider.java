package com.nurby.tinkerslegacy.datagen;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.registry.TLItems;
import com.nurby.tinkerslegacy.registry.TLBlocks;
import com.nurby.tinkerslegacy.registry.TLMetalFamilies;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import java.util.concurrent.CompletableFuture;

public class TLRecipeProvider extends RecipeProvider {
        public TLRecipeProvider(
                        PackOutput output,
                        CompletableFuture<HolderLookup.Provider> lookupProvider) {
                super(output, lookupProvider);
        }

        @Override
        protected void buildRecipes(RecipeOutput output) {
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
