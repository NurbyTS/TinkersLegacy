package com.nurby.tinkerslegacy.api.trait;

import com.nurby.tinkerslegacy.registry.TLRegistries;
import net.minecraft.resources.ResourceLocation;

public record TraitInstance(ResourceLocation id, int level, int amount) {
    public TraitDefinition getDefinition() {
        return TLRegistries.TRAITS.get(id);
    }
}