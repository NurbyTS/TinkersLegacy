package com.nurby.tinkerslegacy.event;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.network.StationOpenPayload;
import com.nurby.tinkerslegacy.network.StationSelectionPayload;
import com.nurby.tinkerslegacy.network.StationStatusPayload;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = TinkersLegacy.MODID)
public final class NetworkEvents {

    private NetworkEvents() {
        }

        @SubscribeEvent
        public static void registerPayloads(RegisterPayloadHandlersEvent event) {
                var registrar = event.registrar("2");
                registrar.playToServer(StationOpenPayload.TYPE, StationOpenPayload.STREAM_CODEC, StationOpenPayload::handle);
                registrar.playToServer(StationSelectionPayload.TYPE, StationSelectionPayload.STREAM_CODEC, StationSelectionPayload::handle);
                registrar.playToClient(StationStatusPayload.TYPE, StationStatusPayload.STREAM_CODEC, StationStatusPayload::handle);
        }
}
