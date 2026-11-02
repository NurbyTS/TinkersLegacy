package com.nurby.tinkerslegacy.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class StationCraftingContainer implements CraftingContainer {
        private final AbstractContainerMenu menu;
        private final Container inputs;
        private int batches;
        private boolean changed;

        public StationCraftingContainer(AbstractContainerMenu menu, Container inputs) {
                if (inputs.getContainerSize() != 9) {
                        throw new IllegalArgumentException("Crafting stations require nine inventory slots");
                }
                this.menu = menu;
                this.inputs = inputs;
        }

        // Getters
        @Override
        public int getWidth() {
                return 3;
        }

        @Override
        public int getHeight() {
                return 3;
        }

        @Override
        public int getContainerSize() {
                return inputs.getContainerSize();
        }

        @Override
        public boolean isEmpty() {
                return inputs.isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
                return inputs.getItem(slot);
        }

        @Override
        public List<ItemStack> getItems() {
                List<ItemStack> items = new ArrayList<>(9);
                for (int slot = 0; slot < 9; slot++) {
                        items.add(inputs.getItem(slot));
                }
                return List.copyOf(items);
        }

        @Override
        public boolean stillValid(Player player) {
                return inputs.stillValid(player);
        }

        // Inventory
        @Override
        public ItemStack removeItem(int slot, int amount) {
                ItemStack removed = inputs.removeItem(slot, amount);
                if (!removed.isEmpty()) setChanged();
                return removed;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
                ItemStack removed = inputs.removeItemNoUpdate(slot);
                if (!removed.isEmpty()) setChanged();
                return removed;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
                inputs.setItem(slot, stack);
                setChanged();
        }

        @Override
        public void clearContent() {
                inputs.clearContent();
                setChanged();
        }

        @Override
        public void setChanged() {
                inputs.setChanged();
                if (batches > 0) {
                        changed = true;
                } else {
                        menu.slotsChanged(this);
                }
        }

        @Override
        public void fillStackedContents(StackedContents contents) {
                for (ItemStack stack : getItems()) {
                        contents.accountSimpleStack(stack);
                }
        }

        // Transactions
        public void beginBatch() {
                batches++;
        }

        public void endBatch() {
                if (batches <= 0) throw new IllegalStateException("Crafting update batch is not open");
                if (--batches == 0 && changed) {
                        changed = false;
                        menu.slotsChanged(this);
                }
        }
}
