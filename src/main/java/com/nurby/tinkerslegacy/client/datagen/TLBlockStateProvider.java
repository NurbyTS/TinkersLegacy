package com.nurby.tinkerslegacy.client.datagen;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.block.SlimyGrassBlock;
import com.nurby.tinkerslegacy.block.ColoredSlimeBlock;
import com.nurby.tinkerslegacy.block.CongealedSlimeBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;
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
            } else if (block instanceof SlimyGrassBlock grass) {
                var soil = grass.soil() == Blocks.DIRT ? mcLoc("block/dirt")
                        : modLoc("block/slime/" + BuiltInRegistries.BLOCK.getKey(grass.soil()).getPath());
                simpleBlockWithItem(block, models().withExistingParent(holder.getId().getPath(), mcLoc("block/grass_block"))
                        .texture("bottom", soil).texture("side", soil).texture("particle", soil)
                        .texture("top", modLoc("block/slime/slimegrass_top"))
                        .texture("overlay", modLoc("block/slime/slimegrass_overlay")).renderType("cutout_mipped"));
            } else if (block instanceof ColoredSlimeBlock) {
                String name = holder.getId().getPath();
                var model = models().withExistingParent(name, modLoc("block/slime_cube"))
                        .texture("texture", modLoc("block/slime/" + name))
                        .texture("inner", name.equals("magma_slime_block") ? mcLoc("block/lava_still") : modLoc("block/slime/" + name));
                simpleBlockWithItem(block, model);
            } else if (block instanceof CongealedSlimeBlock) {
                simpleBlockWithItem(block, models().cubeAll(holder.getId().getPath(), modLoc("block/slime/" + holder.getId().getPath())));
            } else if (holder.getId().getPath().endsWith("_slimy_dirt")) {
                simpleBlockWithItem(block, models().cubeAll(holder.getId().getPath(), modLoc("block/slime/" + holder.getId().getPath())));
            } else {
                simpleBlockWithItem(block, cubeAll(block));
            }
        }
    }
}
