package com.nurby.tinkerslegacy.library.tool;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;

public record ToolPartRequirement(
                ResourceLocation part,
                List<ResourceLocation> statTypes) {

        public ToolPartRequirement {
                Objects.requireNonNull(part);
                statTypes = List.copyOf(statTypes);

                if (statTypes.isEmpty() || statTypes.stream().distinct().count() != statTypes.size()) {
                        throw new IllegalArgumentException("A physical part requires distinct material stat roles");
                }
        }
}
