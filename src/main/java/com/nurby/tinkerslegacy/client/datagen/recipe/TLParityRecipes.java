package com.nurby.tinkerslegacy.client.datagen.recipe;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.registry.TLBlocks;
import com.nurby.tinkerslegacy.registry.TLItems;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import java.util.List;
import java.util.Locale;

public final class TLParityRecipes {
        private TLParityRecipes() {
        }

        // Recipes
        public static void buildRecipes(RecipeOutput output) {
                slime(output, TLBlocks.SlimyBlocks.SlimeType.GREEN, Items.SLIME_BALL);
                for (var type : TLItems.Items.Slimeballs.Type.values()) {
                        slime(output, TLBlocks.SlimyBlocks.SlimeType.valueOf(type.name()), TLItems.item(type));
                }
                seared(output);
        }

        // Slime
        private static void slime(RecipeOutput output, TLBlocks.SlimyBlocks.SlimeType type, ItemLike slimeball) {
                String name = type.name().toLowerCase(Locale.ROOT);
                TagKey<Item> tag = TagKey.create(Registries.ITEM,
                                ResourceLocation.fromNamespaceAndPath("c", "slime_balls/" + name));
                var congealed = TLBlocks.SlimyBlocks.congealed(type).get();
                compression(output, slimeball, tag, congealed, 2);
                if (type != TLBlocks.SlimyBlocks.SlimeType.GREEN) {
                        // Vanilla already provides both green slime-block recipes.
                        compression(output, slimeball, tag, TLBlocks.SlimyBlocks.slime(type).get(), 3);
                }
        }

        private static void compression(RecipeOutput output, ItemLike slimeball, TagKey<Item> tag,
                        ItemLike block, int width) {
                var recipe = ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, block)
                                .define('#', tag)
                                .group(TinkersLegacy.MODID + (width == 2 ? ":congealed_slime_blocks" : ":slime_blocks"))
                                .unlockedBy("has_slimeball", InventoryChangeTrigger.TriggerInstance.hasItems(slimeball));
                for (int row = 0; row < width; row++) recipe.pattern("#".repeat(width));
                recipe.save(output, conversionId(block, slimeball));
                ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, slimeball, width * width)
                                .requires(block)
                                .group(TinkersLegacy.MODID + ":slime_balls")
                                .unlockedBy("has_slime_block", InventoryChangeTrigger.TriggerInstance.hasItems(block))
                                .save(output, conversionId(slimeball, block));
        }

        // Seared Blocks
        private static void seared(RecipeOutput output) {
                // Original one-block decorative cycle; Seared Stone also enters through Paver.
                List<TLBlocks.SearedBlocks.Type> cycle = List.of(
                                TLBlocks.SearedBlocks.Type.PAVER,
                                TLBlocks.SearedBlocks.Type.BRICKS,
                                TLBlocks.SearedBlocks.Type.BRICK_FANCY,
                                TLBlocks.SearedBlocks.Type.BRICK_SQUARE,
                                TLBlocks.SearedBlocks.Type.BRICK_TRIANGLE,
                                TLBlocks.SearedBlocks.Type.CREEPER,
                                TLBlocks.SearedBlocks.Type.BRICK_SMALL,
                                TLBlocks.SearedBlocks.Type.TILE,
                                TLBlocks.SearedBlocks.Type.ROAD);
                for (int index = 0; index < cycle.size(); index++) {
                        searedConversion(output, cycle.get(index), cycle.get((index + 1) % cycle.size()));
                }
                searedConversion(output, TLBlocks.SearedBlocks.Type.STONE, TLBlocks.SearedBlocks.Type.PAVER);

                var bricks = TLBlocks.block(TLBlocks.SearedBlocks.Type.BRICKS);
                var cracked = TLBlocks.block(TLBlocks.SearedBlocks.Type.BRICK_CRACKED);
                SimpleCookingRecipeBuilder.smelting(Ingredient.of(bricks), RecipeCategory.BUILDING_BLOCKS,
                                cracked, 0.1F, 200)
                                .unlockedBy("has_seared_bricks", InventoryChangeTrigger.TriggerInstance.hasItems(bricks))
                                .save(output, conversionId(cracked, bricks).withSuffix("_smelting"));
        }

        private static void searedConversion(RecipeOutput output,
                        TLBlocks.SearedBlocks.Type input, TLBlocks.SearedBlocks.Type result) {
                var ingredient = TLBlocks.block(input);
                var block = TLBlocks.block(result);
                ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, block)
                                .requires(ingredient)
                                .group(TinkersLegacy.MODID + ":seared_blocks")
                                .unlockedBy("has_seared_block", InventoryChangeTrigger.TriggerInstance.hasItems(ingredient))
                                .save(output, conversionId(block, ingredient));
        }

        // Names
        private static ResourceLocation conversionId(ItemLike result, ItemLike ingredient) {
                return ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID,
                                BuiltInRegistries.ITEM.getKey(result.asItem()).getPath() + "_from_"
                                                + BuiltInRegistries.ITEM.getKey(ingredient.asItem()).getPath());
        }
}
