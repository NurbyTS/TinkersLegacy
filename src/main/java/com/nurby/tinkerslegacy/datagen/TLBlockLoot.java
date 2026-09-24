package com.nurby.tinkerslegacy.datagen;

import com.nurby.tinkerslegacy.registry.TLBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

import java.util.Set;

public class TLBlockLoot extends BlockLootSubProvider {
    public TLBlockLoot(HolderLookup.Provider lookupProvider) {
        super(Set.of(), FeatureFlags.DEFAULT_FLAGS, lookupProvider);
    }

    @Override 
    protected void generate() {
        for (Block block : getKnownBlocks()) {
            dropSelf(block);
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
