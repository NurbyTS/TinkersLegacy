package com.nurby.tinkerslegacy.client.screen;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.library.tool.ToolDefinition;
import com.nurby.tinkerslegacy.registry.ToolStatTypes;

public final class StationPalette {
        private static final int BROWN = 0x684E1E;
        private static final int GRAY = 0xC1C1C1;
        private static final int BLUE = 0x2376DD;
        private static final int PURPLE = 0x7146B0;
        private static final int WHITE = 0xFFFFFF;

        private static final int[] ORIGINAL = {BROWN, GRAY, BLUE, PURPLE};
        private static final int[] HEAD_FIRST = {GRAY, BROWN, BLUE};
        private static final int[] MATTOCK = {GRAY, BLUE, BROWN};
        private static final int[] SHORTBOW = {BROWN, BLUE, WHITE};
        private static final int[] BOW = {BROWN, BLUE, GRAY, WHITE};
        private static final int[] BOLT = {GRAY, BLUE};

        private StationPalette() {
        }

        // Colors
        public static int part(ToolDefinition tool, int index) {
                var requirement = tool.partRequirements().get(index);
                if (tool.id().getNamespace().equals(TinkersLegacy.MODID)) {
                        int[] palette = switch (tool.id().getPath()) {
                                // The first eight tools retain their existing saved material order.
                                case "pickaxe", "shovel", "hatchet", "kama", "broadsword", "longsword", "rapier" -> HEAD_FIRST;
                                case "mattock" -> MATTOCK;
                                case "shortbow" -> SHORTBOW;
                                case "longbow", "crossbow" -> BOW;
                                case "bolt" -> BOLT;
                                case "frypan", "battlesign", "hammer", "excavator", "lumberaxe", "scythe", "cleaver",
                                                "arrow", "shuriken" -> ORIGINAL;
                                default -> null;
                        };
                        if (palette != null && index < palette.length) {
                                return palette[index];
                        }
                }
                if (requirement.statTypes().contains(ToolStatTypes.BOWSTRING.getId())) {
                        return WHITE;
                }
                if (requirement.statTypes().contains(ToolStatTypes.HANDLE.getId())) {
                        return BROWN;
                }
                int colored = 0;
                for (int part = 0; part < index; part++) {
                        if (!tool.partRequirements().get(part).statTypes().contains(ToolStatTypes.HANDLE.getId())) {
                                colored++;
                        }
                }
                return colored == 0 ? GRAY : colored == 1 ? BLUE : PURPLE;
        }

        public static int layer(ToolDefinition tool, int index) {
                if (tool.id().getNamespace().equals(TinkersLegacy.MODID) && tool.id().getPath().equals("bolt")) {
                        // One physical bolt core has separately rendered shaft and coating materials.
                        return ORIGINAL[index % ORIGINAL.length];
                }
                int firstLayer = 0;
                for (int part = 0; part < tool.partRequirements().size(); part++) {
                        firstLayer += tool.partRequirements().get(part).statTypes().size();
                        if (index < firstLayer) {
                                return part(tool, part);
                        }
                }
                throw new IllegalArgumentException("Unknown tool render layer: " + tool.id() + "/" + index);
        }
}
