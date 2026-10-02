package com.nurby.tinkerslegacy.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class TooltipBlockItem extends BlockItem {
        private final String tooltipKey;

        public TooltipBlockItem(
                        Block block,
                        Item.Properties properties,
                        String tooltipKey) {
                super(block, properties);
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
