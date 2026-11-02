package com.nurby.tinkerslegacy.client;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.library.station.StationRecipes;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLItems;
import com.nurby.tinkerslegacy.registry.TLRegistries;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@EventBusSubscriber(modid = TinkersLegacy.MODID, value = Dist.CLIENT)
public final class StationItemEvents {
        private StationItemEvents() {
        }

        @SubscribeEvent
        public static void setup(FMLClientSetupEvent event) {
                event.enqueueWork(() -> ItemProperties.register(
                                TLItems.item(TLItems.Items.IntermediaryItems.Type.PATTERN),
                                ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, "part"),
                                (stack, level, entity, seed) -> {
                                        var part = stack.get(TLDataComponents.PART);
                                        return part == null ? 0 : StationRecipes.shapes().indexOf(part) + 1;
                                }));
        }

        @SubscribeEvent
        public static void colors(RegisterColorHandlersEvent.Item event) {
                event.register((stack, tint) -> {
                        var id = stack.get(TLDataComponents.MATERIAL_ID);
                        var material = id == null ? null : TLRegistries.MATERIALS.get(id);
                        return material == null ? -1 : 0xFF000000 | material.color();
                }, TLItems.Parts.MATERIAL_SHARD.get());
        }
}
