package com.nurby.tinkerslegacy.client.datagen;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.registry.TLBlocks;
import com.nurby.tinkerslegacy.registry.TLTags;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.concurrent.CompletableFuture;

public class TLBlockTagsProvider extends BlockTagsProvider {
        public TLBlockTagsProvider(
                        PackOutput output,
                        CompletableFuture<HolderLookup.Provider> lookupProvider,
                        ExistingFileHelper existingFileHelper) {
                super(output, lookupProvider, TinkersLegacy.MODID, existingFileHelper);
        }

        @Override
        protected void addTags(HolderLookup.Provider lookupProvider) {
                // Storage Blocks
                addBlocks(TLTags.Blocks.STORAGE_BLOCKS, TLBlocks.StorageBlocks.ALL);

                tag(BlockTags.BEACON_BASE_BLOCKS)
                                .addTag(TLTags.Blocks.STORAGE_BLOCKS);

                // Seared Blocks
                addBlocks(TLTags.Blocks.SEARED_BLOCKS, TLBlocks.SearedBlocks.ALL);

                // Slimy Ground
                addBlocks(TLTags.Blocks.SLIMY_GROUND, TLBlocks.SlimyBlocks.SOILS);
                addBlocks(TLTags.Blocks.SLIMY_GROUND, TLBlocks.SlimyBlocks.GRASSES);

                tag(BlockTags.DIRT)
                                .addTag(TLTags.Blocks.SLIMY_GROUND);

                // Mining
                tag(BlockTags.MINEABLE_WITH_PICKAXE)
                                .addTag(TLTags.Blocks.STORAGE_BLOCKS)
                                .addTag(TLTags.Blocks.SEARED_BLOCKS);

                addBlocks(BlockTags.MINEABLE_WITH_PICKAXE, TLBlocks.OreBlocks.ALL);
                addBlocks(BlockTags.NEEDS_DIAMOND_TOOL, TLBlocks.OreBlocks.ALL);

                tag(BlockTags.MINEABLE_WITH_SHOVEL)
                                .addTag(TLTags.Blocks.SLIMY_GROUND)
                                .add(TLBlocks.block(TLBlocks.IntermediaryBlocks.Type.GROUT));
        }

        private void addBlocks(
                        TagKey<Block> tag,
                        Iterable<? extends DeferredHolder<Block, ? extends Block>> blocks) {
                var appender = tag(tag);

                for (var block : blocks) {
                        appender.add(block.get());
                }
        }
}
