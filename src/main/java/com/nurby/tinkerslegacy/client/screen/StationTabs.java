package com.nurby.tinkerslegacy.client.screen;

import com.nurby.tinkerslegacy.block.entity.StationBlockEntity;
import com.nurby.tinkerslegacy.library.station.StationConnections;
import com.nurby.tinkerslegacy.network.StationOpenPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.network.PacketDistributor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class StationTabs {
        private final Minecraft minecraft = Minecraft.getInstance();
        private final AbstractContainerMenu menu;
        private final BlockPos position;
        private final List<BlockPos> connected;
        private final Rect2i bounds;
        private int page;

        public StationTabs(
                        AbstractContainerMenu menu, BlockPos position, int left, int top,
                        Consumer<AbstractWidget> addWidget) {
                this.menu = menu;
                this.position = position;
                this.bounds = new Rect2i(left, top - 32, 176, 32);
                Map<ResourceLocation, BlockPos> stations = new LinkedHashMap<>();
                if (minecraft.level != null) {
                        for (BlockPos neighbor : StationConnections.find(minecraft.level, position)) {
                                if (minecraft.level.getBlockEntity(neighbor) instanceof StationBlockEntity station) {
                                        stations.putIfAbsent(station.definition().id(), neighbor);
                                        if (neighbor.equals(position)) {
                                                stations.put(station.definition().id(), neighbor);
                                        }
                                }
                        }
                }
                connected = stations.entrySet().stream().sorted(Map.Entry.comparingByKey())
                                .map(Map.Entry::getValue).toList();
                if (connected.size() <= 1) {
                        return;
                }
                page = Math.max(0, connected.indexOf(position)) / 5;
                for (int index = 0; index < 5; index++) {
                        addWidget.accept(new Tab(left + 4 + index * 28, top - 28, index));
                }
                if (connected.size() > 5) {
                        addWidget.accept(Button.builder(Component.literal(">"), button ->
                                        page = (page + 1) % ((connected.size() + 4) / 5))
                                        .bounds(left + 148, top - 22, 20, 18)
                                        .tooltip(Tooltip.create(Component.translatable("screen.tinkerslegacy.station.more_stations")))
                                        .build());
                }
        }

        // Getters
        public List<Rect2i> areas() {
                return connected.size() > 1 ? List.of(bounds) : List.of();
        }

        public boolean contains(double mouseX, double mouseY) {
                return connected.size() > 1 && mouseX >= bounds.getX() && mouseX < bounds.getX() + bounds.getWidth()
                                && mouseY >= bounds.getY() && mouseY < bounds.getY() + bounds.getHeight();
        }

        // Widgets
        private final class Tab extends Button {
                private final int tab;

                private Tab(int x, int y, int tab) {
                        super(x, y, 28, 28, Component.empty(), button -> {
                                int index = page * 5 + tab;
                                if (index < connected.size()) {
                                        PacketDistributor.sendToServer(new StationOpenPayload(menu.containerId, connected.get(index)));
                                }
                        }, DEFAULT_NARRATION);
                        this.tab = tab;
                        active = page * 5 + tab < connected.size();
                }

                @Override
                protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                        int index = page * 5 + tab;
                        active = index < connected.size();
                        if (!active || minecraft.level == null) {
                                return;
                        }
                        BlockPos neighbor = connected.get(index);
                        boolean selected = neighbor.equals(position);
                        var sprite = ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_"
                                        + (selected ? "selected_" : "unselected_") + (tab + 1));
                        graphics.blitSprite(sprite, getX(), getY() - (selected ? 4 : 0), 28, selected ? 32 : 28);
                        ItemStack stack = new ItemStack(minecraft.level.getBlockState(neighbor).getBlock());
                        graphics.renderItem(stack, getX() + 6, getY() + 7);
                        setMessage(stack.getHoverName());
                        setTooltip(Tooltip.create(stack.getHoverName()));
                }
        }
}
