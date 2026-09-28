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
        public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(
                        BuiltInRegistries.BLOCK,
                        TinkersLegacy.MODID);

        public static void register(IEventBus modBus) {
                Intermediary_Blocks.register();
                Storage_Blocks.register();
                Seared_Stone.register();
                BLOCKS.register(modBus);
        }

        public static final class Intermediary_Blocks {
                public static final DeferredHolder<Block, Block> GROUT = register(
                                "grout",
                                BlockBehaviour.Properties.ofFullCopy(Blocks.CLAY));

                private static void register() {
                }

                private static DeferredHolder<Block, Block> register(
                                String name,
                                BlockBehaviour.Properties properties) {
                        return BLOCKS.register(name, () -> new Block(properties));
                }
        }

        public static final class Storage_Blocks {
                public static final DeferredHolder<Block, Block> COBALT_BLOCK = register(
                                "cobalt_block",
                                BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));

                public static final DeferredHolder<Block, Block> ARDITE_BLOCK = register(
                                "ardite_block",
                                BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));

                public static final DeferredHolder<Block, Block> ALUBRASS_BLOCK = register(
                                "alubrass_block",
                                BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));

                public static final DeferredHolder<Block, Block> KNIGHTSLIME_BLOCK = register(
                                "knightslime_block",
                                BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));

                public static final DeferredHolder<Block, Block> MANYULLYN_BLOCK = register(
                                "manyullyn_block",
                                BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));

                public static final DeferredHolder<Block, Block> PIGIRON_BLOCK = register(
                                "pigiron_block",
                                BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));

                public static final DeferredHolder<Block, Block> SILKY_JEWEL_BLOCK = register(
                                "silky_jewel_block",
                                BlockBehaviour.Properties.ofFullCopy(Blocks.EMERALD_BLOCK));

                private static void register() {
                }

                private static DeferredHolder<Block, Block> register(
                                String name,
                                BlockBehaviour.Properties properties) {
                        return BLOCKS.register(name, () -> new Block(properties));
                }
        }

        public static final class Seared_Stone {
                public static final DeferredHolder<Block, Block> SEARED_STONE = register(
                                "seared_stone",
                                BlockBehaviour.Properties.ofFullCopy(Blocks.STONE));

                public static final DeferredHolder<Block, Block> SEARED_COBBLESTONE = register(
                                "seared_cobble",
                                BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE));

                public static final DeferredHolder<Block, Block> SEARED_PAVER = register(
                                "seared_paver",
                                BlockBehaviour.Properties.ofFullCopy(Blocks.SMOOTH_STONE));

                public static final DeferredHolder<Block, Block> SEARED_BRICKS = register(
                                "seared_bricks",
                                BlockBehaviour.Properties.ofFullCopy(Blocks.BRICK_WALL));

                public static final DeferredHolder<Block, Block> CRACKED_SEARED_BRICKS = register(
                                "seared_brick_cracked",
                                BlockBehaviour.Properties.ofFullCopy(Blocks.CRACKED_STONE_BRICKS));

                public static final DeferredHolder<Block, Block> FANCY_SEARED_BRICKS = register(
                                "seared_brick_fancy",
                                BlockBehaviour.Properties.ofFullCopy(Blocks.BRICK_WALL));

                public static final DeferredHolder<Block, Block> SQUARE_SEARED_BRICKS = register(
                                "seared_brick_square",
                                BlockBehaviour.Properties.ofFullCopy(Blocks.BRICK_WALL));

                public static final DeferredHolder<Block, Block> SEARED_ROAD = register(
                                "seared_road",
                                BlockBehaviour.Properties.ofFullCopy(Blocks.SMOOTH_STONE));

                public static final DeferredHolder<Block, Block> SEARED_CREEPERFACE = register(
                                "seared_creeper",
                                BlockBehaviour.Properties.ofFullCopy(Blocks.SMOOTH_STONE));

                public static final DeferredHolder<Block, Block> TRIANGLE_SEARED_BRICKS = register(
                                "seared_brick_triangle",
                                BlockBehaviour.Properties.ofFullCopy(Blocks.BRICK_WALL));

                public static final DeferredHolder<Block, Block> SMALL_SEARED_BRICKS = register(
                                "seared_brick_small",
                                BlockBehaviour.Properties.ofFullCopy(Blocks.BRICK_WALL));

                public static final DeferredHolder<Block, Block> SEARED_TILES = register(
                                "seared_tile",
                                BlockBehaviour.Properties.ofFullCopy(Blocks.SMOOTH_STONE));

                private static void register() {
                }

                private static DeferredHolder<Block, Block> register(
                                String name,
                                BlockBehaviour.Properties properties) {
                        return BLOCKS.register(name, () -> new Block(properties));
                }
        }
}
