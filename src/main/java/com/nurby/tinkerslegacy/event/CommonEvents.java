package com.nurby.tinkerslegacy.event;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.command.ToolCommand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = TinkersLegacy.MODID)
public class CommonEvents {
    @SubscribeEvent
    public static void onCommandRegister(RegisterCommandsEvent event) {
        ToolCommand.register(event);
    }
}
