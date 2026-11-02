package com.nurby.tinkerslegacy.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.api.ClientHandler;
import com.nurby.tinkerslegacy.api.station.MaterialInputs;
import com.nurby.tinkerslegacy.api.station.StationOperations;
import com.nurby.tinkerslegacy.api.station.StationRecipes;
import com.nurby.tinkerslegacy.api.tool.*;
import com.nurby.tinkerslegacy.content.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.menu.StationMenu;
import com.nurby.tinkerslegacy.network.StationSelectionPayload;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLItems;
import com.nurby.tinkerslegacy.registry.TLMaterials;
import com.nurby.tinkerslegacy.registry.TLRegistries;
import com.nurby.tinkerslegacy.util.TinkersUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

public class StationScreen extends AbstractContainerScreen<StationMenu> {

        // Textures
        private static final ResourceLocation TOOL_STATION = texture("toolstation");
        private static final ResourceLocation PART_BUILDER = texture("partbuilder");
        private static final ResourceLocation STENCIL_TABLE = texture("stenciltable");
        private static final ResourceLocation ICONS = texture("icons");
        private static final ResourceLocation PANEL = texture("panel");

        private final Map<ResourceLocation, ItemStack> icons = new HashMap<>();
        private final List<ShapeButton> choices = new ArrayList<>();
        private final List<InfoPanel> panels = new ArrayList<>();
        private EditBox name;
        private int selectionRevision;
        private boolean updatingName;
        private int columns;
        private int buttonRows;
        private int firstRow;
        private int gridX;
        private int panelWidth;
        private StationTabs tabs;

        public StationScreen(StationMenu menu, Inventory inventory, Component title) {
                super(menu, inventory, title);
                imageWidth = 176;
                imageHeight = menu.inventoryY() + 82;
                inventoryLabelX = 8;
                inventoryLabelY = menu.inventoryY() - 10;
        }

        // Controls
        @Override
        protected void init() {
                super.init();
                choices.clear();
                panels.clear();
                boolean tools = menu.isToolStation();
                boolean stencils = menu.definition().supports(StationOperations.CUT_PATTERN);
                // Keep the inventory centered exactly like vanilla. Side panels use the remaining margins.
                panelWidth = tools || !stencils ? Math.min(126, Math.max(48, width - leftPos - imageWidth - 4)) : 0;
                columns = tools || stencils ? Math.clamp(leftPos / 22, 1, tools ? 5 : 4) : 0;
                int leftWidth = columns == 0 ? 0 : columns * 22;
                gridX = leftPos - leftWidth;
                buttonRows = Math.max(1, (Math.min(imageHeight, height - topPos - 6) - 5) / 22);

                if (tools) {
                        addChoice(StationOperations.MAINTAIN, null, text("repair_and_modify"));
                        for (ToolDefinition tool : TLRegistries.TOOLS) {
                                if (StationRecipes.supports(menu.definition(), tool, StationOperations.ASSEMBLE)) {
                                        addChoice(StationOperations.ASSEMBLE, tool.id(), choiceName(tool.id()));
                                }
                        }
                        panels.add(new InfoPanel(leftPos + imageWidth + 2, topPos + 11, panelWidth, 83, true));
                        panels.add(new InfoPanel(leftPos + imageWidth + 2, topPos + 98, panelWidth, 83, true));
                } else if (stencils) {
                        for (ResourceLocation part : menu.selections()) {
                                addChoice(StationOperations.CUT_PATTERN, part, choiceName(part));
                        }
                } else {
                        panels.add(new InfoPanel(leftPos + imageWidth + 2, topPos, panelWidth, imageHeight, false));
                }

                name = new EditBox(font, leftPos + 70, topPos + 7, 92, 12, text("name"));
                name.setBordered(false);
                name.setMaxLength(50);
                name.setTextColor(0xFFFFFF);
                name.setHint(text("name").withStyle(ChatFormatting.GRAY));
                name.setValue(menu.name());
                name.setResponder(value -> {
                        if (!updatingName) {
                                sendSelection(menu.operation(), menu.selection(), value);
                        }
                });
                name.visible = tools;
                addRenderableWidget(name);
                tabs = new StationTabs(menu, menu.position(), leftPos, topPos, this::addRenderableWidget);
                updateGrid();
        }

        private void addChoice(ResourceLocation operation, ResourceLocation selection, Component label) {
                ShapeButton button = new ShapeButton(label, operation, selection);
                choices.add(button);
                addRenderableWidget(button);
        }

        private void updateGrid() {
                if (columns == 0) {
                        return;
                }
                int maximum = Math.max(0, (choices.size() + columns - 1) / columns - buttonRows);
                firstRow = Math.clamp(firstRow, 0, maximum);
                for (int index = 0; index < choices.size(); index++) {
                        ShapeButton button = choices.get(index);
                        int row = index / columns - firstRow;
                        button.visible = row >= 0 && row < buttonRows;
                        button.setPosition(gridX + index % columns * 22, topPos + (menu.isToolStation() ? 9 : 0) + row * 22);
                }
        }

        // Getters
        public List<Rect2i> getGuiExtraAreas() {
                List<Rect2i> areas = new ArrayList<>();
                if (columns > 0) {
                        areas.add(new Rect2i(gridX, topPos, columns * 22, imageHeight));
                }
                for (InfoPanel panel : panels) {
                        areas.add(new Rect2i(panel.x, panel.y - 4, panel.width, panel.height + 4));
                }
                if (tabs != null) {
                        areas.addAll(tabs.areas());
                }
                return List.copyOf(areas);
        }

        private void sendSelection(ResourceLocation operation, ResourceLocation selection, String value) {
                menu.select(operation, selection, value, menu.replacementIndex());
                PacketDistributor.sendToServer(new StationSelectionPayload(
                                menu.containerId, menu.operation(), menu.selection(), menu.name(), menu.replacementIndex(),
                                menu.nextSelectionRequest()));
                panels.forEach(panel -> panel.scroll = 0);
        }

        private boolean metal() {
                return menu.isToolStation() && menu.definition().tier() > 1;
        }

        // Rendering
        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                menu.updateLayout();
                boolean inputChanged = menu.updateToolName();
                boolean sharedSelectionChanged = selectionRevision != menu.selectionRevision();
                selectionRevision = menu.selectionRevision();
                if (!name.getValue().equals(menu.name()) && (inputChanged || sharedSelectionChanged || !name.isFocused())) {
                        updatingName = true;
                        name.setValue(menu.name());
                        updatingName = false;
                }
                updateInformation();
                super.render(graphics, mouseX, mouseY, partialTick);
                renderTooltip(graphics, mouseX, mouseY);
                if (hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.index < menu.resultSlot()) {
                        graphics.renderTooltip(font, inputHint(hoveredSlot.index), mouseX, mouseY);
                }
                for (InfoPanel panel : panels) {
                        panel.tooltip(graphics, mouseX, mouseY);
                }
        }

        @Override
        protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
                ResourceLocation background = menu.isToolStation() ? TOOL_STATION
                                : menu.definition().supports(StationOperations.BUILD_PART) ? PART_BUILDER : STENCIL_TABLE;
                int originalInventoryY = menu.isToolStation() ? 92 : 84;
                int extraHeight = menu.extraRows() * 18;
                graphics.blit(background, leftPos, topPos, 0, 0, imageWidth, originalInventoryY - 8);
                if (extraHeight > 0) {
                        graphics.blit(background, leftPos, topPos + originalInventoryY - 8, imageWidth, extraHeight,
                                        0, 68, imageWidth, 8, 256, 256);
                }
                graphics.blit(background, leftPos, topPos + originalInventoryY - 8 + extraHeight,
                                0, originalInventoryY - 8, imageWidth, 90);
                if (menu.isToolStation()) {
                        renderToolInputs(graphics);
                        if (name.isFocused()) {
                                graphics.blit(TOOL_STATION, leftPos + 68, topPos + 6, 0, 210, 102, 12);
                        }
                        renderBeam(graphics, gridX, columns * 22 - 4);
                        renderBeam(graphics, leftPos + imageWidth + 2, panelWidth);
                } else {
                        renderPartInputs(graphics);
                }
                for (int index = 6; index < menu.resultSlot(); index++) {
                        Slot slot = menu.getSlot(index);
                        if (slot.isActive()) {
                                graphics.blit(TOOL_STATION, leftPos + slot.x - 1, topPos + slot.y - 1, 176, 0, 18, 18);
                        }
                }
                for (InfoPanel panel : panels) {
                        panel.render(graphics);
                }
        }

        private void renderBeam(GuiGraphics graphics, int x, int width) {
                int v = metal() ? 187 : 180;
                graphics.blit(TOOL_STATION, x - 2, topPos, 0, v, 2, 7);
                graphics.blit(TOOL_STATION, x, topPos, width, 7, 2, v, 129, 7, 256, 256);
                graphics.blit(TOOL_STATION, x + width, topPos, 131, v, 2, 7);
        }

        private void renderToolInputs(GuiGraphics graphics) {
                boolean assembly = menu.operation().equals(StationOperations.ASSEMBLE);
                graphics.pose().pushPose();
                graphics.pose().translate(leftPos + 10, topPos + 22, 0);
                graphics.pose().scale(3.7F, 3.7F, 1);
                if (assembly) {
                        renderChoice(graphics, menu.selection(), 0, 0);
                } else {
                        graphics.blit(ICONS, 0, 0, 54, 0, 18, 18);
                }
                graphics.pose().popPose();
                RenderSystem.enableBlend();
                graphics.setColor(1, 1, 1, 0.82F);
                graphics.blit(TOOL_STATION, leftPos + 7, topPos + 18, 176, 18, 80, 64);
                graphics.setColor(1, 1, 1, 1);
                ToolDefinition tool = assembly && menu.selection() != null ? TLRegistries.TOOLS.get(menu.selection()) : null;
                for (int index = 0; index < Math.min(6, menu.resultSlot()); index++) {
                        Slot slot = menu.getSlot(index);
                        if (!slot.isActive()) {
                                continue;
                        }
                        graphics.fill(leftPos + slot.x, topPos + slot.y,
                                        leftPos + slot.x + 16, topPos + slot.y + 16, 0xFF8B8B8B);
                        RenderSystem.enableBlend();
                        graphics.blit(TOOL_STATION, leftPos + slot.x - 1, topPos + slot.y - 1, 194, 0, 18, 18);
                        if (!slot.hasItem()) {
                                if (tool != null && index < tool.partRequirements().size()) {
                                        ResourceLocation part = tool.partRequirements().get(index).part();
                                        if (!StationPreviews.part(graphics, part, leftPos + slot.x, topPos + slot.y,
                                                        StationPreviews.color(tool, index))) {
                                                graphics.renderItem(icon(part), leftPos + slot.x, topPos + slot.y);
                                        }
                                } else if (!assembly) {
                                        ghost(graphics, slot, index * 18, 234);
                                }
                        }
                }
        }

        private void renderPartInputs(GuiGraphics graphics) {
                if (menu.definition().supports(StationOperations.BUILD_PART)) {
                        ghost(graphics, menu.getSlot(0), 0, 216);
                        ghost(graphics, menu.getSlot(1), 54, 234);
                        ghost(graphics, menu.getSlot(2), 36, 216);
                        ghost(graphics, menu.getSlot(3), 18, 216);
                } else {
                        ghost(graphics, menu.getSlot(0), 0, 216);
                }
        }

        private void ghost(GuiGraphics graphics, Slot slot, int u, int v) {
                if (!slot.hasItem()) {
                        graphics.blit(ICONS, leftPos + slot.x - 1, topPos + slot.y - 1, u, v, 18, 18);
                }
        }

        @Override
        protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
                graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
                if (!menu.isToolStation()) {
                        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
                        Component message = menu.definition().supports(StationOperations.CUT_PATTERN)
                                        ? menu.selection() == null ? text("choose_shape") : choiceName(menu.selection())
                                        : materialAmount();
                        if (message != null) {
                                var label = font.substrByWidth(message, 160);
                                graphics.drawString(font, Language.getInstance().getVisualOrder(label),
                                                (imageWidth - font.width(label)) / 2, 63, 0x777777, false);
                        }
                }
        }

        // Information
        private void updateInformation() {
                if (panels.isEmpty()) {
                        return;
                }
                ItemStack result = menu.getSlot(menu.resultSlot()).getItem();
                ItemStack shown = result.isEmpty() ? menu.getSlot(0).getItem() : result;
                if (menu.isToolStation()) {
                        List<Component> stats = new ArrayList<>();
                        List<Component> details = new ArrayList<>();
                        Component caption = operationName();
                        Component detailTitle = text("modifiers");
                        if (shown.getItem() instanceof DynamicTool tool) {
                                caption = shown.getHoverName();
                                var values = shown.getOrDefault(TLDataComponents.MODIFIED_STATS, Map.<String, Number>of());
                                if (!ClientHandler.addRangedStats(shown, stats, true)) {
                                        stats.add(stat("durability", (ToolState.broken(shown) ? 0 : shown.getMaxDamage() - shown.getDamageValue())
                                                        + " / " + shown.getMaxDamage()));
                                        for (String key : List.of("attack_damage", "attack_speed", "mining_speed", "harvest_level")) {
                                                if (values.containsKey(key) && (tool.getDefinition().isHarvestTool() || !key.startsWith("mining") && !key.startsWith("harvest"))) {
                                                        if (key.equals("harvest_level")) {
                                                                stats.add(Component.translatable("tinkerslegacy.stat.harvest_level").append(": ")
                                                                                .append(Component.translatable("tinkerslegacy.mininglevel." + values.get(key).intValue())));
                                                        } else {
                                                                stats.add(stat(key, number(key.equals("attack_damage")
                                                                                ? TinkersUtils.getActualDamage(shown) : values.get(key).doubleValue())));
                                                        }
                                                }
                                        }
                                }
                                stats.add(Component.translatable("tinkerslegacy.tool.free_modifiers", ToolState.freeModifiers(shown)));
                                ToolProgress progress = shown.getOrDefault(TLDataComponents.PROGRESS, ToolProgress.EMPTY);
                                stats.add(Component.translatable("tinkerslegacy.tool.level", ToolLeveling.levelName(progress)));
                                if (shown.has(TLDataComponents.NEXT_LEVEL_XP)) {
                                        stats.add(Component.translatable("tinkerslegacy.tool.xp", progress.xp(), shown.get(TLDataComponents.NEXT_LEVEL_XP)));
                                }
                                details.addAll(ClientHandler.traitNames(shown, com.nurby.tinkerslegacy.api.tool.traits.ToolTraits.traits(shown)));
                                details.addAll(ToolEffectText.modifierNames(shown, true));
                                if (details.isEmpty()) {
                                        details.add(text("no_modifiers"));
                                }
                        } else if (menu.operation().equals(StationOperations.ASSEMBLE) && menu.selection() != null) {
                                ToolDefinition tool = TLRegistries.TOOLS.get(menu.selection());
                                caption = choiceName(menu.selection());
                                stats.add(text("hint.assemble"));
                                detailTitle = text("components");
                                if (tool != null) {
                                        for (int index = 0; index < tool.partRequirements().size(); index++) {
                                                var requirement = tool.partRequirements().get(index);
                                                boolean valid = index < menu.resultSlot()
                                                                && ToolAssembly.matches(menu.getSlot(index).getItem(), requirement);
                                                details.add(Component.literal("• ").append(choiceName(requirement.part()))
                                                                .withStyle(valid ? ChatFormatting.GREEN : ChatFormatting.RED));
                                        }
                                }
                        } else {
                                stats.add(text("hint." + menu.operation().getPath()));
                                details.add(text("repair_help"));
                                details.add(text("replacement_help"));
                                details.add(text("modification_help"));
                                details.add(text("rename_help"));
                        }
                        if (hasInputs() && result.isEmpty() && !menu.error().getString().isBlank()) {
                                details.add(menu.error().copy().withStyle(ChatFormatting.RED));
                        }
                        panels.get(0).set(caption, stats);
                        panels.get(1).set(detailTitle, details);
                } else {
                        List<Component> lines = new ArrayList<>();
                        var match = MaterialInputs.match(menu.getSlot(1).getItem());
                        if (match == null) {
                                match = MaterialInputs.match(menu.getSlot(2).getItem());
                        }
                        Component caption = title;
                        if (match != null && TLRegistries.MATERIALS.containsKey(match.material())) {
                                caption = materialName(match.material());
                                var material = TLRegistries.MATERIALS.get(match.material());
                                material.stats().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
                                        lines.add(Component.translatable(entry.getKey().getNamespace() + ".material_stat_type." + entry.getKey().getPath())
                                                        .withStyle(ChatFormatting.UNDERLINE));
                                        entry.getValue().statMap().values().entrySet().stream().sorted(Map.Entry.comparingByKey())
                                                        .forEach(stat -> lines.add(stat(stat.getKey(), number(stat.getValue().doubleValue()))));
                                        lines.add(Component.empty());
                                });
                        } else {
                                lines.add(text("hint.build_part"));
                        }
                        if (hasInputs() && result.isEmpty() && !menu.error().getString().isBlank()) {
                                lines.add(menu.error().copy().withStyle(ChatFormatting.RED));
                        }
                        panels.getFirst().set(caption, lines);
                }
        }

        private Component materialAmount() {
                ResourceLocation material = null;
                long total = 0;
                for (int index = 1; index <= 2; index++) {
                        ItemStack input = menu.getSlot(index).getItem();
                        var match = MaterialInputs.match(input);
                        if (match == null) {
                                continue;
                        }
                        if (material != null && !material.equals(match.material())) {
                                return text("mixed_materials");
                        }
                        material = match.material();
                        total += (long) match.value() * input.getCount();
                }
                if (material == null) {
                        return null;
                }
                var amount = Component.literal(number((double) total / MaterialInputs.UNIT));
                ResourceLocation part = menu.getSlot(0).getItem().get(TLDataComponents.PART);
                if (part != null && total < StationRecipes.partCost(part)) {
                        amount.withStyle(ChatFormatting.DARK_RED);
                }
                return Component.translatable("screen.tinkerslegacy.station.material_value", amount, materialName(material));
        }

        private boolean hasInputs() {
                for (int index = 0; index < menu.resultSlot(); index++) {
                        if (!menu.isRemainder(index) && menu.getSlot(index).hasItem()) {
                                return true;
                        }
                }
                return false;
        }

        private Component inputHint(int index) {
                if (menu.operation().equals(StationOperations.ASSEMBLE) && menu.selection() != null) {
                        var tool = TLRegistries.TOOLS.get(menu.selection());
                        return tool != null && index < tool.partRequirements().size()
                                        ? choiceName(tool.partRequirements().get(index).part()) : text("unused");
                }
                if (menu.operation().equals(StationOperations.CUT_PATTERN)) {
                        return text("pattern");
                }
                if (menu.operation().equals(StationOperations.BUILD_PART)) {
                        return text(index == 0 ? "stencil" : menu.isRemainder(index) ? "remainder" : "material");
                }
                return text(index == 0 ? "tool" : "maintenance_material");
        }

        private void renderChoice(GuiGraphics graphics, ResourceLocation id, int x, int y) {
                if (id == null) {
                        return;
                }
                ToolDefinition tool = TLRegistries.TOOLS.get(id);
                boolean rendered = tool != null ? StationPreviews.tool(graphics, tool, x, y)
                                : StationPreviews.part(graphics, id, x, y, StationPreviews.color(id));
                if (!rendered) {
                        graphics.renderItem(icon(id), x, y);
                }
        }

        private ItemStack icon(ResourceLocation id) {
                if (id == null) {
                        return ItemStack.EMPTY;
                }
                return icons.computeIfAbsent(id, key -> {
                        if (key.equals(TLItems.Parts.MATERIAL_SHARD.getId())) {
                                return TLItems.Parts.MATERIAL_SHARD.get().withMaterial(TLMaterials.IRON.getId(), 1);
                        }
                        if (TLRegistries.PARTS.containsKey(key)) {
                                var item = TLItems.Parts.get(key);
                                if (item == null) {
                                        return ItemStack.EMPTY;
                                }
                                ItemStack stack = new ItemStack(item.get());
                                stack.set(TLDataComponents.MATERIALS, TLRegistries.PARTS.get(key).statTypes().stream().map(stat -> TLMaterials.IRON.getId()).toList());
                                return stack;
                        }
                        ToolDefinition tool = TLRegistries.TOOLS.get(key);
                        return tool == null ? ItemStack.EMPTY : ToolAssembly.create(tool, tool.defaultMaterials(TLMaterials.IRON.get()));
                });
        }

        private Component operationName() {
                return Component.translatable("station." + menu.operation().getNamespace()
                                + ".operation." + menu.operation().getPath());
        }

        private static Component choiceName(ResourceLocation id) {
                return Component.translatable((TLRegistries.PARTS.containsKey(id) || id.equals(TLItems.Parts.MATERIAL_SHARD.getId()) ? "part." : "tool.")
                                + id.getNamespace() + "." + id.getPath());
        }

        private static Component materialName(ResourceLocation id) {
                return Component.translatable(id.getNamespace() + ".material." + id.getPath());
        }

        private static Component stat(String key, String value) {
                return Component.translatable("tinkerslegacy.stat." + key).append(": " + value);
        }

        private static String number(double value) {
                return value == Math.rint(value) ? String.format(Locale.ROOT, "%.0f", value) : String.format(Locale.ROOT, "%.2f", value);
        }

        private static net.minecraft.network.chat.MutableComponent text(String key) {
                return Component.translatable("screen.tinkerslegacy.station." + key);
        }

        private static ResourceLocation texture(String path) {
                return ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, "textures/gui/station/" + path + ".png");
        }

        // Input
        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
                for (InfoPanel panel : panels) {
                        if (panel.contains(mouseX, mouseY)) {
                                panel.scroll = Math.clamp(panel.scroll - (int) Math.signum(vertical) * 2, 0, panel.maximumScroll());
                                return true;
                        }
                }
                if (columns > 0 && mouseX >= gridX && mouseX < leftPos && mouseY >= topPos && mouseY < topPos + imageHeight) {
                        firstRow -= (int) Math.signum(vertical);
                        updateGrid();
                        return true;
                }
                return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
        }

        @Override
        protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int button) {
                if (columns > 0 && mouseX >= gridX && mouseX < leftPos && mouseY >= topPos && mouseY < topPos + imageHeight) {
                        return false;
                }
                for (InfoPanel panel : panels) {
                        if (panel.contains(mouseX, mouseY)) {
                                return false;
                        }
                }
                if (tabs != null && tabs.contains(mouseX, mouseY)) {
                        return false;
                }
                return super.hasClickedOutside(mouseX, mouseY, left, top, button);
        }

        @Override
        public boolean keyPressed(int key, int scanCode, int modifiers) {
                if (name.visible && name.isFocused() && key != 256) {
                        if (name.keyPressed(key, scanCode, modifiers) || name.canConsumeInput()) {
                                return true;
                        }
                }
                return super.keyPressed(key, scanCode, modifiers);
        }

        // Widgets
        private final class ShapeButton extends Button {
                private final ResourceLocation operation;
                private final ResourceLocation selection;

                private ShapeButton(Component label, ResourceLocation operation, ResourceLocation selection) {
                        super(0, 0, 18, 18, label, button -> sendSelection(operation, selection, name.getValue()), DEFAULT_NARRATION);
                        this.operation = operation;
                        this.selection = selection;
                        Component tooltip = selection == null ? label.copy().append("\n").append(text("maintenance_modes")) : label;
                        if (operation.equals(StationOperations.CUT_PATTERN)) {
                                tooltip = label.copy().append("\n").append(Component.translatable(
                                                "screen.tinkerslegacy.station.part_cost",
                                                number((double) StationRecipes.partCost(selection) / MaterialInputs.UNIT)));
                        }
                        setTooltip(Tooltip.create(tooltip));
                }

                @Override
                public boolean mouseClicked(double mouseX, double mouseY, int button) {
                        if (selection == null && button == 1 && active && visible && isMouseOver(mouseX, mouseY)) {
                                List<ResourceLocation> modes = List.of(StationOperations.MAINTAIN,
                                                StationOperations.REPAIR, StationOperations.MODIFY).stream()
                                                .filter(menu.definition()::supports).toList();
                                if (!modes.isEmpty()) {
                                        int index = modes.indexOf(menu.operation());
                                        sendSelection(modes.get((index + 1) % modes.size()), null, name.getValue());
                                        setTooltip(Tooltip.create(operationName().copy().append("\n").append(text("maintenance_modes"))));
                                }
                                return true;
                        }
                        return super.mouseClicked(mouseX, mouseY, button);
                }

                @Override
                protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                        boolean selected = selection == null ? !menu.operation().equals(StationOperations.ASSEMBLE)
                                        : operation.equals(menu.operation()) && Objects.equals(selection, menu.selection());
                        int u = selected ? 144 : isHoveredOrFocused() ? 216 : 180;
                        int v = menu.isToolStation() ? metal() ? 198 : 180 : 234;
                        RenderSystem.enableBlend();
                        graphics.blit(ICONS, getX(), getY(), u, v, 18, 18);
                        if (selection == null) {
                                graphics.blit(ICONS, getX(), getY(), 54, 0, 18, 18);
                        } else if (!operation.equals(StationOperations.CUT_PATTERN)
                                        || !StationPreviews.stencil(graphics, selection, getX() + 1, getY() + 1)) {
                                renderChoice(graphics, selection, getX() + 1, getY() + 1);
                        }
                        if (menu.isToolStation()) {
                                int decoration = metal() ? 36 : 18;
                                graphics.blit(TOOL_STATION, getX(), getY() - 2, decoration, 176, 18, 2);
                                int nextRow = choices.indexOf(this) + columns;
                                if (nextRow < choices.size() && choices.get(nextRow).visible) {
                                        graphics.blit(TOOL_STATION, getX(), getY() + 18, decoration, 174, 18, 2);
                                }
                        }
                }
        }

        private final class InfoPanel {
                private final int x;
                private final int y;
                private final int width;
                private final int height;
                private final boolean wood;
                private Component caption = Component.empty();
                private List<FormattedCharSequence> lines = List.of();
                private int scroll;

                private InfoPanel(int x, int y, int width, int height, boolean wood) {
                        this.x = x;
                        this.y = y;
                        this.width = width;
                        this.height = height;
                        this.wood = wood;
                }

                private void set(Component caption, List<Component> text) {
                        this.caption = caption;
                        lines = text.stream().flatMap(line -> font.split(line, width - 16).stream()).toList();
                        scroll = Math.min(scroll, maximumScroll());
                }

                private int maximumScroll() {
                        return Math.max(0, lines.size() - (height - 25) / 10);
                }

                private boolean contains(double mouseX, double mouseY) {
                        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
                }

                private void render(GuiGraphics graphics) {
                        if (wood) {
                                int decoration = metal() ? 36 : 18;
                                graphics.blit(TOOL_STATION, x + 5, y - 4, decoration, 174, 5, 4);
                                graphics.blit(TOOL_STATION, x + width - 14, y - 4, decoration + 9, 174, 9, 4);
                        }
                        int u = wood ? 126 : 0;
                        int v = wood && metal() ? 83 : 0;
                        graphics.blit(PANEL, x, y, u, v, 4, 4);
                        graphics.blit(PANEL, x + width - 4, y, u + 122, v, 4, 4);
                        graphics.blit(PANEL, x, y + height - 4, u, v + 79, 4, 4);
                        graphics.blit(PANEL, x + width - 4, y + height - 4, u + 122, v + 79, 4, 4);
                        graphics.blit(PANEL, x + 4, y, width - 8, 4, u + 4, v, 118, 4, 256, 256);
                        graphics.blit(PANEL, x + 4, y + height - 4, width - 8, 4, u + 4, v + 79, 118, 4, 256, 256);
                        graphics.blit(PANEL, x, y + 4, 4, height - 8, u, v + 4, 4, 75, 256, 256);
                        graphics.blit(PANEL, x + width - 4, y + 4, 4, height - 8, u + 122, v + 4, 4, 75, 256, 256);
                        graphics.blit(PANEL, x + 4, y + 4, width - 8, height - 8, u + 4, 4, 118, 75, 256, 256);
                        var heading = font.substrByWidth(caption.copy().withStyle(ChatFormatting.UNDERLINE), width - 12);
                        graphics.drawString(font, Language.getInstance().getVisualOrder(heading),
                                        x + (width - font.width(heading)) / 2, y + 6, 0xF0F0F0, true);
                        graphics.enableScissor(x + 4, y + 19, x + width - 5, y + height - 5);
                        for (int index = scroll; index < lines.size(); index++) {
                                int lineY = y + 20 + (index - scroll) * 10;
                                if (lineY + 9 > y + height - 5) {
                                        break;
                                }
                                graphics.drawString(font, lines.get(index), x + 6, lineY, 0xF0F0F0, true);
                        }
                        graphics.disableScissor();
                        if (maximumScroll() > 0) {
                                int thumbY = y + 20 + (height - 31) * scroll / maximumScroll();
                                graphics.blit(PANEL, x + width - 7, thumbY, wood ? metal() ? 12 : 6 : 0, 83, 3, 5);
                        }
                }

                private void tooltip(GuiGraphics graphics, int mouseX, int mouseY) {
                        if (contains(mouseX, mouseY) && mouseY < y + 18 && font.width(caption) > width - 12) {
                                graphics.renderTooltip(font, caption, mouseX, mouseY);
                        }
                }
        }
}
