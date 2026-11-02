package com.nurby.tinkerslegacy.client.screen;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.menu.CraftingStationMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public class CraftingStationScreen extends AbstractContainerScreen<CraftingStationMenu> {

        // Textures
        private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace(
                        "textures/gui/container/crafting_table.png");
        private static final ResourceLocation PANEL = ResourceLocation.fromNamespaceAndPath(
                        TinkersLegacy.MODID, "textures/gui/station/generic.png");

        private StationTabs tabs;
        private int visibleRows;
        private int firstRow;
        private boolean draggingScroll;

        public CraftingStationScreen(CraftingStationMenu menu, Inventory inventory, Component title) {
                super(menu, inventory, title);
                imageWidth = 176;
                imageHeight = 166;
                titleLabelX = 29;
        }

        // Controls
        @Override
        protected void init() {
                super.init();
                visibleRows = Math.min(6, Math.max(1, Math.min(menu.sideRows(), (height - topPos - 24) / 18)));
                menu.setSideRows(visibleRows);
                setScroll(firstRow);
                draggingScroll = false;
                tabs = new StationTabs(menu, menu.position(), leftPos, topPos, this::addRenderableWidget);
        }

        private void setScroll(int row) {
                firstRow = Math.clamp(row, 0, maximumScroll());
                menu.setSideScroll(firstRow);
        }

        private void scrollTo(double mouseY) {
                double progress = (mouseY - topPos - 18 - 7.5) / Math.max(1, visibleRows * 18 - 17);
                setScroll((int) Math.round(progress * maximumScroll()));
        }

        // Getters
        public List<Rect2i> getGuiExtraAreas() {
                List<Rect2i> areas = new ArrayList<>();
                if (menu.sideSlotCount() > 0) {
                        areas.add(sideBounds());
                }
                if (tabs != null) {
                        areas.addAll(tabs.areas());
                }
                return List.copyOf(areas);
        }

        private Rect2i sideBounds() {
                int panelWidth = maximumScroll() > 0 ? 134 : 122;
                return new Rect2i(leftPos - panelWidth, topPos, panelWidth, 24 + visibleRows * 18);
        }

        private int maximumScroll() {
                return Math.max(0, menu.sideRows() - visibleRows);
        }

        private boolean overSide(double mouseX, double mouseY) {
                Rect2i bounds = sideBounds();
                return menu.sideSlotCount() > 0 && mouseX >= bounds.getX() && mouseX < bounds.getX() + bounds.getWidth()
                                && mouseY >= bounds.getY() && mouseY < bounds.getY() + bounds.getHeight();
        }

        private boolean overScroll(double mouseX, double mouseY) {
                return menu.sideSlotCount() > 0 && maximumScroll() > 0 && mouseX >= leftPos - 19 && mouseX < leftPos - 7
                                && mouseY >= topPos + 17 && mouseY < topPos + 17 + visibleRows * 18;
        }

        // Rendering
        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                super.render(graphics, mouseX, mouseY, partialTick);
                renderTooltip(graphics, mouseX, mouseY);
                if (overSide(mouseX, mouseY) && mouseY < topPos + 17 && font.width(menu.sideTitle()) > sideBounds().getWidth() - 14) {
                        graphics.renderTooltip(font, menu.sideTitle(), mouseX, mouseY);
                }
        }

        @Override
        protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
                graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight);
                if (menu.sideSlotCount() > 0) {
                        renderSide(graphics, mouseX, mouseY);
                }
        }

        private void renderSide(GuiGraphics graphics, int mouseX, int mouseY) {
                Rect2i bounds = sideBounds();
                int x = bounds.getX();
                int y = bounds.getY();
                int panelWidth = bounds.getWidth();
                int panelHeight = bounds.getHeight();
                graphics.blit(PANEL, x, y, 0, 0, 7, 7, 64, 64);
                graphics.blit(PANEL, x + panelWidth - 7, y, 57, 0, 7, 7, 64, 64);
                graphics.blit(PANEL, x, y + panelHeight - 7, 0, 57, 7, 7, 64, 64);
                graphics.blit(PANEL, x + panelWidth - 7, y + panelHeight - 7, 57, 57, 7, 7, 64, 64);
                graphics.blit(PANEL, x + 7, y, panelWidth - 14, 7, 7, 0, 50, 7, 64, 64);
                graphics.blit(PANEL, x + 7, y + panelHeight - 7, panelWidth - 14, 7, 7, 57, 50, 7, 64, 64);
                graphics.blit(PANEL, x, y + 7, 7, panelHeight - 14, 0, 7, 7, 50, 64, 64);
                graphics.blit(PANEL, x + panelWidth - 7, y + 7, 7, panelHeight - 14, 57, 7, 7, 50, 64, 64);
                graphics.blit(PANEL, x + 7, y + 7, panelWidth - 14, 10, 25, 7, 18, 10, 64, 64);
                for (int row = 0; row < visibleRows; row++) {
                        for (int column = 0; column < 6; column++) {
                                int index = (row + firstRow) * 6 + column;
                                graphics.blit(PANEL, x + 7 + column * 18, y + 17 + row * 18,
                                                index < menu.sideSlotCount() ? 7 : 25, 7, 18, 18, 64, 64);
                        }
                }
                var title = font.substrByWidth(menu.sideTitle(), panelWidth - 14);
                graphics.drawString(font, Language.getInstance().getVisualOrder(title), x + 7, y + 6, 0x404040, false);
                if (maximumScroll() > 0) {
                        int scrollX = leftPos - 19;
                        int scrollY = topPos + 17;
                        int scrollHeight = visibleRows * 18;
                        graphics.blit(PANEL, scrollX, scrollY, 43, 7, 12, 1, 64, 64);
                        graphics.blit(PANEL, scrollX, scrollY + 1, 12, scrollHeight - 2, 43, 8, 12, 30, 64, 64);
                        graphics.blit(PANEL, scrollX, scrollY + scrollHeight - 1, 43, 38, 12, 1, 64, 64);
                        int thumbY = scrollY + 1 + (scrollHeight - 17) * firstRow / maximumScroll();
                        int thumbU = draggingScroll ? 17 : overScroll(mouseX, mouseY) ? 27 : 7;
                        graphics.blit(PANEL, scrollX + 1, thumbY, thumbU, 25, 10, 15, 64, 64);
                }
        }

        // Input
        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
                if (overSide(mouseX, mouseY)) {
                        setScroll(firstRow - (int) Math.signum(vertical));
                        return true;
                }
                return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button == 0 && overScroll(mouseX, mouseY)) {
                        draggingScroll = true;
                        scrollTo(mouseY);
                        return true;
                }
                return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
                if (button == 0 && draggingScroll) {
                        scrollTo(mouseY);
                        return true;
                }
                return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
                if (button == 0 && draggingScroll) {
                        draggingScroll = false;
                        return true;
                }
                return super.mouseReleased(mouseX, mouseY, button);
        }

        @Override
        protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int button) {
                if (overSide(mouseX, mouseY) || tabs != null && tabs.contains(mouseX, mouseY)) {
                        return false;
                }
                return super.hasClickedOutside(mouseX, mouseY, left, top, button);
        }
}
