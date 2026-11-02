package com.nurby.tinkerslegacy.client.screen;

import com.nurby.tinkerslegacy.client.model.PartModelOffsets;
import com.nurby.tinkerslegacy.library.tool.ToolDefinition;
import com.nurby.tinkerslegacy.registry.TLRegistries;
import com.nurby.tinkerslegacy.registry.TLItems;
import com.nurby.tinkerslegacy.registry.ToolStatTypes;
import com.nurby.tinkerslegacy.registry.ToolParts;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class StationPreviews {

        // Colors
        private static final int HANDLE = 0x684E1E;
        private static final int HEAD = 0xC1C1C1;
        private static final int EXTRA = 0x2376DD;

        private StationPreviews() {
        }

        // Rendering
        public static boolean tool(GuiGraphics graphics, ToolDefinition tool, int x, int y) {
                for (int layer : tool.renderLayers()) {
                        if (!exists(toolTexture(tool, layer))) {
                                return false;
                        }
                }
                for (int layer : tool.renderLayers()) {
                        draw(graphics, toolTexture(tool, layer), x, y, StationPalette.layer(tool, layer));
                }
                return true;
        }

        public static boolean part(GuiGraphics graphics, ResourceLocation part, int x, int y, int color) {
                if (part.equals(ToolParts.get(ToolParts.Type.BOLT_CORE).getId())) {
                        ResourceLocation shaft = ResourceLocation.fromNamespaceAndPath(part.getNamespace(),
                                        "textures/item/part_templates/bolt_shaft.png");
                        ResourceLocation tip = ResourceLocation.fromNamespaceAndPath(part.getNamespace(),
                                        "textures/item/part_templates/bolt_tip.png");
                        if (!exists(shaft) || !exists(tip)) {
                                return false;
                        }
                        draw(graphics, shaft, x, y, color);
                        draw(graphics, tip, x, y, color);
                        return true;
                }
                ResourceLocation texture = partTexture(part);
                if (!exists(texture)) {
                        return false;
                }
                var offset = PartModelOffsets.get(part);
                draw(graphics, texture, x + offset.x(), y + offset.y(), color);
                return true;
        }

        // Getters
        public static int color(ToolDefinition tool, int index) {
                return StationPalette.part(tool, index);
        }

        public static int color(ResourceLocation part) {
                var definition = TLRegistries.PARTS.get(part);
                if (definition != null && definition.statTypes().contains(ToolStatTypes.HANDLE.getId())) {
                        return HANDLE;
                }
                return definition != null && definition.statTypes().contains(ToolStatTypes.HEAD.getId()) ? HEAD : EXTRA;
        }

        private static ResourceLocation partTexture(ResourceLocation part) {
                String path = part.equals(TLItems.Parts.MATERIAL_SHARD.getId()) ? "shard" : part.getPath();
                return ResourceLocation.fromNamespaceAndPath(part.getNamespace(),
                                "textures/item/part_templates/" + path + ".png");
        }

        private static ResourceLocation toolTexture(ToolDefinition tool, int layer) {
                return ResourceLocation.fromNamespaceAndPath(tool.id().getNamespace(),
                                "textures/item/tool_templates/" + tool.id().getPath() + "/layer" + layer + ".png");
        }

        private static boolean exists(ResourceLocation texture) {
                return Minecraft.getInstance().getResourceManager().getResource(texture).isPresent();
        }

        private static void draw(GuiGraphics graphics, ResourceLocation texture, int x, int y, int color) {
                RenderSystem.enableBlend();
                graphics.setColor((color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F,
                                (color & 255) / 255.0F, 1);
                graphics.blit(texture, x, y, 0, 0, 16, 16, 16, 16);
                graphics.setColor(1, 1, 1, 1);
        }

        // Stencils
        public static boolean stencil(GuiGraphics graphics, ResourceLocation part, int x, int y) {
                ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(part.getNamespace(),
                                "textures/gui/pattern/" + part.getPath() + ".png");
                if (!exists(texture)) {
                        return false;
                }
                draw(graphics, texture, x, y, 0xFFFFFF);
                return true;
        }

}
