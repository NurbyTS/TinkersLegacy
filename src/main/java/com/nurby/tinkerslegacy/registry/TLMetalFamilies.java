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
                    TLItems.get(TLItems.Items.Nuggets.Type.COBALT),
                    TLItems.get(TLItems.Items.Ingots.Type.COBALT),
                    TLBlocks.get(TLBlocks.StorageBlocks.Type.COBALT),
                    List.of(
                            TLItems.get(TLItems.Items.RawOres.Type.COBALT),
                            TLBlocks.get(TLBlocks.OreBlocks.Type.COBALT))),

            new MetalFamily(
                    TLItems.get(TLItems.Items.Nuggets.Type.ARDITE),
                    TLItems.get(TLItems.Items.Ingots.Type.ARDITE),
                    TLBlocks.get(TLBlocks.StorageBlocks.Type.ARDITE),
                    List.of(
                            TLItems.get(TLItems.Items.RawOres.Type.ARDITE),
                            TLBlocks.get(TLBlocks.OreBlocks.Type.ARDITE))),

            new MetalFamily(
                    TLItems.get(TLItems.Items.Nuggets.Type.ALUBRASS),
                    TLItems.get(TLItems.Items.Ingots.Type.ALUBRASS),
                    TLBlocks.get(TLBlocks.StorageBlocks.Type.ALUBRASS),
                    List.of()),

            new MetalFamily(
                    TLItems.get(TLItems.Items.Nuggets.Type.KNIGHTSLIME),
                    TLItems.get(TLItems.Items.Ingots.Type.KNIGHTSLIME),
                    TLBlocks.get(TLBlocks.StorageBlocks.Type.KNIGHTSLIME),
                    List.of()),

            new MetalFamily(
                    TLItems.get(TLItems.Items.Nuggets.Type.MANYULLYN),
                    TLItems.get(TLItems.Items.Ingots.Type.MANYULLYN),
                    TLBlocks.get(TLBlocks.StorageBlocks.Type.MANYULLYN),
                    List.of()),

            new MetalFamily(
                    TLItems.get(TLItems.Items.Nuggets.Type.PIGIRON),
                    TLItems.get(TLItems.Items.Ingots.Type.PIGIRON),
                    TLBlocks.get(TLBlocks.StorageBlocks.Type.PIGIRON),
                    List.of()));
}
