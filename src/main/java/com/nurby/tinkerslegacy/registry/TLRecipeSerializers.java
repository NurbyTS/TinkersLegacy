package com.nurby.tinkerslegacy.registry;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.recipe.ToolRepairRecipe;
import com.nurby.tinkerslegacy.recipe.StationRecipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class TLRecipeSerializers {
        public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(
                        Registries.RECIPE_SERIALIZER, TinkersLegacy.MODID);

        public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<ToolRepairRecipe>> TOOL_REPAIR = SERIALIZERS.register(
                        "tool_repair", () -> new SimpleCraftingRecipeSerializer<>(ToolRepairRecipe::new));

        public static final DeferredHolder<RecipeSerializer<?>, StationRecipe.Serializer> STATION = SERIALIZERS.register(
                        "station", StationRecipe.Serializer::new);

        private TLRecipeSerializers() {
        }

        // Registration
        public static void register(IEventBus modBus) {
                SERIALIZERS.register(modBus);
        }
}
