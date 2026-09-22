package com.nurby.tinkerslegacy.registry;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.library.part.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ToolParts {

    public static final DeferredRegister<PartDefinition> PARTS =
            DeferredRegister.create(
                    TLRegistries.PART_KEY,
                    TinkersLegacy.MODID
            );

    public static final DeferredHolder<PartDefinition, PartDefinition> PICKAXE_HEAD =
            PARTS.register(
                    "pick_head",
                    () -> part(
                            "pick_head",
                            2,
                            ToolStatTypes.HEAD.getId()
                    )
            );

    public static final DeferredHolder<PartDefinition, PartDefinition> AXE_HEAD =
            PARTS.register(
                    "axe_head",
                    () -> part(
                            "axe_head",
                            2,
                            ToolStatTypes.HEAD.getId()
                    )
            );

    public static final DeferredHolder<PartDefinition, PartDefinition> SHOVEL_HEAD =
            PARTS.register(
                    "shovel_head",
                    () -> part(
                            "shovel_head",
                            2,
                            ToolStatTypes.HEAD.getId()
                    )
            );

    public static final DeferredHolder<PartDefinition, PartDefinition> KAMA_HEAD =
            PARTS.register(
                    "kama_head",
                    () -> part(
                            "kama_head",
                            2,
                            ToolStatTypes.HEAD.getId()
                    )
            );

    public static final DeferredHolder<PartDefinition, PartDefinition> SWORD_BLADE =
            PARTS.register(
                    "sword_blade",
                    () -> part(
                            "sword_blade",
                            2,
                            ToolStatTypes.HEAD.getId()
                    )
            );

    public static final DeferredHolder<PartDefinition, PartDefinition> HAMMER_HEAD =
            PARTS.register(
                    "hammer_head",
                    () -> part(
                            "hammer_head",
                            8,
                            ToolStatTypes.HEAD.getId()
                    )
            );

    public static final DeferredHolder<PartDefinition, PartDefinition> BROAD_AXE_HEAD =
            PARTS.register(
                    "broad_axe_head",
                    () -> part(
                            "broad_axe_head",
                            8,
                            ToolStatTypes.HEAD.getId()
                    )
            );

    public static final DeferredHolder<PartDefinition, PartDefinition> LARGE_SWORD_BLADE =
            PARTS.register(
                    "large_sword_blade",
                    () -> part(
                            "large_sword_blade",
                            8,
                            ToolStatTypes.HEAD.getId()
                    )
            );

    public static final DeferredHolder<PartDefinition, PartDefinition> EXCAVATOR_HEAD =
            PARTS.register(
                    "excavator_head",
                    () -> part(
                            "excavator_head",
                            8,
                            ToolStatTypes.HEAD.getId()
                    )
            );

    public static final DeferredHolder<PartDefinition, PartDefinition> SCYTHE_HEAD =
            PARTS.register(
                    "scythe_head",
                    () -> part(
                            "scythe_head",
                            8,
                            ToolStatTypes.HEAD.getId()
                    )
            );

    public static final DeferredHolder<PartDefinition, PartDefinition> PAN_HEAD =
            PARTS.register(
                    "pan_head",
                    () -> part(
                            "pan_head",
                            3,
                            ToolStatTypes.HEAD.getId()
                    )
            );

    public static final DeferredHolder<PartDefinition, PartDefinition> SIGN_HEAD =
            PARTS.register(
                    "sign_head",
                    () -> part(
                            "sign_head",
                            3,
                            ToolStatTypes.HEAD.getId()
                    )
            );

    public static final DeferredHolder<PartDefinition, PartDefinition> LARGE_PLATE =
            PARTS.register(
                    "large_plate",
                    () -> part(
                            "large_plate",
                            8,
                            ToolStatTypes.HEAD.getId(),
                            ToolStatTypes.EXTRA.getId()
                    )
            );

    public static final DeferredHolder<PartDefinition, PartDefinition> KNIFE_BLADE =
            PARTS.register(
                    "knife_blade",
                    () -> part(
                            "knife_blade",
                            3,
                            ToolStatTypes.HEAD.getId(),
                            ToolStatTypes.EXTRA.getId()
                    )
            );

    public static final DeferredHolder<PartDefinition, PartDefinition> BOW_LIMB =
            PARTS.register(
                    "bow_limb",
                    () -> part(
                            "bow_limb",
                            3,
                            ToolStatTypes.HEAD.getId(),
                            ToolStatTypes.BOW.getId()
                    )
            );

    public static final DeferredHolder<PartDefinition, PartDefinition> BOW_STRING =
            PARTS.register(
                    "bow_string",
                    () -> part(
                            "bow_string",
                            1,
                            ToolStatTypes.EXTRA.getId()
                    )
            );

    public static final DeferredHolder<PartDefinition, PartDefinition> ARROW_HEAD =
            PARTS.register(
                    "arrow_head",
                    () -> part(
                            "arrow_head",
                            2,
                            ToolStatTypes.HEAD.getId()
                    )
            );

    public static final DeferredHolder<PartDefinition, PartDefinition> ARROW_SHAFT =
            PARTS.register(
                    "arrow_shaft",
                    () -> part(
                            "arrow_shaft",
                            2,
                            ToolStatTypes.ARROW_SHAFT.getId()
                    )
            );

    public static final DeferredHolder<PartDefinition, PartDefinition> FLETCHING =
            PARTS.register(
                    "fletching",
                    () -> part(
                            "fletching",
                            2,
                            ToolStatTypes.FLETCHING.getId()
                    )
            );

    public static final DeferredHolder<PartDefinition, PartDefinition> TOOL_ROD =
            PARTS.register(
                    "tool_rod",
                    () -> part(
                            "tool_rod",
                            1,
                            ToolStatTypes.HANDLE.getId()
                    )
            );

    public static final DeferredHolder<PartDefinition, PartDefinition> TOUGH_TOOL_ROD =
            PARTS.register(
                    "tough_tool_rod",
                    () -> part(
                            "tough_tool_rod",
                            3,
                            ToolStatTypes.HANDLE.getId(),
                            ToolStatTypes.EXTRA.getId()
                    )
            );

    public static final DeferredHolder<PartDefinition, PartDefinition> BINDING =
            PARTS.register(
                    "binding",
                    () -> part(
                            "binding",
                            1,
                            ToolStatTypes.EXTRA.getId()
                    )
            );

    public static final DeferredHolder<PartDefinition, PartDefinition> TOUGH_BINDING =
            PARTS.register(
                    "tough_binding",
                    () -> part(
                            "tough_binding",
                            3,
                            ToolStatTypes.EXTRA.getId()
                    )
            );

    public static final DeferredHolder<PartDefinition, PartDefinition> WIDE_GUARD =
            PARTS.register(
                    "wide_guard",
                    () -> part(
                            "wide_guard",
                            1,
                            ToolStatTypes.EXTRA.getId()
                    )
            );

    public static final DeferredHolder<PartDefinition, PartDefinition> CROSS_GUARD =
            PARTS.register(
                    "cross_guard",
                    () -> part(
                            "cross_guard",
                            1,
                            ToolStatTypes.EXTRA.getId()
                    )
            );

    public static final DeferredHolder<PartDefinition, PartDefinition> BOLT_CORE =
            PARTS.register(
                    "bolt_core",
                    () -> part(
                            "bolt_core",
                            2,
                            ToolStatTypes.HEAD.getId(),
                            ToolStatTypes.ARROW_SHAFT.getId()
                    )
            );

    private ToolParts() {}

    public static void register(IEventBus bus) {
        PARTS.register(bus);
    }

    private static PartDefinition part(
            String name,
            int size,
            ResourceLocation... statTypes
    ) {
        return new PartDefinition(
                id(name),
                size,
                statTypes
        );
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(
                TinkersLegacy.MODID,
                path
        );
    }
}