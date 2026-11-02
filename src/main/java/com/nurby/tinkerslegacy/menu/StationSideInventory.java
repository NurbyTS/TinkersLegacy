package com.nurby.tinkerslegacy.menu;

import com.nurby.tinkerslegacy.content.block.StationBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;

final class StationSideInventory {
        private static final int MAX_SLOTS = 4096;

        private final IItemHandlerModifiable inventory;
        private final Component title;
        private final int slots;
        @Nullable
        private final BlockEntity blockEntity;
        @Nullable
        private final BlockCapabilityCache<IItemHandler, Direction> cache;
        private boolean closed;
        private boolean invalidated;

        private StationSideInventory(int slots, Component title) {
                if (slots < 0 || slots > MAX_SLOTS) {
                        throw new IllegalArgumentException("Invalid crafting station side inventory size");
                }
                this.inventory = new ItemStackHandler(slots);
                this.slots = slots;
                this.title = title;
                this.blockEntity = null;
                this.cache = null;
        }

        private StationSideInventory(
                        ServerLevel level, BlockEntity blockEntity, @Nullable Direction direction,
                        IItemHandlerModifiable inventory) {
                this.blockEntity = blockEntity;
                this.title = blockEntity instanceof Nameable named ? named.getDisplayName()
                                : Component.translatable(blockEntity.getBlockState().getBlock().getDescriptionId());
                this.cache = BlockCapabilityCache.create(
                                Capabilities.ItemHandler.BLOCK, level, blockEntity.getBlockPos(), direction,
                                () -> !closed, () -> invalidated = true);
                // Arm invalidation before opening; a replaced chest must never leave writable old slots.
                IItemHandler current = cache.getCapability();
                this.inventory = current instanceof IItemHandlerModifiable modifiable ? modifiable : inventory;
                this.slots = this.inventory.getSlots();
                if (!(current instanceof IItemHandlerModifiable)) invalidated = true;
        }

        // Getters
        public IItemHandlerModifiable inventory() {
                return inventory;
        }

        public Component title() {
                return title;
        }

        public int size() {
                return slots;
        }

        public boolean valid(Player player) {
                return !closed && !invalidated && (blockEntity == null
                                || accessible(blockEntity, player) && inventory.getSlots() == slots);
        }

        public void close() {
                closed = true;
        }

        // Discovery
        public static StationSideInventory client(int slots, Component title) {
                return new StationSideInventory(slots, title);
        }

        public static StationSideInventory find(Player player, BlockPos position) {
                if (!(player.level() instanceof ServerLevel level)) {
                        return client(0, Component.empty());
                }
                for (Direction direction : Direction.Plane.HORIZONTAL) {
                        BlockPos neighbor = position.relative(direction);
                        if (!level.hasChunkAt(neighbor)
                                        || level.getBlockState(neighbor).getBlock() instanceof StationBlock) continue;
                        BlockEntity blockEntity = level.getBlockEntity(neighbor);
                        if (blockEntity == null || !accessible(blockEntity, player)) continue;
                        Direction context = null;
                        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, neighbor, context);
                        if (!(handler instanceof IItemHandlerModifiable)
                                        || handler.getSlots() == 0 || handler.getSlots() > MAX_SLOTS) {
                                context = direction.getOpposite();
                                handler = level.getCapability(Capabilities.ItemHandler.BLOCK, neighbor, context);
                        }
                        if (handler instanceof IItemHandlerModifiable modifiable
                                        && handler.getSlots() > 0 && handler.getSlots() <= MAX_SLOTS) {
                                StationSideInventory side = new StationSideInventory(
                                                level, blockEntity, context, modifiable);
                                if (side.valid(player)) return side;
                                side.close();
                        }
                }
                return client(0, Component.empty());
        }

        private static boolean accessible(BlockEntity blockEntity, Player player) {
                if (blockEntity.isRemoved() || blockEntity.getLevel() != player.level()
                                || !player.level().hasChunkAt(blockEntity.getBlockPos())
                                || player.level().getBlockEntity(blockEntity.getBlockPos()) != blockEntity) return false;
                if (blockEntity instanceof Container container && !container.stillValid(player)) return false;
                if (blockEntity instanceof BaseContainerBlockEntity container && !container.canOpen(player)) return false;
                BlockState state = blockEntity.getBlockState();
                if (state.getBlock() instanceof ChestBlock chest) {
                        if (ChestBlock.getContainer(chest, state, player.level(), blockEntity.getBlockPos(), false) == null) {
                                return false;
                        }
                        if (state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                                BlockPos other = blockEntity.getBlockPos().relative(ChestBlock.getConnectedDirection(state));
                                if (!(player.level().getBlockEntity(other) instanceof BaseContainerBlockEntity container)
                                                || !container.canOpen(player) || !container.stillValid(player)) return false;
                        }
                }
                return true;
        }
}
