package com.nurby.tinkerslegacy.registry;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.library.part.PartDefinition;

import net.minecraft.resources.ResourceLocation;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collections;

public final class ToolParts {
        public static final DeferredRegister<PartDefinition> PARTS = DeferredRegister.create(
                        TLRegistries.PART_KEY,
                        TinkersLegacy.MODID);

        public enum Type {
                PICKAXE_HEAD("pick_head", 2,
                                () -> List.of(ToolStatTypes.HEAD.getId())),

                AXE_HEAD("axe_head", 2,
                                () -> List.of(ToolStatTypes.HEAD.getId())),

                SHOVEL_HEAD("shovel_head", 2,
                                () -> List.of(ToolStatTypes.HEAD.getId())),

                KAMA_HEAD("kama_head", 2,
                                () -> List.of(ToolStatTypes.HEAD.getId())),

                SWORD_BLADE("sword_blade", 2,
                                () -> List.of(ToolStatTypes.HEAD.getId())),

                HAMMER_HEAD("hammer_head", 8,
                                () -> List.of(ToolStatTypes.HEAD.getId())),

                BROAD_AXE_HEAD("broad_axe_head", 8,
                                () -> List.of(ToolStatTypes.HEAD.getId())),

                LARGE_SWORD_BLADE("large_sword_blade", 8,
                                () -> List.of(ToolStatTypes.HEAD.getId())),

                EXCAVATOR_HEAD("excavator_head", 8,
                                () -> List.of(ToolStatTypes.HEAD.getId())),

                SCYTHE_HEAD("scythe_head", 8,
                                () -> List.of(ToolStatTypes.HEAD.getId())),

                PAN_HEAD("pan_head", 3,
                                () -> List.of(ToolStatTypes.HEAD.getId())),

                SIGN_HEAD("sign_head", 3,
                                () -> List.of(ToolStatTypes.HEAD.getId())),

                LARGE_PLATE("large_plate", 8,
                                () -> List.of(ToolStatTypes.HEAD.getId(), ToolStatTypes.EXTRA.getId())),

                KNIFE_BLADE("knife_blade", 3,
                                () -> List.of(ToolStatTypes.HEAD.getId(), ToolStatTypes.EXTRA.getId())),

                BOW_LIMB("bow_limb", 3,
                                () -> List.of(ToolStatTypes.HEAD.getId(), ToolStatTypes.BOW.getId())),

                BOW_STRING("bow_string", 1,
                                () -> List.of(ToolStatTypes.EXTRA.getId())),

                ARROW_HEAD("arrow_head", 2,
                                () -> List.of(ToolStatTypes.HEAD.getId())),

                ARROW_SHAFT("arrow_shaft", 2,
                                () -> List.of(ToolStatTypes.ARROW_SHAFT.getId())),

                FLETCHING("fletching", 2,
                                () -> List.of(ToolStatTypes.FLETCHING.getId())),

                TOOL_ROD("tool_rod", 1,
                                () -> List.of(ToolStatTypes.HANDLE.getId())),

                TOUGH_TOOL_ROD("tough_tool_rod", 3,
                                () -> List.of(ToolStatTypes.HANDLE.getId(), ToolStatTypes.EXTRA.getId())),

                BINDING("binding", 1,
                                () -> List.of(ToolStatTypes.EXTRA.getId())),

                TOUGH_BINDING("tough_binding", 3,
                                () -> List.of(ToolStatTypes.EXTRA.getId())),

                WIDE_GUARD("wide_guard", 1,
                                () -> List.of(ToolStatTypes.EXTRA.getId())),

                CROSS_GUARD("cross_guard", 1,
                                () -> List.of(ToolStatTypes.EXTRA.getId())),

                BOLT_CORE("bolt_core", 2,
                                () -> List.of(ToolStatTypes.HEAD.getId(), ToolStatTypes.ARROW_SHAFT.getId()));

                private final String name;
                private final int castingCost;
                private final Supplier<List<ResourceLocation>> statTypes;

                Type(String name, int castingCost, Supplier<List<ResourceLocation>> statTypes) {
                        this.name = name;
                        this.castingCost = castingCost;
                        this.statTypes = statTypes;
                }
        }

        private static final Map<Type, DeferredHolder<PartDefinition, PartDefinition>> VARIANTS = registerVariants();

        public static final List<DeferredHolder<PartDefinition, PartDefinition>> ALL = List.copyOf(VARIANTS.values());

        private ToolParts() {
        }

        // Getters
        public static DeferredHolder<PartDefinition, PartDefinition> get(Type type) {
                return VARIANTS.get(type);
        }

        public static PartDefinition part(Type type) {
                return get(type).get();
        }

        // Registration
        public static void register(IEventBus modBus) {
                PARTS.register(modBus);
        }

        private static Map<Type, DeferredHolder<PartDefinition, PartDefinition>> registerVariants() {
                Map<Type, DeferredHolder<PartDefinition, PartDefinition>> result = new LinkedHashMap<>();

                for (Type type : Type.values()) {
                        DeferredHolder<PartDefinition, PartDefinition> holder = PARTS.register(
                                        type.name,
                                        () -> new PartDefinition(
                                                        ResourceLocation.fromNamespaceAndPath(
                                                                        TinkersLegacy.MODID,
                                                                        type.name),
                                                        type.castingCost,
                                                        type.statTypes.get().toArray(ResourceLocation[]::new)));

                        result.put(type, holder);
                }

                return Collections.unmodifiableMap(result);
        }
}
