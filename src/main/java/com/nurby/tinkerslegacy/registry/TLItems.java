
package com.nurby.tinkerslegacy.registry;

import com.nurby.tinkerslegacy.TinkersLegacy;
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

        public static void register(IEventBus modBus) {
                Items.register();
                Blocks.register();
                Tools.register();
                Parts.register();
                ITEMS.register(modBus);
        }

        public static final class Items {
                public static final DeferredHolder<Item, Item> SEARED_BRICK = TLItems.ITEMS.register(
                                "seared_brick",
                                () -> new Item(new Item.Properties()));

                private static void register() {
                }
        }

        public static final class Blocks {
                private static final Map<ResourceLocation, DeferredHolder<Item, BlockItem>> ITEMS = new LinkedHashMap<>();

                public static final DeferredHolder<Item, BlockItem> GROUT = register(TLBlocks.GROUT);
                public static final DeferredHolder<Item, BlockItem> SEARED_BRICKS = register(TLBlocks.SEARED_BRICKS);

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