package com.nurby.tinkerslegacy.api.tool.traits;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record ToolTraitData(Map<String, Double> values) {
        public static final ToolTraitData EMPTY = new ToolTraitData(Map.of());
        public static final Codec<ToolTraitData> CODEC = Codec.unboundedMap(Codec.STRING, Codec.DOUBLE)
                        .xmap(ToolTraitData::new, ToolTraitData::values);
        public static final StreamCodec<ByteBuf, ToolTraitData> STREAM_CODEC = ByteBufCodecs.<ByteBuf, String, Double, Map<String, Double>>map(
                        LinkedHashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.DOUBLE)
                        .map(ToolTraitData::new, ToolTraitData::values);

        public ToolTraitData {
                values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
                if (values.values().stream().anyMatch(value -> !Double.isFinite(value))) {
                        throw new IllegalArgumentException("Trait progress must be finite");
                }
        }

        // Getters
        public double get(String name) {
                return values.getOrDefault(name, 0.0);
        }

        public boolean contains(String name) {
                return values.containsKey(name);
        }

        // Updates
        public ToolTraitData with(String name, double value) {
                Map<String, Double> updated = new LinkedHashMap<>(values);
                updated.put(name, value);
                return new ToolTraitData(updated);
        }
}
