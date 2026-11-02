package com.nurby.tinkerslegacy.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLRecipeSerializers;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;

public final class StationRecipe extends ShapedRecipe {
        private final ShapedRecipe recipe;
        private final Ingredient appearance;

        public StationRecipe(ShapedRecipe recipe, Ingredient appearance) {
                super(recipe.getGroup(), recipe.category(), recipe.pattern, recipe.getResultItem(null), recipe.showNotification());
                this.recipe = recipe;
                this.appearance = appearance;
        }

        // Crafting
        @Override
        public ItemStack getResultItem(HolderLookup.Provider registries) {
                ItemStack result = super.getResultItem(registries).copy();
                // Resolve only when requested, after registry/tag loading; constructors stay deferred.
                ItemStack[] choices = appearance.getItems();
                if (choices.length == 1 && choices[0].getItem() instanceof BlockItem block) {
                        result.set(TLDataComponents.STATION_BASE, BuiltInRegistries.BLOCK.getKey(block.getBlock()));
                }
                return result;
        }

        @Override
        public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
                ItemStack result = super.assemble(input, registries);
                for (ItemStack stack : input.items()) {
                        if (appearance.test(stack) && stack.getItem() instanceof BlockItem block) {
                                result.set(TLDataComponents.STATION_BASE, BuiltInRegistries.BLOCK.getKey(block.getBlock()));
                                break;
                        }
                }
                return result;
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
                return TLRecipeSerializers.STATION.get();
        }

        // Serialization
        public static final class Serializer implements RecipeSerializer<StationRecipe> {
                private static final MapCodec<StationRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                                ShapedRecipe.Serializer.CODEC.forGetter(value -> value.recipe),
                                Ingredient.CODEC_NONEMPTY.fieldOf("appearance").forGetter(value -> value.appearance))
                                .apply(instance, StationRecipe::new));
                private static final StreamCodec<RegistryFriendlyByteBuf, StationRecipe> STREAM_CODEC = StreamCodec.of(
                                (buffer, value) -> {
                                        ShapedRecipe.Serializer.STREAM_CODEC.encode(buffer, value.recipe);
                                        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, value.appearance);
                                }, buffer -> new StationRecipe(ShapedRecipe.Serializer.STREAM_CODEC.decode(buffer),
                                                Ingredient.CONTENTS_STREAM_CODEC.decode(buffer)));

                @Override
                public MapCodec<StationRecipe> codec() {
                        return CODEC;
                }

                @Override
                public StreamCodec<RegistryFriendlyByteBuf, StationRecipe> streamCodec() {
                        return STREAM_CODEC;
                }
        }
}
