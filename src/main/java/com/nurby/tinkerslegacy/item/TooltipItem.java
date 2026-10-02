package com.nurby.tinkerslegacy.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class TooltipItem extends Item {
        private final String tooltipKey;

        public TooltipItem(
                        Item.Properties properties,
                        String tooltipKey) {
                super(properties);
                this.tooltipKey = tooltipKey;
        }

        @Override
        public void appendHoverText(
                        ItemStack stack,
                        Item.TooltipContext context,
                        List<Component> tooltip,
                        TooltipFlag flag) {
                super.appendHoverText(stack, context, tooltip, flag);
                TooltipHelper.addTooltip(tooltip, tooltipKey);
        }
}
