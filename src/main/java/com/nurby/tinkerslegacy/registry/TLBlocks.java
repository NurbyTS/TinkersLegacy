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
                public static final DeferredHolder<Block, Block> STORAGE_BLOCK_COBALT = register(
                                "storage_block_cobalt",
                                BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));

                public static final DeferredHolder<Block, Block> STORAGE_BLOCK_ARDITE = register(
                                "storage_block_ardite",
                                BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));

                public static final DeferredHolder<Block, Block> STORAGE_BLOCK_ALUBRASS = register(
                                "storage_block_alubrass",
                                BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));

                public static final DeferredHolder<Block, Block> STORAGE_BLOCK_KNIGHTSLIME = register(
                                "storage_block_knightslime",
                                BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));

                public static final DeferredHolder<Block, Block> STORAGE_BLOCK_MANYULLYN = register(
                                "storage_block_manyullyn",
                                BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));

                public static final DeferredHolder<Block, Block> STORAGE_BLOCK_PIGIRON = register(
                                "storage_block_pigiron",
                                BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));

                public static final DeferredHolder<Block, Block> STORAGE_BLOCK_SILKY_JEWEL = register(
                                "storage_block_silky_jewel",
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

                public static final DeferredHolder<Block, Block> SEARED_COBBLE = register(
                                "seared_cobble",
                                BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE));

                public static final DeferredHolder<Block, Block> SEARED_PAVER = register(
                                "seared_paver",
                                BlockBehaviour.Properties.ofFullCopy(Blocks.SMOOTH_STONE));

                public static final DeferredHolder<Block, Block> SEARED_BRICKS = register(
                                "seared_bricks",
                                BlockBehaviour.Properties.ofFullCopy(Blocks.BRICK_WALL));

                public static final DeferredHolder<Block, Block> SEARED_BRICK_CRACKED = register(
                                "seared_brick_cracked",
                                BlockBehaviour.Properties.ofFullCopy(Blocks.CRACKED_STONE_BRICKS));

                public static final DeferredHolder<Block, Block> SEARED_BRICK_FANCY = register(
                                "seared_brick_fancy",
                                BlockBehaviour.Properties.ofFullCopy(Blocks.BRICK_WALL));

                public static final DeferredHolder<Block, Block> SEARED_BRICK_SQUARE = register(
                                "seared_brick_square",
                                BlockBehaviour.Properties.ofFullCopy(Blocks.BRICK_WALL));

                public static final DeferredHolder<Block, Block> SEARED_ROAD = register(
                                "seared_road",
                                BlockBehaviour.Properties.ofFullCopy(Blocks.SMOOTH_STONE));

                public static final DeferredHolder<Block, Block> SEARED_CREEPER = register(
                                "seared_creeper",
                                BlockBehaviour.Properties.ofFullCopy(Blocks.SMOOTH_STONE));

                public static final DeferredHolder<Block, Block> SEARED_BRICK_TRIANGLE = register(
                                "seared_brick_triangle",
                                BlockBehaviour.Properties.ofFullCopy(Blocks.BRICK_WALL));

                public static final DeferredHolder<Block, Block> SEARED_BRICK_SMALL = register(
                                "seared_brick_small",
                                BlockBehaviour.Properties.ofFullCopy(Blocks.BRICK_WALL));

                public static final DeferredHolder<Block, Block> SEARED_TILE = register(
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
