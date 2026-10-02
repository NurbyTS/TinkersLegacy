package com.nurby.tinkerslegacy.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class TooltipHelper {
        private TooltipHelper() {
        }

        public static void addTooltip(List<Component> tooltip, String tooltipKey) {
                String translated = Component.translatable(tooltipKey).getString();

                for (String line : translated.split("\\R", -1)) {
                        tooltip.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
                }
        }
}
