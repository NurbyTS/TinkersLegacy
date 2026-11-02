package com.nurby.tinkerslegacy.network;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.menu.StationMenu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nullable;

public record StationSelectionPayload(
                int containerId, ResourceLocation operation, @Nullable ResourceLocation selection,
                String name, int replacementIndex, long request) implements CustomPacketPayload {
        public static final Type<StationSelectionPayload> TYPE = new Type<>(
                        ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, "station_selection"));

        public static final StreamCodec<RegistryFriendlyByteBuf, StationSelectionPayload> STREAM_CODEC = new StreamCodec<>() {
                @Override
                public StationSelectionPayload decode(RegistryFriendlyByteBuf buffer) {
                        return new StationSelectionPayload(buffer.readVarInt(), buffer.readResourceLocation(),
                                        buffer.readBoolean() ? buffer.readResourceLocation() : null,
                                        buffer.readUtf(50), buffer.readVarInt(), buffer.readVarLong());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, StationSelectionPayload value) {
                        buffer.writeVarInt(value.containerId());
                        buffer.writeResourceLocation(value.operation());
                        buffer.writeBoolean(value.selection() != null);
                        if (value.selection() != null) {
                                buffer.writeResourceLocation(value.selection());
                        }
                        buffer.writeUtf(value.name(), 50);
                        buffer.writeVarInt(value.replacementIndex());
                        buffer.writeVarLong(value.request());
                }
        };

        @Override
        public Type<? extends CustomPacketPayload> type() {
                return TYPE;
        }

        public static void handle(StationSelectionPayload payload, IPayloadContext context) {
                context.enqueueWork(() -> {
                        if (context.player().containerMenu instanceof StationMenu menu
                                        && menu.containerId == payload.containerId() && menu.stillValid(context.player())) {
                                menu.selectRequest(payload.operation(), payload.selection(), payload.name(),
                                                payload.replacementIndex(), payload.request());
                        }
                });
        }
}
