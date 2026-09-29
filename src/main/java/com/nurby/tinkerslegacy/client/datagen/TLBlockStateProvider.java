package com.nurby.tinkerslegacy.client.datagen;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.registry.TLBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class TLBlockStateProvider extends BlockStateProvider {
    public TLBlockStateProvider(
        PackOutput output,
        ExistingFileHelper helper
    ) {
        super(output, TinkersLegacy.MODID, helper);
    }

    @Override
    protected void registerStatesAndModels() {
        for (var holder : TLBlocks.BLOCKS.getEntries()) {
            Block block = holder.get();
            if (block == TLBlocks.OreBlocks.NETHER_COBALT_ORE.get() || block == TLBlocks.OreBlocks.NETHER_ARDITE_ORE.get()) {
                // The template draws netherrack first, then the transparent ore layer.
                simpleBlockWithItem(block, models()
                        .withExistingParent(holder.getId().getPath(), modLoc("block/ore_overlay"))
                        .texture("base", mcLoc("block/netherrack"))
                        .texture("overlay", blockTexture(block)));
            } else {
                simpleBlockWithItem(block, cubeAll(block));
            }
        }
    }
}
