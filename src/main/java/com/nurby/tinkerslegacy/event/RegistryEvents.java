package com.nurby.tinkerslegacy.event;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.registry.TLRegistries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.NewRegistryEvent;

@EventBusSubscriber(modid = TinkersLegacy.MODID)
public class RegistryEvents {
    @SubscribeEvent
    public static void registerRegistries(NewRegistryEvent event) {
        event.register(TLRegistries.MATERIALS);
        event.register(TLRegistries.PARTS);
        event.register(TLRegistries.PART_STATS);
        event.register(TLRegistries.TOOLS);
    }
}
