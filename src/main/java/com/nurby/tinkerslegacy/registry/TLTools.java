package com.nurby.tinkerslegacy.registry;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.library.tool.ToolDefinition;
import com.nurby.tinkerslegacy.tools.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class TLTools {

    public static final DeferredRegister<ToolDefinition> TOOLS = DeferredRegister.create(
            TLRegistries.TOOL_KEY,
            TinkersLegacy.MODID
    );

    public static final DeferredHolder<ToolDefinition, ToolDefinition> PICKAXE = TOOLS.register(
            "pickaxe",
            PickaxeDefinition::new
    );

    public static final DeferredHolder<ToolDefinition, ToolDefinition> SHOVEL = TOOLS.register(
            "shovel",
            ShovelDefinition::new
    );

    public static final DeferredHolder<ToolDefinition, ToolDefinition> HATCHET = TOOLS.register(
            "hatchet",
            HatchetDefinition::new
    );

    public static final DeferredHolder<ToolDefinition, ToolDefinition> MATTOCK = TOOLS.register(
            "mattock",
            MattockDefinition::new
    );

    public static final DeferredHolder<ToolDefinition, ToolDefinition> KAMA = TOOLS.register(
            "kama",
            KamaDefinition::new
    );

    public static final DeferredHolder<ToolDefinition, ToolDefinition> BROADSWORD = TOOLS.register(
            "broadsword",
            BroadswordDefinition::new
    );

    public static final DeferredHolder<ToolDefinition, ToolDefinition> LONGSWORD = TOOLS.register(
            "longsword",
            LongswordDefinition::new
    );

    public static final DeferredHolder<ToolDefinition, ToolDefinition> RAPIER = TOOLS.register(
            "rapier",
            RapierDefinition::new
    );

    private TLTools() {}

    public static void register(IEventBus bus) {
        TOOLS.register(bus);
    }
}