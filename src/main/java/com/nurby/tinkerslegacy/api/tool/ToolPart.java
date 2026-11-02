package com.nurby.tinkerslegacy.api.tool;

import net.minecraft.resources.ResourceLocation;

public record ToolPart(
        ResourceLocation part,
        ResourceLocation statType
) {
}