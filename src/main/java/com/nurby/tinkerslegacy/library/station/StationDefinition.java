package com.nurby.tinkerslegacy.library.station;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;

public record StationDefinition(
                ResourceLocation id,
                ResourceLocation family,
                int tier,
                int inputSlots,
                List<ResourceLocation> operations) {

        public StationDefinition {
                Objects.requireNonNull(id);
                Objects.requireNonNull(family);
                operations = List.copyOf(operations);
                if (tier < 1 || inputSlots < 1 || inputSlots > 18 || operations.isEmpty()
                                || operations.stream().distinct().count() != operations.size()) {
                        throw new IllegalArgumentException("Invalid station definition: " + id);
                }
        }

        public boolean supports(ResourceLocation operation) {
                return operations.contains(operation);
        }
}
