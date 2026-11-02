package com.nurby.tinkerslegacy.registry.tools;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.api.tool.ToolDefinition;
import com.nurby.tinkerslegacy.content.tools.*;
import com.nurby.tinkerslegacy.registry.TLRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class TLTools {
        public static final DeferredRegister<ToolDefinition> TOOLS = DeferredRegister.create(
                        TLRegistries.TOOL_KEY,
                        TinkersLegacy.MODID);

        public enum Type {
                PICKAXE("pickaxe", PickaxeDefinition::new),
                SHOVEL("shovel", ShovelDefinition::new),
                HATCHET("hatchet", HatchetDefinition::new),
                MATTOCK("mattock", MattockDefinition::new),
                KAMA("kama", KamaDefinition::new),
                BROADSWORD("broadsword", BroadswordDefinition::new),
                LONGSWORD("longsword", LongswordDefinition::new),
                RAPIER("rapier", RapierDefinition::new),
                FRYPAN("frypan", FrypanDefinition::new),
                BATTLESIGN("battlesign", BattlesignDefinition::new),
                HAMMER("hammer", HammerDefinition::new),
                EXCAVATOR("excavator", ExcavatorDefinition::new),
                LUMBERAXE("lumberaxe", LumberaxeDefinition::new),
                SCYTHE("scythe", ScytheDefinition::new),
                CLEAVER("cleaver", CleaverDefinition::new),
                SHORTBOW("shortbow", ShortbowDefinition::new),
                LONGBOW("longbow", LongbowDefinition::new),
                CROSSBOW("crossbow", CrossbowDefinition::new),
                ARROW("arrow", ArrowDefinition::new),
                BOLT("bolt", BoltDefinition::new),
                SHURIKEN("shuriken", ShurikenDefinition::new);

                private final String name;
                private final Supplier<? extends ToolDefinition> factory;

                Type(String name, Supplier<? extends ToolDefinition> factory) {
                        this.name = name;
                        this.factory = factory;
                }
        }

        private static final Map<Type, DeferredHolder<ToolDefinition, ToolDefinition>> VARIANTS = registerVariants();

        public static final List<DeferredHolder<ToolDefinition, ToolDefinition>> ALL = List.copyOf(VARIANTS.values());

        private TLTools() {
        }

        // Getters
        public static DeferredHolder<ToolDefinition, ToolDefinition> get(Type type) {
                return VARIANTS.get(type);
        }

        public static ToolDefinition tool(Type type) {
                return get(type).get();
        }

        // Registration
        public static void register(IEventBus modBus) {
                TOOLS.register(modBus);
        }

        private static Map<Type, DeferredHolder<ToolDefinition, ToolDefinition>> registerVariants() {
                Map<Type, DeferredHolder<ToolDefinition, ToolDefinition>> result = new LinkedHashMap<>();

                for (Type type : Type.values()) {
                        result.put(type, TOOLS.register(type.name, type.factory));
                }

                return Collections.unmodifiableMap(result);
        }
}
