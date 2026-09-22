package com.nurby.tinkerslegacy.library.material.recipe;

import net.minecraft.resources.ResourceLocation;

public record CastingRecipeInfo(
        ResourceLocation fluidLocation
) implements MaterialRecipeInfo {}