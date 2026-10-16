package com.nurby.tinkerslegacy.library;

import com.nurby.tinkerslegacy.item.dynamic.DynamicPart;
import com.nurby.tinkerslegacy.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.library.part.PartDefinition;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLRegistries;
import com.nurby.tinkerslegacy.util.TinkersUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
        List<ResourceLocation> materials =
                stack.get(TLDataComponents.MATERIALS);

        // TODO: Display traits, add a blank component, and only then show the following text.

        boolean first = true;
        if (Screen.hasShiftDown()) {
            if (materials != null && !materials.isEmpty()) {
                if (stack.getItem() instanceof DynamicPart partItem) {
                    PartDefinition partDef = partItem.definition();
                    for (int i = 0; i < materials.size(); i++) {
                        if (!first) {
                            tooltipComponents.add(Component.empty());
                        }
                        first = false;
                        ResourceLocation materialId = materials.get(i);
                        ResourceLocation statType = i < partDef.statTypes().size() 
                                ? partDef.statTypes().get(i) 
                                : partDef.statTypes().isEmpty() ? null : partDef.statTypes().get(0);
                        if (statType != null) {
                            List<Component> stats = TinkersUtils.getStatsForLayer(materialId, statType);
                            tooltipComponents.addAll(stats);
                        }
                    }
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