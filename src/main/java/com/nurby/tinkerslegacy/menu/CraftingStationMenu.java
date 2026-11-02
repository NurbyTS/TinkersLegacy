package com.nurby.tinkerslegacy.menu;

import com.nurby.tinkerslegacy.block.entity.StationBlockEntity;
import com.nurby.tinkerslegacy.library.station.StationDefinition;
import com.nurby.tinkerslegacy.library.tool.ToolState;
import com.nurby.tinkerslegacy.registry.TLStationMenus;
import com.nurby.tinkerslegacy.registry.TLStations;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

public class CraftingStationMenu extends AbstractContainerMenu implements StationMenuAccess {
        public static final int RESULT_SLOT = 0;
        public static final int GRID_START = 1;
        public static final int GRID_END = 10;
        public static final int INVENTORY_START = 10;
        public static final int INVENTORY_END = 46;
        public static final int SIDE_START = 46;

        private final Player owner;
        private final BlockPos position;
        private final StationDefinition definition;
        private final StationCraftingContainer grid;
        private final ResultContainer result = new ResultContainer();
        private final StationSideInventory side;
        private List<ItemStack> previewInputs = List.of();
        private int visibleSideRows = 6;
        private int sideScroll;
        private boolean refreshing;
        @Nullable
        private RecipeHolder<CraftingRecipe> lastRecipe;

        public CraftingStationMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
                this(id, inventory, buffer.readBlockPos(), TLStations.get(buffer.readResourceLocation()),
                                null, StationSideInventory.client(buffer.readVarInt(),
                                                ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buffer)));
        }

        public CraftingStationMenu(int id, Inventory inventory, StationBlockEntity station) {
                this(id, inventory, station.getBlockPos(), station.definition(), station,
                                StationSideInventory.find(inventory.player, station.getBlockPos()));
        }

        private CraftingStationMenu(
                        int id, Inventory inventory, BlockPos position, StationDefinition definition,
                        @Nullable StationBlockEntity station, StationSideInventory side) {
                super(TLStationMenus.CRAFTING_STATION.get(), id);
                this.owner = inventory.player;
                this.position = position;
                this.definition = definition;
                this.side = side;
                this.grid = new StationCraftingContainer(this,
                                station == null ? new SimpleContainer(9) : station);

                addSlot(new CraftingResultSlot(owner));
                for (int row = 0; row < 3; row++) {
                        for (int column = 0; column < 3; column++) {
                                addSlot(new Slot(grid, column + row * 3, 30 + column * 18, 17 + row * 18));
                        }
                }
                for (int row = 0; row < 3; row++) {
                        for (int column = 0; column < 9; column++) {
                                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
                        }
                }
                for (int column = 0; column < 9; column++) {
                        addSlot(new Slot(inventory, column, 8 + column * 18, 142));
                }
                for (int index = 0; index < side.size(); index++) {
                        addSlot(sideSlot(index));
                }
                refresh();
        }

        // Getters
        @Override
        public BlockPos position() {
                return position;
        }

        public StationDefinition definition() {
                return definition;
        }

        public StationCraftingContainer grid() {
                return grid;
        }

        public int sideSlotCount() {
                return side.size();
        }

        public Component sideTitle() {
                return side.title();
        }

        public int sideRows() {
                return (side.size() + 5) / 6;
        }

        public void setSideRows(int rows) {
                visibleSideRows = Math.clamp(rows, 1, 6);
                setSideScroll(sideScroll);
        }

        public void setSideScroll(int row) {
                sideScroll = Math.clamp(row, 0, Math.max(0, sideRows() - visibleSideRows));
                for (int index = 0; index < side.size(); index++) {
                        Slot slot = sideSlot(index);
                        slot.index = SIDE_START + index;
                        slots.set(slot.index, slot);
                }
        }

        // Synchronization
        public static void writeExtraData(RegistryFriendlyByteBuf buffer, CraftingStationMenu menu) {
                buffer.writeVarInt(menu.sideSlotCount());
                ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buffer, menu.sideTitle());
        }

        @Override
        public void slotsChanged(Container container) {
                refresh();
        }

        @Override
        public void broadcastChanges() {
                if (!owner.level().isClientSide && inputsChanged()) refresh();
                super.broadcastChanges();
        }

        private boolean inputsChanged() {
                if (previewInputs.size() != grid.getContainerSize()) return true;
                for (int slot = 0; slot < previewInputs.size(); slot++) {
                        if (!ItemStack.matches(previewInputs.get(slot), grid.getItem(slot))) return true;
                }
                return false;
        }

        private void refresh() {
                if (owner.level().isClientSide || refreshing) return;
                refreshing = true;
                try {
                        ItemStack previous = result.getItem(0);
                        ItemStack output = ItemStack.EMPTY;
                        result.setRecipeUsed(null);
                        CraftingInput input = grid.asCraftInput();
                        Optional<RecipeHolder<CraftingRecipe>> recipe = owner.level().getRecipeManager().getRecipeFor(
                                        RecipeType.CRAFTING, input, owner.level(), lastRecipe);
                        lastRecipe = recipe.orElse(null);
                        if (lastRecipe != null && owner instanceof ServerPlayer serverPlayer
                                        && result.setRecipeUsed(owner.level(), serverPlayer, lastRecipe)) {
                                ItemStack assembled = lastRecipe.value().assemble(input, owner.level().registryAccess());
                                if (assembled.isItemEnabled(owner.level().enabledFeatures())) output = assembled;
                        }
                        result.setItem(0, output);
                        previewInputs = grid.getItems().stream().map(ItemStack::copy).toList();
                        if (!ItemStack.matches(previous, output) && owner instanceof ServerPlayer serverPlayer) {
                                setRemoteSlot(RESULT_SLOT, output);
                                serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(
                                                containerId, incrementStateId(), RESULT_SLOT, output));
                        }
                } finally {
                        refreshing = false;
                }
        }

        private boolean revalidateOutput(Player player) {
                if (player.level().isClientSide) return true;
                if (!stillValid(player)) return false;
                boolean changed = inputsChanged();
                ItemStack previous = result.getItem(0).copy();
                refresh();
                return !changed && !previous.isEmpty() && ItemStack.matches(previous, result.getItem(0));
        }

        // Transactions
        @Override
        public boolean stillValid(Player player) {
                return grid.stillValid(player) && side.valid(player);
        }

        @Override
        public void clicked(int slot, int button, ClickType type, Player player) {
                if (!player.level().isClientSide) {
                        if (!stillValid(player)) return;
                        if (inputsChanged()) {
                                refresh();
                                // A second viewer changed the grid after this player's preview was calculated.
                                if (slot == RESULT_SLOT) return;
                        }
                }
                super.clicked(slot, button, type, player);
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
                if (index < 0 || index >= slots.size() || !stillValid(player)) return ItemStack.EMPTY;
                Slot slot = slots.get(index);
                if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
                if (index == RESULT_SLOT) return moveResult(player, slot);
                if (index >= SIDE_START) return moveSideStack(player, slot);

                ItemStack original = slot.getItem().copy();
                ItemStack remaining = original.copy();
                grid.beginBatch();
                try {
                        boolean moved;
                        if (index >= INVENTORY_START && index < INVENTORY_END) {
                                moved = moveItemStackTo(remaining, GRID_START, GRID_END, false);
                                if (!moved) {
                                        moved = index < 37 ? moveItemStackTo(remaining, 37, INVENTORY_END, false)
                                                        : moveItemStackTo(remaining, INVENTORY_START, 37, false);
                                }
                        } else {
                                moved = moveItemStackTo(remaining, INVENTORY_START, INVENTORY_END, false);
                        }
                        if (!moved || original.getCount() == remaining.getCount()) return ItemStack.EMPTY;
                        slot.setByPlayer(remaining);
                        slot.onTake(player, original.copyWithCount(original.getCount() - remaining.getCount()));
                        return original;
                } finally {
                        grid.endBatch();
                }
        }

        private ItemStack moveSideStack(Player player, Slot slot) {
                ItemStack original = slot.getItem().copy();
                if (!canMoveResult(original)) return ItemStack.EMPTY;
                // Deliver only what the capability actually extracted, even if its simulation was optimistic.
                ItemStack extracted = slot.remove(original.getCount());
                if (extracted.isEmpty()) return ItemStack.EMPTY;
                ItemStack taken = extracted.copy();
                boolean moved = moveItemStackTo(extracted, INVENTORY_START, INVENTORY_END, false);
                int delivered = taken.getCount() - extracted.getCount();
                if (!extracted.isEmpty()) {
                        ItemStack remainder = side.valid(player)
                                        ? side.inventory().insertItem(slot.getSlotIndex(), extracted, false) : extracted;
                        if (!remainder.isEmpty()) player.getInventory().placeItemBackInInventory(remainder);
                }
                if (!moved || delivered <= 0) return ItemStack.EMPTY;
                slot.onTake(player, taken.copyWithCount(delivered));
                return original;
        }

        private ItemStack moveResult(Player player, Slot slot) {
                if (!revalidateOutput(player)) return ItemStack.EMPTY;
                ItemStack original = slot.getItem().copy();
                if (!canMoveResult(original)) return ItemStack.EMPTY;
                ItemStack remaining = original.copy();
                if (!player.level().isClientSide) {
                        remaining.getItem().onCraftedBy(remaining, player.level(), player);
                        ToolState.initialize(remaining, true);
                }
                if (!moveItemStackTo(remaining, INVENTORY_START, INVENTORY_END, true)) return ItemStack.EMPTY;
                ItemStack crafted = original.copy();
                slot.onQuickCraft(remaining, crafted);
                slot.setByPlayer(remaining);
                slot.onTake(player, remaining);
                if (!remaining.isEmpty()) player.drop(remaining, false);
                return original;
        }

        private boolean canMoveResult(ItemStack output) {
                for (int index = INVENTORY_START; index < INVENTORY_END; index++) {
                        Slot slot = slots.get(index);
                        ItemStack stack = slot.getItem();
                        if (stack.isEmpty() || ItemStack.isSameItemSameComponents(stack, output)
                                        && stack.getCount() < slot.getMaxStackSize(output)) return true;
                }
                return false;
        }

        @Override
        public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
                return slot.container != result && super.canTakeItemForPickAll(stack, slot);
        }

        @Override
        public void removed(Player player) {
                super.removed(player);
                // The grid belongs to the block entity; only the cursor is returned on close.
                side.close();
        }

        // Slots
        private Slot sideSlot(int index) {
                int x = (sideRows() > visibleSideRows ? -126 : -114) + index % 6 * 18;
                int row = index / 6;
                return new SlotItemHandler(side.inventory(), index, x, 18 + (row - sideScroll) * 18) {
                        @Override
                        public ItemStack getItem() {
                                return side.valid(owner) ? super.getItem().copy() : ItemStack.EMPTY;
                        }

                        @Override
                        public void set(ItemStack stack) {
                                if (side.valid(owner)) super.set(stack);
                        }

                        @Override
                        public boolean mayPlace(ItemStack stack) {
                                return side.valid(owner) && super.mayPlace(stack);
                        }

                        @Override
                        public boolean mayPickup(Player player) {
                                if (!side.valid(player)) return false;
                                ItemStack stack = getItem();
                                return !stack.isEmpty() && side.inventory().extractItem(index, stack.getCount(), true)
                                                .getCount() == stack.getCount();
                        }

                        @Override
                        public ItemStack remove(int amount) {
                                return side.valid(owner) ? super.remove(amount) : ItemStack.EMPTY;
                        }

                        @Override
                        public boolean isActive() {
                                return !owner.level().isClientSide || row >= sideScroll && row < sideScroll + visibleSideRows;
                        }
                };
        }

        private final class CraftingResultSlot extends ResultSlot {
                private CraftingResultSlot(Player player) {
                        super(player, grid, result, 0, 124, 35);
                }

                @Override
                public boolean mayPickup(Player player) {
                        return revalidateOutput(player);
                }

                @Override
                public void onTake(Player player, ItemStack stack) {
                        if (!player.level().isClientSide) ToolState.initialize(stack, true);
                        grid.beginBatch();
                        try {
                                super.onTake(player, stack);
                        } finally {
                                grid.endBatch();
                        }
                }
        }
}
