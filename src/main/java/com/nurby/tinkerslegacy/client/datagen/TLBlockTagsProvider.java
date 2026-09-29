package com.nurby.tinkerslegacy.client.datagen;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.registry.TLBlocks;
import com.nurby.tinkerslegacy.registry.TLMetalFamilies;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

public class TLBlockTagsProvider extends BlockTagsProvider {
    private static final TagKey<Block> STORAGE_BLOCKS = modTag("storage_blocks");
    private static final TagKey<Block> SEARED_BLOCKS = modTag("seared_blocks");
    private static final TagKey<Block> SLIMY_GROUND = modTag("slimy_ground");

    public TLBlockTagsProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, TinkersLegacy.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider lookupProvider) {
        // Every defined metal contributes its storage block.
        var storage = tag(STORAGE_BLOCKS);

        for (var family : TLMetalFamilies.ALL) {
            storage.add(family.storageBlock().get());
        }

        // Silky jewel is not in metal family
        storage.add(TLBlocks.StorageBlocks.STORAGE_BLOCK_SILKY_JEWEL.get());

        tag(BlockTags.BEACON_BASE_BLOCKS)
                .addTag(STORAGE_BLOCKS);

        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .addTag(STORAGE_BLOCKS)
                .addTag(SEARED_BLOCKS)
                .add(
                        TLBlocks.OreBlocks.NETHER_COBALT_ORE.get(),
                        TLBlocks.OreBlocks.NETHER_ARDITE_ORE.get());

        tag(BlockTags.NEEDS_DIAMOND_TOOL)
                .add(
                        TLBlocks.OreBlocks.NETHER_COBALT_ORE.get(),
                        TLBlocks.OreBlocks.NETHER_ARDITE_ORE.get());

        tag(BlockTags.MINEABLE_WITH_SHOVEL)
                .addTag(SLIMY_GROUND)
                .add(
                        TLBlocks.IntermediaryBlocks.GROUT.get());
    }

    private static TagKey<Block> modTag(String name) {
        return TagKey.create(
                Registries.BLOCK,
                ResourceLocation.fromNamespaceAndPath(
                        TinkersLegacy.MODID,
                        name));
    }
}
