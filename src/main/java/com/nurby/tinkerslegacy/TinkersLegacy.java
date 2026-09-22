package com.nurby.tinkerslegacy;

import com.nurby.tinkerslegacy.registry.*;
import com.nurby.tinkerslegacy.registry.TLTools;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;

@Mod(TinkersLegacy.MODID)
public class TinkersLegacy {
    public static final String MODID = "tinkerslegacy";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TinkersLegacy(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, TLConfig.SPEC);

        // TODO: Make these load conditionally
        TLStatTypes.register(modEventBus);
        ToolStatTypes.register(modEventBus);
        ArmoryStatTypes.register(modEventBus);

        TLMaterials.register(modEventBus);
        ToolParts.register(modEventBus);
        // TODO: ArmorParts.register(modEventBus);

        TLTools.register(modEventBus);

        TLDataComponents.register(modEventBus);
        TLItems.register(modEventBus);

        TLCreativeTabs.register(modEventBus);
    }
}