package com.nurby.tinkerslegacy.network;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.api.station.StationConnections;
import com.nurby.tinkerslegacy.content.block.entity.StationBlockEntity;
import com.nurby.tinkerslegacy.menu.StationMenuAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record StationOpenPayload(int containerId, BlockPos position) implements CustomPacketPayload {
        public static final Type<StationOpenPayload> TYPE = new Type<>(
                        ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, "station_open"));

        public static final StreamCodec<RegistryFriendlyByteBuf, StationOpenPayload> STREAM_CODEC = new StreamCodec<>() {
                @Override
                public StationOpenPayload decode(RegistryFriendlyByteBuf buffer) {
                        return new StationOpenPayload(buffer.readVarInt(), buffer.readBlockPos());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, StationOpenPayload value) {
                        buffer.writeVarInt(value.containerId());
                        buffer.writeBlockPos(value.position());
                }
        };

        @Override
        public Type<? extends CustomPacketPayload> type() {
                return TYPE;
        }

        // Selection
        public static void handle(StationOpenPayload payload, IPayloadContext context) {
                context.enqueueWork(() -> {
                        var player = context.player();
                        var menu = player.containerMenu;
                        if (!(menu instanceof StationMenuAccess access)
                                        || menu.containerId != payload.containerId() || !menu.stillValid(player)
                                        || access.position().equals(payload.position())) {
                                return;
                        }
                        var level = player.level();
                        if (!level.hasChunkAt(payload.position())
                                        || !StationConnections.find(level, access.position()).contains(payload.position())
                                        || !(level.getBlockEntity(payload.position()) instanceof StationBlockEntity station)
                                        || !station.stillValid(player)) {
                                return;
                        }
                        player.openMenu(station);
                });
        }
}
