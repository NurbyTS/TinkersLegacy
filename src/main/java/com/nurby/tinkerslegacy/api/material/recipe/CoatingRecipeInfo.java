package com.nurby.tinkerslegacy.api.material.recipe;

import net.minecraft.resources.ResourceLocation;

public record CoatingRecipeInfo(ResourceLocation fluidId,
        ResourceLocation materialId
) implements MaterialRecipeInfo {
}