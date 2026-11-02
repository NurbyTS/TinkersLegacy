package com.nurby.tinkerslegacy.content.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

public class SlimyLeavesBlock extends LeavesBlock {
        public static final MapCodec<SlimyLeavesBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                        propertiesCodec(),
                        Codec.INT.fieldOf("foliage_color").forGetter(SlimyLeavesBlock::foliageColor))
                        .apply(instance, SlimyLeavesBlock::new));

        private final int color;

        public SlimyLeavesBlock(Properties properties, int color) {
                super(properties);
                this.color = color;
        }

        @Override
        public MapCodec<SlimyLeavesBlock> codec() {
                return CODEC;
        }

        // Leaf Support
        @Override
        protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
                int distance = 7;
                for (Direction direction : Direction.values()) {
                        BlockState neighbor = level.getBlockState(pos.relative(direction));
                        int support = neighbor.getBlock() instanceof CongealedSlimeBlock
                                        ? 0 : LeavesBlock.getOptionalDistanceAt(neighbor).orElse(7);
                        distance = Math.min(distance, support + 1);
                        if (distance == 1) break;
                }
                level.setBlock(pos, state.setValue(DISTANCE, distance), 3);
        }

        public int foliageColor() {
                return color;
        }
}
