package com.nurby.tinkerslegacy.registry;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.item.dynamic.DynamicPart;
import com.nurby.tinkerslegacy.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.library.material.MaterialDefinition;
import com.nurby.tinkerslegacy.library.material.layer.MaterialLayer;
import com.nurby.tinkerslegacy.library.part.PartDefinition;
import com.nurby.tinkerslegacy.library.tool.ToolDefinition;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class TLCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(
                    Registries.CREATIVE_MODE_TAB,
                    TinkersLegacy.MODID
            );

    private static final List<Consumer<CreativeModeTab.Output>> ITEM_PROVIDERS =
            new ArrayList<>();

    private static DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN;

    public static void addItems(
            Consumer<CreativeModeTab.Output> provider
    ) {
        ITEM_PROVIDERS.add(provider);
    }

    public static void register(IEventBus modBus) {
        MAIN = CREATIVE_TABS.register(
                "main",
                () -> CreativeModeTab.builder()
                        .title(Component.translatable(
                                "itemGroup.tinkerslegacy"
                        ))
                        .icon(() -> new ItemStack(Items.DIAMOND))
                        .displayItems((parameters, output) -> {
                            for (Consumer<CreativeModeTab.Output> provider : ITEM_PROVIDERS) {
                                provider.accept(output);
                            }
                        })
                        .build()
        );

        Parts.register();
        Tools.register();

        CREATIVE_TABS.register(modBus);
    }

    private static final class Parts {

        private static void register() {
            addItems(Parts::populate);
        }

        private static void populate(
                CreativeModeTab.Output output
        ) {
            addRegularParts(output);
            addBolts(output);
        }

        private static void addRegularParts(
                CreativeModeTab.Output output
        ) {
            for (Map.Entry<ResourceKey<PartDefinition>, PartDefinition> entry :
                    TLRegistries.PARTS.entrySet()) {

                PartDefinition part = entry.getValue();

                if (ToolParts.BOLT_CORE.is(part.id())) {
                    continue;
                }

                DeferredHolder<Item, DynamicPart> item =
                        TLItems.Parts.get(part.id());

                if (item == null) {
                    continue;
                }

                for (Map.Entry<ResourceKey<MaterialDefinition>, MaterialDefinition> materialEntry :
                        TLRegistries.MATERIALS.entrySet()) {

                    MaterialDefinition material = materialEntry.getValue();

                    if (!material.parts().contains(part.id())) {
                        continue;
                    }

                    ItemStack stack =
                            new ItemStack(item.get());

                    List<MaterialLayer> layers = part.statTypes()
                            .stream()
                            .map(statType -> new MaterialLayer(
                                    statType,
                                    material.id()
                            ))
                            .toList();

                    stack.set(
                            TLDataComponents.MATERIALS,
                            layers
                    );

                    output.accept(stack);
                }
            }
        }

        private static void addBolts(
                CreativeModeTab.Output output
        ) {
            DeferredHolder<Item, DynamicPart> item =
                    TLItems.Parts.get(
                            ToolParts.BOLT_CORE.getId()
                    );

            if (item == null) {
                return;
            }

            MaterialDefinition iron = TLMaterials.IRON.value();

            for (Map.Entry<ResourceKey<MaterialDefinition>, MaterialDefinition> entry :
                    TLRegistries.MATERIALS.entrySet()) {

                MaterialDefinition material = entry.getValue();

                if (!material.stats().containsKey(
                        ToolStatTypes.ARROW_SHAFT.getId()
                )) {
                    continue;
                }

                ItemStack stack =
                        new ItemStack(item.get());

                stack.set(
                        TLDataComponents.MATERIALS,
                        List.of(
                                new MaterialLayer(
                                        ToolStatTypes.ARROW_SHAFT.getId(),
                                        material.id()
                                ),
                                new MaterialLayer(
                                        ToolStatTypes.HEAD.getId(),
                                        iron.id()
                                )
                        )
                );

                output.accept(stack);
            }
        }
    }

    private static final class Tools {

        private static void register() {
            addItems(Tools::populate);
        }

        private static void populate(
                CreativeModeTab.Output output
        ) {
            for (Map.Entry<ResourceKey<ToolDefinition>, ToolDefinition> entry :
                    TLRegistries.TOOLS.entrySet()) {

                ToolDefinition definition = entry.getValue();

                if (TLItems.Tools.get(definition.id()) == null) {
                    continue;
                }

                for (Map.Entry<ResourceKey<MaterialDefinition>, MaterialDefinition> materialEntry :
                        TLRegistries.MATERIALS.entrySet()) {

                    MaterialDefinition material = materialEntry.getValue();

                    if (!definition.supports(material)) {
                        continue;
                    }

                    DynamicTool tool =
                            TLItems.Tools.get(definition);

                    if (tool == null) {
                        continue;
                    }

                    ItemStack stack =
                            new ItemStack(tool);

                    List<MaterialLayer> layers = definition.parts()
                            .stream()
                            .map(part -> new MaterialLayer(
                                    part.statType(),
                                    material.id()
                            ))
                            .toList();

                    stack.set(
                            TLDataComponents.MATERIALS,
                            layers
                    );

                    tool.calculateStats(stack);

                    output.accept(stack);
                }
            }
        }
    }

    private TLCreativeTabs() {
    }
}