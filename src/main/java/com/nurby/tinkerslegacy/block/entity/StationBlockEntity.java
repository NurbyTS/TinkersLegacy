package com.nurby.tinkerslegacy.block.entity;

import com.nurby.tinkerslegacy.block.StationBlock;
import com.nurby.tinkerslegacy.library.station.StationDefinition;
import com.nurby.tinkerslegacy.menu.StationMenu;
import com.nurby.tinkerslegacy.menu.CraftingStationMenu;
import com.nurby.tinkerslegacy.library.station.StationOperations;
import net.minecraft.network.RegistryFriendlyByteBuf;
import com.nurby.tinkerslegacy.registry.TLBlockEntities;
import com.nurby.tinkerslegacy.registry.TLStations;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.library.station.StationAppearance;
import com.nurby.tinkerslegacy.library.station.StationSelection;
import net.neoforged.neoforge.client.model.data.ModelData;

public class StationBlockEntity extends BlockEntity implements Container, MenuProvider {
        private final StationDefinition definition;
        private final NonNullList<ItemStack> inputs;
        private com.nurby.tinkerslegacy.library.station.StationSelection selection;
        private ResourceLocation base;

        public StationBlockEntity(BlockPos pos, BlockState state) {
                super(TLBlockEntities.STATION.get(), pos, state);
                definition = TLStations.get(((StationBlock) state.getBlock()).definitionId());
                inputs = NonNullList.withSize(definition.inputSlots(), ItemStack.EMPTY);
        }

        // Getters
        public StationSelection selection() {
                return selection;
        }

        public void select(StationSelection value) {
                selection = value;
        }

        public ResourceLocation base() {
                return base == null ? StationAppearance.defaultBase(definition.id()) : base;
        }

        public void setBase(ResourceLocation value) {
                ResourceLocation previous = base();
                base = StationAppearance.validBase(value, definition.id());
                refreshModel(previous);
                setChanged();
        }

        private void refreshModel(ResourceLocation previous) {
                if (level != null && level.isClientSide && !previous.equals(base())) {
                        requestModelDataUpdate();
                        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                }
        }

        @Override
        public ModelData getModelData() {
                return ModelData.builder().with(StationAppearance.BASE, base()).build();
        }

        @Override
        public void setChanged() {
                super.setChanged();
                if (level != null && !level.isClientSide) {
                        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                }
        }

        public StationDefinition definition() {
                return definition;
        }

        @Override
        public int getContainerSize() {
                return inputs.size();
        }

        @Override
        public boolean isEmpty() {
                return inputs.stream().allMatch(ItemStack::isEmpty);
        }

        @Override
        public ItemStack getItem(int slot) {
                return inputs.get(slot);
        }

        @Override
        public boolean stillValid(Player player) {
                return !isRemoved() && level != null && level.getBlockEntity(worldPosition) == this
                                && player.distanceToSqr(worldPosition.getX() + 0.5D,
                                                worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64.0D;
        }

        @Override
        public Component getDisplayName() {
                return Component.translatable(getBlockState().getBlock().getDescriptionId());
        }

        // Inventory
        @Override
        public ItemStack removeItem(int slot, int count) {
                ItemStack removed = ContainerHelper.removeItem(inputs, slot, count);
                if (!removed.isEmpty()) {
                        setChanged();
                }
                return removed;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
                return ContainerHelper.takeItem(inputs, slot);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
                inputs.set(slot, stack);
                stack.limitSize(getMaxStackSize(stack));
                setChanged();
        }

        public void replaceInputs(List<ItemStack> replacement) {
                if (replacement.size() != inputs.size()) {
                        throw new IllegalArgumentException("Incorrect station inventory size");
                }
                for (int slot = 0; slot < inputs.size(); slot++) {
                        inputs.set(slot, replacement.get(slot).copy());
                }
                setChanged();
        }

        @Override
        public void clearContent() {
                inputs.clear();
                setChanged();
        }

        // Persistence
        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
                super.loadAdditional(tag, registries);
                inputs.clear();
                ContainerHelper.loadAllItems(tag, inputs, registries);
                ResourceLocation previous = base();
                base = StationAppearance.validBase(ResourceLocation.tryParse(tag.getString("Base")), definition.id());
                refreshModel(previous);
        }

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
                super.saveAdditional(tag, registries);
                ContainerHelper.saveAllItems(tag, inputs, registries);
                tag.putString("Base", base().toString());
        }

        @Override
        protected void applyImplicitComponents(DataComponentInput components) {
                super.applyImplicitComponents(components);
                ResourceLocation previous = base();
                base = StationAppearance.validBase(components.get(TLDataComponents.STATION_BASE.get()), definition.id());
                refreshModel(previous);
        }

        @Override
        protected void collectImplicitComponents(DataComponentMap.Builder components) {
                super.collectImplicitComponents(components);
                components.set(TLDataComponents.STATION_BASE.get(), base());
        }

        @Override
        public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
                return saveWithoutMetadata(registries);
        }

        @Override
        public ClientboundBlockEntityDataPacket getUpdatePacket() {
                return ClientboundBlockEntityDataPacket.create(this);
        }

        // Menus
        @Override
        public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
                buffer.writeBlockPos(worldPosition);
                buffer.writeResourceLocation(definition.id());
                if (menu instanceof CraftingStationMenu crafting) {
                        CraftingStationMenu.writeExtraData(buffer, crafting);
                }
        }

        @Override
        public boolean shouldTriggerClientSideContainerClosingOnOpen() {
                // Switch directly between station screens without re-centering the mouse.
                return false;
        }

        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
                return definition.supports(StationOperations.CRAFT)
                                ? new CraftingStationMenu(id, inventory, this)
                                : new StationMenu(id, inventory, this);
        }
}
