package com.nurby.tinkerslegacy.library.material.recipe;

import net.minecraft.resources.ResourceLocation;

public record CastingRecipeInfo(
                ResourceLocation fluidLocation,
                boolean craftable) implements MaterialRecipeInfo {

        public CastingRecipeInfo(ResourceLocation fluidLocation) {
                this(fluidLocation, false);
        }

        @Override
        public boolean castable() {
                return true;
        }
}
