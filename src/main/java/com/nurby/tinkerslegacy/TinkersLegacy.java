package com.nurby.tinkerslegacy;

import com.mojang.logging.LogUtils;
import com.nurby.tinkerslegacy.registry.*;
import com.nurby.tinkerslegacy.registry.armory.ArmoryStatTypes;
import com.nurby.tinkerslegacy.registry.tools.TLToolEntities;
import com.nurby.tinkerslegacy.registry.tools.TLTools;
import com.nurby.tinkerslegacy.registry.tools.ToolParts;
import com.nurby.tinkerslegacy.registry.tools.ToolStatTypes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

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
                TLAttachments.register(modEventBus);
                TLBlocks.register(modEventBus);
                TLItems.register(modEventBus);
                TLBlockEntities.register(modEventBus);
                TLStationMenus.register(modEventBus);
                TLRecipeSerializers.register(modEventBus);
                TLToolEntities.register(modEventBus);
                com.nurby.tinkerslegacy.registry.TLSounds.register(modEventBus);
            com.nurby.tinkerslegacy.api.tool.traits.ToolTraitEffects.register(modEventBus);

                TLCreativeTabs.register(modEventBus);
        }
}
