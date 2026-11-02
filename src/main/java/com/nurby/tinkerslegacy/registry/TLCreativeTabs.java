package com.nurby.tinkerslegacy.registry;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.api.material.MaterialDefinition;
import com.nurby.tinkerslegacy.api.part.PartDefinition;
import com.nurby.tinkerslegacy.api.station.StationRecipes;
import com.nurby.tinkerslegacy.api.tool.ToolAssembly;
import com.nurby.tinkerslegacy.api.tool.ToolDefinition;
import com.nurby.tinkerslegacy.content.item.PatternItem;
import com.nurby.tinkerslegacy.content.item.dynamic.DynamicPart;
import com.nurby.tinkerslegacy.content.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.registry.tools.TLTools;
import com.nurby.tinkerslegacy.registry.tools.ToolParts;
import com.nurby.tinkerslegacy.registry.tools.ToolStatTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class TLCreativeTabs {

        public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(
                        Registries.CREATIVE_MODE_TAB,
                        TinkersLegacy.MODID);

        public static void register(IEventBus modBus) {
                // All.register();
                Items.register();
                Blocks.register();
                Parts.register();
                Tools.register();

                CREATIVE_TABS.register(modBus);
        }

        private static final List<String> TAB_ORDER = List.of("all", "items", "blocks", "parts", "tools");

        private static void registerTab(
                        String name,
                        Supplier<ItemStack> icon,
                        CreativeModeTab.DisplayItemsGenerator contents) {
                CREATIVE_TABS.register(name, () -> {
                        CreativeModeTab.Builder builder = CreativeModeTab.builder()
                                        .title(Component.translatable(
                                                        "itemGroup.tinkerslegacy." + name))
                                        .icon(icon)
                                        .displayItems(contents);

                        int index = TAB_ORDER.indexOf(name);

                        if (index > 0) {
                                builder.withTabsBefore(
                                                ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID,
                                                                TAB_ORDER.get(index - 1)));
                        }

                        return builder.build();
                });
        }

        private static final class All {
                private static void register() {
                        registerTab(
                                        "all",
                                        () -> new ItemStack(TLBlocks.block(TLBlocks.SearedBlocks.Type.BRICKS)),
                                        All::populate);
                }

                private static void populate(
                                CreativeModeTab.ItemDisplayParameters parameters,
                                CreativeModeTab.Output output) {
                        Items.populate(parameters, output);
                        Blocks.populate(parameters, output);
                        Parts.populate(parameters, output);
                        Tools.populate(parameters, output);
                }
        }

        private static final class Items {
                private static void register() {
                        registerTab(
                                        "items",
                                        () -> new ItemStack(TLItems.item(TLItems.Items.Bricks.Type.SEARED)),
                                        Items::populate);
                }

                private static void populate(
                                CreativeModeTab.ItemDisplayParameters parameters,
                                CreativeModeTab.Output output) {
                        for (Item item : TLItems.registeredItems()) {
                                if (!(item instanceof BlockItem)
                                                && !TLItems.Parts.isPart(item)
                                                && !(item instanceof DynamicTool)) {
                                        if (item instanceof PatternItem) {
                                                for (var shape : StationRecipes.shapes()) {
                                                        ItemStack pattern = new ItemStack(item);
                                                        pattern.set(TLDataComponents.PART, shape);
                                                        output.accept(pattern);
                                                }
                                        } else {
                                                output.accept(item);
                                        }
                                }
                        }
                }
        }

        private static final class Blocks {
                private static void register() {
                        registerTab(
                                        "blocks",
                                        () -> new ItemStack(TLBlocks.block(TLBlocks.SearedBlocks.Type.BRICKS)),
                                        Blocks::populate);
                }

                private static void populate(
                                CreativeModeTab.ItemDisplayParameters parameters,
                                CreativeModeTab.Output output) {
                        for (Item item : TLItems.registeredItems()) {
                                if (item instanceof BlockItem) {
                                        output.accept(item);
                                }
                        }
                }
        }

        private static final class Parts {
                private static void register() {
                        registerTab(
                                        "parts",
                                        Parts::createIcon,
                                        Parts::populate);
                }

                private static ItemStack createIcon() {
                        PartDefinition part = ToolParts.part(ToolParts.Type.BINDING);
                        ItemStack stack = new ItemStack(TLItems.Parts.item(ToolParts.Type.BINDING));
                        stack.set(
                                        TLDataComponents.MATERIALS, List.of(TLMaterials.IRON.getId()));
                        return stack;
                }

                private static void populate(
                                CreativeModeTab.ItemDisplayParameters parameters,
                                CreativeModeTab.Output output) {
                        addRegularParts(output);
                        addBolts(output);
                        TLItems.Parts.MATERIAL_SHARD.get().variants().forEach(output::accept);
                }

                private static void addRegularParts(
                                CreativeModeTab.Output output) {
                        for (Map.Entry<ResourceKey<PartDefinition>, PartDefinition> entry : TLRegistries.PARTS
                                        .entrySet()) {

                                PartDefinition part = entry.getValue();

                                if (ToolParts.get(ToolParts.Type.BOLT_CORE).is(part.id())) {
                                        continue;
                                }

                                DeferredHolder<Item, DynamicPart> item = TLItems.Parts.get(part.id());

                                if (item == null) {
                                        continue;
                                }

                                for (Map.Entry<ResourceKey<MaterialDefinition>, MaterialDefinition> materialEntry : TLRegistries.MATERIALS
                                                .entrySet()) {

                                        MaterialDefinition material = materialEntry.getValue();

                                        if (!material.available() || !material.parts().contains(part.id())) {
                                                continue;
                                        }

                                        ItemStack stack = new ItemStack(item.get());

                                    List<ResourceLocation> layers = part.statTypes()
                                                        .stream().map(statType -> material.id())
                                                        .toList();

                                        stack.set(
                                                        TLDataComponents.MATERIALS,
                                                        layers);

                                        output.accept(stack);
                                }
                        }
                }

                private static void addBolts(
                                CreativeModeTab.Output output) {
                        DeferredHolder<Item, DynamicPart> item = TLItems.Parts.get(ToolParts.Type.BOLT_CORE);

                        if (item == null) {
                                return;
                        }

                        MaterialDefinition iron = TLMaterials.IRON.value();

                        for (Map.Entry<ResourceKey<MaterialDefinition>, MaterialDefinition> entry : TLRegistries.MATERIALS
                                        .entrySet()) {

                                MaterialDefinition material = entry.getValue();

                                if (!material.available() || !material.stats().containsKey(
                                                ToolStatTypes.ARROW_SHAFT.getId())) {
                                        continue;
                                }

                                ItemStack stack = new ItemStack(item.get());

                                stack.set(
                                                TLDataComponents.MATERIALS,
                                                List.of(material.id(), material.id(), iron.id()));

                                output.accept(stack);
                        }
                }
        }

        private static final class Tools {
                private static void register() {
                        registerTab(
                                        "tools",
                                        Tools::createIcon,
                                        Tools::populate);
                }

                private static ItemStack createIcon() {
                        ToolDefinition definition = TLTools.tool(TLTools.Type.PICKAXE);
                        return ToolAssembly.create(
                                        definition, List.of(TLMaterials.IRON.get().id(), TLMaterials.IRON.get().id(), TLMaterials.IRON.get().id()));
                }

                private static void populate(
                                CreativeModeTab.ItemDisplayParameters parameters,
                                CreativeModeTab.Output output) {
                        for (Map.Entry<ResourceKey<ToolDefinition>, ToolDefinition> entry : TLRegistries.TOOLS
                                        .entrySet()) {

                                ToolDefinition definition = entry.getValue();

                                if (TLItems.Tools.get(definition.id()) == null) {
                                        continue;
                                }

                                for (Map.Entry<ResourceKey<MaterialDefinition>, MaterialDefinition> materialEntry : TLRegistries.MATERIALS
                                                .entrySet()) {

                                        MaterialDefinition material = materialEntry.getValue();

                                        if (!material.available()) {
                                                continue;
                                        }

                                    List<ResourceLocation> layers = definition.defaultMaterials(material);

                                        ItemStack stack = ToolAssembly.create(definition, layers, parameters.holders());

                                        if (!stack.isEmpty()) {
                                                output.accept(stack);
                                        }
                                }
                        }
                }
        }

        private TLCreativeTabs() {
        }
}