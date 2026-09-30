package com.nurby.tinkerslegacy.registry;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.block.CongealedSlimeBlock;
import com.nurby.tinkerslegacy.block.ColoredSlimeBlock;
import com.nurby.tinkerslegacy.block.SlimyGrassBlock;
import com.nurby.tinkerslegacy.block.SlimyFoliage;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.EnumMap;

public final class TLBlocks {
        public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(
                        BuiltInRegistries.BLOCK,
                        TinkersLegacy.MODID);

        public static final class OreBlocks {
                public static final DeferredHolder<Block, Block> NETHER_COBALT_ORE = TLBlocks.registerBlock(
                                "nether_cobalt_ore",
                                netherOreProperties());

                public static final DeferredHolder<Block, Block> NETHER_ARDITE_ORE = TLBlocks.registerBlock(
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
        }

        public static final class IntermediaryBlocks {
                public static final DeferredHolder<Block, Block> GROUT = TLBlocks.registerBlock(
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
        }

        public static final class StorageBlocks {
                public static final DeferredHolder<Block, Block> STORAGE_BLOCK_COBALT = TLBlocks.registerBlock(
                                "storage_block_cobalt",
                                storageBlockProperties());

                public static final DeferredHolder<Block, Block> STORAGE_BLOCK_ARDITE = TLBlocks.registerBlock(
                                "storage_block_ardite",
                                storageBlockProperties());

                public static final DeferredHolder<Block, Block> STORAGE_BLOCK_ALUBRASS = TLBlocks.registerBlock(
                                "storage_block_alubrass",
                                storageBlockProperties());

                public static final DeferredHolder<Block, Block> STORAGE_BLOCK_KNIGHTSLIME = TLBlocks.registerBlock(
                                "storage_block_knightslime",
                                storageBlockProperties());

                public static final DeferredHolder<Block, Block> STORAGE_BLOCK_MANYULLYN = TLBlocks.registerBlock(
                                "storage_block_manyullyn",
                                storageBlockProperties());

                public static final DeferredHolder<Block, Block> STORAGE_BLOCK_PIGIRON = TLBlocks.registerBlock(
                                "storage_block_pigiron",
                                storageBlockProperties());

                public static final DeferredHolder<Block, Block> STORAGE_BLOCK_SILKY_JEWEL = TLBlocks.registerBlock(
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
        }

        public static final class SearedBlocks {
                public static final DeferredHolder<Block, Block> SEARED_STONE = TLBlocks.registerBlock(
                                "seared_stone",
                                searedProperties());

                public static final DeferredHolder<Block, Block> SEARED_COBBLE = TLBlocks.registerBlock(
                                "seared_cobble",
                                searedProperties());

                public static final DeferredHolder<Block, Block> SEARED_PAVER = TLBlocks.registerBlock(
                                "seared_paver",
                                searedProperties());

                public static final DeferredHolder<Block, Block> SEARED_BRICKS = TLBlocks.registerBlock(
                                "seared_bricks",
                                searedProperties());

                public static final DeferredHolder<Block, Block> SEARED_BRICK_CRACKED = TLBlocks.registerBlock(
                                "seared_brick_cracked",
                                searedProperties());

                public static final DeferredHolder<Block, Block> SEARED_BRICK_FANCY = TLBlocks.registerBlock(
                                "seared_brick_fancy",
                                searedProperties());

                public static final DeferredHolder<Block, Block> SEARED_BRICK_SQUARE = TLBlocks.registerBlock(
                                "seared_brick_square",
                                searedProperties());

                public static final DeferredHolder<Block, Block> SEARED_ROAD = TLBlocks.registerBlock(
                                "seared_road",
                                searedProperties());

                public static final DeferredHolder<Block, Block> SEARED_CREEPER = TLBlocks.registerBlock(
                                "seared_creeper",
                                searedProperties());

                public static final DeferredHolder<Block, Block> SEARED_BRICK_TRIANGLE = TLBlocks.registerBlock(
                                "seared_brick_triangle",
                                searedProperties());

                public static final DeferredHolder<Block, Block> SEARED_BRICK_SMALL = TLBlocks.registerBlock(
                                "seared_brick_small",
                                searedProperties());

                public static final DeferredHolder<Block, Block> SEARED_TILE = TLBlocks.registerBlock(
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
        }

        public static final class SlimyBlocks {
                public enum Soil {
                        DIRT("dirt", MapColor.DIRT),
                        GREEN("green", MapColor.COLOR_GREEN),
                        BLUE("blue", MapColor.COLOR_CYAN),
                        PURPLE("purple", MapColor.COLOR_PURPLE),
                        MAGMA("magma", MapColor.COLOR_ORANGE);

                        private final String name;
                        private final MapColor color;

                        Soil(String name, MapColor color) {
                                this.name = name;
                                this.color = color;
                        }
                }

                private record GrassVariant(Soil soil, SlimyFoliage foliage) {
                }

                private static final Map<Soil, DeferredHolder<Block, Block>> SLIMY_SOIL = registerSoilVariants();
                private static final Map<GrassVariant, DeferredHolder<Block, Block>> SLIMY_GRASS = registerGrassVariants();

                private static Map<Soil, DeferredHolder<Block, Block>> registerSoilVariants() {
                        Map<Soil, DeferredHolder<Block, Block>> variants = new EnumMap<>(Soil.class);

                        for (Soil soil : Soil.values()) {
                                if (soil == Soil.DIRT)
                                        continue;

                                DeferredHolder<Block, Block> holder = TLBlocks.registerBlock(
                                                soil.name + "_slimy_dirt",
                                                slimyDirtProperties(soil.color));

                                variants.put(soil, holder);
                        }

                        return variants;
                }

                private static Map<GrassVariant, DeferredHolder<Block, Block>> registerGrassVariants() {
                        Map<GrassVariant, DeferredHolder<Block, Block>> variants = new LinkedHashMap<>();
                        new LinkedHashMap<>();

                        for (SlimyFoliage foliage : SlimyFoliage.values()) {
                                for (Soil soil : Soil.values()) {
                                        String name = soil.name + "_slimy_" + foliage.getSerializedName() + "_grass";

                                        Supplier<Block> ground = () -> soil == Soil.DIRT ? Blocks.DIRT
                                                        : slimySoil(soil).get();

                                        DeferredHolder<Block, Block> holder = registerGrass(name, ground, foliage);

                                        variants.put(new GrassVariant(soil, foliage), holder);
                                }
                        }

                        return variants;
                }

                public static DeferredHolder<Block, Block> slimySoil(Soil soil) {
                        if (soil == Soil.DIRT) {
                                throw new IllegalArgumentException("Soil.DIRT uses vanilla Blocks.DIRT");
                        }

                        return SLIMY_SOIL.get(soil);
                }

                public static DeferredHolder<Block, Block> slimyGrass(Soil soil, SlimyFoliage foliage) {
                        return SLIMY_GRASS.get(new GrassVariant(soil, foliage));
                }

                public static final DeferredHolder<Block, Block> GREEN_CONGEALED_SLIME_BLOCK = TLBlocks.registerBlock(
                                "green_congealed_slime_block",
                                CongealedSlimeBlock::new,
                                congealedSlimeBlockProperties(MapColor.COLOR_GREEN));
                public static final DeferredHolder<Block, Block> BLUE_CONGEALED_SLIME_BLOCK = TLBlocks.registerBlock(
                                "blue_congealed_slime_block",
                                CongealedSlimeBlock::new,
                                congealedSlimeBlockProperties(MapColor.COLOR_CYAN));
                public static final DeferredHolder<Block, Block> PURPLE_CONGEALED_SLIME_BLOCK = TLBlocks.registerBlock(
                                "purple_congealed_slime_block",
                                CongealedSlimeBlock::new,
                                congealedSlimeBlockProperties(MapColor.COLOR_PURPLE));
                public static final DeferredHolder<Block, Block> MAGMA_CONGEALED_SLIME_BLOCK = TLBlocks.registerBlock(
                                "magma_congealed_slime_block",
                                CongealedSlimeBlock::new,
                                congealedSlimeBlockProperties(MapColor.COLOR_ORANGE));
                public static final DeferredHolder<Block, Block> BLOOD_CONGEALED_SLIME_BLOCK = TLBlocks.registerBlock(
                                "blood_congealed_slime_block",
                                CongealedSlimeBlock::new,
                                congealedSlimeBlockProperties(MapColor.COLOR_RED));

                public static final DeferredHolder<Block, Block> BLUE_SLIME_BLOCK = TLBlocks.registerBlock(
                                "blue_slime_block",
                                ColoredSlimeBlock::new,
                                slimeBlockProperties(MapColor.COLOR_CYAN));
                public static final DeferredHolder<Block, Block> PURPLE_SLIME_BLOCK = TLBlocks.registerBlock(
                                "purple_slime_block",
                                ColoredSlimeBlock::new,
                                slimeBlockProperties(MapColor.COLOR_PURPLE));
                public static final DeferredHolder<Block, Block> MAGMA_SLIME_BLOCK = TLBlocks.registerBlock(
                                "magma_slime_block",
                                ColoredSlimeBlock::new,
                                slimeBlockProperties(MapColor.COLOR_ORANGE));
                public static final DeferredHolder<Block, Block> BLOOD_SLIME_BLOCK = TLBlocks.registerBlock(
                                "blood_slime_block",
                                ColoredSlimeBlock::new,
                                slimeBlockProperties(MapColor.COLOR_RED));

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

                private static DeferredHolder<Block, Block> registerGrass(
                                String name,
                                Supplier<Block> soil,
                                SlimyFoliage foliage) {
                        return BLOCKS.register(
                                        name,
                                        () -> SlimyGrassBlock.registerVariant(
                                                        new SlimyGrassBlock(
                                                                        slimyGrassProperties(),
                                                                        soil.get(),
                                                                        foliage)));
                }
        }

        public static void register(IEventBus modBus) {
                OreBlocks.register();
                IntermediaryBlocks.register();
                StorageBlocks.register();
                SearedBlocks.register();
                SlimyBlocks.register();
                BLOCKS.register(modBus);
        }

        public static DeferredHolder<Block, Block> registerBlock(
                        String name,
                        BlockBehaviour.Properties properties) {
                return registerBlock(name, Block::new, properties);
        }

        private static DeferredHolder<Block, Block> registerBlock(
                        String name,
                        Function<BlockBehaviour.Properties, ? extends Block> factory,
                        BlockBehaviour.Properties properties) {
                return BLOCKS.register(name, () -> factory.apply(properties));
        }
}
