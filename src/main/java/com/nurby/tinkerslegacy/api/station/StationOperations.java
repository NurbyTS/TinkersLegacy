package com.nurby.tinkerslegacy.api.station;

import com.nurby.tinkerslegacy.TinkersLegacy;

import net.minecraft.resources.ResourceLocation;

public final class StationOperations {
        public static final ResourceLocation CRAFTING_FAMILY = id("crafting_station");
        public static final ResourceLocation TOOL_FAMILY = id("tool_station");
        public static final ResourceLocation STENCIL_FAMILY = id("stencil_table");
        public static final ResourceLocation PART_FAMILY = id("part_builder");

        public static final ResourceLocation CRAFT = id("craft");
        public static final ResourceLocation CUT_PATTERN = id("cut_pattern");
        public static final ResourceLocation BUILD_PART = id("build_part");
        public static final ResourceLocation ASSEMBLE = id("assemble");
        public static final ResourceLocation MAINTAIN = id("maintain");
        public static final ResourceLocation REPAIR = id("repair");
        public static final ResourceLocation REPLACE = id("replace");
        public static final ResourceLocation MODIFY = id("modify");
        public static final ResourceLocation RENAME = id("rename");

        private StationOperations() {
        }

        private static ResourceLocation id(String path) {
                return ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, path);
        }
}
