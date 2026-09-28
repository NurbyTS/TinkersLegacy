
package com.nurby.tinkerslegacy.registry;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.item.TooltipItem;
import com.nurby.tinkerslegacy.item.TooltipBlockItem;
import com.nurby.tinkerslegacy.item.dynamic.DynamicPart;
import com.nurby.tinkerslegacy.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.library.part.PartDefinition;
import com.nurby.tinkerslegacy.library.tool.ToolDefinition;
import com.nurby.tinkerslegacy.registry.TLTools;
import com.nurby.tinkerslegacy.registry.ToolParts;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.Map;

public final class TLItems {
        private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
                        BuiltInRegistries.ITEM,
                        TinkersLegacy.MODID);

        // Read when tab contents are built, after item registration has finished.
        static java.util.List<Item> registeredItems() {
                return ITEMS.getEntries().stream().<Item>map(holder -> holder.get()).toList();
        }

        public static void register(IEventBus modBus) {
                Items.register();
                Blocks.register();
                Tools.register();
                Parts.register();
                ITEMS.register(modBus);
        }

        public static final class Items {
                public static final class IntermediaryItems {
                        public static final DeferredHolder<Item, Item> SILKY_CLOTH = TLItems.ITEMS.register(
                                        "silky_cloth",
                                        () -> new Item(new Item.Properties()));

                        private static void register() {
                        }
                }

                public static final class Miscellaneous {
                        public static final DeferredHolder<Item, Item> CREATIVE_MODIFIER = TLItems.ITEMS.register(
                                        "creative_modifier",
                                        () -> new TooltipItem(
                                                        new Item.Properties(),
                                                        "tooltip.tinkerslegacy.creative_modifier"));

                        public static final DeferredHolder<Item, Item> BALL_OF_MOSS = TLItems.ITEMS.register(
                                        "ball_of_moss",
                                        () -> new TooltipItem(
                                                        new Item.Properties(),
                                                        "tooltip.tinkerslegacy.ball_of_moss"));

                        public static final DeferredHolder<Item, Item> MENDING_MOSS = TLItems.ITEMS.register(
                                        "mending_moss",
                                        () -> new TooltipItem(
                                                        new Item.Properties(),
                                                        "tooltip.tinkerslegacy.mending_moss"));

                        public static final DeferredHolder<Item, Item> EXPANDER_VERTICAL = TLItems.ITEMS.register(
                                        "expander_vertical",
                                        () -> new TooltipItem(
                                                        new Item.Properties(),
                                                        "tooltip.tinkerslegacy.expander_vertical"));

                        public static final DeferredHolder<Item, Item> EXPANDER_HORIZONTAL = TLItems.ITEMS.register(
                                        "expander_horizontal",
                                        () -> new TooltipItem(
                                                        new Item.Properties(),
                                                        "tooltip.tinkerslegacy.expander_horizontal"));

                        private static void register() {
                        }
                }

                public static final class RawOres {
                        public static final DeferredHolder<Item, Item> RAW_COBALT = TLItems.ITEMS.register(
                                        "raw_cobalt",
                                        () -> new Item(new Item.Properties()));

                        public static final DeferredHolder<Item, Item> RAW_ARDITE = TLItems.ITEMS.register(
                                        "raw_ardite",
                                        () -> new Item(new Item.Properties()));

                        private static void register() {
                        }
                }

                public static final class Nuggets {
                        public static final DeferredHolder<Item, Item> COBALT_NUGGET = TLItems.ITEMS.register(
                                        "cobalt_nugget",
                                        () -> new Item(new Item.Properties()));

                        public static final DeferredHolder<Item, Item> ARDITE_NUGGET = TLItems.ITEMS.register(
                                        "ardite_nugget",
                                        () -> new Item(new Item.Properties()));

                        public static final DeferredHolder<Item, Item> ALUBRASS_NUGGET = TLItems.ITEMS.register(
                                        "alubrass_nugget",
                                        () -> new Item(new Item.Properties()));

                        public static final DeferredHolder<Item, Item> KNIGHTSLIME_NUGGET = TLItems.ITEMS.register(
                                        "knightslime_nugget",
                                        () -> new Item(new Item.Properties()));

                        public static final DeferredHolder<Item, Item> MANYULLYN_NUGGET = TLItems.ITEMS.register(
                                        "manyullyn_nugget",
                                        () -> new Item(new Item.Properties()));

                        public static final DeferredHolder<Item, Item> PIGIRON_NUGGET = TLItems.ITEMS.register(
                                        "pigiron_nugget",
                                        () -> new Item(new Item.Properties()));

                        private static void register() {
                        }
                }

                public static final class Ingots {
                        public static final DeferredHolder<Item, Item> COBALT_INGOT = TLItems.ITEMS.register(
                                        "cobalt_ingot",
                                        () -> new Item(new Item.Properties()));

                        public static final DeferredHolder<Item, Item> ARDITE_INGOT = TLItems.ITEMS.register(
                                        "ardite_ingot",
                                        () -> new Item(new Item.Properties()));

                        public static final DeferredHolder<Item, Item> ALUBRASS_INGOT = TLItems.ITEMS.register(
                                        "alubrass_ingot",
                                        () -> new Item(new Item.Properties()));

                        public static final DeferredHolder<Item, Item> KNIGHTSLIME_INGOT = TLItems.ITEMS.register(
                                        "knightslime_ingot",
                                        () -> new TooltipItem(
                                                        new Item.Properties(),
                                                        "tooltip.tinkerslegacy.knightslime_ingot"));

                        public static final DeferredHolder<Item, Item> MANYULLYN_INGOT = TLItems.ITEMS.register(
                                        "manyullyn_ingot",
                                        () -> new Item(new Item.Properties()));

                        public static final DeferredHolder<Item, Item> PIGIRON_INGOT = TLItems.ITEMS.register(
                                        "pigiron_ingot",
                                        () -> new TooltipItem(
                                                        new Item.Properties(),
                                                        "tooltip.tinkerslegacy.pigiron_ingot"));

                        private static void register() {
                        }
                }

                public static final class Gems {
                        public static final DeferredHolder<Item, Item> SILKY_JEWEL = TLItems.ITEMS.register(
                                        "silky_jewel",
                                        () -> new TooltipItem(
                                                        new Item.Properties(),
                                                        "tooltip.tinkerslegacy.silky_jewel"));

                        public static final DeferredHolder<Item, Item> GREEN_SLIME_CRYSTAL = TLItems.ITEMS.register(
                                        "green_slime_crystal",
                                        () -> new TooltipItem(
                                                        new Item.Properties(),
                                                        "tooltip.tinkerslegacy.green_slime_crystal"));

                        public static final DeferredHolder<Item, Item> BLUE_SLIME_CRYSTAL = TLItems.ITEMS.register(
                                        "blue_slime_crystal",
                                        () -> new TooltipItem(
                                                        new Item.Properties(),
                                                        "tooltip.tinkerslegacy.blue_slime_crystal"));

                        public static final DeferredHolder<Item, Item> MAGMA_SLIME_CRYSTAL = TLItems.ITEMS.register(
                                        "magma_slime_crystal",
                                        () -> new TooltipItem(
                                                        new Item.Properties(),
                                                        "tooltip.tinkerslegacy.magma_slime_crystal"));

                        private static void register() {
                        }
                }

                public static final class Bricks {
                        public static final DeferredHolder<Item, Item> SEARED_BRICK = TLItems.ITEMS.register(
                                        "seared_brick",
                                        () -> new Item(new Item.Properties()));

                        public static final DeferredHolder<Item, Item> MUD_BRICK = TLItems.ITEMS.register(
                                        "mud_brick",
                                        () -> new TooltipItem(
                                                        new Item.Properties(),
                                                        "tooltip.tinkerslegacy.mud_brick"));
                        public static final DeferredHolder<Item, Item> DRIED_BRICK = TLItems.ITEMS.register(
                                        "dried_brick",
                                        () -> new Item(new Item.Properties()));

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
                }
        }

        public static final class Blocks {
                private static final Map<ResourceLocation, DeferredHolder<Item, BlockItem>> ITEMS = new LinkedHashMap<>();

                public static final class OreBlocks {
                        public static final DeferredHolder<Item, BlockItem> NETHER_COBALT_ORE = register(
                                        TLBlocks.OreBlocks.NETHER_COBALT_ORE);

                        public static final DeferredHolder<Item, BlockItem> NETHER_ARDITE_ORE = register(
                                        TLBlocks.OreBlocks.NETHER_ARDITE_ORE);

                        private static void register() {
                        }

                        private static DeferredHolder<Item, BlockItem> register(
                                        DeferredHolder<Block, ? extends Block> block) {
                                DeferredHolder<Item, BlockItem> holder = TLItems.ITEMS.register(
                                                block.getId().getPath(),
                                                () -> new BlockItem(block.get(), new Item.Properties()));

                                ITEMS.put(block.getId(), holder);
                                return holder;
                        }

                        private static DeferredHolder<Item, BlockItem> register(
                                        DeferredHolder<Block, ? extends Block> block,
                                        String tooltipKey) {
                                DeferredHolder<Item, BlockItem> holder = TLItems.ITEMS.register(
                                                block.getId().getPath(),
                                                () -> new TooltipBlockItem(
                                                                block.get(),
                                                                new Item.Properties(),
                                                                tooltipKey));

                                ITEMS.put(block.getId(), holder);
                                return holder;
                        }

                        public static DeferredHolder<Item, BlockItem> get(ResourceLocation id) {
                                return ITEMS.get(id);
                        }

                        public static BlockItem get(Block block) {
                                DeferredHolder<Item, BlockItem> holder = ITEMS
                                                .get(BuiltInRegistries.BLOCK.getKey(block));
                                return holder == null ? null : holder.get();
                        }
                }

                public static final class IntermediaryBlocks {
                        public static final DeferredHolder<Item, BlockItem> GROUT = register(
                                        TLBlocks.IntermediaryBlocks.GROUT,
                                        "tooltip.tinkerslegacy.grout");

                        private static void register() {
                        }

                        private static DeferredHolder<Item, BlockItem> register(
                                        DeferredHolder<Block, ? extends Block> block) {
                                DeferredHolder<Item, BlockItem> holder = TLItems.ITEMS.register(
                                                block.getId().getPath(),
                                                () -> new BlockItem(block.get(), new Item.Properties()));

                                ITEMS.put(block.getId(), holder);
                                return holder;
                        }

                        private static DeferredHolder<Item, BlockItem> register(
                                        DeferredHolder<Block, ? extends Block> block,
                                        String tooltipKey) {
                                DeferredHolder<Item, BlockItem> holder = TLItems.ITEMS.register(
                                                block.getId().getPath(),
                                                () -> new TooltipBlockItem(
                                                                block.get(),
                                                                new Item.Properties(),
                                                                tooltipKey));

                                ITEMS.put(block.getId(), holder);
                                return holder;
                        }

                        public static DeferredHolder<Item, BlockItem> get(ResourceLocation id) {
                                return ITEMS.get(id);
                        }

                        public static BlockItem get(Block block) {
                                DeferredHolder<Item, BlockItem> holder = ITEMS
                                                .get(BuiltInRegistries.BLOCK.getKey(block));
                                return holder == null ? null : holder.get();
                        }
                }

                public static final class StorageBlocks {
                        public static final DeferredHolder<Item, BlockItem> STORAGE_BLOCK_COBALT = register(
                                        TLBlocks.StorageBlocks.STORAGE_BLOCK_COBALT,
                                        "tooltip.tinkerslegacy.storage_block_cobalt");
                        public static final DeferredHolder<Item, BlockItem> STORAGE_BLOCK_ARDITE = register(
                                        TLBlocks.StorageBlocks.STORAGE_BLOCK_ARDITE,
                                        "tooltip.tinkerslegacy.storage_block_ardite");
                        public static final DeferredHolder<Item, BlockItem> STORAGE_BLOCK_ALUBRASS = register(
                                        TLBlocks.StorageBlocks.STORAGE_BLOCK_ALUBRASS,
                                        "tooltip.tinkerslegacy.storage_block_alubrass");
                        public static final DeferredHolder<Item, BlockItem> STORAGE_BLOCK_KNIGHTSLIME = register(
                                        TLBlocks.StorageBlocks.STORAGE_BLOCK_KNIGHTSLIME,
                                        "tooltip.tinkerslegacy.storage_block_knightslime");
                        public static final DeferredHolder<Item, BlockItem> STORAGE_BLOCK_MANYULLYN = register(
                                        TLBlocks.StorageBlocks.STORAGE_BLOCK_MANYULLYN,
                                        "tooltip.tinkerslegacy.storage_block_manyullyn");
                        public static final DeferredHolder<Item, BlockItem> STORAGE_BLOCK_PIGIRON = register(
                                        TLBlocks.StorageBlocks.STORAGE_BLOCK_PIGIRON,
                                        "tooltip.tinkerslegacy.storage_block_pigiron");
                        public static final DeferredHolder<Item, BlockItem> STORAGE_BLOCK_SILKY_JEWEL = register(
                                        TLBlocks.StorageBlocks.STORAGE_BLOCK_SILKY_JEWEL,
                                        "tooltip.tinkerslegacy.storage_block_silky_jewel");

                        private static void register() {
                        }

                        private static DeferredHolder<Item, BlockItem> register(
                                        DeferredHolder<Block, ? extends Block> block) {
                                DeferredHolder<Item, BlockItem> holder = TLItems.ITEMS.register(
                                                block.getId().getPath(),
                                                () -> new BlockItem(block.get(), new Item.Properties()));

                                ITEMS.put(block.getId(), holder);
                                return holder;
                        }

                        private static DeferredHolder<Item, BlockItem> register(
                                        DeferredHolder<Block, ? extends Block> block,
                                        String tooltipKey) {
                                DeferredHolder<Item, BlockItem> holder = TLItems.ITEMS.register(
                                                block.getId().getPath(),
                                                () -> new TooltipBlockItem(
                                                                block.get(),
                                                                new Item.Properties(),
                                                                tooltipKey));

                                ITEMS.put(block.getId(), holder);
                                return holder;
                        }

                        public static DeferredHolder<Item, BlockItem> get(ResourceLocation id) {
                                return ITEMS.get(id);
                        }

                        public static BlockItem get(Block block) {
                                DeferredHolder<Item, BlockItem> holder = ITEMS
                                                .get(BuiltInRegistries.BLOCK.getKey(block));
                                return holder == null ? null : holder.get();
                        }
                }

                public static class SearedBlocks {
                        public static final DeferredHolder<Item, BlockItem> SEARED_STONE = register(
                                        TLBlocks.SearedBlocks.SEARED_STONE,
                                        "tooltip.tinkerslegacy.seared_stone");
                        public static final DeferredHolder<Item, BlockItem> SEARED_COBBLE = register(
                                        TLBlocks.SearedBlocks.SEARED_COBBLE,
                                        "tooltip.tinkerslegacy.seared_cobble");
                        public static final DeferredHolder<Item, BlockItem> SEARED_PAVER = register(
                                        TLBlocks.SearedBlocks.SEARED_PAVER,
                                        "tooltip.tinkerslegacy.seared_paver");
                        public static final DeferredHolder<Item, BlockItem> SEARED_BRICKS = register(
                                        TLBlocks.SearedBlocks.SEARED_BRICKS,
                                        "tooltip.tinkerslegacy.seared_bricks");
                        public static final DeferredHolder<Item, BlockItem> SEARED_BRICK_CRACKED = register(
                                        TLBlocks.SearedBlocks.SEARED_BRICK_CRACKED,
                                        "tooltip.tinkerslegacy.seared_brick_cracked");
                        public static final DeferredHolder<Item, BlockItem> SEARED_BRICK_FANCY = register(
                                        TLBlocks.SearedBlocks.SEARED_BRICK_FANCY,
                                        "tooltip.tinkerslegacy.seared_brick_fancy");
                        public static final DeferredHolder<Item, BlockItem> SEARED_BRICK_SQUARE = register(
                                        TLBlocks.SearedBlocks.SEARED_BRICK_SQUARE,
                                        "tooltip.tinkerslegacy.seared_brick_square");
                        public static final DeferredHolder<Item, BlockItem> SEARED_ROAD = register(
                                        TLBlocks.SearedBlocks.SEARED_ROAD,
                                        "tooltip.tinkerslegacy.seared_road");
                        public static final DeferredHolder<Item, BlockItem> SEARED_CREEPER = register(
                                        TLBlocks.SearedBlocks.SEARED_CREEPER,
                                        "tooltip.tinkerslegacy.seared_creeper");
                        public static final DeferredHolder<Item, BlockItem> SEARED_BRICK_TRIANGLE = register(
                                        TLBlocks.SearedBlocks.SEARED_BRICK_TRIANGLE,
                                        "tooltip.tinkerslegacy.seared_brick_triangle");
                        public static final DeferredHolder<Item, BlockItem> SEARED_BRICK_SMALL = register(
                                        TLBlocks.SearedBlocks.SEARED_BRICK_SMALL,
                                        "tooltip.tinkerslegacy.seared_brick_small");
                        public static final DeferredHolder<Item, BlockItem> SEARED_TILE = register(
                                        TLBlocks.SearedBlocks.SEARED_TILE,
                                        "tooltip.tinkerslegacy.seared_tile");

                        private static void register() {
                        }

                        private static DeferredHolder<Item, BlockItem> register(
                                        DeferredHolder<Block, ? extends Block> block) {
                                DeferredHolder<Item, BlockItem> holder = TLItems.ITEMS.register(
                                                block.getId().getPath(),
                                                () -> new BlockItem(block.get(), new Item.Properties()));

                                ITEMS.put(block.getId(), holder);
                                return holder;
                        }

                        private static DeferredHolder<Item, BlockItem> register(
                                        DeferredHolder<Block, ? extends Block> block,
                                        String tooltipKey) {
                                DeferredHolder<Item, BlockItem> holder = TLItems.ITEMS.register(
                                                block.getId().getPath(),
                                                () -> new TooltipBlockItem(
                                                                block.get(),
                                                                new Item.Properties(),
                                                                tooltipKey));

                                ITEMS.put(block.getId(), holder);
                                return holder;
                        }

                        public static DeferredHolder<Item, BlockItem> get(ResourceLocation id) {
                                return ITEMS.get(id);
                        }

                        public static BlockItem get(Block block) {
                                DeferredHolder<Item, BlockItem> holder = ITEMS
                                                .get(BuiltInRegistries.BLOCK.getKey(block));
                                return holder == null ? null : holder.get();
                        }
                }

                private static void register() {
                        OreBlocks.register();
                        IntermediaryBlocks.register();
                        StorageBlocks.register();
                        SearedBlocks.register();
                }
        }

        public static final class Parts {
                private static final Map<ResourceLocation, DeferredHolder<Item, DynamicPart>> ITEMS = new LinkedHashMap<>();

                public static final DeferredHolder<Item, DynamicPart> PICKAXE_HEAD = register(ToolParts.PICKAXE_HEAD);

                public static final DeferredHolder<Item, DynamicPart> AXE_HEAD = register(ToolParts.AXE_HEAD);

                public static final DeferredHolder<Item, DynamicPart> SHOVEL_HEAD = register(ToolParts.SHOVEL_HEAD);

                public static final DeferredHolder<Item, DynamicPart> KAMA_HEAD = register(ToolParts.KAMA_HEAD);

                public static final DeferredHolder<Item, DynamicPart> SWORD_BLADE = register(ToolParts.SWORD_BLADE);

                public static final DeferredHolder<Item, DynamicPart> HAMMER_HEAD = register(ToolParts.HAMMER_HEAD);

                public static final DeferredHolder<Item, DynamicPart> BROAD_AXE_HEAD = register(
                                ToolParts.BROAD_AXE_HEAD);

                public static final DeferredHolder<Item, DynamicPart> LARGE_SWORD_BLADE = register(
                                ToolParts.LARGE_SWORD_BLADE);

                public static final DeferredHolder<Item, DynamicPart> EXCAVATOR_HEAD = register(
                                ToolParts.EXCAVATOR_HEAD);

                public static final DeferredHolder<Item, DynamicPart> SCYTHE_HEAD = register(ToolParts.SCYTHE_HEAD);

                public static final DeferredHolder<Item, DynamicPart> PAN_HEAD = register(ToolParts.PAN_HEAD);

                public static final DeferredHolder<Item, DynamicPart> SIGN_HEAD = register(ToolParts.SIGN_HEAD);

                public static final DeferredHolder<Item, DynamicPart> LARGE_PLATE = register(ToolParts.LARGE_PLATE);

                public static final DeferredHolder<Item, DynamicPart> KNIFE_BLADE = register(ToolParts.KNIFE_BLADE);

                public static final DeferredHolder<Item, DynamicPart> BOW_LIMB = register(ToolParts.BOW_LIMB);

                public static final DeferredHolder<Item, DynamicPart> BOW_STRING = register(ToolParts.BOW_STRING);

                public static final DeferredHolder<Item, DynamicPart> ARROW_HEAD = register(ToolParts.ARROW_HEAD);

                public static final DeferredHolder<Item, DynamicPart> ARROW_SHAFT = register(ToolParts.ARROW_SHAFT);

                public static final DeferredHolder<Item, DynamicPart> FLETCHING = register(ToolParts.FLETCHING);

                public static final DeferredHolder<Item, DynamicPart> TOOL_ROD = register(ToolParts.TOOL_ROD);

                public static final DeferredHolder<Item, DynamicPart> TOUGH_TOOL_ROD = register(
                                ToolParts.TOUGH_TOOL_ROD);

                public static final DeferredHolder<Item, DynamicPart> BINDING = register(ToolParts.BINDING);

                public static final DeferredHolder<Item, DynamicPart> TOUGH_BINDING = register(ToolParts.TOUGH_BINDING);

                public static final DeferredHolder<Item, DynamicPart> WIDE_GUARD = register(ToolParts.WIDE_GUARD);

                public static final DeferredHolder<Item, DynamicPart> CROSS_GUARD = register(ToolParts.CROSS_GUARD);

                public static final DeferredHolder<Item, DynamicPart> BOLT_CORE = register(ToolParts.BOLT_CORE);

                private static void register() {
                }

                private static DeferredHolder<Item, DynamicPart> register(
                                DeferredHolder<PartDefinition, PartDefinition> definition) {
                        DeferredHolder<Item, DynamicPart> holder = TLItems.ITEMS.register(
                                        definition.getId().getPath(),
                                        () -> new DynamicPart(definition));

                        ITEMS.put(definition.getId(), holder);
                        return holder;
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

                public static final DeferredHolder<Item, DynamicTool> PICKAXE = register(TLTools.PICKAXE);

                public static final DeferredHolder<Item, DynamicTool> SHOVEL = register(TLTools.SHOVEL);

                public static final DeferredHolder<Item, DynamicTool> HATCHET = register(TLTools.HATCHET);

                public static final DeferredHolder<Item, DynamicTool> MATTOCK = register(TLTools.MATTOCK);

                public static final DeferredHolder<Item, DynamicTool> KAMA = register(TLTools.KAMA);

                public static final DeferredHolder<Item, DynamicTool> BROADSWORD = register(TLTools.BROADSWORD);

                public static final DeferredHolder<Item, DynamicTool> LONGSWORD = register(TLTools.LONGSWORD);

                public static final DeferredHolder<Item, DynamicTool> RAPIER = register(TLTools.RAPIER);

                private static void register() {
                }

                private static DeferredHolder<Item, DynamicTool> register(
                                DeferredHolder<ToolDefinition, ToolDefinition> definition) {
                        DeferredHolder<Item, DynamicTool> holder = TLItems.ITEMS.register(
                                        definition.getId().getPath(),
                                        () -> new DynamicTool(definition));

                        ITEMS.put(definition.getId(), holder);
                        return holder;
                }

                public static DeferredHolder<Item, DynamicTool> get(ResourceLocation id) {
                        return ITEMS.get(id);
                }

                public static DynamicTool get(ToolDefinition definition) {
                        DeferredHolder<Item, DynamicTool> holder = ITEMS.get(definition.id());
                        return holder == null ? null : holder.get();
                }
        }

}