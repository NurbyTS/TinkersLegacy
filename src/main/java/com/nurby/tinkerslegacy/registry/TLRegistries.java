package com.nurby.tinkerslegacy.registry;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.api.material.MaterialDefinition;
import com.nurby.tinkerslegacy.api.material.stat.PartStatDefinition;
import com.nurby.tinkerslegacy.api.part.PartDefinition;
import com.nurby.tinkerslegacy.api.tool.ToolDefinition;
import com.nurby.tinkerslegacy.api.trait.TraitDefinition;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.RegistryBuilder;

public class TLRegistries {
    public static final ResourceKey<Registry<MaterialDefinition>> MATERIAL_KEY = ResourceKey.createRegistryKey(
            ResourceLocation.fromNamespaceAndPath(
                    TinkersLegacy.MODID,
                    "material"
            )
    );

    public static final Registry<MaterialDefinition> MATERIALS = new RegistryBuilder<>(MATERIAL_KEY)
            .sync(true)
            .defaultKey(ResourceLocation.fromNamespaceAndPath(
                    TinkersLegacy.MODID,
                    "unknown"
            ))
            .create();

    public static final ResourceKey<Registry<PartDefinition>> PART_KEY = ResourceKey.createRegistryKey(
            ResourceLocation.fromNamespaceAndPath(
                    TinkersLegacy.MODID,
                    "part"
            )
    );

    public static final Registry<PartDefinition> PARTS = new RegistryBuilder<>(PART_KEY)
            .sync(true)
            .create();

    public static final ResourceKey<Registry<PartStatDefinition>> PART_STAT_KEY = ResourceKey.createRegistryKey(
            ResourceLocation.fromNamespaceAndPath(
                    TinkersLegacy.MODID,
                    "material_stat"
            )
    );

    public static final Registry<PartStatDefinition> PART_STATS = new RegistryBuilder<>(PART_STAT_KEY)
            .sync(true)
            .create();

    public static final ResourceKey<Registry<ToolDefinition>> TOOL_KEY = ResourceKey.createRegistryKey(
            ResourceLocation.fromNamespaceAndPath(
                    TinkersLegacy.MODID,
                    "tool"
            )
    );

    public static final Registry<ToolDefinition> TOOLS = new RegistryBuilder<>(TOOL_KEY)
            .sync(true)
            .create();

    public static final ResourceKey<Registry<TraitDefinition>> TRAIT_KEY = ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, "trait"));

    public static final Registry<TraitDefinition> TRAITS = new RegistryBuilder<>(TRAIT_KEY).sync(true).create();
}
