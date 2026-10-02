package com.nurby.tinkerslegacy.registry;

import com.nurby.tinkerslegacy.TinkersLegacy;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class TLTags {
        private TLTags() {
        }

        public static final class Blocks {
                public static final TagKey<Block> STORAGE_BLOCKS = create("storage_blocks");
                public static final TagKey<Block> SEARED_BLOCKS = create("seared_blocks");
                public static final TagKey<Block> SLIMY_GROUND = create("slimy_ground");

                private Blocks() {
                }

                private static TagKey<Block> create(String name) {
                        return TagKey.create(
                                        Registries.BLOCK,
                                        ResourceLocation.fromNamespaceAndPath(
                                                        TinkersLegacy.MODID,
                                                        name));
                }
        }
}
