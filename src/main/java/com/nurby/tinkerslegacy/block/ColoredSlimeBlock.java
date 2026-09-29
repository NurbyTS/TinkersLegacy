package com.nurby.tinkerslegacy.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Vanilla bouncing and slowing, with NeoForge piston adhesion for custom slime IDs. */
public class ColoredSlimeBlock extends SlimeBlock {
    public static final MapCodec<SlimeBlock> CODEC = simpleCodec(ColoredSlimeBlock::new);

    public ColoredSlimeBlock(Properties properties) { super(properties); }

    @Override
    public MapCodec<SlimeBlock> codec() { return CODEC; }

    @Override
    public boolean isStickyBlock(BlockState state) { return true; }

    @Override
    public boolean canStickTo(BlockState state, BlockState other) {
        return !other.is(Blocks.HONEY_BLOCK);
    }
}
