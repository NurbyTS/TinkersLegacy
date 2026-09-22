package com.nurby.tinkerslegacy.library.part;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;

public final class PartDefinition {
    private final ResourceLocation id;
    private final int castingCost;
    private final List<ResourceLocation> statTypes;

    public PartDefinition(ResourceLocation id, int castingCost, ResourceLocation... statTypes) {
        this.id = id;
        this.castingCost = castingCost;
        this.statTypes = List.of(statTypes);
    }

    public ResourceLocation id() {
        return id;
    }

    public int castingCost() {
        return castingCost;
    }

    public List<ResourceLocation> statTypes() {
        return statTypes;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || obj.getClass() != this.getClass())
            return false;
        var that = (PartDefinition) obj;
        return Objects.equals(this.id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, castingCost, statTypes);
    }

    @Override
    public String toString() {
        return "PartDefinition[" + "id=" + id + ", " + "castingCost=" + castingCost + ", " + "statTypes=" + statTypes + ']';
    }
}