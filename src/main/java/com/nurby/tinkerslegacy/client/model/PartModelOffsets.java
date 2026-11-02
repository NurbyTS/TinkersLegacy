package com.nurby.tinkerslegacy.client.model;

import com.nurby.tinkerslegacy.TinkersLegacy;

import net.minecraft.resources.ResourceLocation;

public final class PartModelOffsets {

        private static final Offset ZERO = new Offset(0, 0);

        private PartModelOffsets() {
        }

        // Getters
        public static Offset get(ResourceLocation part) {
                if (!part.getNamespace().equals(TinkersLegacy.MODID)) {
                        return ZERO;
                }
                // Original part models shift shared tool textures by these pixel offsets.
                return switch (part.getPath()) {
                        case "axe_head", "kama_head" -> new Offset(-2, 4);
                        case "bow_limb" -> new Offset(4, -2);
                        case "broad_axe_head" -> new Offset(-3, 4);
                        case "cross_guard" -> new Offset(3, -3);
                        case "excavator_head", "hammer_head", "shovel_head" -> new Offset(-3, 3);
                        case "hand_guard" -> new Offset(4, -4);
                        case "large_sword_blade" -> new Offset(-1, 1);
                        case "pan_head", "pick_head" -> new Offset(-2, 2);
                        case "scythe_head" -> new Offset(-2, 5);
                        case "sign_head" -> new Offset(0, 2);
                        default -> ZERO;
                };
        }

        // Variants
        public record Offset(int x, int y) {
        }

}
