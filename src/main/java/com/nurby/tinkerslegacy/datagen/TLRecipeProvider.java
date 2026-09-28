package com.nurby.tinkerslegacy.datagen;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.registry.TLItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceLocation;
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
        metal(output,
                TLItems.Items.Nuggets.COBALT_NUGGET.get(),
                TLItems.Items.Ingots.COBALT_INGOT.get(),
                TLItems.Blocks.StorageBlocks.STORAGE_BLOCK_COBALT.get());

        metal(output,
                TLItems.Items.Nuggets.ARDITE_NUGGET.get(),
                TLItems.Items.Ingots.ARDITE_INGOT.get(),
                TLItems.Blocks.StorageBlocks.STORAGE_BLOCK_ARDITE.get());

        metal(output,
                TLItems.Items.Nuggets.ALUBRASS_NUGGET.get(),
                TLItems.Items.Ingots.ALUBRASS_INGOT.get(),
                TLItems.Blocks.StorageBlocks.STORAGE_BLOCK_ALUBRASS.get());

        metal(output,
                TLItems.Items.Nuggets.KNIGHTSLIME_NUGGET.get(),
                TLItems.Items.Ingots.KNIGHTSLIME_INGOT.get(),
                TLItems.Blocks.StorageBlocks.STORAGE_BLOCK_KNIGHTSLIME.get());

        metal(output,
                TLItems.Items.Nuggets.MANYULLYN_NUGGET.get(),
                TLItems.Items.Ingots.MANYULLYN_INGOT.get(),
                TLItems.Blocks.StorageBlocks.STORAGE_BLOCK_MANYULLYN.get());

        metal(output,
                TLItems.Items.Nuggets.PIGIRON_NUGGET.get(),
                TLItems.Items.Ingots.PIGIRON_INGOT.get(),
                TLItems.Blocks.StorageBlocks.STORAGE_BLOCK_PIGIRON.get());

        gem(output,
                TLItems.Items.Gems.SILKY_JEWEL.get(),
                TLItems.Blocks.StorageBlocks.STORAGE_BLOCK_SILKY_JEWEL.get());
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

        return ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, resultName + "_from_" + ingredientName);
    }
}
