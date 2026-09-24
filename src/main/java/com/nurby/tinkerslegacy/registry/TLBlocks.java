package com.nurby.tinkerslegacy.registry;

import com.nurby.tinkerslegacy.TinkersLegacy;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class TLBlocks {

        public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, TinkersLegacy.MODID);

        public static final DeferredHolder<Block, Block> COBALT_BLOCK = BLOCKS.register(
                        "cobalt_block",
                        () -> new Block(
                                        BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.IRON_BLOCK)));

        public static final DeferredHolder<Block, Block> ARDITE_BLOCK = BLOCKS.register(
                        "ardite_block",
                        () -> new Block(
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));

        public static final DeferredHolder<Block, Block> ALUBRASS_BLOCK = BLOCKS.register(
                        "alubrass_block",
                        () -> new Block(
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));

        public static final DeferredHolder<Block, Block> KNIGHTSLIME_BLOCK = BLOCKS.register(
                        "knightslime_block",
                        () -> new Block(
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));

        public static final DeferredHolder<Block, Block> MANYULLYN_BLOCK = BLOCKS.register(
                        "manyullyn_block",
                        () -> new Block(
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));

        public static final DeferredHolder<Block, Block> PIGIRON_BLOCK = BLOCKS.register(
                        "pigiron_block",
                        () -> new Block(
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));

        public static final DeferredHolder<Block, Block> SILKY_JEWEL_BLOCK = BLOCKS.register(
                        "silky_jewel_block",
                        () -> new Block(
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.EMERALD_BLOCK)));
                                
                                        

        public static final DeferredHolder<Block, Block> GROUT = BLOCKS.register(
                        "grout",
                        () -> new Block(
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.CLAY)));



        public static final DeferredHolder<Block, Block> SEARED_STONE = BLOCKS.register(
                        "seared_stone",
                        () -> new Block(
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)));

        public static final DeferredHolder<Block, Block> SEARED_COBBLESTONE = BLOCKS.register(
                        "seared_cobble",
                        () -> new Block(
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE)));

        public static final DeferredHolder<Block, Block> SEARED_PAVER = BLOCKS.register(
                        "seared_paver",
                        () -> new Block(
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.SMOOTH_STONE)));
                                        
        public static final DeferredHolder<Block, Block> SEARED_BRICKS = BLOCKS.register(
                        "seared_bricks",
                        () -> new Block(
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.BRICK_WALL)));
                        
        public static final DeferredHolder<Block, Block> CRACKED_SEARED_BRICKS = BLOCKS.register(
                        "seared_brick_cracked",
                        () -> new Block(
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.CRACKED_STONE_BRICKS)));

        public static final DeferredHolder<Block, Block> FANCY_SEARED_BRICKS = BLOCKS.register(
                        "seared_brick_fancy",
                        () -> new Block(
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.BRICK_WALL)));

        public static final DeferredHolder<Block, Block> SQUARE_SEARED_BRICKS = BLOCKS.register(
                        "seared_brick_square",
                        () -> new Block(
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.BRICK_WALL)));

        public static final DeferredHolder<Block, Block> SEARED_ROAD = BLOCKS.register(
                        "seared_road",
                        () -> new Block(
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.SMOOTH_STONE)));

        public static final DeferredHolder<Block, Block> SEARED_CREEPERFACE = BLOCKS.register(
                        "seared_creeper",
                        () -> new Block(
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.SMOOTH_STONE)));

        public static final DeferredHolder<Block, Block> TRIANGLE_SEARED_BRICKS = BLOCKS.register(
                        "seared_brick_triangle",
                        () -> new Block(
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.BRICK_WALL)));

        public static final DeferredHolder<Block, Block> SMALL_SEARED_BRICKS = BLOCKS.register(
                        "seared_brick_small",
                        () -> new Block(
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.BRICK_WALL)));

        public static final DeferredHolder<Block, Block> SEARED_TILES = BLOCKS.register(
                        "seared_tile",
                        () -> new Block(
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.SMOOTH_STONE)));

        public static void register(IEventBus modBus) {
                BLOCKS.register(modBus);
        }

        private TLBlocks() {
        }
}
