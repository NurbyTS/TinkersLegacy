package com.nurby.tinkerslegacy.registry;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.library.station.StationDefinition;
import com.nurby.tinkerslegacy.library.station.StationOperations;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TLStations {
        private static final Map<ResourceLocation, StationDefinition> DEFINITIONS = new LinkedHashMap<>();

        public static final StationDefinition CRAFTING_STATION = add(new StationDefinition(
                        id("crafting_station"), StationOperations.CRAFTING_FAMILY, 1, 9,
                        List.of(StationOperations.CRAFT)));
                        
        public static final StationDefinition STENCIL_TABLE = add(new StationDefinition(
                        id("stencil_table"), StationOperations.STENCIL_FAMILY, 1, 1,
                        List.of(StationOperations.CUT_PATTERN)));

        public static final StationDefinition PART_BUILDER = add(new StationDefinition(
                        id("part_builder"), StationOperations.PART_FAMILY, 1, 4,
                        List.of(StationOperations.BUILD_PART)));

        public static final StationDefinition TOOL_STATION = add(new StationDefinition(
                        id("tool_station"), StationOperations.TOOL_FAMILY, 1, 6,
                        List.of(StationOperations.MAINTAIN, StationOperations.ASSEMBLE, StationOperations.REPAIR,
                                        StationOperations.REPLACE, StationOperations.MODIFY, StationOperations.RENAME)));

        public static final StationDefinition TOOL_FORGE = add(new StationDefinition(
                        id("tool_forge"), StationOperations.TOOL_FAMILY, 2, 6,
                        TOOL_STATION.operations()));

        private TLStations() {
        }

        // Adders
        public static StationDefinition add(StationDefinition definition) {
                if (DEFINITIONS.putIfAbsent(definition.id(), definition) != null) {
                        throw new IllegalArgumentException("Duplicate station definition: " + definition.id());
                }
                return definition;
        }

        // Getters
        public static StationDefinition get(ResourceLocation id) {
                StationDefinition definition = DEFINITIONS.get(id);
                if (definition == null) {
                        throw new IllegalArgumentException("Unknown station definition: " + id);
                }
                return definition;
        }

        public static Map<ResourceLocation, StationDefinition> all() {
                return Collections.unmodifiableMap(DEFINITIONS);
        }

        private static ResourceLocation id(String path) {
                return ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, path);
        }
}
