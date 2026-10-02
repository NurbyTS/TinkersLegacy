package com.nurby.tinkerslegacy.registry;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.function.Supplier;

public final class TLMetalFamilies {
        private TLMetalFamilies() {
        }

        public record MetalFamily(
                        Supplier<? extends Item> nugget,
                        Supplier<? extends Item> ingot,
                        Supplier<? extends Block> storageBlock) {
        }

        // Metals
        public static final MetalFamily COBALT = new MetalFamily(
                        TLItems.get(TLItems.Items.Nuggets.Type.COBALT),
                        TLItems.get(TLItems.Items.Ingots.Type.COBALT),
                        TLBlocks.get(TLBlocks.StorageBlocks.Type.COBALT));

        public static final MetalFamily ARDITE = new MetalFamily(
                        TLItems.get(TLItems.Items.Nuggets.Type.ARDITE),
                        TLItems.get(TLItems.Items.Ingots.Type.ARDITE),
                        TLBlocks.get(TLBlocks.StorageBlocks.Type.ARDITE));

        public static final MetalFamily ALUBRASS = new MetalFamily(
                        TLItems.get(TLItems.Items.Nuggets.Type.ALUBRASS),
                        TLItems.get(TLItems.Items.Ingots.Type.ALUBRASS),
                        TLBlocks.get(TLBlocks.StorageBlocks.Type.ALUBRASS));

        public static final MetalFamily KNIGHTSLIME = new MetalFamily(
                        TLItems.get(TLItems.Items.Nuggets.Type.KNIGHTSLIME),
                        TLItems.get(TLItems.Items.Ingots.Type.KNIGHTSLIME),
                        TLBlocks.get(TLBlocks.StorageBlocks.Type.KNIGHTSLIME));

        public static final MetalFamily MANYULLYN = new MetalFamily(
                        TLItems.get(TLItems.Items.Nuggets.Type.MANYULLYN),
                        TLItems.get(TLItems.Items.Ingots.Type.MANYULLYN),
                        TLBlocks.get(TLBlocks.StorageBlocks.Type.MANYULLYN));

        public static final MetalFamily PIGIRON = new MetalFamily(
                        TLItems.get(TLItems.Items.Nuggets.Type.PIGIRON),
                        TLItems.get(TLItems.Items.Ingots.Type.PIGIRON),
                        TLBlocks.get(TLBlocks.StorageBlocks.Type.PIGIRON));

        public static final List<MetalFamily> ALL = List.of(
                        COBALT,
                        ARDITE,
                        ALUBRASS,
                        KNIGHTSLIME,
                        MANYULLYN,
                        PIGIRON);
}
