package com.nurby.tinkerslegacy.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.api.tool.ToolModifierEntry;
import com.nurby.tinkerslegacy.api.tool.ToolProgress;
import net.minecraft.core.UUIDUtil;
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
import java.util.UUID;
import java.util.stream.Collectors;

public final class TLDataComponents {

        public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
                        DeferredRegister.create(
                                        BuiltInRegistries.DATA_COMPONENT_TYPE,
                                        TinkersLegacy.MODID
                        );

        // Temporary projectile effect copies; never saved onto the original tool.
        public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<ResourceLocation>>> EFFECT_TRAITS =
                        COMPONENTS.register("effect_traits", () -> DataComponentType.<List<ResourceLocation>>builder()
                                        .networkSynchronized(ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()))
                                        .build());

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
                        DataComponentType<?>, DataComponentType<List<ResourceLocation>>
                        > MATERIALS = COMPONENTS.register(
                        "materials", id -> DataComponentType.<List<ResourceLocation>>builder().persistent(ResourceLocation.CODEC.listOf())
                                        .networkSynchronized(ResourceLocation.STREAM_CODEC.apply(
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

        // Stations
        public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> STATION_BASE =
                        component("station_base", ResourceLocation.CODEC, ResourceLocation.STREAM_CODEC);

        // Tool State

        public static final DeferredHolder<DataComponentType<?>, DataComponentType<ToolProgress>> PROGRESS =
                        component("tool_progress", ToolProgress.CODEC, ToolProgress.STREAM_CODEC);

        public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<ToolModifierEntry>>> MODIFIERS =
                        component("modifiers", ToolModifierEntry.CODEC.listOf(), ToolModifierEntry.STREAM_CODEC.apply(ByteBufCodecs.list()));

        public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> STARTING_MODIFIERS =
                        component("starting_modifiers", Codec.intRange(0, Integer.MAX_VALUE), ByteBufCodecs.VAR_INT);

        public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> BROKEN =
                        component("broken", Codec.BOOL, ByteBufCodecs.BOOL);

        public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> BROKEN_DAMAGE =
                        component("broken_damage", Codec.intRange(0, Integer.MAX_VALUE), ByteBufCodecs.VAR_INT);

        public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> REPAIR_COUNT =
                        component("repair_count", Codec.intRange(0, Integer.MAX_VALUE), ByteBufCodecs.VAR_INT);

        public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> TOOL_ID =
                        component("tool_id", UUIDUtil.CODEC, UUIDUtil.STREAM_CODEC);

        public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> NEXT_LEVEL_XP =
                        component("next_level_xp", Codec.LONG.validate(value -> value > 0 ? DataResult.success(value)
                                        : DataResult.error(() -> "Level XP must be positive")), ByteBufCodecs.VAR_LONG);

        public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> LEVELING_ENABLED =
                        component("leveling_enabled", Codec.BOOL, ByteBufCodecs.BOOL);

        public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> LEVEL_CAPPED =
                        component("level_capped", Codec.BOOL, ByteBufCodecs.BOOL);

        public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> MATERIAL_ID =
                        component("material_id", ResourceLocation.CODEC, ResourceLocation.STREAM_CODEC);

        // Material and Modifier State
        public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> MATERIAL_UNITS =
                        component("material_units", Codec.intRange(1, 1296), ByteBufCodecs.VAR_INT);

        public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> TOOL_DATA_VERSION =
                        component("tool_data_version", Codec.intRange(0, Integer.MAX_VALUE), ByteBufCodecs.VAR_INT);

        public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> MOSS_XP =
                        component("moss_xp", Codec.intRange(0, Integer.MAX_VALUE), ByteBufCodecs.VAR_INT);

        public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> MOSS_LAST_HEAL =
                        component("moss_last_heal", Codec.LONG, ByteBufCodecs.VAR_LONG);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<ResourceLocation>>> EMBOSSMENT = component("embossment", ResourceLocation.CODEC.listOf(), ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.nurby.tinkerslegacy.api.tool.traits.ToolTraitData>> TRAIT_DATA = component("trait_data", com.nurby.tinkerslegacy.api.tool.traits.ToolTraitData.CODEC, com.nurby.tinkerslegacy.api.tool.traits.ToolTraitData.STREAM_CODEC);

        // Stats
        public static Map<String, Number> normalizeStats(Map<String, Number> stats) {
                // Persistent and network codecs both represent stat values as doubles.
                return stats.entrySet().stream().collect(Collectors.toUnmodifiableMap(
                                Map.Entry::getKey, entry -> (Number) entry.getValue().doubleValue()));
        }

        // Registration

        private static <T> DeferredHolder<DataComponentType<?>, DataComponentType<T>> component(
                        String name, Codec<T> codec, StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec) {
                return COMPONENTS.register(name, () -> DataComponentType.<T>builder()
                                .persistent(codec).networkSynchronized(streamCodec).build());
        }

        private TLDataComponents() {
        }

        public static void register(IEventBus modBus) {
                COMPONENTS.register(modBus);
        }
}