package com.nurby.tinkerslegacy.registry;

import com.mojang.serialization.Codec;
import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.library.material.layer.MaterialLayer;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class TLDataComponents {

    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(
                    BuiltInRegistries.DATA_COMPONENT_TYPE,
                    TinkersLegacy.MODID
            );

    public static final Codec<Map<String, Number>> TOOL_STATS_CODEC =
            Codec.unboundedMap(
                    Codec.STRING,
                    Codec.DOUBLE
            ).xmap(
                    map -> map.entrySet().stream()
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    entry -> (Number) entry.getValue()
                            )),
                    map -> map.entrySet().stream()
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    entry -> entry.getValue().doubleValue()
                            ))
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            Map<String, Number>
            > TOOL_STATS_STREAM_CODEC =
            ByteBufCodecs.map(
                    HashMap::new,
                    ByteBufCodecs.STRING_UTF8,
                    ByteBufCodecs.DOUBLE.map(
                            value -> (Number) value,
                            Number::doubleValue
                    )
            );

    public static final DeferredHolder<
            DataComponentType<?>,
            DataComponentType<List<MaterialLayer>>
            > MATERIALS = COMPONENTS.register(
            "materials",
            id -> DataComponentType.<List<MaterialLayer>>builder()
                    .persistent(MaterialLayer.CODEC.listOf())
                    .networkSynchronized(
                            MaterialLayer.STREAM_CODEC.apply(
                                    ByteBufCodecs.list()
                            )
                    )
                    .build()
    );

    public static final DeferredHolder<
            DataComponentType<?>,
            DataComponentType<ResourceLocation>
            > PART = COMPONENTS.register(
            "part",
            id -> DataComponentType.<ResourceLocation>builder()
                    .persistent(ResourceLocation.CODEC)
                    .networkSynchronized(ResourceLocation.STREAM_CODEC)
                    .build()
    );

    public static final DeferredHolder<
            DataComponentType<?>,
            DataComponentType<Map<String, Number>>
            > BASE_STATS = COMPONENTS.register(
            "base_stats",
            id -> DataComponentType.<Map<String, Number>>builder()
                    .persistent(TOOL_STATS_CODEC)
                    .networkSynchronized(TOOL_STATS_STREAM_CODEC)
                    .build()
    );

    public static final DeferredHolder<
            DataComponentType<?>,
            DataComponentType<Map<String, Number>>
            > MODIFIED_STATS = COMPONENTS.register(
            "stats",
            id -> DataComponentType.<Map<String, Number>>builder()
                    .persistent(TOOL_STATS_CODEC)
                    .networkSynchronized(TOOL_STATS_STREAM_CODEC)
                    .build()
    );

    private TLDataComponents() {
    }

    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
    }
}