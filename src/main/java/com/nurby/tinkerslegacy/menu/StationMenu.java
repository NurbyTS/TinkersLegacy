package com.nurby.tinkerslegacy.menu;

import com.nurby.tinkerslegacy.TLConfig;
import com.nurby.tinkerslegacy.block.entity.StationBlockEntity;
import com.nurby.tinkerslegacy.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.item.dynamic.DynamicPart;
import com.nurby.tinkerslegacy.registry.TLItems;
import com.nurby.tinkerslegacy.library.tool.ToolAssembly;
import com.nurby.tinkerslegacy.library.tool.ToolDefinition;
import com.nurby.tinkerslegacy.library.material.layer.MaterialLayer;
import com.nurby.tinkerslegacy.library.tool.ToolModifiers;
import com.nurby.tinkerslegacy.library.tool.ToolPartRequirement;
import com.nurby.tinkerslegacy.library.station.MaterialInputs;
import com.nurby.tinkerslegacy.library.station.StationDefinition;
import com.nurby.tinkerslegacy.library.station.StationOperations;
import com.nurby.tinkerslegacy.library.station.StationLayout;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.library.station.StationRecipes;
import com.nurby.tinkerslegacy.library.station.StationResult;
import com.nurby.tinkerslegacy.library.tool.ToolState;
import com.nurby.tinkerslegacy.network.StationStatusPayload;
import com.nurby.tinkerslegacy.library.station.StationSelection;
import com.nurby.tinkerslegacy.registry.TLSounds;
import net.minecraft.sounds.SoundSource;
import com.nurby.tinkerslegacy.registry.TLRegistries;
import com.nurby.tinkerslegacy.registry.TLStationMenus;
import com.nurby.tinkerslegacy.registry.TLStations;
import com.nurby.tinkerslegacy.registry.ToolStatTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

public class StationMenu extends AbstractContainerMenu implements StationMenuAccess {
        private final StationDefinition definition;
        private final Container inputs;
        private final SimpleContainer preview = new SimpleContainer(1);
        private final Player owner;
        private final BlockPos position;
        private final int resultSlot;
        private final DataSlot materialPolicy;
        private boolean clientAllowsCastableMaterials;
        private List<ItemStack> previewInputs = List.of();
        private ResourceLocation operation;
        private ResourceLocation selection;
        private String name = "";
        private int replacementIndex;
        private Component error = Component.empty();
        private Component sentError;
        private StationSelection sentSelection;
        private int selectionRevision;
        private long selectionRequest;
        private long processedSelectionRequest;
        private long sentSelectionRequest = -1;
        private ItemStack namedInput = ItemStack.EMPTY;

        public StationMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
                this(id, inventory, buffer.readBlockPos(), TLStations.get(buffer.readResourceLocation()), null);
        }

        public StationMenu(int id, Inventory inventory, StationBlockEntity station) {
                this(id, inventory, station.getBlockPos(), station.definition(), station);
        }

        private StationMenu(
                        int id, Inventory inventory, BlockPos position, StationDefinition definition,
                        @Nullable StationBlockEntity station) {
                super(TLStationMenus.STATION.get(), id);
                this.definition = definition;
                this.position = position;
                this.owner = inventory.player;
                this.materialPolicy = addDataSlot(new DataSlot() {
                        @Override
                        public int get() {
                                return (owner.level().isClientSide ? clientAllowsCastableMaterials
                                                : MaterialInputs.allowsCastableMaterials()) ? 1 : 0;
                        }

                        @Override
                        public void set(int value) {
                                clientAllowsCastableMaterials = value == 1;
                        }
                });
                this.inputs = station == null ? new SimpleContainer(definition.inputSlots()) : station;
                this.operation = definition.operations().getFirst();
                List<ResourceLocation> choices = selections();
                this.selection = choices.isEmpty() ? null : choices.getFirst();

                for (int index = 0; index < definition.inputSlots(); index++) {
                        addSlot(inputSlot(index));
                }

                resultSlot = slots.size();
                addSlot(new Slot(preview, 0, isToolStation() ? 124 : 106, isToolStation() ? 38 : 35) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                                return false;
                        }

                        @Override
                        public boolean mayPickup(Player player) {
                                return false;
                        }
                });

                for (int row = 0; row < 3; row++) {
                        for (int column = 0; column < 9; column++) {
                                addSlot(new Slot(inventory, column + row * 9 + 9,
                                                8 + column * 18, inventoryY() + row * 18));
                        }
                }
                for (int column = 0; column < 9; column++) {
                        addSlot(new Slot(inventory, column, 8 + column * 18, inventoryY() + 58));
                }
                refresh();
        }

        // Getters
        public StationDefinition definition() {
                return definition;
        }

        public ResourceLocation operation() {
                return operation;
        }

        @Nullable
        public ResourceLocation selection() {
                return selection;
        }

        public String name() {
                return name;
        }

        public int replacementIndex() {
                return replacementIndex;
        }

        public Component error() {
                return error;
        }

        public void setError(Component error) {
                this.error = error;
        }

        public boolean isRemainder(int index) {
                return definition.supports(StationOperations.BUILD_PART) && index == 3;
        }

        public BlockPos position() {
                return position;
        }

        public boolean isToolStation() {
                return definition.supports(StationOperations.ASSEMBLE);
        }

        public int inventoryY() {
                return (isToolStation() ? 92 : 84) + extraRows() * 18;
        }

        public int extraRows() {
                return Math.max(0, (definition.inputSlots() - 6 + 8) / 9);
        }

        // Layout
        public int activeInputs() {
                if (operation.equals(StationOperations.ASSEMBLE) && selection != null) {
                        ToolDefinition tool = TLRegistries.TOOLS.get(selection);
                        return tool == null ? 0 : Math.min(tool.partRequirements().size(), definition.inputSlots());
                }
                return definition.inputSlots();
        }

        private Slot inputSlot(int index) {
                StationLayout.Position point;
                if (definition.supports(StationOperations.BUILD_PART) && index < 4) {
                        point = StationLayout.PART_BUILDER.get(index);
                } else if (definition.supports(StationOperations.CUT_PATTERN) && index == 0) {
                        point = new StationLayout.Position(48, 35);
                } else if (index >= 6) {
                        point = new StationLayout.Position(8 + (index - 6) % 9 * 18,
                                        (isToolStation() ? 84 : 76) + (index - 6) / 9 * 18);
                } else if (operation.equals(StationOperations.ASSEMBLE)) {
                        point = index < activeInputs() ? StationLayout.part(selection, index)
                                        : StationLayout.dormant(index - activeInputs());
                } else {
                        point = StationLayout.MAINTENANCE.get(index);
                }
                return new Slot(inputs, index, point.x(), point.y()) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                                return index < activeInputs() && acceptsInput(index, stack);
                        }

                        @Override
                        public boolean isActive() {
                                // Switching shapes must never hide an item left in an unused input.
                                return index < activeInputs() || hasItem();
                        }
                };
        }

        public void updateLayout() {
                for (int index = 0; index < definition.inputSlots(); index++) {
                        Slot replacement = inputSlot(index);
                        Slot previous = slots.get(index);
                        if (previous.x != replacement.x || previous.y != replacement.y) {
                                // Slot coordinates are final in 1.21. Preserve its inventory and sync index.
                                replacement.index = index;
                                slots.set(index, replacement);
                        }
                }
        }

        public boolean updateToolName() {
                if (!isToolStation() || operation.equals(StationOperations.ASSEMBLE)) {
                        namedInput = ItemStack.EMPTY;
                        return false;
                }
                ItemStack input = inputs.getItem(0);
                boolean sameTool = ItemStack.isSameItem(input, namedInput)
                                && Objects.equals(input.get(TLDataComponents.TOOL_ID), namedInput.get(TLDataComponents.TOOL_ID))
                                && Objects.equals(input.get(DataComponents.CUSTOM_NAME), namedInput.get(DataComponents.CUSTOM_NAME));
                if (sameTool) {
                        return false;
                }
                namedInput = input.copy();
                Component customName = input.get(DataComponents.CUSTOM_NAME);
                name = customName == null ? "" : customName.getString();
                return true;
        }

        public List<ResourceLocation> selections() {
                if (operation.equals(StationOperations.ASSEMBLE)) {
                        return TLRegistries.TOOLS.stream()
                                        .filter(tool -> StationRecipes.supports(definition, tool, operation))
                                        .map(ToolDefinition::id).toList();
                }
                if (operation.equals(StationOperations.CUT_PATTERN)) {
                        return StationRecipes.shapes();
                }
                return List.of();
        }

        public List<ResourceLocation> replacementParts() {
                if (inputs.getItem(0).getItem() instanceof DynamicTool tool) {
                        return tool.getDefinition().partRequirements().stream().map(part -> part.part()).toList();
                }
                return List.of();
        }

        public int resultSlot() {
                return resultSlot;
        }

        private boolean acceptsInput(int index, ItemStack stack) {
                if (stack.isEmpty() || isRemainder(index)) {
                        return false;
                }
                if (operation.equals(StationOperations.CUT_PATTERN)) {
                        return index == 0 && (stack.is(TLItems.item(TLItems.Items.IntermediaryItems.Type.BLANK_PATTERN))
                                        || TLConfig.REUSE_STENCILS.get() && isPattern(stack));
                }
                if (operation.equals(StationOperations.BUILD_PART)) {
                        if (index == 0) {
                                return isPattern(stack);
                        }
                        if (index > 2) {
                                return false;
                        }
                        boolean allowCastable = materialPolicy.get() != 0;
                        var match = MaterialInputs.match(stack, allowCastable);
                        var other = MaterialInputs.match(inputs.getItem(index == 1 ? 2 : 1), allowCastable);
                        return match != null && match.craftable()
                                        && (other == null || match.material().equals(other.material()));
                }
                if (operation.equals(StationOperations.ASSEMBLE)) {
                        ToolDefinition tool = selection == null ? null : TLRegistries.TOOLS.get(selection);
                        return tool != null && index < tool.partRequirements().size()
                                        && ToolAssembly.matches(stack, tool.partRequirements().get(index));
                }
                if (operation.equals(StationOperations.MAINTAIN) || operation.equals(StationOperations.REPAIR)
                                || operation.equals(StationOperations.MODIFY)
                                || operation.equals(StationOperations.RENAME) || operation.equals(StationOperations.REPLACE)) {
                        if (index == 0) {
                                return stack.getItem() instanceof DynamicTool tool
                                                && StationRecipes.supports(definition, tool.getDefinition(), operation);
                        }
                        if (operation.equals(StationOperations.RENAME)) {
                                return false;
                        }
                        if (operation.equals(StationOperations.REPLACE)) {
                                return index == 1 && acceptsPart(stack, true);
                        }
                        if (operation.equals(StationOperations.REPAIR)) {
                                return acceptsRepair(stack);
                        }
                        if (operation.equals(StationOperations.MODIFY)) {
                                return ToolModifiers.isIngredient(stack);
                        }
                        return acceptsPart(stack, false) || acceptsRepair(stack) || ToolModifiers.isIngredient(stack);
                }
                return true;
        }

        private boolean isPattern(ItemStack stack) {
                ResourceLocation part = stack.get(TLDataComponents.PART);
                return stack.is(TLItems.item(TLItems.Items.IntermediaryItems.Type.PATTERN))
                                && part != null && StationRecipes.shapes().contains(part);
        }

        private boolean acceptsPart(ItemStack stack, boolean selected) {
                if (!(stack.getItem() instanceof DynamicPart part)) {
                        return false;
                }
                if (inputs.getItem(0).getItem() instanceof DynamicTool tool) {
                        var requirements = tool.getDefinition().partRequirements();
                        return selected ? replacementIndex < requirements.size()
                                        && ToolAssembly.matches(stack, requirements.get(replacementIndex))
                                        : requirements.stream().anyMatch(requirement -> ToolAssembly.matches(stack, requirement));
                }
                return ToolAssembly.matches(stack, new ToolPartRequirement(
                                part.definition().id(), part.definition().statTypes()));
        }

        private boolean acceptsRepair(ItemStack stack) {
                var match = MaterialInputs.match(stack);
                if (match == null) {
                        return false;
                }
                if (inputs.getItem(0).getItem() instanceof DynamicTool tool) {
                        List<MaterialLayer> materials = inputs.getItem(0).getOrDefault(TLDataComponents.MATERIALS, List.of());
                        return tool.getDefinition().repairParts().stream()
                                        .filter(index -> index >= 0 && index < materials.size())
                                        .anyMatch(index -> materials.get(index).material().equals(match.material()));
                }
                return true;
        }

        // Selection
        public StationSelection selectionState() {
                return new StationSelection(operation, selection, name, replacementIndex);
        }

        public int selectionRevision() {
                return selectionRevision;
        }

        public long nextSelectionRequest() {
                return ++selectionRequest;
        }

        public void acceptStatus(Component error, StationSelection state, long acknowledgedRequest) {
                // An echo of an earlier keystroke must not erase newer local text.
                if (acknowledgedRequest < selectionRequest) return;
                this.error = error;
                acceptSelection(state);
        }

        public void selectRequest(ResourceLocation operation, @Nullable ResourceLocation selection,
                        String name, int replacementIndex, long request) {
                if (request <= processedSelectionRequest) return;
                processedSelectionRequest = request;
                select(operation, selection, name, replacementIndex);
                broadcastChanges();
        }

        public void acceptSelection(StationSelection state) {
                if (state.equals(selectionState())) return;
                operation = state.operation();
                selection = state.selection();
                name = state.name();
                replacementIndex = state.replacementIndex();
                namedInput = inputs.getItem(0).copy();
                selectionRevision++;
                updateLayout();
        }

        private void shareSelection() {
                if (inputs instanceof StationBlockEntity station) station.select(selectionState());
        }

        private void synchronizeSelection() {
                if (inputs instanceof StationBlockEntity station && station.selection() != null) {
                        acceptSelection(station.selection());
                }
        }

        public void select(ResourceLocation operation, @Nullable ResourceLocation selection, String name, int replacementIndex) {
                if (!definition.supports(operation) || name.length() > 50
                                || replacementIndex < 0 || replacementIndex >= 64) {
                        return;
                }
                this.operation = operation;
                updateToolName();
                List<ResourceLocation> choices = selections();
                this.selection = selection != null && choices.contains(selection)
                                ? selection : choices.isEmpty() ? null : choices.getFirst();
                this.name = name.strip().replaceAll("[\\p{Cntrl}\\u00a7]", "");
                this.replacementIndex = replacementIndex;
                shareSelection();
                updateLayout();
                refresh();
                broadcastChanges();
                if (inputs instanceof StationBlockEntity station) {
                        for (Player viewer : owner.level().players()) {
                                if (viewer != owner && viewer.containerMenu instanceof StationMenu other
                                                && other.inputs == station) other.broadcastChanges();
                        }
                }
        }

        // Preview
        private List<ItemStack> copyInputs() {
                List<ItemStack> copies = new ArrayList<>();
                for (int index = 0; index < inputs.getContainerSize(); index++) {
                        copies.add(inputs.getItem(index).copy());
                }
                return copies;
        }

        private boolean matchesInputs(List<ItemStack> expected) {
                if (expected.size() != inputs.getContainerSize()) {
                        return false;
                }
                for (int index = 0; index < expected.size(); index++) {
                        if (!ItemStack.matches(expected.get(index), inputs.getItem(index))) {
                                return false;
                        }
                }
                return true;
        }

        private void refresh() {
                synchronizeSelection();
                if (updateToolName()) shareSelection();
                if (inputs instanceof StationBlockEntity station && station.selection() == null) shareSelection();
                if (owner.level().isClientSide) {
                        return;
                }
                previewInputs = copyInputs();
                StationResult result = StationRecipes.evaluate(
                                definition, previewInputs, operation, selection, name, replacementIndex);
                preview.setItem(0, result.output().copy());
                error = result.error();
        }

        @Override
        public void broadcastChanges() {
                refresh();
                super.broadcastChanges();
                StationSelection state = selectionState();
                if (owner instanceof ServerPlayer player && (!error.equals(sentError) || !state.equals(sentSelection)
                                || processedSelectionRequest != sentSelectionRequest)) {
                        PacketDistributor.sendToPlayer(player, new StationStatusPayload(containerId, error, state, processedSelectionRequest));
                        sentError = error;
                        sentSelection = state;
                        sentSelectionRequest = processedSelectionRequest;
                }
        }

        @Override
        public boolean stillValid(Player player) {
                return inputs instanceof StationBlockEntity station ? station.stillValid(player)
                                : player.distanceToSqr(position.getCenter()) <= 64.0D;
        }

        // Transactions
        private ItemStack commit(Player player) {
                if (player.level().isClientSide || !stillValid(player) || !matchesInputs(previewInputs)) {
                        refresh();
                        return ItemStack.EMPTY;
                }
                StationResult result = StationRecipes.evaluate(
                                definition, copyInputs(), operation, selection, name, replacementIndex);
                if (!result.valid() || result.inputsAfter().size() != inputs.getContainerSize()
                                || !ItemStack.matches(preview.getItem(0), result.output())) {
                        refresh();
                        return ItemStack.EMPTY;
                }
                ItemStack output = result.output().copy();
                if (output.getItem() instanceof DynamicTool) {
                        ToolState.initialize(output, true);
                }
                ((StationBlockEntity) inputs).replaceInputs(result.inputsAfter());
                output.onCraftedBy(player.level(), player, output.getCount());
                if (isToolStation()) {
                        player.level().playSound(null, position,
                                        definition.tier() > 1 ? TLSounds.TOOL_FORGE.get() : TLSounds.TOOL_STATION.get(),
                                        SoundSource.BLOCKS, definition.tier() > 1 ? 0.9F : 0.8F,
                                        definition.tier() > 1 ? 0.95F + player.getRandom().nextFloat() * 0.2F
                                                        : 0.8F + player.getRandom().nextFloat() * 0.4F);
                }
                refresh();
                return output;
        }

        private boolean fits(ItemStack existing, ItemStack result) {
                return existing.isEmpty() ? result.getCount() <= result.getMaxStackSize()
                                : ItemStack.isSameItemSameComponents(existing, result)
                                                && existing.getCount() + result.getCount() <= existing.getMaxStackSize();
        }

        private boolean fitsInventory(ItemStack result) {
                int remaining = result.getCount();
                for (int index = resultSlot + 1; index < slots.size(); index++) {
                        Slot slot = slots.get(index);
                        ItemStack existing = slot.getItem();
                        if (slot.mayPlace(result) && (existing.isEmpty()
                                        || ItemStack.isSameItemSameComponents(existing, result))) {
                                remaining -= slot.getMaxStackSize(result) - existing.getCount();
                                if (remaining <= 0) {
                                        return true;
                                }
                        }
                }
                return false;
        }

        @Override
        public void clicked(int slot, int button, ClickType click, Player player) {
                if (slot != resultSlot) {
                        super.clicked(slot, button, click, player);
                        return;
                }
                if (player.level().isClientSide || !stillValid(player)) {
                        return;
                }
                ItemStack result = preview.getItem(0);
                if (result.isEmpty()) {
                        return;
                }
                if (click == ClickType.PICKUP && (button == 0 || button == 1) && fits(getCarried(), result)) {
                        ItemStack crafted = commit(player);
                        if (!crafted.isEmpty()) {
                                if (getCarried().isEmpty()) {
                                        setCarried(crafted);
                                } else {
                                        getCarried().grow(crafted.getCount());
                                }
                        }
                } else if (click == ClickType.QUICK_MOVE && fitsInventory(result)) {
                        ItemStack crafted = commit(player);
                        if (!crafted.isEmpty()) {
                                moveItemStackTo(crafted, resultSlot + 1, slots.size(), true);
                                if (!crafted.isEmpty()) {
                                        player.drop(crafted, false);
                                }
                        }
                } else if (click == ClickType.SWAP && ((button >= 0 && button < 9) || button == 40)
                                && fits(player.getInventory().getItem(button), result)) {
                        ItemStack crafted = commit(player);
                        if (!crafted.isEmpty()) {
                                ItemStack current = player.getInventory().getItem(button);
                                if (current.isEmpty()) {
                                        player.getInventory().setItem(button, crafted);
                                } else {
                                        current.grow(crafted.getCount());
                                }
                        }
                } else if (click == ClickType.THROW && getCarried().isEmpty()) {
                        ItemStack crafted = commit(player);
                        if (!crafted.isEmpty()) {
                                player.drop(crafted, false);
                        }
                }
                broadcastChanges();
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
                if (index < 0 || index >= slots.size() || index == resultSlot) {
                        return ItemStack.EMPTY;
                }
                Slot slot = slots.get(index);
                if (!slot.hasItem() || !slot.mayPickup(player)) {
                        return ItemStack.EMPTY;
                }
                ItemStack stack = slot.getItem();
                ItemStack original = stack.copy();
                if (index < resultSlot) {
                        if (!moveItemStackTo(stack, resultSlot + 1, slots.size(), true)) {
                                return ItemStack.EMPTY;
                        }
                } else {
                        if (!moveToInputs(stack)) {
                                int inventoryStart = resultSlot + 1;
                                int hotbarStart = inventoryStart + 27;
                                boolean moved = index < hotbarStart
                                                ? moveItemStackTo(stack, hotbarStart, slots.size(), false)
                                                : moveItemStackTo(stack, inventoryStart, hotbarStart, false);
                                if (!moved) {
                                        return ItemStack.EMPTY;
                                }
                        }
                }
                if (stack.isEmpty()) {
                        slot.setByPlayer(ItemStack.EMPTY);
                } else {
                        slot.setChanged();
                }
                slot.onTake(player, stack);
                return original;
        }

        private boolean moveToInputs(ItemStack stack) {
                boolean moved = false;
                if (operation.equals(StationOperations.ASSEMBLE)) {
                        // Supply each physical requirement before stacking spare copies into a matching slot.
                        for (int input = 0; input < resultSlot && !stack.isEmpty(); input++) {
                                Slot slot = slots.get(input);
                                if (!slot.hasItem() && slot.mayPlace(stack)) {
                                        slot.setByPlayer(stack.split(1));
                                        moved = true;
                                }
                        }
                }
                // Merge existing stacks before opening another material slot. Check mayPlace on both paths.
                for (boolean occupied : List.of(true, false)) {
                        for (int input = 0; input < resultSlot && !stack.isEmpty(); input++) {
                                Slot slot = slots.get(input);
                                if (slot.hasItem() == occupied && slot.mayPlace(stack)) {
                                        moved |= moveItemStackTo(stack, input, input + 1, false);
                                }
                        }
                }
                return moved;
        }

        @Override
        public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
                return slot.container != preview && super.canTakeItemForPickAll(stack, slot);
        }
}
