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
import net.minecraft.resources.ResourceLocation;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Collections;

public final class TLBlocks {
        public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(
                        BuiltInRegistries.BLOCK,
                        TinkersLegacy.MODID);

        public static final class OreBlocks {
                public enum Type {
                        COBALT,
                        ARDITE
                }

                private static final Map<Type, DeferredHolder<Block, Block>> NETHER_VARIANTS = registerVariants(
                                Type.values(),
                                type -> "nether_" + variantName(type) + "_ore",
                                type -> new Block(netherOreProperties()));

                public static final List<DeferredHolder<Block, Block>> ALL = List.copyOf(NETHER_VARIANTS.values());

                public static DeferredHolder<Block, Block> get(Type type) {
                        return NETHER_VARIANTS.get(type);
                }

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
                public enum Type {
                        GROUT(() -> new Block(groutProperties()));

                        private final Supplier<? extends Block> factory;

                        Type(Supplier<? extends Block> factory) {
                                this.factory = factory;
                        }
                }

                private static final Map<Type, DeferredHolder<Block, Block>> VARIANTS = registerVariants(
                                Type.values(),
                                TLBlocks::variantName,
                                type -> type.factory.get());

                public static final List<DeferredHolder<Block, Block>> ALL = List.copyOf(VARIANTS.values());

                public static DeferredHolder<Block, Block> get(Type type) {
                        return VARIANTS.get(type);
                }

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
                public enum Type {
                        COBALT,
                        ARDITE,
                        ALUBRASS,
                        KNIGHTSLIME,
                        MANYULLYN,
                        PIGIRON,
                        SILKY_JEWEL
                }

                private static final Map<Type, DeferredHolder<Block, Block>> VARIANTS = registerVariants(
                                Type.values(),
                                type -> "storage_block_" + variantName(type),
                                type -> new Block(storageBlockProperties()));

                public static final List<DeferredHolder<Block, Block>> ALL = List.copyOf(VARIANTS.values());

                public static DeferredHolder<Block, Block> get(Type type) {
                        return VARIANTS.get(type);
                }

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
                public enum Type {
                        STONE,
                        COBBLE,
                        PAVER,
                        BRICKS,
                        BRICK_CRACKED,
                        BRICK_FANCY,
                        BRICK_SQUARE,
                        ROAD,
                        CREEPER,
                        BRICK_TRIANGLE,
                        BRICK_SMALL,
                        TILE
                }

                private static final Map<Type, DeferredHolder<Block, Block>> VARIANTS = registerVariants(
                                Type.values(),
                                type -> "seared_" + variantName(type),
                                type -> new Block(searedProperties()));

                public static final List<DeferredHolder<Block, Block>> ALL = List.copyOf(VARIANTS.values());

                public static DeferredHolder<Block, Block> get(Type type) {
                        return VARIANTS.get(type);
                }

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
                // Soil
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

                private static final Map<Soil, DeferredHolder<Block, Block>> SLIMY_SOIL = registerSoilVariants();

                public static final List<DeferredHolder<Block, Block>> SOILS = List.copyOf(SLIMY_SOIL.values());

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

                public static DeferredHolder<Block, Block> slimySoil(Soil soil) {
                        if (soil == Soil.DIRT) {
                                throw new IllegalArgumentException("Soil.DIRT uses vanilla Blocks.DIRT");
                        }

                        return SLIMY_SOIL.get(soil);
                }

                // Grass
                private record GrassVariant(Soil soil, SlimyFoliage foliage) {
                }

                private static final Map<GrassVariant, DeferredHolder<Block, Block>> SLIMY_GRASS = registerGrassVariants();

                public static final List<DeferredHolder<Block, Block>> GRASSES = List.copyOf(SLIMY_GRASS.values());

                private static Map<GrassVariant, DeferredHolder<Block, Block>> registerGrassVariants() {
                        Map<GrassVariant, DeferredHolder<Block, Block>> variants = new LinkedHashMap<>();

                        for (SlimyFoliage foliage : SlimyFoliage.values()) {
                                for (Soil soil : Soil.values()) {
                                        String name = soil.name + "_slimy_" + foliage.getSerializedName() + "_grass";

                                        Supplier<Block> ground = () -> soil == Soil.DIRT ? Blocks.DIRT
                                                        : TLBlocks.block(soil);

                                        DeferredHolder<Block, Block> holder = registerGrass(name, ground, foliage);

                                        variants.put(new GrassVariant(soil, foliage), holder);
                                }
                        }

                        return variants;
                }

                public static DeferredHolder<Block, Block> slimyGrass(Soil soil, SlimyFoliage foliage) {
                        return SLIMY_GRASS.get(new GrassVariant(soil, foliage));
                }

                // Slime Types
                public enum SlimeType {
                        GREEN(MapColor.COLOR_GREEN),
                        BLUE(MapColor.COLOR_CYAN),
                        PURPLE(MapColor.COLOR_PURPLE),
                        MAGMA(MapColor.COLOR_ORANGE),
                        BLOOD(MapColor.COLOR_RED);

                        private final MapColor color;

                        SlimeType(MapColor color) {
                                this.color = color;
                        }
                }

                // Congealed Slime
                public static final Map<SlimeType, DeferredHolder<Block, Block>> CONGEALED_VARIANTS = registerVariants(
                                SlimeType.values(),
                                type -> variantName(type) + "_congealed_slime_block",
                                type -> new CongealedSlimeBlock(congealedSlimeBlockProperties(type.color)));

                public static final List<DeferredHolder<Block, Block>> CONGEALED = List
                                .copyOf(CONGEALED_VARIANTS.values());

                public static DeferredHolder<Block, Block> congealed(SlimeType type) {
                        return CONGEALED_VARIANTS.get(type);
                }

                // Slime Blocks
                private static final Map<SlimeType, DeferredHolder<Block, Block>> SLIME_VARIANTS = registerVariants(
                                new SlimeType[] {
                                                SlimeType.BLUE,
                                                SlimeType.PURPLE,
                                                SlimeType.MAGMA,
                                                SlimeType.BLOOD
                                },
                                type -> variantName(type) + "_slime_block",
                                type -> new ColoredSlimeBlock(slimeBlockProperties(type.color)));

                public static final List<DeferredHolder<Block, Block>> SLIME = List.copyOf(SLIME_VARIANTS.values());

                public static DeferredHolder<Block, Block> slime(SlimeType type) {
                        if (type == SlimeType.GREEN) {
                                throw new IllegalArgumentException(
                                                "Green slime uses vanilla Blocks.SLIME_BLOCK");
                        }

                        return SLIME_VARIANTS.get(type);
                }

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
                        return TLBlocks.add(
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

        private static final Map<ResourceLocation, DeferredHolder<Block, Block>> BY_ID = new LinkedHashMap<>();

        // Adders
        public static DeferredHolder<Block, Block> add(
                        String name,
                        Supplier<? extends Block> factory) {
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, name);

                if (BY_ID.containsKey(id)) {
                        throw new IllegalArgumentException(
                                        "Block already registered: " + id);
                }

                DeferredHolder<Block, Block> holder = BLOCKS.register(name, factory);

                BY_ID.put(id, holder);
                return holder;
        }

        public static DeferredHolder<Block, Block> add(
                        String name,
                        BlockBehaviour.Properties properties) {
                return add(name, () -> new Block(properties));
        }

        public static DeferredHolder<Block, Block> add(
                        String name,
                        Function<BlockBehaviour.Properties, ? extends Block> factory,
                        BlockBehaviour.Properties properties) {
                return add(name, () -> factory.apply(properties));
        }

        // Getters
        public static DeferredHolder<Block, Block> get(String name) {
                return get(ResourceLocation.fromNamespaceAndPath(
                                TinkersLegacy.MODID,
                                name));
        }

        public static DeferredHolder<Block, Block> get(ResourceLocation id) {
                DeferredHolder<Block, Block> holder = BY_ID.get(id);

                if (holder == null) {
                        throw new IllegalArgumentException(
                                        "No block holder registered for " + id
                                                        + ". Check the ID and group initialization.");
                }

                return holder;
        }

        public static DeferredHolder<Block, Block> get(OreBlocks.Type type) {
                return OreBlocks.get(type);
        }

        public static DeferredHolder<Block, Block> get(IntermediaryBlocks.Type type) {
                return IntermediaryBlocks.get(type);
        }

        public static DeferredHolder<Block, Block> get(StorageBlocks.Type type) {
                return StorageBlocks.get(type);
        }

        public static DeferredHolder<Block, Block> get(SearedBlocks.Type type) {
                return SearedBlocks.get(type);
        }

        public static DeferredHolder<Block, Block> get(SlimyBlocks.Soil soil) {
                return SlimyBlocks.slimySoil(soil);
        }

        public static DeferredHolder<Block, Block> get(SlimyBlocks.Soil soil, SlimyFoliage foliage) {

                return SlimyBlocks.slimyGrass(soil, foliage);
        }

        public static Block block(String name) {
                return get(name).get();
        }

        public static Block block(ResourceLocation id) {
                return get(id).get();
        }

        public static Block block(OreBlocks.Type type) {
                return get(type).get();
        }

        public static Block block(IntermediaryBlocks.Type type) {
                return get(type).get();
        }

        public static Block block(StorageBlocks.Type type) {
                return get(type).get();
        }

        public static Block block(SearedBlocks.Type type) {
                return get(type).get();
        }

        public static Block block(SlimyBlocks.Soil soil) {
                return get(soil).get();
        }

        public static Block block(SlimyBlocks.Soil soil, SlimyFoliage foliage) {
                return get(soil, foliage).get();
        }

        // Registration
        public static DeferredHolder<Block, Block> registerBlock(
                        String name,
                        BlockBehaviour.Properties properties) {
                return add(name, properties);
        }

        private static <E extends Enum<E>> Map<E, DeferredHolder<Block, Block>> registerVariants(
                        E[] variants,
                        Function<E, String> idFactory,
                        Function<E, ? extends Block> blockFactory) {
                Map<E, DeferredHolder<Block, Block>> result = new LinkedHashMap<>();

                for (E variant : variants) {
                        String id = idFactory.apply(variant);

                        DeferredHolder<Block, Block> holder = TLBlocks.add(
                                        id,
                                        () -> blockFactory.apply(variant));

                        result.put(variant, holder);
                }

                return Collections.unmodifiableMap(result);
        }

        // Variants
        private static String variantName(Enum<?> variant) {
                return variant.name().toLowerCase(Locale.ROOT);
        }

}
