package com.nurby.tinkerslegacy.client;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.content.item.dynamic.DynamicTool;
import net.minecraft.ChatFormatting;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = TinkersLegacy.MODID, value = Dist.CLIENT)
public final class ToolTooltipEvents {

        private ToolTooltipEvents() {
        }

        // Names

        @SubscribeEvent
        public static void tooltip(ItemTooltipEvent event) {
                if (event.getItemStack().getItem() instanceof DynamicTool && !event.getToolTip().isEmpty()) {
                        // Original tools keep common-rarity names even when modifiers supply vanilla enchantments.
                        event.getToolTip().set(0, event.getToolTip().getFirst().copy().withStyle(ChatFormatting.WHITE));
                }
        }
}
