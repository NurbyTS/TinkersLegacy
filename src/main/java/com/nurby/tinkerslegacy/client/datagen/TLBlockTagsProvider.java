package com.nurby.tinkerslegacy.client.datagen;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.registry.TLBlocks;
import com.nurby.tinkerslegacy.registry.TLTags;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;

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

                // Slimy Plants
                for (var type : TLBlocks.SlimyPlants.Type.values()) {
                        Block block = TLBlocks.block(type);
                    if (block instanceof com.nurby.tinkerslegacy.content.block.SlimyVineBlock) {
                                tag(BlockTags.CLIMBABLE).add(block);
                                tag(BlockTags.MINEABLE_WITH_AXE).add(block);
                        } else {
                                tag(BlockTags.LEAVES).add(block);
                                tag(BlockTags.MINEABLE_WITH_HOE).add(block);
                        }
                }

                // Material Traits
                tag(TLTags.Blocks.PETRAMOR_BLOCKS)
                                .addTag(BlockTags.BASE_STONE_OVERWORLD)
                                .addTag(BlockTags.BASE_STONE_NETHER)
                                .addTag(BlockTags.STONE_BRICKS)
                                .addTag(BlockTags.TERRACOTTA)
                                .addTag(Tags.Blocks.ORES)
                                .addTag(Tags.Blocks.COBBLESTONES)
                                .add(Blocks.NETHERRACK, Blocks.END_STONE, Blocks.END_STONE_BRICKS,
                                                Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN, Blocks.SANDSTONE,
                                                Blocks.RED_SANDSTONE, Blocks.PRISMARINE, Blocks.PRISMARINE_BRICKS,
                                                Blocks.DARK_PRISMARINE, Blocks.BRICKS, Blocks.NETHER_BRICKS,
                                                Blocks.RED_NETHER_BRICKS, Blocks.PURPUR_BLOCK, Blocks.PURPUR_PILLAR,
                                                Blocks.CALCITE, Blocks.DRIPSTONE_BLOCK, Blocks.MAGMA_BLOCK);
                addBlocks(TLTags.Blocks.PETRAMOR_BLOCKS, TLBlocks.OreBlocks.ALL);
                tag(TLTags.Blocks.PETRAMOR_BLOCKS).addTag(TLTags.Blocks.SEARED_BLOCKS);

                // Workstations
                for (var type : TLBlocks.Workstations.Type.values()) {
                        tag(type == TLBlocks.Workstations.Type.TOOL_FORGE
                                        ? BlockTags.MINEABLE_WITH_PICKAXE : BlockTags.MINEABLE_WITH_AXE)
                                        .add(TLBlocks.block(type));
                }

                // Mining
                tag(BlockTags.MINEABLE_WITH_PICKAXE)
                                .addTag(TLTags.Blocks.STORAGE_BLOCKS)
                                .addTag(TLTags.Blocks.SEARED_BLOCKS);

                addBlocks(BlockTags.MINEABLE_WITH_PICKAXE, TLBlocks.OreBlocks.ALL);
                addBlocks(BlockTags.NEEDS_DIAMOND_TOOL, TLBlocks.OreBlocks.ALL);
                addBlocks(TLTags.Blocks.NEEDS_COBALT_TOOL, TLBlocks.OreBlocks.ALL);
                tag(BlockTags.INCORRECT_FOR_DIAMOND_TOOL).addTag(TLTags.Blocks.NEEDS_COBALT_TOOL);
                tag(BlockTags.MINEABLE_WITH_SHOVEL)
                                .add(TLBlocks.block(TLBlocks.IntermediaryBlocks.Type.CONSECRATED_SOIL),
                                                TLBlocks.block(TLBlocks.IntermediaryBlocks.Type.GRAVEYARD_SOIL),
                                                TLBlocks.block(TLBlocks.IntermediaryBlocks.Type.GREEN_SLIMY_MUD),
                                                TLBlocks.block(TLBlocks.IntermediaryBlocks.Type.BLUE_SLIMY_MUD),
                                                TLBlocks.block(TLBlocks.IntermediaryBlocks.Type.MAGMA_SLIMY_MUD));
                tag(BlockTags.MINEABLE_WITH_AXE)
                                .add(TLBlocks.block(TLBlocks.IntermediaryBlocks.Type.FIREWOOD),
                                                TLBlocks.block(TLBlocks.IntermediaryBlocks.Type.LAVAWOOD));

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
