package com.nurby.tinkerslegacy.library.part;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class PartDefinition {
    private final ResourceLocation id;
    private final int castingCost;
    private final List<ResourceLocation> statTypes;
    private final List<String> textureNames;
    private final List<ResourceLocation> defaultSlotMaterials; // null = variable slot, ResourceLocation = fixed default

    public PartDefinition(
            ResourceLocation id,
            int castingCost,
            List<String> textureNames,
            List<ResourceLocation> defaultSlotMaterials,
            ResourceLocation... statTypes
    ) {
        this.id = id;
        this.castingCost = castingCost;
        this.statTypes = List.of(statTypes);
        this.textureNames = (textureNames == null || textureNames.isEmpty()) ? List.of(id.getPath()) : List.copyOf(textureNames);

        if (defaultSlotMaterials == null || defaultSlotMaterials.isEmpty()) {
            this.defaultSlotMaterials = Collections.nCopies(this.textureNames.size(), null);
        } else {
            // Replaced List.copyOf with unmodifiable ArrayList to permit null elements
            this.defaultSlotMaterials = Collections.unmodifiableList(new ArrayList<>(defaultSlotMaterials));
        }
    }

    public PartDefinition(ResourceLocation id, int castingCost, ResourceLocation... statTypes) {
        this(id, castingCost, List.of(id.getPath()), null, statTypes);
    }

    public ResourceLocation id() { return id; }
    public int castingCost() { return castingCost; }
    public List<ResourceLocation> statTypes() { return statTypes; }
    public List<String> textureNames() { return textureNames; }
    public List<ResourceLocation> defaultSlotMaterials() { return defaultSlotMaterials; }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (PartDefinition) obj;
        return Objects.equals(this.id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, castingCost, statTypes, textureNames, defaultSlotMaterials);
    }
}