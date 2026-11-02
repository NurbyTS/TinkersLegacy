package com.nurby.tinkerslegacy.api.material.stat;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record PartStatDefinition(
        ResourceLocation id,
        List<StatField> fields
) {
    public PartStatDefinition {
        fields = List.copyOf(fields);
    }
}