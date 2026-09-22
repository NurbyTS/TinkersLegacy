package com.nurby.tinkerslegacy.library.tool;

import net.minecraft.resources.ResourceLocation;

public record ToolPart(
        ResourceLocation part,
        ResourceLocation statType
) {
}