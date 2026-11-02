package com.nurby.tinkerslegacy.library;


import com.nurby.tinkerslegacy.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.library.tool.ToolState;
import com.nurby.tinkerslegacy.library.tool.ToolTooltip;
import com.nurby.tinkerslegacy.library.tool.ToolRangedActions;
import com.nurby.tinkerslegacy.tools.RangedToolDefinition;
import com.nurby.tinkerslegacy.tools.ProjectileToolDefinition;
import com.nurby.tinkerslegacy.library.tool.ToolEffectText;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class ClientHandler {

        public static void addPartTooltip(
                        ItemStack stack,
                        Item.TooltipContext context,
                        List<Component> tooltipComponents,
                        TooltipFlag tooltipFlag
        ) {
                ToolTooltip.addPart(stack, tooltipComponents, Screen.hasShiftDown());
        }

        public static void addToolTooltip(
                        ItemStack stack,
                        Item.TooltipContext context,
                        List<Component> tooltipComponents,
                        TooltipFlag tooltipFlag
        ) {
                ToolTooltip.addTool(stack, tooltipComponents,
                                Screen.hasShiftDown(), Screen.hasControlDown(), net.minecraft.client.Minecraft.getInstance().player != null);
        }

        // Effect Names
        public static List<Component> traitNames(List<net.minecraft.resources.ResourceLocation> traits) {
                return ToolEffectText.traitNames(ItemStack.EMPTY, traits);
        }

        public static List<Component> traitNames(ItemStack stack, List<net.minecraft.resources.ResourceLocation> traits) {
                return ToolEffectText.traitNames(stack, traits);
        }

        public static net.minecraft.network.chat.MutableComponent traitName(net.minecraft.resources.ResourceLocation id) {
                return ToolEffectText.traitName(id);
        }

        public static net.minecraft.network.chat.MutableComponent modifierName(ItemStack stack, net.minecraft.resources.ResourceLocation id) {
                return ToolEffectText.modifierName(stack, id);
        }

        public static net.minecraft.network.chat.MutableComponent modifierName(net.minecraft.resources.ResourceLocation id) {
                return ToolEffectText.modifierName(id);
        }

        // Ranged Stats
        public static boolean addRangedStats(ItemStack stack, List<Component> lines, boolean includeAmmo) {
                if (!(stack.getItem() instanceof DynamicTool tool)) {
                        return false;
                }
                var values = stack.getOrDefault(TLDataComponents.MODIFIED_STATS, Map.<String, Number>of());
                if (tool.getDefinition() instanceof RangedToolDefinition launcher) {
                        int remaining = ToolState.broken(stack) ? 0 : Math.max(0, stack.getMaxDamage() - ToolState.damage(stack));
                        lines.add(stat("durability", remaining + " / " + stack.getMaxDamage()));
                        double speed = Math.max(0.001, values.getOrDefault("draw_speed", 1).doubleValue());
                        lines.add(Component.translatable("tinkerslegacy.tool.draw_time", number(launcher.drawTime() / (20 * speed))));
                        lines.add(Component.translatable("tinkerslegacy.tool.range", number(values.getOrDefault("range", 1).doubleValue())));
                        lines.add(stat("bonus_damage", number(values.getOrDefault("bonus_damage", 0).doubleValue())));
                        return true;
                }
                if (tool.getDefinition() instanceof ProjectileToolDefinition) {
                        if (includeAmmo) {
                                lines.add(Component.translatable("tinkerslegacy.tool.ammo",
                                                ToolRangedActions.ammo(stack), ToolRangedActions.maxAmmo(stack)));
                        }
                        double accuracy = Math.clamp(values.getOrDefault("accuracy", 1).doubleValue(), 0, 1);
                        lines.add(stat("accuracy", number(accuracy * 100) + "%"));
                        return true;
                }
                return false;
        }

        private static Component stat(String key, String value) {
                return Component.translatable("tinkerslegacy.stat." + key).append(": " + value);
        }

        private static String number(double value) {
                return value == Math.rint(value) ? String.format(Locale.ROOT, "%.0f", value) : String.format(Locale.ROOT, "%.2f", value);
        }
}
