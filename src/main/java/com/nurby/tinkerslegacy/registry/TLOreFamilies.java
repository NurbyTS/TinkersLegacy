package com.nurby.tinkerslegacy.registry;

import com.nurby.tinkerslegacy.registry.TLBlocks.OreBlocks;
import com.nurby.tinkerslegacy.registry.TLItems.Items.RawOres;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public final class TLOreFamilies {
        private TLOreFamilies() {
        }

        public record OreFamily(
                        List<Supplier<? extends Block>> variants,
                        Supplier<? extends Item> rawItem,
                        TLMetalFamilies.MetalFamily metal) {
                public OreFamily {
                        variants = List.copyOf(variants);
                        Objects.requireNonNull(rawItem, "Raw item");
                        Objects.requireNonNull(metal, "Metal family");

                        if (variants.isEmpty()) {
                                throw new IllegalArgumentException("An ore family needs at least one variant");
                        }
                }
        }

        // Ore Families
        public static final OreFamily COBALT = new OreFamily(
                        List.of(TLBlocks.get(OreBlocks.Type.NETHER_COBALT)),
                        TLItems.get(RawOres.Type.COBALT),
                        TLMetalFamilies.COBALT);

        public static final OreFamily ARDITE = new OreFamily(
                        List.of(TLBlocks.get(OreBlocks.Type.NETHER_ARDITE)),
                        TLItems.get(RawOres.Type.ARDITE),
                        TLMetalFamilies.ARDITE);

        public static final List<OreFamily> ALL = List.of(COBALT, ARDITE);
}
