package com.nurby.tinkerslegacy.library;

import com.nurby.tinkerslegacy.library.material.layer.MaterialLayer;
import com.nurby.tinkerslegacy.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.util.TinkersUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ClientHandler {

    public static void addPartTooltip(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltipComponents,
            TooltipFlag tooltipFlag
    ) {
        List<MaterialLayer> materials =
                stack.get(TLDataComponents.MATERIALS);

        // TODO: Display traits, add a blank component, and only then show the following text.

        boolean first = true;
        if (Screen.hasShiftDown()) {
            if (materials != null && !materials.isEmpty()) {
                for (MaterialLayer layer : materials) {
                    if (!first) {
                        tooltipComponents.add(Component.empty());
                    }
                    first = false;
                    List<Component> stats = TinkersUtils.getStatsForLayer(layer);
                    tooltipComponents.addAll(stats);
                }
            }
        }
        else {
            Component shift = Component.translatable(
                    "tinkerslegacy.tooltip.shift"
            ).withStyle(style -> style
                    .withColor(ChatFormatting.YELLOW)
                    .withItalic(true)
            );

            tooltipComponents.add(
                    Component.translatable("tinkerslegacy.tooltip.hold_stats",
                            shift
                    ).withStyle(ChatFormatting.GRAY)
            );
        }
    }

    public static void addToolTooltip(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltipComponents,
            TooltipFlag tooltipFlag
    ) {
        if (Screen.hasShiftDown()) {
            TinkersUtils.addToolStatsTooltip(
                    stack,
                    tooltipComponents
            );
        }
        else if (Screen.hasControlDown()) {
            if (stack.getItem() instanceof DynamicTool tool)
            {
                TinkersUtils.addToolPartTooltips(
                        tool.getDefinition(),
                        stack,
                        tooltipComponents
                );
            }
        }
        else {
            // TODO: Display traits, add a blank component, and only then show the following text.

            Component shift = Component.translatable(
                    "tinkerslegacy.tooltip.shift"
            ).withStyle(style -> style
                    .withColor(ChatFormatting.YELLOW)
                    .withItalic(true)
            );

            Component ctrl = Component.translatable(
                    "tinkerslegacy.tooltip.ctrl"
            ).withStyle(style -> style
                    .withColor(0x55FFFF)
            );

            tooltipComponents.add(
                    Component.translatable("tinkerslegacy.tooltip.hold_stats",
                            shift
                    ).withStyle(ChatFormatting.GRAY)
            );

            tooltipComponents.add(
                    Component.translatable("tinkerslegacy.tooltip.hold_more",
                            ctrl
                    ).withStyle(ChatFormatting.GRAY)
            );
        }
    }
}