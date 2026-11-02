package com.nurby.tinkerslegacy.api.station;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record StationRequirement(
                ResourceLocation family,
                int tier) {

        public StationRequirement {
                Objects.requireNonNull(family);

                if (tier < 1) {
                        throw new IllegalArgumentException("Station tier must be positive");
                }
        }
}
