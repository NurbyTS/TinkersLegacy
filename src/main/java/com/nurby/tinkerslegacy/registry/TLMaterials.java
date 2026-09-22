package com.nurby.tinkerslegacy.registry;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.util.TinkersUtils;
import com.nurby.tinkerslegacy.library.material.MaterialBuilder;
import com.nurby.tinkerslegacy.library.material.MaterialDefinition;
import com.nurby.tinkerslegacy.client.datagen.render_info.CompositeRenderInfo;
import com.nurby.tinkerslegacy.client.datagen.render_info.MetalRenderInfo;
import com.nurby.tinkerslegacy.client.datagen.render_info.TintMaterialRenderInfo;
import com.nurby.tinkerslegacy.library.material.recipe.CastingRecipeInfo;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class TLMaterials {

    public static final DeferredRegister<MaterialDefinition> MATERIALS =
            DeferredRegister.create(
                    TLRegistries.MATERIAL_KEY,
                    TinkersLegacy.MODID
            );

    public static final DeferredHolder<MaterialDefinition, MaterialDefinition> WOOD =
            MATERIALS.register(
                    "wood",
                    () -> MaterialBuilder.material(id("wood"))
                            .renderInfo(new TintMaterialRenderInfo(0x8E5B3A))
                            .color(0x8E5B3A)
                            .stats(
                                    TinkersUtils.createMaterialStat(
                                            ToolStatTypes.HEAD,
                                            "durability", 35,
                                            "mining_speed", 2.0f,
                                            "attack_damage", 2.0f,
                                            "harvest_level", 0
                                    ),
                                    TinkersUtils.createMaterialStat(
                                            ToolStatTypes.HANDLE,
                                            "modifier", 1.0f,
                                            "durability", 25
                                    ),
                                    TinkersUtils.createMaterialStat(
                                            ToolStatTypes.EXTRA,
                                            "durability", 15
                                    ),
                                    TinkersUtils.createMaterialStat(
                                            ToolStatTypes.ARROW_SHAFT,
                                            "modifier", 1.0f,
                                            "bonus_ammo", 2
                                    )
                            )
                            .build()
            );

    public static final DeferredHolder<MaterialDefinition, MaterialDefinition> STONE =
            MATERIALS.register(
                    "stone",
                    () -> MaterialBuilder.material(id("stone"))
                            .renderInfo(new TintMaterialRenderInfo(0x777777))
                            .color(0x777777)
                            .stats(
                                    TinkersUtils.createMaterialStat(
                                            ToolStatTypes.HEAD,
                                            "durability", 100,
                                            "mining_speed", 3.0f,
                                            "attack_damage", 3.0f,
                                            "harvest_level", 1
                                    ),
                                    TinkersUtils.createMaterialStat(
                                            ToolStatTypes.HANDLE,
                                            "modifier", 0.8f,
                                            "durability", 20
                                    ),
                                    TinkersUtils.createMaterialStat(
                                            ToolStatTypes.EXTRA,
                                            "durability", 30
                                    )
                            )
                            .build()
            );

    public static final DeferredHolder<MaterialDefinition, MaterialDefinition> IRON =
            MATERIALS.register(
                    "iron",
                    () -> MaterialBuilder.material(id("iron"))
                            .renderInfo(new MetalRenderInfo(
                                    0xD8D8D8,
                                    0.0f,
                                    0.3f,
                                    0.0f
                            ))
                            .color(0xD8D8D8)
                            .recipeInfo(new CastingRecipeInfo(id("iron")))
                            .stats(
                                    TinkersUtils.createMaterialStat(
                                            ToolStatTypes.HEAD,
                                            "durability", 250,
                                            "mining_speed", 6.0f,
                                            "attack_damage", 5.0f,
                                            "harvest_level", 2
                                    ),
                                    TinkersUtils.createMaterialStat(
                                            ToolStatTypes.HANDLE,
                                            "modifier", 1.0f,
                                            "durability", 50
                                    ),
                                    TinkersUtils.createMaterialStat(
                                            ToolStatTypes.EXTRA,
                                            "durability", 50
                                    )
                            )
                            .build()
            );

    public static final DeferredHolder<MaterialDefinition, MaterialDefinition> DIAMOND =
            MATERIALS.register(
                    "diamond",
                    () -> MaterialBuilder.material(id("diamond"))
                            .renderInfo(new MetalRenderInfo(
                                    0x5DE0D0,
                                    0.2f,
                                    0.15f,
                                    0.0f
                            ))
                            .color(0x5DE0D0)
                            .stats(
                                    TinkersUtils.createMaterialStat(
                                            ToolStatTypes.HEAD,
                                            "durability", 750,
                                            "mining_speed", 8.0f,
                                            "attack_damage", 7.0f,
                                            "harvest_level", 3
                                    ),
                                    TinkersUtils.createMaterialStat(
                                            ToolStatTypes.HANDLE,
                                            "modifier", 1.2f,
                                            "durability", 75
                                    ),
                                    TinkersUtils.createMaterialStat(
                                            ToolStatTypes.EXTRA,
                                            "durability", 100
                                    )
                            )
                            .build()
            );

    public static final DeferredHolder<MaterialDefinition, MaterialDefinition> NETHERRACK =
            MATERIALS.register(
                    "netherrack",
                    () -> MaterialBuilder.material(id("netherrack"))
                            .renderInfo(new CompositeRenderInfo(
                                    ResourceLocation.withDefaultNamespace(
                                            "block/netherrack"
                                    )
                            ))
                            .color(0x7B2A2A)
                            .stats(
                                    TinkersUtils.createMaterialStat(
                                            ToolStatTypes.HEAD,
                                            "durability", 750,
                                            "mining_speed", 8.0f,
                                            "attack_damage", 7.0f,
                                            "harvest_level", 3
                                    ),
                                    TinkersUtils.createMaterialStat(
                                            ToolStatTypes.HANDLE,
                                            "modifier", 1.2f,
                                            "durability", 75
                                    ),
                                    TinkersUtils.createMaterialStat(
                                            ToolStatTypes.EXTRA,
                                            "durability", 100
                                    )
                            )
                            .build()
            );

    public static void register(IEventBus bus) {
        MATERIALS.register(bus);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(
                TinkersLegacy.MODID,
                path
        );
    }
}