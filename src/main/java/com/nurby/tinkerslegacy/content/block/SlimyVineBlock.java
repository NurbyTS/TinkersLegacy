package com.nurby.tinkerslegacy.content.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class SlimyVineBlock extends VineBlock {
        public static final MapCodec<VineBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                        propertiesCodec(),
                        Codec.INT.fieldOf("foliage_color").forGetter(block -> ((SlimyVineBlock) block).foliageColor()))
                        .apply(instance, SlimyVineBlock::new));
        public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 2);

        private final int color;

        public SlimyVineBlock(Properties properties, int color) {
                super(properties);
                this.color = color;
                registerDefaultState(defaultBlockState().setValue(STAGE, 0));
        }

        @Override
        public MapCodec<VineBlock> codec() {
                return CODEC;
        }

        public int foliageColor() {
                return color;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
                super.createBlockStateDefinition(builder);
                builder.add(STAGE);
        }

        // Support
        private BlockState supported(BlockState state, LevelReader level, BlockPos pos) {
                BlockState above = level.getBlockState(pos.above());
                for (Direction direction : Direction.Plane.HORIZONTAL) {
                        var face = getPropertyForFace(direction);
                        if (state.getValue(face)) {
                                boolean supported = isAcceptableNeighbour(level, pos.relative(direction), direction)
                                                || above.getBlock() instanceof SlimyLeavesBlock
                                                || above.getBlock() instanceof VineBlock && above.getValue(face);
                                state = state.setValue(face, supported);
                        }
                }
                return state.setValue(UP, state.getValue(UP)
                                && isAcceptableNeighbour(level, pos.above(), Direction.UP));
        }

        @Override
        protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
                BlockState supported = supported(state, level, pos);
                return PROPERTY_BY_DIRECTION.values().stream().anyMatch(supported::getValue);
        }

        @Override
        protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                        LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
                BlockState supported = supported(state, level, pos);
                return canSurvive(supported, level, pos) ? supported : Blocks.AIR.defaultBlockState();
        }

        // Growth
        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
                if (state.getValue(STAGE) == 2 || random.nextInt(4) != 0
                                || !level.getGameRules().getBoolean(GameRules.RULE_DO_VINES_SPREAD)
                                || !level.isAreaLoaded(pos, 1) || !level.isEmptyBlock(pos.below())) {
                        return;
                }
                boolean attached = false;
                for (Direction direction : Direction.Plane.HORIZONTAL) {
                        if (state.getValue(getPropertyForFace(direction))
                                        && isAcceptableNeighbour(level, pos.relative(direction), direction)) {
                                attached = true;
                        }
                }
                int stage = state.getValue(STAGE);
                if (!attached) {
                        int length = 0;
                        BlockState above;
                        while ((above = level.getBlockState(pos.above(length))).is(this) && above.getValue(STAGE) == stage) {
                                length++;
                        }
                        if (length > 2 || random.nextBoolean()) {
                                stage++;
                        }
                }
                BlockState grown = state.setValue(STAGE, stage).setValue(UP, false);
                if (grown.canSurvive(level, pos.below())) {
                        level.setBlockAndUpdate(pos.below(), grown);
                }
        }
}
