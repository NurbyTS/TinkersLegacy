package com.nurby.tinkerslegacy.registry;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.function.Supplier;

public final class TLMetalFamilies {
    private TLMetalFamilies() {
    }

    public record MetalFamily(
            Supplier<? extends Item> nugget,
            Supplier<? extends Item> ingot,
            Supplier<? extends Item> storageBlock,
            List<Supplier<? extends ItemLike>> smeltingInputs) {
    }

    public static final List<MetalFamily> ALL = List.of(
            new MetalFamily(
                    TLItems.Items.Nuggets.COBALT_NUGGET,
                    TLItems.Items.Ingots.COBALT_INGOT,
                    TLItems.Blocks.StorageBlocks.STORAGE_BLOCK_COBALT,
                    List.of(
                            TLItems.Items.RawOres.RAW_COBALT,
                            TLItems.Blocks.OreBlocks.NETHER_COBALT_ORE)),

            new MetalFamily(
                    TLItems.Items.Nuggets.ARDITE_NUGGET,
                    TLItems.Items.Ingots.ARDITE_INGOT,
                    TLItems.Blocks.StorageBlocks.STORAGE_BLOCK_ARDITE,
                    List.of(
                            TLItems.Items.RawOres.RAW_ARDITE,
                            TLItems.Blocks.OreBlocks.NETHER_ARDITE_ORE)),

            new MetalFamily(
                    TLItems.Items.Nuggets.ARDITE_NUGGET,
                    TLItems.Items.Ingots.ARDITE_INGOT,
                    TLItems.Blocks.StorageBlocks.STORAGE_BLOCK_ARDITE,
                    List.of()),

            new MetalFamily(
                    TLItems.Items.Nuggets.ARDITE_NUGGET,
                    TLItems.Items.Ingots.ARDITE_INGOT,
                    TLItems.Blocks.StorageBlocks.STORAGE_BLOCK_ARDITE,
                    List.of()),

            new MetalFamily(
                    TLItems.Items.Nuggets.ARDITE_NUGGET,
                    TLItems.Items.Ingots.ARDITE_INGOT,
                    TLItems.Blocks.StorageBlocks.STORAGE_BLOCK_ARDITE,
                    List.of()),

            new MetalFamily(
                    TLItems.Items.Nuggets.ARDITE_NUGGET,
                    TLItems.Items.Ingots.ARDITE_INGOT,
                    TLItems.Blocks.StorageBlocks.STORAGE_BLOCK_ARDITE,
                    List.of()));
}
