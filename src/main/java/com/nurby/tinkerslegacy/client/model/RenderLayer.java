package com.nurby.tinkerslegacy.client.model;

import net.minecraft.resources.ResourceLocation;

public record RenderLayer(ResourceLocation texture, int priority) implements Comparable<RenderLayer> {
    @Override
    public int compareTo(RenderLayer o) {
        return Integer.compare(this.priority, o.priority);
    }
}