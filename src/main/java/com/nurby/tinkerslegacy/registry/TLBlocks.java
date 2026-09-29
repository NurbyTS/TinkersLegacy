package com.nurby.tinkerslegacy.registry;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.block.CongealedSlimeBlock;
import com.nurby.tinkerslegacy.block.ColoredSlimeBlock;
import com.nurby.tinkerslegacy.block.SlimyGrassBlock;
import java.util.function.Supplier;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class TLBlocks {
        public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(
                        BuiltInRegistries.BLOCK,
                        TinkersLegacy.MODID);

        public static void register(IEventBus modBus) {
                OreBlocks.register();
                IntermediaryBlocks.register();
                StorageBlocks.register();
                SearedBlocks.register();
                SlimyBlocks.register();
                BLOCKS.register(modBus);
        }

        public static final class OreBlocks {
                public static final DeferredHolder<Block, Block> NETHER_COBALT_ORE = register(
                                "nether_cobalt_ore",
                                netherOreProperties());

                public static final DeferredHolder<Block, Block> NETHER_ARDITE_ORE = register(
                                "nether_ardite_ore",
                                netherOreProperties());

                private static BlockBehaviour.Properties netherOreProperties() {
                        return BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.STONE)
                                        .strength(10.0F, 10.0F)
                                        .sound(SoundType.STONE)
                                        .requiresCorrectToolForDrops();
                }

                private static void register() {
                }

                private static DeferredHolder<Block, Block> register(
                                String name,
                                BlockBehaviour.Properties properties) {
                        return BLOCKS.register(name, () -> new Block(properties));
                }
        }

        public static final class IntermediaryBlocks {
                public static final DeferredHolder<Block, Block> GROUT = register(
                                "grout",
                                groutProperties());

                private static BlockBehaviour.Properties groutProperties() {
                        return BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.SAND)
                                        .strength(3.0F, 3.0F)
                                        .sound(SoundType.SAND)
                                        .friction(0.8F);
                }

                private static void register() {
                }

                private static DeferredHolder<Block, Block> register(
                                String name,
                                BlockBehaviour.Properties properties) {
                        return BLOCKS.register(name, () -> new Block(properties));
                }
        }

        public static final class StorageBlocks {
                public static final DeferredHolder<Block, Block> STORAGE_BLOCK_COBALT = register(
                                "storage_block_cobalt",
                                storageBlockProperties());

                public static final DeferredHolder<Block, Block> STORAGE_BLOCK_ARDITE = register(
                                "storage_block_ardite",
                                storageBlockProperties());

                public static final DeferredHolder<Block, Block> STORAGE_BLOCK_ALUBRASS = register(
                                "storage_block_alubrass",
                                storageBlockProperties());

                public static final DeferredHolder<Block, Block> STORAGE_BLOCK_KNIGHTSLIME = register(
                                "storage_block_knightslime",
                                storageBlockProperties());

                public static final DeferredHolder<Block, Block> STORAGE_BLOCK_MANYULLYN = register(
                                "storage_block_manyullyn",
                                storageBlockProperties());

                public static final DeferredHolder<Block, Block> STORAGE_BLOCK_PIGIRON = register(
                                "storage_block_pigiron",
                                storageBlockProperties());

                public static final DeferredHolder<Block, Block> STORAGE_BLOCK_SILKY_JEWEL = register(
                                "storage_block_silky_jewel",
                                storageBlockProperties());

                private static BlockBehaviour.Properties storageBlockProperties() {
                        return BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .strength(5.0F, 5.0F)
                                        .sound(SoundType.STONE)
                                        .requiresCorrectToolForDrops();
                }

                private static void register() {
                }

                private static DeferredHolder<Block, Block> register(
                                String name,
                                BlockBehaviour.Properties properties) {
                        return BLOCKS.register(name, () -> new Block(properties));
                }
        }

        public static final class SearedBlocks {
                public static final DeferredHolder<Block, Block> SEARED_STONE = register(
                                "seared_stone",
                                searedProperties());
                public static final DeferredHolder<Block, Block> SEARED_COBBLE = register(
                                "seared_cobble",
                                searedProperties());
                public static final DeferredHolder<Block, Block> SEARED_PAVER = register(
                                "seared_paver",
                                searedProperties());
                public static final DeferredHolder<Block, Block> SEARED_BRICKS = register(
                                "seared_bricks",
                                searedProperties());
                public static final DeferredHolder<Block, Block> SEARED_BRICK_CRACKED = register(
                                "seared_brick_cracked",
                                searedProperties());
                public static final DeferredHolder<Block, Block> SEARED_BRICK_FANCY = register(
                                "seared_brick_fancy",
                                searedProperties());
                public static final DeferredHolder<Block, Block> SEARED_BRICK_SQUARE = register(
                                "seared_brick_square",
                                searedProperties());
                public static final DeferredHolder<Block, Block> SEARED_ROAD = register(
                                "seared_road",
                                searedProperties());
                public static final DeferredHolder<Block, Block> SEARED_CREEPER = register(
                                "seared_creeper",
                                searedProperties());
                public static final DeferredHolder<Block, Block> SEARED_BRICK_TRIANGLE = register(
                                "seared_brick_triangle",
                                searedProperties());
                public static final DeferredHolder<Block, Block> SEARED_BRICK_SMALL = register(
                                "seared_brick_small",
                                searedProperties());
                public static final DeferredHolder<Block, Block> SEARED_TILE = register(
                                "seared_tile",
                                searedProperties());

                private static BlockBehaviour.Properties searedProperties() {
                        return BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.STONE)
                                        .strength(3.0F, 12.0F)
                                        .sound(SoundType.METAL)
                                        .requiresCorrectToolForDrops();
                }

                private static void register() {
                }

                private static DeferredHolder<Block, Block> register(
                                String name,
                                BlockBehaviour.Properties properties) {
                        return BLOCKS.register(name, () -> new Block(properties));
                }
        }

        public static final class SlimyBlocks {
                public static final DeferredHolder<Block, Block> GREEN_SLIMY_DIRT = register(
                                "green_slimy_dirt",
                                slimyDirtProperties(MapColor.COLOR_GREEN));
                public static final DeferredHolder<Block, Block> BLUE_SLIMY_DIRT = register(
                                "blue_slimy_dirt",
                                slimyDirtProperties(MapColor.COLOR_CYAN));
                public static final DeferredHolder<Block, Block> PURPLE_SLIMY_DIRT = register(
                                "purple_slimy_dirt",
                                slimyDirtProperties(MapColor.COLOR_PURPLE));
                public static final DeferredHolder<Block, Block> MAGMA_SLIMY_DIRT = register(
                                "magma_slimy_dirt",
                                slimyDirtProperties(MapColor.COLOR_ORANGE));

                public static final DeferredHolder<Block, Block> DIRT_SLIMY_GREEN_GRASS = registerGrass(
                                "dirt_slimy_green_grass", () -> Blocks.DIRT, "green");
                public static final DeferredHolder<Block, Block> GREEN_SLIMY_GREEN_GRASS = registerGrass(
                                "green_slimy_green_grass", () -> GREEN_SLIMY_DIRT.get(), "green");
                public static final DeferredHolder<Block, Block> BLUE_SLIMY_GREEN_GRASS = registerGrass(
                                "blue_slimy_green_grass", () -> BLUE_SLIMY_DIRT.get(), "green");
                public static final DeferredHolder<Block, Block> PURPLE_SLIMY_GREEN_GRASS = registerGrass(
                                "purple_slimy_green_grass", () -> PURPLE_SLIMY_DIRT.get(), "green");
                public static final DeferredHolder<Block, Block> MAGMA_SLIMY_GREEN_GRASS = registerGrass(
                                "magma_slimy_green_grass", () -> MAGMA_SLIMY_DIRT.get(), "green");

                public static final DeferredHolder<Block, Block> DIRT_SLIMY_BLUE_GRASS = registerGrass(
                                "dirt_slimy_blue_grass", () -> Blocks.DIRT, "blue");
                public static final DeferredHolder<Block, Block> GREEN_SLIMY_BLUE_GRASS = registerGrass(
                                "green_slimy_blue_grass", () -> GREEN_SLIMY_DIRT.get(), "blue");
                public static final DeferredHolder<Block, Block> BLUE_SLIMY_BLUE_GRASS = registerGrass(
                                "blue_slimy_blue_grass", () -> BLUE_SLIMY_DIRT.get(), "blue");
                public static final DeferredHolder<Block, Block> PURPLE_SLIMY_BLUE_GRASS = registerGrass(
                                "purple_slimy_blue_grass", () -> PURPLE_SLIMY_DIRT.get(), "blue");
                public static final DeferredHolder<Block, Block> MAGMA_SLIMY_BLUE_GRASS = registerGrass(
                                "magma_slimy_blue_grass", () -> MAGMA_SLIMY_DIRT.get(), "blue");

                public static final DeferredHolder<Block, Block> DIRT_SLIMY_PURPLE_GRASS = registerGrass(
                                "dirt_slimy_purple_grass", () -> Blocks.DIRT, "purple");
                public static final DeferredHolder<Block, Block> GREEN_SLIMY_PURPLE_GRASS = registerGrass(
                                "green_slimy_purple_grass", () -> GREEN_SLIMY_DIRT.get(), "purple");
                public static final DeferredHolder<Block, Block> BLUE_SLIMY_PURPLE_GRASS = registerGrass(
                                "blue_slimy_purple_grass", () -> BLUE_SLIMY_DIRT.get(), "purple");
                public static final DeferredHolder<Block, Block> PURPLE_SLIMY_PURPLE_GRASS = registerGrass(
                                "purple_slimy_purple_grass", () -> PURPLE_SLIMY_DIRT.get(), "purple");
                public static final DeferredHolder<Block, Block> MAGMA_SLIMY_PURPLE_GRASS = registerGrass(
                                "magma_slimy_purple_grass", () -> MAGMA_SLIMY_DIRT.get(), "purple");

                public static final DeferredHolder<Block, Block> DIRT_SLIMY_MAGMA_GRASS = registerGrass(
                                "dirt_slimy_magma_grass", () -> Blocks.DIRT, "magma");
                public static final DeferredHolder<Block, Block> GREEN_SLIMY_MAGMA_GRASS = registerGrass(
                                "green_slimy_magma_grass", () -> GREEN_SLIMY_DIRT.get(), "magma");
                public static final DeferredHolder<Block, Block> BLUE_SLIMY_MAGMA_GRASS = registerGrass(
                                "blue_slimy_magma_grass", () -> BLUE_SLIMY_DIRT.get(), "magma");
                public static final DeferredHolder<Block, Block> PURPLE_SLIMY_MAGMA_GRASS = registerGrass(
                                "purple_slimy_magma_grass", () -> PURPLE_SLIMY_DIRT.get(), "magma");
                public static final DeferredHolder<Block, Block> MAGMA_SLIMY_MAGMA_GRASS = registerGrass(
                                "magma_slimy_magma_grass", () -> MAGMA_SLIMY_DIRT.get(), "magma");

                public static final DeferredHolder<Block, Block> GREEN_CONGEALED_SLIME_BLOCK = registerCongealed(
                                "green_congealed_slime_block",
                                congealedSlimeBlockProperties(MapColor.COLOR_GREEN));
                public static final DeferredHolder<Block, Block> BLUE_CONGEALED_SLIME_BLOCK = registerCongealed(
                                "blue_congealed_slime_block",
                                congealedSlimeBlockProperties(MapColor.COLOR_CYAN));
                public static final DeferredHolder<Block, Block> PURPLE_CONGEALED_SLIME_BLOCK = registerCongealed(
                                "purple_congealed_slime_block",
                                congealedSlimeBlockProperties(MapColor.COLOR_PURPLE));
                public static final DeferredHolder<Block, Block> MAGMA_CONGEALED_SLIME_BLOCK = registerCongealed(
                                "magma_congealed_slime_block",
                                congealedSlimeBlockProperties(MapColor.COLOR_ORANGE));
                public static final DeferredHolder<Block, Block> BLOOD_CONGEALED_SLIME_BLOCK = registerCongealed(
                                "blood_congealed_slime_block",
                                congealedSlimeBlockProperties(MapColor.COLOR_RED));

                public static final DeferredHolder<Block, Block> BLUE_SLIME_BLOCK = registerSlime(
                                "blue_slime_block",
                                slimeBlockProperties(MapColor.COLOR_CYAN));
                public static final DeferredHolder<Block, Block> PURPLE_SLIME_BLOCK = registerSlime(
                                "purple_slime_block",
                                slimeBlockProperties(MapColor.COLOR_PURPLE));
                public static final DeferredHolder<Block, Block> MAGMA_SLIME_BLOCK = registerSlime(
                                "magma_slime_block",
                                slimeBlockProperties(MapColor.COLOR_ORANGE));
                public static final DeferredHolder<Block, Block> BLOOD_SLIME_BLOCK = registerSlime(
                                "blood_slime_block",
                                slimeBlockProperties(MapColor.COLOR_RED));

                // Each call returns fresh mutable properties. Colors share behavior, not
                // instances.
                private static BlockBehaviour.Properties slimyDirtProperties(MapColor color) {
                        return BlockBehaviour.Properties.of()
                                        .mapColor(color)
                                        .strength(0.55F)
                                        .sound(SoundType.SLIME_BLOCK);
                }

                private static BlockBehaviour.Properties slimyGrassProperties() {
                        return BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.GRASS)
                                        .strength(0.65F)
                                        .sound(SoundType.GRASS).friction(0.65F).randomTicks();
                }

                private static BlockBehaviour.Properties congealedSlimeBlockProperties(MapColor color) {
                        return BlockBehaviour.Properties.of()
                                        .mapColor(color)
                                        .strength(0.5F)
                                        .sound(SoundType.SLIME_BLOCK).friction(0.5F).noOcclusion();
                }

                private static BlockBehaviour.Properties slimeBlockProperties(MapColor color) {
                        return BlockBehaviour.Properties.ofFullCopy(Blocks.SLIME_BLOCK).mapColor(color);
                }

                private static void register() {
                }

                private static DeferredHolder<Block, Block> register(String name,
                                BlockBehaviour.Properties properties) {
                        return BLOCKS.register(name, () -> new Block(properties));
                }

                private static DeferredHolder<Block, Block> registerCongealed(String name,
                                BlockBehaviour.Properties properties) {
                        return BLOCKS.register(name, () -> new CongealedSlimeBlock(properties));
                }

                private static DeferredHolder<Block, Block> registerSlime(String name,
                                BlockBehaviour.Properties properties) {
                        return BLOCKS.register(name, () -> new ColoredSlimeBlock(properties));
                }

                private static DeferredHolder<Block, Block> registerGrass(String name, Supplier<Block> soil,
                                String foliage) {
                        return BLOCKS.register(name,
                                        () -> new SlimyGrassBlock(slimyGrassProperties(), soil.get(), foliage));
                }
        }
}
