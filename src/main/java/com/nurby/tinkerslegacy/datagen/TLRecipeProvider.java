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

                        for (var input : family.smeltingInputs()) {
                                smeltingAndBlasting(output, input.get(), family.ingot().get(), 0.7F, 200);
                        }
                }

                gem(
                                output,
                                TLItems.item(TLItems.Items.Gems.Type.SILKY_JEWEL),
                                TLBlocks.block(TLBlocks.StorageBlocks.Type.SILKY_JEWEL));

                smelting(
                                output,
                                "seared_brick",
                                TLBlocks.block(TLBlocks.IntermediaryBlocks.Type.GROUT),
                                TLItems.item(TLItems.Items.Bricks.Type.SEARED),
                                0.1F,
                                200);
        }

        private void smelting(
                        RecipeOutput output,
                        String recipeName,
                        ItemLike input,
                        ItemLike result,
                        float experience,
                        int cookingTime) {
                SimpleCookingRecipeBuilder.smelting(
                                Ingredient.of(input),
                                RecipeCategory.MISC,
                                result,
                                experience,
                                cookingTime)
                                .unlockedBy("has_ingredient", has(input))
                                .save(output, ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, recipeName));
        }

        private void blasting(
                        RecipeOutput output,
                        String recipeName,
                        ItemLike input,
                        ItemLike result,
                        float experience,
                        int cookingTime) {
                SimpleCookingRecipeBuilder.blasting(
                                Ingredient.of(input),
                                RecipeCategory.MISC,
                                result, experience,
                                cookingTime)
                                .unlockedBy("has_ingredient", has(input))
                                .save(output, ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, recipeName));
        }

        private void smeltingAndBlasting(
                        RecipeOutput output,
                        ItemLike input,
                        ItemLike result,
                        float experience,
                        int cookingTime) {
                String baseName = conversionId(result, input).getPath();

                smelting(output, baseName + "_smelting", input, result, experience, cookingTime);
                blasting(output, baseName + "_blasting", input, result, experience, cookingTime / 2);
        }

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
