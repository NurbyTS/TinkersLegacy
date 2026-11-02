package com.nurby.tinkerslegacy.block;

import com.nurby.tinkerslegacy.block.entity.StationBlockEntity;
import com.nurby.tinkerslegacy.registry.TLStations;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.item.ItemStack;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class StationBlock extends BaseEntityBlock {
        public static final MapCodec<StationBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                        ResourceLocation.CODEC.fieldOf("station").forGetter(StationBlock::definitionId),
                        propertiesCodec()).apply(instance, StationBlock::new));

        public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

        private static final VoxelShape SHAPE = Shapes.or(
                        box(0, 12, 0, 16, 16, 16),
                        box(0, 0, 0, 4, 12, 4),
                        box(12, 0, 0, 16, 12, 4),
                        box(0, 0, 12, 4, 12, 16),
                        box(12, 0, 12, 16, 12, 16));

        private final ResourceLocation definitionId;

        public StationBlock(ResourceLocation definitionId, Properties properties) {
                super(properties.noOcclusion());
                TLStations.get(definitionId);
                this.definitionId = definitionId;
                registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH));
        }

        // Facing
        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
                builder.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
                return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }

        @Override
        protected BlockState rotate(BlockState state, Rotation rotation) {
                return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
        }

        @Override
        protected BlockState mirror(BlockState state, Mirror mirror) {
                return state.rotate(mirror.getRotation(state.getValue(FACING)));
        }

        public ResourceLocation definitionId() {
                return definitionId;
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
                return CODEC;
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
                return RenderShape.MODEL;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
                return SHAPE;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
                return new StationBlockEntity(pos, state);
        }

        @Override
        public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
                ItemStack stack = super.getCloneItemStack(level, pos, state);
                if (level.getBlockEntity(pos) instanceof StationBlockEntity station) {
                        stack.set(TLDataComponents.STATION_BASE, station.base());
                }
                return stack;
        }

        @Override
        protected InteractionResult useWithoutItem(
                        BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
                if (!level.isClientSide && level.getBlockEntity(pos) instanceof StationBlockEntity station) {
                        player.openMenu(station);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
        }

        @Override
        protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
                if (!state.is(replacement.getBlock())) {
                        if (level.getBlockEntity(pos) instanceof StationBlockEntity station) {
                                Containers.dropContents(level, pos, station);
                                level.updateNeighbourForOutputSignal(pos, this);
                        }
                        super.onRemove(state, level, pos, replacement, moving);
                }
        }
}
