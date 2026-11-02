package com.nurby.tinkerslegacy.registry;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.item.TooltipItem;
import com.nurby.tinkerslegacy.item.PatternItem;
import com.nurby.tinkerslegacy.item.MaterialShardItem;
import com.nurby.tinkerslegacy.item.TooltipBlockItem;
import com.nurby.tinkerslegacy.item.dynamic.DynamicPart;
import com.nurby.tinkerslegacy.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.library.part.PartDefinition;
import com.nurby.tinkerslegacy.library.tool.ToolDefinition;
import com.nurby.tinkerslegacy.block.SlimyFoliage;
import com.nurby.tinkerslegacy.registry.TLBlocks.SlimyBlocks.Soil;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.List;
import java.util.Locale;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TLItems {
        private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
                        BuiltInRegistries.ITEM,
                        TinkersLegacy.MODID);

        // Read when tab contents are built, after item registration has finished.
        static List<Item> registeredItems() {
                return ITEMS.getEntries().stream().<Item>map(holder -> holder.get()).toList();
        }

        public static final class Items {
                public static final class IntermediaryItems {
                        public enum Type {
                                SILKY_CLOTH,
                                BLANK_PATTERN,
                                PATTERN(() -> new PatternItem(new Item.Properties()));

                                private final Supplier<? extends Item> factory;

                                Type() {
                                        this(() -> new Item(new Item.Properties()));
                                }

                                Type(Supplier<? extends Item> factory) {
                                        this.factory = factory;
                                }
                        }

                        private static final Map<Type, DeferredHolder<Item, Item>> VARIANTS = registerVariants(
                                        Type.values(),
                                        TLItems::variantName,
                                        type -> type.factory.get());

                        public static final List<DeferredHolder<Item, Item>> ALL = List.copyOf(VARIANTS.values());

                        public static DeferredHolder<Item, Item> get(Type type) {
                                return VARIANTS.get(type);
                        }

                        private static void register() {
                        }
                }

                public static final class Miscellaneous {
                        public enum Type {
                                CREATIVE_MODIFIER(() -> new TooltipItem(
                                                new Item.Properties(),
                                                "tooltip.tinkerslegacy.creative_modifier")),

                                BALL_OF_MOSS(() -> new TooltipItem(
                                                new Item.Properties(),
                                                "tooltip.tinkerslegacy.ball_of_moss")),

                                MENDING_MOSS(() -> new TooltipItem(
                                                new Item.Properties(),
                                                "tooltip.tinkerslegacy.mending_moss")),

                                EXPANDER_VERTICAL(() -> new TooltipItem(
                                                new Item.Properties(),
                                                "tooltip.tinkerslegacy.expander_vertical")),

                                EXPANDER_HORIZONTAL(() -> new TooltipItem(
                                                new Item.Properties(),
                                                "tooltip.tinkerslegacy.expander_horizontal")),

                                REINFORCED_PLATE(() -> new Item(new Item.Properties())),
                                NECROTIC_BONE(() -> new Item(new Item.Properties())),
                                BACON(() -> new Item(new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder()
                                                .nutrition(4).saturationModifier(0.6F).build())));

                                private final Supplier<? extends Item> factory;

                                Type(Supplier<? extends Item> factory) {
                                        this.factory = factory;
                                }
                        }

                        private static final Map<Type, DeferredHolder<Item, Item>> VARIANTS = registerVariants(
                                        Type.values(),
                                        TLItems::variantName,
                                        type -> type.factory.get());

                        public static final List<DeferredHolder<Item, Item>> ALL = List.copyOf(VARIANTS.values());

                        public static DeferredHolder<Item, Item> get(Type type) {
                                return VARIANTS.get(type);
                        }

                        private static void register() {
                        }
                }

                public static final class RawOres {
                        public enum Type {
                                COBALT,
                                ARDITE;

                                private final Supplier<? extends Item> factory;

                                Type() {
                                        this(() -> new Item(new Item.Properties()));
                                }

                                Type(Supplier<? extends Item> factory) {
                                        this.factory = factory;
                                }
                        }

                        private static final Map<Type, DeferredHolder<Item, Item>> VARIANTS = registerVariants(
                                        Type.values(),
                                        type -> "raw_" + variantName(type),
                                        type -> type.factory.get());

                        public static final List<DeferredHolder<Item, Item>> ALL = List.copyOf(VARIANTS.values());

                        public static DeferredHolder<Item, Item> get(Type type) {
                                return VARIANTS.get(type);
                        }

                        private static void register() {
                        }
                }

                public static final class Nuggets {
                        public enum Type {
                                COBALT,
                                ARDITE,
                                ALUBRASS,
                                KNIGHTSLIME,
                                MANYULLYN,
                                PIGIRON;

                                private final Supplier<? extends Item> factory;

                                Type() {
                                        this(() -> new Item(new Item.Properties()));
                                }

                                Type(Supplier<? extends Item> factory) {
                                        this.factory = factory;
                                }
                        }

                        private static final Map<Type, DeferredHolder<Item, Item>> VARIANTS = registerVariants(
                                        Type.values(),
                                        type -> variantName(type) + "_nugget",
                                        type -> type.factory.get());

                        public static final List<DeferredHolder<Item, Item>> ALL = List.copyOf(VARIANTS.values());

                        public static DeferredHolder<Item, Item> get(Type type) {
                                return VARIANTS.get(type);
                        }

                        private static void register() {
                        }
                }

                public static final class Ingots {
                        public enum Type {
                                COBALT,
                                ARDITE,
                                ALUBRASS,

                                KNIGHTSLIME(() -> new TooltipItem(
                                                new Item.Properties(),
                                                "tooltip.tinkerslegacy.knightslime_ingot")),

                                MANYULLYN,

                                PIGIRON(() -> new TooltipItem(
                                                new Item.Properties(),
                                                "tooltip.tinkerslegacy.pigiron_ingot"));

                                private final Supplier<? extends Item> factory;

                                Type() {
                                        this(() -> new Item(new Item.Properties()));
                                }

                                Type(Supplier<? extends Item> factory) {
                                        this.factory = factory;
                                }
                        }

                        private static final Map<Type, DeferredHolder<Item, Item>> VARIANTS = registerVariants(
                                        Type.values(),
                                        type -> variantName(type) + "_ingot",
                                        type -> type.factory.get());

                        public static final List<DeferredHolder<Item, Item>> ALL = List.copyOf(VARIANTS.values());

                        public static DeferredHolder<Item, Item> get(Type type) {
                                return VARIANTS.get(type);
                        }

                        private static void register() {
                        }
                }

                public static final class Gems {
                        public enum Type {
                                SILKY_JEWEL(() -> new TooltipItem(
                                                new Item.Properties(),
                                                "tooltip.tinkerslegacy.silky_jewel")),

                                GREEN_SLIME_CRYSTAL(() -> new TooltipItem(
                                                new Item.Properties(),
                                                "tooltip.tinkerslegacy.slime_crystal")),

                                BLUE_SLIME_CRYSTAL(() -> new TooltipItem(
                                                new Item.Properties(),
                                                "tooltip.tinkerslegacy.slime_crystal")),

                                MAGMA_SLIME_CRYSTAL(() -> new TooltipItem(
                                                new Item.Properties(),
                                                "tooltip.tinkerslegacy.slime_crystal"));

                                private final Supplier<? extends Item> factory;

                                Type(Supplier<? extends Item> factory) {
                                        this.factory = factory;
                                }
                        }

                        private static final Map<Type, DeferredHolder<Item, Item>> VARIANTS = registerVariants(
                                        Type.values(),
                                        TLItems::variantName,
                                        type -> type.factory.get());

                        public static final List<DeferredHolder<Item, Item>> ALL = List.copyOf(VARIANTS.values());

                        public static DeferredHolder<Item, Item> get(Type type) {
                                return VARIANTS.get(type);
                        }

                        private static void register() {
                        }
                }

                public static final class Bricks {
                        public enum Type {
                                SEARED,

                                MUD(() -> new TooltipItem(
                                                new Item.Properties(),
                                                "tooltip.tinkerslegacy.mud_brick")),

                                DRIED;

                                private final Supplier<? extends Item> factory;

                                Type() {
                                        this(() -> new Item(new Item.Properties()));
                                }

                                Type(Supplier<? extends Item> factory) {
                                        this.factory = factory;
                                }
                        }

                        private static final Map<Type, DeferredHolder<Item, Item>> VARIANTS = registerVariants(
                                        Type.values(),
                                        type -> variantName(type) + "_brick",
                                        type -> type.factory.get());

                        public static final List<DeferredHolder<Item, Item>> ALL = List.copyOf(VARIANTS.values());

                        public static DeferredHolder<Item, Item> get(Type type) {
                                return VARIANTS.get(type);
                        }

                        private static void register() {
                        }
                }

                public static final class Slimeballs {
                        public enum Type {
                                BLUE(() -> new TooltipItem(
                                                new Item.Properties().food(new FoodProperties.Builder()
                                                                .nutrition(1).saturationModifier(1.0F).alwaysEdible()
                                                                .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 900, 2), 1.0F)
                                                                .effect(() -> new MobEffectInstance(MobEffects.JUMP, 1200, 2), 1.0F)
                                                                .build()),
                                                "tooltip.tinkerslegacy.blue_slimeball")),

                                PURPLE(() -> new TooltipItem(
                                                new Item.Properties().food(new FoodProperties.Builder()
                                                                .nutrition(1).saturationModifier(2.0F).alwaysEdible()
                                                                .effect(() -> new MobEffectInstance(MobEffects.UNLUCK, 900), 1.0F)
                                                                .effect(() -> new MobEffectInstance(MobEffects.LUCK, 1200), 1.0F)
                                                                .build()),
                                                "tooltip.tinkerslegacy.purple_slimeball")),

                                MAGMA(() -> new TooltipItem(
                                                new Item.Properties().food(new FoodProperties.Builder()
                                                                .nutrition(2).saturationModifier(1.0F).alwaysEdible()
                                                                .effect(() -> new MobEffectInstance(MobEffects.WEAKNESS, 900), 1.0F)
                                                                .effect(() -> new MobEffectInstance(MobEffects.WITHER, 300), 1.0F)
                                                                .effect(() -> new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1200), 1.0F)
                                                                .build()),
                                                "tooltip.tinkerslegacy.magma_slimeball")),

                                BLOOD(() -> new TooltipItem(
                                                new Item.Properties().food(new FoodProperties.Builder()
                                                                .nutrition(1).saturationModifier(1.5F).alwaysEdible()
                                                                .effect(() -> new MobEffectInstance(MobEffects.POISON, 900, 2), 1.0F)
                                                                .effect(() -> new MobEffectInstance(MobEffects.HEALTH_BOOST, 1200), 1.0F)
                                                                .build()),
                                                "tooltip.tinkerslegacy.blood_slimeball"));

                                private final Supplier<? extends Item> factory;

                                Type(Supplier<? extends Item> factory) {
                                        this.factory = factory;
                                }
                        }

                        private static final Map<Type, DeferredHolder<Item, Item>> VARIANTS = registerVariants(
                                        Type.values(),
                                        type -> variantName(type) + "_slimeball",
                                        type -> type.factory.get());

                        public static final List<DeferredHolder<Item, Item>> ALL = List.copyOf(VARIANTS.values());

                        public static DeferredHolder<Item, Item> get(Type type) {
                                return VARIANTS.get(type);
                        }

                        private static void register() {
                        }
                }

                private static void register() {
                        IntermediaryItems.register();
                        Miscellaneous.register();
                        RawOres.register();
                        Nuggets.register();
                        Ingots.register();
                        Gems.register();
                        Bricks.register();
                        Slimeballs.register();
                }
        }

        public static final class Blocks {
                private static final Map<ResourceLocation, DeferredHolder<Item, BlockItem>> ITEMS = new LinkedHashMap<>();

                public static final class OreBlocks {
                        static {
                                for (var block : TLBlocks.OreBlocks.ALL) {
                                        TLItems.Blocks.registerBlockItem(block);
                                }
                        }

                        private static void register() {
                        }
                }

                public static final class IntermediaryBlocks {
                        public static final DeferredHolder<Item, BlockItem> GROUT = TLItems.Blocks
                                        .registerBlockItem(
                                                        TLBlocks.get(TLBlocks.IntermediaryBlocks.Type.GROUT),
                                                        "tooltip.tinkerslegacy.grout");

                        static {
                                for (var type : TLBlocks.IntermediaryBlocks.Type.values()) {
                                        if (type != TLBlocks.IntermediaryBlocks.Type.GROUT && type != TLBlocks.IntermediaryBlocks.Type.GLOW) {
                                                TLItems.Blocks.registerBlockItem(TLBlocks.get(type), "tooltip.tinkerslegacy." + variantName(type));
                                        }
                                }
                        }

                        private static void register() {
                        }
                }

                public static final class StorageBlocks {
                        static {
                                for (var block : TLBlocks.StorageBlocks.ALL) {
                                        TLItems.Blocks.registerBlockItem(
                                                        block,
                                                        "tooltip.tinkerslegacy." + block.getId().getPath());
                                }
                        }

                        private static void register() {
                        }
                }

                public static class SearedBlocks {
                        static {
                                for (var block : TLBlocks.SearedBlocks.ALL) {
                                        TLItems.Blocks.registerBlockItem(
                                                        block,
                                                        "tooltip.tinkerslegacy.seared_block");
                                }
                        }

                        private static void register() {
                        }
                }

                public static class SlimyBlocks {
                        static {
                                registerSlimySoilItems();
                                registerSlimyGrassItems();
                                registerCongealedItems();
                                registerSlimeItems();
                        }

                        // Soil
                        private static void registerSlimySoilItems() {
                                for (Soil soil : Soil.values()) {
                                        if (soil == Soil.DIRT)
                                                continue;

                                        TLItems.Blocks.registerBlockItem(
                                                        TLBlocks.get(soil),
                                                        "tooltip.tinkerslegacy.slimy_dirt");
                                }
                        }

                        public static final DeferredHolder<Item, BlockItem> slimyDirt(Soil soil) {
                                return TLItems.Blocks.get(
                                                TLBlocks.get(soil).getId());
                        }

                        // Grass
                        private static void registerSlimyGrassItems() {
                                for (SlimyFoliage foliage : SlimyFoliage.values()) {
                                        for (Soil soil : Soil.values()) {
                                                TLItems.Blocks.registerBlockItem(
                                                                TLBlocks.get(soil, foliage),
                                                                "tooltip.tinkerslegacy.slimy_grass");
                                        }
                                }
                        }

                        public static final DeferredHolder<Item, BlockItem> slimyGrass(
                                        Soil soil,
                                        SlimyFoliage foliage) {
                                return TLItems.Blocks.get(
                                                TLBlocks.get(soil, foliage).getId());
                        }

                        // Congealed Slime
                        private static void registerCongealedItems() {
                                for (var block : TLBlocks.SlimyBlocks.CONGEALED) {
                                        TLItems.Blocks.registerBlockItem(
                                                        block,
                                                        "tooltip.tinkerslegacy.congealed_slime_block");
                                }
                        }

                        // Slime Blocks
                        private static void registerSlimeItems() {
                                for (var block : TLBlocks.SlimyBlocks.SLIME) {
                                        TLItems.Blocks.registerBlockItem(
                                                        block,
                                                        "tooltip.tinkerslegacy.slime_block");
                                }
                        }

                        private static void register() {
                        }
                }

                public static final class SlimyPlants {
                        static {
                                for (var block : TLBlocks.SlimyPlants.ALL) {
                                        registerBlockItem(block);
                                }
                        }

                        private static void register() {
                        }
                }

                public static final class Workstations {
                        static {
                                for (var block : TLBlocks.Workstations.ALL) {
                                        registerBlockItem(block);
                                }
                        }

                        private static void register() {
                        }
                }

                private static void register() {
                        OreBlocks.register();
                        IntermediaryBlocks.register();
                        StorageBlocks.register();
                        SearedBlocks.register();
                        SlimyBlocks.register();
                        SlimyPlants.register();
                        Workstations.register();
                }

                private static DeferredHolder<Item, BlockItem> registerBlockItem(
                                DeferredHolder<Block, ? extends Block> block) {
                        return registerBlockItem(block, null);
                }

                private static DeferredHolder<Item, BlockItem> registerBlockItem(
                                DeferredHolder<Block, ? extends Block> block,
                                String tooltipKey) {
                        DeferredHolder<Item, BlockItem> holder = TLItems.add(
                                        block.getId().getPath(), () -> tooltipKey == null
                                                        ? new BlockItem(block.get(), new Item.Properties())
                                                        : new TooltipBlockItem(block.get(), new Item.Properties(),
                                                                        tooltipKey));

                        ITEMS.put(block.getId(), holder);
                        return holder;
                }

                public static DeferredHolder<Item, BlockItem> get(ResourceLocation id) {
                        return ITEMS.get(id);
                }

                public static BlockItem get(Block block) {
                        DeferredHolder<Item, BlockItem> holder = ITEMS.get(BuiltInRegistries.BLOCK.getKey(block));

                        return holder == null ? null : holder.get();
                }

        }

        public static final class Parts {
                private static final Map<ResourceLocation, DeferredHolder<Item, DynamicPart>> ITEMS = new LinkedHashMap<>();

                public static final DeferredHolder<Item, MaterialShardItem> MATERIAL_SHARD = TLItems.add(
                                "material_shard",
                                () -> new MaterialShardItem(new Item.Properties()));

                static {
                        for (var part : ToolParts.ALL) {
                                register(part);
                        }
                }

                private static void register() {
                }

                private static DeferredHolder<Item, DynamicPart> register(
                                DeferredHolder<PartDefinition, PartDefinition> definition) {
                        DeferredHolder<Item, DynamicPart> holder = TLItems.add(
                                        definition.getId().getPath(),
                                        () -> new DynamicPart(definition));

                        ITEMS.put(definition.getId(), holder);
                        return holder;
                }

                // Getters
                public static boolean isPart(Item item) {
                        return item instanceof DynamicPart || item instanceof MaterialShardItem;
                }

                public static DeferredHolder<Item, DynamicPart> get(ToolParts.Type type) {
                        return get(ToolParts.get(type).getId());
                }

                public static DynamicPart item(ToolParts.Type type) {
                        return get(type).get();
                }

                public static DeferredHolder<Item, DynamicPart> get(ResourceLocation id) {
                        return ITEMS.get(id);
                }

                public static DynamicPart get(PartDefinition definition) {
                        DeferredHolder<Item, DynamicPart> holder = ITEMS.get(definition.id());
                        return holder == null ? null : holder.get();
                }
        }

        public static final class Tools {
                private static final Map<ResourceLocation, DeferredHolder<Item, DynamicTool>> ITEMS = new LinkedHashMap<>();

                static {
                        for (var tool : TLTools.ALL) {
                                register(tool);
                        }
                }

                // Registration
                private static void register() {
                }

                private static DeferredHolder<Item, DynamicTool> register(
                                DeferredHolder<ToolDefinition, ToolDefinition> definition) {
                        DeferredHolder<Item, DynamicTool> holder = TLItems.add(
                                        definition.getId().getPath(),
                                        () -> new DynamicTool(definition));

                        ITEMS.put(definition.getId(), holder);
                        return holder;
                }

                // Getters
                public static DeferredHolder<Item, DynamicTool> get(TLTools.Type type) {
                        return get(TLTools.get(type).getId());
                }

                public static DynamicTool item(TLTools.Type type) {
                        return get(type).get();
                }

                public static DeferredHolder<Item, DynamicTool> get(ResourceLocation id) {
                        return ITEMS.get(id);
                }

                public static DynamicTool get(ToolDefinition definition) {
                        DeferredHolder<Item, DynamicTool> holder = ITEMS.get(definition.id());
                        return holder == null ? null : holder.get();
                }
        }

        // Adders
        public static <T extends Item> DeferredHolder<Item, T> add(
                        String name,
                        Supplier<T> factory) {
                return ITEMS.register(name, factory);
        }

        // Getters
        public static DeferredHolder<Item, Item> get(Items.IntermediaryItems.Type type) {
                return Items.IntermediaryItems.get(type);
        }

        public static DeferredHolder<Item, Item> get(Items.Miscellaneous.Type type) {
                return Items.Miscellaneous.get(type);
        }

        public static DeferredHolder<Item, Item> get(Items.RawOres.Type type) {
                return Items.RawOres.get(type);
        }

        public static DeferredHolder<Item, Item> get(Items.Nuggets.Type type) {
                return Items.Nuggets.get(type);
        }

        public static DeferredHolder<Item, Item> get(Items.Ingots.Type type) {
                return Items.Ingots.get(type);
        }

        public static DeferredHolder<Item, Item> get(Items.Gems.Type type) {
                return Items.Gems.get(type);
        }

        public static DeferredHolder<Item, Item> get(Items.Bricks.Type type) {
                return Items.Bricks.get(type);
        }

        public static DeferredHolder<Item, Item> get(Items.Slimeballs.Type type) {
                return Items.Slimeballs.get(type);
        }

        public static Item item(Items.IntermediaryItems.Type type) {
                return get(type).get();
        }

        public static Item item(Items.Miscellaneous.Type type) {
                return get(type).get();
        }

        public static Item item(Items.RawOres.Type type) {
                return get(type).get();
        }

        public static Item item(Items.Nuggets.Type type) {
                return get(type).get();
        }

        public static Item item(Items.Ingots.Type type) {
                return get(type).get();
        }

        public static Item item(Items.Gems.Type type) {
                return get(type).get();
        }

        public static Item item(Items.Bricks.Type type) {
                return get(type).get();
        }

        public static Item item(Items.Slimeballs.Type type) {
                return get(type).get();
        }

        // Registration
        public static void register(IEventBus modBus) {
                Items.register();
                Blocks.register();
                Tools.register();
                Parts.register();
                ITEMS.register(modBus);
        }

        private static <E extends Enum<E>> Map<E, DeferredHolder<Item, Item>> registerVariants(
                        E[] variants,
                        Function<E, String> idFactory,
                        Function<E, ? extends Item> itemFactory) {
                Map<E, DeferredHolder<Item, Item>> result = new LinkedHashMap<>();

                for (E variant : variants) {
                        String id = idFactory.apply(variant);

                        DeferredHolder<Item, Item> holder = TLItems.add(
                                        id,
                                        () -> itemFactory.apply(variant));

                        result.put(variant, holder);
                }

                return Collections.unmodifiableMap(result);
        }

        // Variants
        private static String variantName(Enum<?> variant) {
                return variant.name().toLowerCase(Locale.ROOT);
        }
}
