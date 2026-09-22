package com.nurby.tinkerslegacy.library.material.stat;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record PartStatDefinition(
        ResourceLocation id,
        List<StatField> fields
) {
    public PartStatDefinition {
        fields = List.copyOf(fields);
    }
}