package com.nurby.tinkerslegacy.client;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.client.datagen.TLBlockLootProvider;
import com.nurby.tinkerslegacy.client.datagen.TLBlockStateProvider;
import com.nurby.tinkerslegacy.client.datagen.TLBlockTagsProvider;
import com.nurby.tinkerslegacy.client.datagen.recipe.TLRecipeProvider;
import com.nurby.tinkerslegacy.client.datagen.texture.MaterialTextureProvider;
import com.nurby.tinkerslegacy.client.datagen.texture.PatternTextureProvider;
import com.nurby.tinkerslegacy.client.datagen.texture.ToolModifierTextureProvider;
import com.nurby.tinkerslegacy.client.model.StationModel;
import com.nurby.tinkerslegacy.client.model.TLItemModelProvider;
import com.nurby.tinkerslegacy.client.model.part.DynamicMaterialPartGeometry;
import com.nurby.tinkerslegacy.client.model.tool.DynamicToolGeometry;
import com.nurby.tinkerslegacy.client.renderer.StationRenderer;
import com.nurby.tinkerslegacy.client.renderer.TLToolProjectileRenderer;
import com.nurby.tinkerslegacy.client.renderer.TraitBlueSlimeRenderer;
import com.nurby.tinkerslegacy.client.screen.CraftingStationScreen;
import com.nurby.tinkerslegacy.client.screen.StationScreen;
import com.nurby.tinkerslegacy.content.block.SlimyGrassBlock;
import com.nurby.tinkerslegacy.content.block.SlimyLeavesBlock;
import com.nurby.tinkerslegacy.content.block.SlimyVineBlock;
import com.nurby.tinkerslegacy.content.block.StationBlock;
import com.nurby.tinkerslegacy.registry.TLBlockEntities;
import com.nurby.tinkerslegacy.registry.TLBlocks;
import com.nurby.tinkerslegacy.registry.TLStationMenus;
import com.nurby.tinkerslegacy.registry.tools.TLToolEntities;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.List;
import java.util.Set;

@EventBusSubscriber(modid = TinkersLegacy.MODID, value = Dist.CLIENT)
public final class ClientEvents {

        @SubscribeEvent
        public static void registerEntities(EntityRenderersEvent.RegisterRenderers event) {
                event.registerEntityRenderer(TLToolEntities.PROJECTILE.get(),
                                TLToolProjectileRenderer::new);
                event.registerEntityRenderer(TLToolEntities.BLUE_SLIME.get(), TraitBlueSlimeRenderer::new);
            event.registerBlockEntityRenderer(TLBlockEntities.STATION.get(), StationRenderer::new);
        }

        @SubscribeEvent
        public static void stationModels(ModelEvent.ModifyBakingResult event) {
                event.getModels().replaceAll((id, model) -> {
                    var block = BuiltInRegistries.BLOCK.get(id.id());
                    return block instanceof StationBlock station ? new StationModel(model, station.definitionId(), null) : model;
                });
        }

        @SubscribeEvent
        public static void registerScreens(RegisterMenuScreensEvent event) {
                event.register(TLStationMenus.STATION.get(), StationScreen::new);
                event.register(TLStationMenus.CRAFTING_STATION.get(), CraftingStationScreen::new);
        }

        @SubscribeEvent
        public static void gatherData(GatherDataEvent event) {
                PackOutput output = event.getGenerator().getPackOutput();
                ExistingFileHelper helper = event.getExistingFileHelper();

                event.getGenerator().addProvider(
                        event.includeServer(),
                        new TLBlockTagsProvider(output, event.getLookupProvider(), helper));

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
                                new ToolModifierTextureProvider(output, helper));

                event.getGenerator().addProvider(
                                event.includeClient(),
                                new PatternTextureProvider(output, helper));

                event.getGenerator().addProvider(
                                event.includeClient(),
                                new TLItemModelProvider(output, helper));
        }

        @SubscribeEvent
        public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
                for (var holder : TLBlocks.BLOCKS.getEntries()) {
                        if (holder.get() instanceof SlimyGrassBlock grass) {
                                event.register((state, level, pos, tint) -> tint == 0 ? grass.foliageColor() : -1, grass);
                        } else if (holder.get() instanceof SlimyLeavesBlock leaves) {
                                event.register((state, level, pos, tint) -> tint == 0 ? leaves.foliageColor() : -1, leaves);
                        } else if (holder.get() instanceof SlimyVineBlock vine) {
                                event.register((state, level, pos, tint) -> tint == 0 ? vine.foliageColor() : -1, vine);
                        }
                }
        }

        @SubscribeEvent
        public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
                for (var holder : TLBlocks.BLOCKS.getEntries()) {
                        if (holder.get() instanceof SlimyGrassBlock grass) {
                                event.register((stack, tint) -> tint == 0 ? grass.foliageColor() : -1, grass.asItem());
                        } else if (holder.get() instanceof SlimyLeavesBlock leaves) {
                                event.register((stack, tint) -> tint == 0 ? leaves.foliageColor() : -1, leaves.asItem());
                        } else if (holder.get() instanceof SlimyVineBlock vine) {
                                event.register((stack, tint) -> tint == 0 ? vine.foliageColor() : -1, vine.asItem());
                        }
                }
        }

        @SubscribeEvent
        public static void registerGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
                event.register(DynamicMaterialPartGeometry.Loader.ID, DynamicMaterialPartGeometry.Loader.INSTANCE);

                event.register(DynamicToolGeometry.Loader.ID, DynamicToolGeometry.Loader.INSTANCE);
        }
}
