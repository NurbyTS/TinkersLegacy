package com.nurby.tinkerslegacy.client;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.block.SlimyGrassBlock;
import com.nurby.tinkerslegacy.registry.TLBlocks;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import com.nurby.tinkerslegacy.client.model.TLItemModelProvider;
import com.nurby.tinkerslegacy.client.model.part.DynamicMaterialPartModelLoader;
import com.nurby.tinkerslegacy.client.datagen.MaterialTextureProvider;
import com.nurby.tinkerslegacy.client.model.tool.DynamicToolModelLoader;
import com.nurby.tinkerslegacy.client.datagen.TLBlockStateProvider;
import com.nurby.tinkerslegacy.datagen.TLBlockLootProvider;
import com.nurby.tinkerslegacy.datagen.TLRecipeProvider;

import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.List;
import java.util.Set;

@EventBusSubscriber(modid = TinkersLegacy.MODID, value = Dist.CLIENT)
public final class ClientEvents {
        @SubscribeEvent
        public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
                for (var holder : TLBlocks.BLOCKS.getEntries()) {
                        if (holder.get() instanceof SlimyGrassBlock grass) {
                                event.register((state, level, pos, tint) -> tint == 0 ? grass.foliageColor() : -1, grass);
                        }
                }
        }

        @SubscribeEvent
        public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
                for (var holder : TLBlocks.BLOCKS.getEntries()) {
                        if (holder.get() instanceof SlimyGrassBlock grass) {
                                event.register((stack, tint) -> tint == 0 ? grass.foliageColor() : -1, grass.asItem());
                        }
                }
        }


        @SubscribeEvent
        public static void gatherData(GatherDataEvent event) {
                PackOutput output = event.getGenerator().getPackOutput();
                ExistingFileHelper helper = event.getExistingFileHelper();

                event.getGenerator().addProvider(
                                event.includeServer(),
                                new TLRecipeProvider(output, event.getLookupProvider()));

                event.getGenerator().addProvider(
                                event.includeServer(),
                                new LootTableProvider(
                                                output,
                                                Set.of(),
                                                List.of(new LootTableProvider.SubProviderEntry(TLBlockLootProvider::new,
                                                                LootContextParamSets.BLOCK)),
                                                event.getLookupProvider()));

                event.getGenerator().addProvider(
                                event.includeClient(),
                                new TLBlockStateProvider(output, helper));

                event.getGenerator().addProvider(
                                event.includeClient(),
                                new MaterialTextureProvider(output, helper));

                event.getGenerator().addProvider(
                                event.includeClient(),
                                new TLItemModelProvider(output, helper));
        }

        @SubscribeEvent
        public static void registerGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
                event.register(
                                ResourceLocation.fromNamespaceAndPath(
                                                TinkersLegacy.MODID,
                                                "dynamic_material_part"),
                                DynamicMaterialPartModelLoader.INSTANCE);

                event.register(
                                ResourceLocation.fromNamespaceAndPath(
                                                TinkersLegacy.MODID,
                                                "dynamic_tool"),
                                DynamicToolModelLoader.INSTANCE);
        }
}