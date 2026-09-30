package com.nurby.tinkerslegacy.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

public class SlimyGrassBlock extends Block {
    public static final MapCodec<SlimyGrassBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            propertiesCodec(),
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf("soil").forGetter(SlimyGrassBlock::soil),
            SlimeFoliage.CODEC.fieldOf("foliage").forGetter(SlimyGrassBlock::foliage))
            .apply(instance, SlimyGrassBlock::new));

    private final Block soil;
    private final SlimeFoliage foliage;

    public SlimyGrassBlock(Properties properties, Block soil, SlimeFoliage foliage) {
        super(properties);
        this.soil = soil;
        this.foliage = foliage;
    }

    @Override
    public MapCodec<SlimyGrassBlock> codec() {
        return CODEC;
    }

    public Block soil() {
        return soil;
    }

    public SlimeFoliage foliage() {
        return foliage;
    }

    public String foliageName() {
        return foliage.name();
    }

    public int foliageColor() {
        return foliage.color();
    }

    private record GrassKey(Block soil, SlimeFoliage foliage) {
    }

    private static final Map<GrassKey, SlimyGrassBlock> VARIANTS = new HashMap<>();

    public static SlimyGrassBlock registerVariant(SlimyGrassBlock grass) {
        GrassKey key = new GrassKey(grass.soil(), grass.foliage);

        if (VARIANTS.putIfAbsent(key, grass) != null) {
            throw new IllegalStateException("Duplicate slime grass variant: " + key);
        }

        return grass;
    }

    public static SlimyGrassBlock findVariant(
            Block soil,
            SlimeFoliage foliage) {
        return VARIANTS.get(new GrassKey(soil, foliage));
    }

    @Override
    protected void randomTick(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            RandomSource random) {

        if (!level.isAreaLoaded(pos, 3) || level.getMaxLocalRawBrightness(pos.above()) < 9)
            return;

        for (int i = 0; i < 4; i++) {
            BlockPos target = pos.offset(
                    random.nextInt(3) - 1,
                    random.nextInt(5) - 3,
                    random.nextInt(3) - 1);

            if (level.isOutsideBuildHeight(target))
                continue;

            BlockState above = level.getBlockState(target.above());

            if (level.getMaxLocalRawBrightness(target.above()) < 4 || above.getLightBlock(level, target.above()) > 2)
                continue;

            Block soiBlock = level.getBlockState(target).getBlock();
            SlimyGrassBlock grass = findVariant(soiBlock, foliage);

            if (grass != null) {
                level.setBlockAndUpdate(target, grass.defaultBlockState());
            }
        }
    }
}
