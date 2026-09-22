package com.nurby.tinkerslegacy.client;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.client.datagen.TLItemModelProvider;
import com.nurby.tinkerslegacy.client.model.part.DynamicMaterialPartModelLoader;
import com.nurby.tinkerslegacy.client.datagen.MaterialTextureProvider;
import com.nurby.tinkerslegacy.client.model.tool.DynamicToolModelLoader;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(
        modid = TinkersLegacy.MODID,
        value = Dist.CLIENT
)
public final class ClientEvents {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        PackOutput output = event.getGenerator().getPackOutput();
        ExistingFileHelper helper = event.getExistingFileHelper();

        event.getGenerator().addProvider(
                event.includeClient(),
                new MaterialTextureProvider(output, helper)
        );

        event.getGenerator().addProvider(
                event.includeClient(),
                new TLItemModelProvider(output, helper)
        );
    }

    @SubscribeEvent
    public static void registerGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(
                ResourceLocation.fromNamespaceAndPath(
                        TinkersLegacy.MODID,
                        "dynamic_material_part"
                ), DynamicMaterialPartModelLoader.INSTANCE
        );

        event.register(
                ResourceLocation.fromNamespaceAndPath(
                        TinkersLegacy.MODID,
                        "dynamic_tool"
                ), DynamicToolModelLoader.INSTANCE
        );
    }
}