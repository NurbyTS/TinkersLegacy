package com.nurby.tinkerslegacy.registry;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.library.material.stat.PartStatDefinition;
import com.nurby.tinkerslegacy.library.material.stat.StatField;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public final class ToolStatTypes {
    public static final DeferredRegister<PartStatDefinition> STAT_TYPES =
            DeferredRegister.create(TLRegistries.PART_STAT_KEY, TinkersLegacy.MODID);

    public static final DeferredHolder<PartStatDefinition, PartStatDefinition> ARROW_SHAFT = register(
            "arrow_shaft",
            new StatField("modifier", float.class),
            new StatField("bonus_ammo", int.class)
    );

    public static final DeferredHolder<PartStatDefinition, PartStatDefinition> BOW = register(
            "bow",
            new StatField("draw_speed", float.class),
            new StatField("range", float.class),
            new StatField("bonus_damage", float.class)
    );

    public static final DeferredHolder<PartStatDefinition, PartStatDefinition> EXTRA = register(
            "extra",
            new StatField("durability", int.class)
    );

    public static final DeferredHolder<PartStatDefinition, PartStatDefinition> FLETCHING = register(
            "fletching",
            new StatField("accuracy", float.class),
            new StatField("modifier", float.class)
    );

    public static final DeferredHolder<PartStatDefinition, PartStatDefinition> HANDLE = register(
            "handle",
            new StatField("modifier", float.class),
            new StatField("durability", int.class)
    );

    public static final DeferredHolder<PartStatDefinition, PartStatDefinition> HEAD = register(
            "head",
            new StatField("durability", int.class),
            new StatField("mining_speed", float.class),
            new StatField("attack_damage", float.class),
            new StatField("harvest_level", int.class)
    );

    public static final DeferredHolder<PartStatDefinition, PartStatDefinition> PROJECTILE = register(
            "projectile"
    );

    private ToolStatTypes() {}

    public static void register(IEventBus bus) {
        STAT_TYPES.register(bus);
    }

    private static DeferredHolder<PartStatDefinition, PartStatDefinition> register(
            String name,
            StatField... fields
    ) {
        return STAT_TYPES.register(
                name,
                () -> new PartStatDefinition(
                        ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, name),
                        List.of(fields)
                )
        );
    }
}