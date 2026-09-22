package com.nurby.tinkerslegacy.registry;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.library.material.stat.PartStatDefinition;
import com.nurby.tinkerslegacy.library.material.stat.StatField;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public final class ArmoryStatTypes {

    public static final DeferredRegister<PartStatDefinition> STAT_TYPES =
            DeferredRegister.create(
                    TLRegistries.PART_STAT_KEY,
                    TinkersLegacy.MODID
            );

    public static final DeferredHolder<PartStatDefinition, PartStatDefinition> ARMOR_CORE = register(
            "armor_core",
            new StatField("durability", float.class),
            new StatField("defense", float.class)
    );

    public static final DeferredHolder<PartStatDefinition, PartStatDefinition> ARMOR_PLATES = register(
            "armor_plates",
            new StatField("modifier", float.class),
            new StatField("durability", float.class),
            new StatField("toughness", float.class)
    );

    public static final DeferredHolder<PartStatDefinition, PartStatDefinition> ARMOR_TRIM = register(
            "armor_trim",
            new StatField("modifier", float.class),
            new StatField("durability", float.class),
            new StatField("toughness", float.class)
    );

    private ArmoryStatTypes() {}

    public static void register(IEventBus bus) {
        STAT_TYPES.register(bus);
    }

    private static DeferredHolder<PartStatDefinition, PartStatDefinition> register(
            String name,
            StatField... fields
    ) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                TinkersLegacy.MODID,
                name
        );

        return STAT_TYPES.register(
                name,
                () -> new PartStatDefinition(id, List.of(fields))
        );
    }
}