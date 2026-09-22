package com.nurby.tinkerslegacy.library.material.recipe;

import net.minecraft.resources.ResourceLocation;

public record CoatingRecipeInfo(
        ResourceLocation materialId
) implements MaterialRecipeInfo {
}