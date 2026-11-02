package com.nurby.tinkerslegacy.network;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.api.station.StationSelection;
import com.nurby.tinkerslegacy.menu.StationMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record StationStatusPayload(int containerId, Component error, StationSelection selection, long acknowledgedRequest) implements CustomPacketPayload {
        public static final Type<StationStatusPayload> TYPE = new Type<>(
                        ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, "station_status"));

        public static final StreamCodec<RegistryFriendlyByteBuf, StationStatusPayload> STREAM_CODEC = new StreamCodec<>() {
                @Override
                public StationStatusPayload decode(RegistryFriendlyByteBuf buffer) {
                        int containerId = buffer.readVarInt();
                        Component error = ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buffer);
                        var operation = buffer.readResourceLocation();
                        var selected = buffer.readBoolean() ? buffer.readResourceLocation() : null;
                        return new StationStatusPayload(containerId, error,
                                        new StationSelection(operation, selected, buffer.readUtf(50), buffer.readVarInt()), buffer.readVarLong());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, StationStatusPayload value) {
                        buffer.writeVarInt(value.containerId());
                        ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buffer, value.error());
                        buffer.writeResourceLocation(value.selection().operation());
                        buffer.writeBoolean(value.selection().selection() != null);
                        if (value.selection().selection() != null) buffer.writeResourceLocation(value.selection().selection());
                        buffer.writeUtf(value.selection().name(), 50);
                        buffer.writeVarInt(value.selection().replacementIndex());
                        buffer.writeVarLong(value.acknowledgedRequest());
                }
        };

        @Override
        public Type<? extends CustomPacketPayload> type() {
                return TYPE;
        }

        public static void handle(StationStatusPayload payload, IPayloadContext context) {
                context.enqueueWork(() -> {
                        if (context.player().containerMenu instanceof StationMenu menu && menu.containerId == payload.containerId()) {
                                menu.acceptStatus(payload.error(), payload.selection(), payload.acknowledgedRequest());
                        }
                });
        }
}
