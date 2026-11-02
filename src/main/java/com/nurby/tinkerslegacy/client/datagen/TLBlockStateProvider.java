package com.nurby.tinkerslegacy.client.datagen;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.content.block.*;
import com.nurby.tinkerslegacy.registry.TLBlocks;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VineBlock;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class TLBlockStateProvider extends BlockStateProvider {
        public TLBlockStateProvider(
                        PackOutput output,
                        ExistingFileHelper helper) {
                super(output, TinkersLegacy.MODID, helper);
        }

        @Override
        protected void registerStatesAndModels() {
                for (var holder : TLBlocks.BLOCKS.getEntries()) {
                        Block block = holder.get();

                        if (block instanceof GlowBlock) {
                                var model = models().withExistingParent(holder.getId().getPath(), modLoc("block/glow_surface"))
                                                .texture("all", modLoc("block/glow")).renderType("translucent");
                                getVariantBuilder(block).forAllStates(state -> {
                                        Direction support = state.getValue(GlowBlock.FACING).getOpposite();
                                        return ConfiguredModel.builder().modelFile(model)
                                                        .rotationX(support == Direction.DOWN ? 90 : support == Direction.UP ? 270 : 0)
                                                        .rotationY(support.getAxis().isVertical() ? 0 : ((int) support.toYRot() + 180) % 360)
                                                        .build();
                                });
                        } else if (block instanceof SlimyLeavesBlock) {
                                simpleBlockWithItem(block, models().withExistingParent(holder.getId().getPath(), mcLoc("block/leaves"))
                                                .texture("all", modLoc("block/slime/slimeleaves")).renderType("cutout_mipped"));
                        } else if (block instanceof SlimyVineBlock vine) {
                                vine(vine, holder.getId().getPath());
                        } else if (block == TLBlocks.block(TLBlocks.Workstations.Type.TOOL_FORGE)) {
                                var model = models().withExistingParent(holder.getId().getPath(), modLoc("block/station_forge"));
                                horizontalBlock(block, model);
                                simpleBlockItem(block, model);
                        } else if (block instanceof StationBlock) {
                                String name = holder.getId().getPath();
                                var side = block == TLBlocks.block(TLBlocks.Workstations.Type.CRAFTING_STATION)
                                                ? modLoc("block/station/crafting_station_side")
                                                : modLoc("block/station/table_side");
                                var model = models().withExistingParent(name, modLoc("block/station_table"))
                                                .texture("particle", modLoc("block/station/" + name + "_top"))
                                                .texture("top", modLoc("block/station/" + name + "_top"))
                                                .texture("side", side)
                                                .texture("leg", modLoc("block/station/table_side"))
                                                .texture("bottom", mcLoc("block/oak_planks"))
                                                .texture("legBottom", mcLoc("block/oak_planks"));
                                horizontalBlock(block, model);
                                simpleBlockItem(block, model);
                        } else if (block == TLBlocks.block(TLBlocks.OreBlocks.Type.NETHER_COBALT)
                                        || block == TLBlocks.block(TLBlocks.OreBlocks.Type.NETHER_ARDITE)) {
                                // The template draws netherrack first, then the transparent ore layer.
                                simpleBlockWithItem(block, models()
                                                .withExistingParent(holder.getId().getPath(), modLoc("block/ore_overlay"))
                                                .texture("base", mcLoc("block/netherrack"))
                                                .texture("overlay", blockTexture(block)));
                        } else if (block instanceof SlimyGrassBlock grass) {
                                var soil = grass.soil() == Blocks.DIRT ? mcLoc("block/dirt")
                                                : modLoc("block/slime/" + BuiltInRegistries.BLOCK.getKey(grass.soil()).getPath());
                                simpleBlockWithItem(block,
                                                models().withExistingParent(holder.getId().getPath(), mcLoc("block/grass_block"))
                                                                .texture("bottom", soil).texture("side", soil).texture("particle", soil)
                                                                .texture("top", modLoc("block/slime/slimegrass_top"))
                                                                .texture("overlay", modLoc("block/slime/slimegrass_overlay"))
                                                                .renderType("cutout_mipped"));
                        } else if (block instanceof ColoredSlimeBlock) {
                                String name = holder.getId().getPath();
                                var model = models().withExistingParent(name, modLoc("block/slime_cube"))
                                                .texture("texture", modLoc("block/slime/" + name))
                                                .texture("inner", name.equals("magma_slime_block") ? mcLoc("block/lava_still")
                                                                : modLoc("block/slime/" + name));
                                simpleBlockWithItem(block, model);
                        } else if (block instanceof CongealedSlimeBlock) {
                                simpleBlockWithItem(block,
                                                models().cubeAll(holder.getId().getPath(), modLoc("block/slime/" + holder.getId().getPath())));
                        } else if (holder.getId().getPath().endsWith("_slimy_dirt")) {
                                simpleBlockWithItem(block,
                                                models().cubeAll(holder.getId().getPath(), modLoc("block/slime/" + holder.getId().getPath())));
                        } else {
                                simpleBlockWithItem(block, cubeAll(block));
                        }
                }
        }

        // Vines
        private void vine(SlimyVineBlock vine, String name) {
                for (int stage = 0; stage <= 2; stage++) {
                        String suffix = stage == 0 ? "" : stage == 1 ? "_mid" : "_end";
                        var side = models().withExistingParent(name + suffix, modLoc("block/slime/vine_north"))
                                        .texture("vine", modLoc("block/slime/slimevine" + suffix));
                        var top = models().withExistingParent(name + suffix + "_up", modLoc("block/slime/vine_up"))
                                        .texture("vine", modLoc("block/slime/slimevine" + suffix));
                        for (Direction direction : Direction.Plane.HORIZONTAL) {
                                getMultipartBuilder(vine).part().modelFile(side)
                                                .rotationY(((int) direction.toYRot() + 180) % 360).addModel()
                                                .condition(VineBlock.getPropertyForFace(direction), true)
                                                .condition(SlimyVineBlock.STAGE, stage).end();
                        }
                        getMultipartBuilder(vine).part().modelFile(top).addModel()
                                        .condition(VineBlock.UP, true).condition(SlimyVineBlock.STAGE, stage).end();
                }
        }
}
