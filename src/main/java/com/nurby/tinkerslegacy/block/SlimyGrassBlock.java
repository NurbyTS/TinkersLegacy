package com.nurby.tinkerslegacy.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nurby.tinkerslegacy.registry.TLBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** One implementation for every combination of soil and foliage. */
public class SlimyGrassBlock extends Block {
    public static final MapCodec<SlimyGrassBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            propertiesCodec(),
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf("soil").forGetter(SlimyGrassBlock::soil),
            Codec.STRING.fieldOf("foliage").forGetter(SlimyGrassBlock::foliage)
    ).apply(instance, SlimyGrassBlock::new));

    private final Block soil;
    private final String foliage;

    public SlimyGrassBlock(Properties properties, Block soil, String foliage) {
        super(properties);
        this.soil = soil;
        this.foliage = foliage;
    }

    @Override
    public MapCodec<SlimyGrassBlock> codec() { return CODEC; }
    public Block soil() { return soil; }
    public String foliage() { return foliage; }

    public int foliageColor() {
        return switch (foliage) {
            case "blue" -> 0x2AEC81;
            case "purple" -> 0xA92DFF;
            case "magma" -> 0xD09800;
            default -> 0x69BC5E; // Green grass is an additional variant in this port.
        };
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isAreaLoaded(pos, 3) || level.getMaxLocalRawBrightness(pos.above()) < 9) return;
        for (int i = 0; i < 4; i++) {
            BlockPos target = pos.offset(random.nextInt(3) - 1, random.nextInt(5) - 3, random.nextInt(3) - 1);
            if (level.isOutsideBuildHeight(target)) continue;
            BlockState above = level.getBlockState(target.above());
            if (level.getMaxLocalRawBrightness(target.above()) < 4 || above.getLightBlock(level, target.above()) > 2) continue;
            Block dirt = level.getBlockState(target).getBlock();
            for (var holder : TLBlocks.BLOCKS.getEntries()) {
                if (holder.get() instanceof SlimyGrassBlock grass && grass.soil == dirt && grass.foliage.equals(foliage)) {
                    level.setBlockAndUpdate(target, grass.defaultBlockState());
                    break;
                }
            }
        }
    }
}
