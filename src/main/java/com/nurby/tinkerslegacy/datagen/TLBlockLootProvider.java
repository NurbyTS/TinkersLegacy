package com.nurby.tinkerslegacy.datagen;

import com.nurby.tinkerslegacy.block.SlimyGrassBlock;
import com.nurby.tinkerslegacy.registry.TLBlocks;
import com.nurby.tinkerslegacy.registry.TLOreFamilies;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

import java.util.HashSet;
import java.util.Set;

public class TLBlockLootProvider extends BlockLootSubProvider {
        public TLBlockLootProvider(HolderLookup.Provider lookupProvider) {
                super(Set.of(), FeatureFlags.DEFAULT_FLAGS, lookupProvider);
        }

        @Override
        protected void generate() {
                // Ores
                Set<Block> ores = new HashSet<>();

                for (var family : TLOreFamilies.ALL) {
                        for (var variant : family.variants()) {
                                Block ore = variant.get();

                                if (!ores.add(ore)) {
                                        throw new IllegalStateException("Duplicate ore variant: " + ore);
                                }

                                add(ore, createOreDrop(ore, family.rawItem().get()));
                        }
                }

                // Other Blocks
                for (Block block : getKnownBlocks()) {
                        if (ores.contains(block)) {
                                continue;
                        }

                        if (block instanceof SlimyGrassBlock grass) {
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
