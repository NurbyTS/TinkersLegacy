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
            Supplier<? extends Block> storageBlock,
            List<Supplier<? extends ItemLike>> smeltingInputs) {
    }

    public static final List<MetalFamily> ALL = List.of(
            new MetalFamily(
                    TLItems.Items.Nuggets.COBALT_NUGGET,
                    TLItems.Items.Ingots.COBALT_INGOT,
                    TLBlocks.get(TLBlocks.StorageBlocks.Type.COBALT),
                    List.of(
                            TLItems.Items.RawOres.RAW_COBALT,
                            TLBlocks.get(TLBlocks.OreBlocks.Type.COBALT))),

            new MetalFamily(
                    TLItems.Items.Nuggets.ARDITE_NUGGET,
                    TLItems.Items.Ingots.ARDITE_INGOT,
                    TLBlocks.get(TLBlocks.StorageBlocks.Type.ARDITE),
                    List.of(
                            TLItems.Items.RawOres.RAW_ARDITE,
                            TLBlocks.get(TLBlocks.OreBlocks.Type.ARDITE))),

            new MetalFamily(
                    TLItems.Items.Nuggets.ALUBRASS_NUGGET,
                    TLItems.Items.Ingots.ALUBRASS_INGOT,
                    TLBlocks.get(TLBlocks.StorageBlocks.Type.ALUBRASS),
                    List.of()),

            new MetalFamily(
                    TLItems.Items.Nuggets.KNIGHTSLIME_NUGGET,
                    TLItems.Items.Ingots.KNIGHTSLIME_INGOT,
                    TLBlocks.get(TLBlocks.StorageBlocks.Type.KNIGHTSLIME),
                    List.of()),

            new MetalFamily(
                    TLItems.Items.Nuggets.MANYULLYN_NUGGET,
                    TLItems.Items.Ingots.MANYULLYN_INGOT,
                    TLBlocks.get(TLBlocks.StorageBlocks.Type.MANYULLYN),
                    List.of()),

            new MetalFamily(
                    TLItems.Items.Nuggets.PIGIRON_NUGGET,
                    TLItems.Items.Ingots.PIGIRON_INGOT,
                    TLBlocks.get(TLBlocks.StorageBlocks.Type.PIGIRON),
                    List.of()));
}
