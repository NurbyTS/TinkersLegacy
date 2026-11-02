package com.nurby.tinkerslegacy.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class GlowBlock extends DirectionalBlock {
        public static final MapCodec<GlowBlock> CODEC = simpleCodec(GlowBlock::new);

        public GlowBlock(Properties properties) {
                super(properties);
                registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
        }

        @Override
        protected MapCodec<? extends DirectionalBlock> codec() {
                return CODEC;
        }

        // Placement
        public boolean addGlow(Level level, BlockPos pos) {
                if (!level.getBlockState(pos).isAir()) return false;
                int start = level.random.nextInt(Direction.values().length);
                for (int offset = 0; offset < Direction.values().length; offset++) {
                        Direction direction = Direction.values()[(start + offset) % Direction.values().length];
                        BlockState state = defaultBlockState().setValue(FACING, direction);
                        if (state.canSurvive(level, pos)) return level.setBlock(pos, state, Block.UPDATE_ALL);
                }
                return false;
        }

        @Override
        protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
                Direction facing = state.getValue(FACING);
                BlockPos support = pos.relative(facing.getOpposite());
                return level.getBlockState(support).isFaceSturdy(level, support, facing);
        }

        @Override
        protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                        LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
                return canSurvive(state, level, pos) ? state : Blocks.AIR.defaultBlockState();
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
                builder.add(FACING);
        }

        // Shapes
        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
                return switch (state.getValue(FACING)) {
                        case DOWN -> Block.box(4, 15, 4, 12, 16, 12);
                        case UP -> Block.box(4, 0, 4, 12, 1, 12);
                        case NORTH -> Block.box(4, 4, 15, 12, 12, 16);
                        case SOUTH -> Block.box(4, 4, 0, 12, 12, 1);
                        case WEST -> Block.box(15, 4, 4, 16, 12, 12);
                        case EAST -> Block.box(0, 4, 4, 1, 12, 12);
                };
        }
}
