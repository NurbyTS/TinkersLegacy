package com.nurby.tinkerslegacy.datagen;

import com.nurby.tinkerslegacy.registry.TLItems;
import com.nurby.tinkerslegacy.block.SlimyGrassBlock;
import com.nurby.tinkerslegacy.registry.TLBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

import java.util.Set;

public class TLBlockLootProvider extends BlockLootSubProvider {
    public TLBlockLootProvider(HolderLookup.Provider lookupProvider) {
        super(Set.of(), FeatureFlags.DEFAULT_FLAGS, lookupProvider);
    }

    @Override 
    protected void generate() {
        Block netherCobaltOre = TLBlocks.block(TLBlocks.OreBlocks.Type.COBALT);
        Block netherArditeOre = TLBlocks.block(TLBlocks.OreBlocks.Type.ARDITE);

        for (Block block : getKnownBlocks()) {
            if (block == netherCobaltOre) {
                add(block, createOreDrop(block, TLItems.Items.RawOres.RAW_COBALT.get()));
            } else if (block == netherArditeOre) {
                add(block, createOreDrop(block, TLItems.Items.RawOres.RAW_ARDITE.get()));
            }
            else if (block instanceof SlimyGrassBlock grass) {
                add(block, createSingleItemTableWithSilkTouch(block, grass.soil()));
            } else {
                dropSelf(block);
            }
        }
    }

    @Override 
    protected Iterable<Block> getKnownBlocks() {
        return TLBlocks.BLOCKS.getEntries()
            .stream()
            .map(holder -> (Block) holder.get())
            .toList();
    }
}
