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
            simpleBlockWithItem(block, cubeAll(block));
        }
    }
}
