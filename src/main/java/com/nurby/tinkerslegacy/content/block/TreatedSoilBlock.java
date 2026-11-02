package com.nurby.tinkerslegacy.content.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class TreatedSoilBlock extends Block {
        public static final MapCodec<TreatedSoilBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                        propertiesCodec(),
                        Codec.BOOL.fieldOf("consecrated").forGetter(TreatedSoilBlock::consecrated))
                        .apply(instance, TreatedSoilBlock::new));

        private final boolean consecrated;

        public TreatedSoilBlock(Properties properties, boolean consecrated) {
                super(properties);
                this.consecrated = consecrated;
        }

        @Override
        public MapCodec<TreatedSoilBlock> codec() {
                return CODEC;
        }

        public boolean consecrated() {
                return consecrated;
        }

        @Override
        public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
                if (!level.isClientSide && entity instanceof Mob mob && mob.getType().is(EntityTypeTags.UNDEAD)) {
                        if (consecrated) {
                                mob.hurt(level.damageSources().magic(), 1);
                                mob.igniteForSeconds(1);
                        } else {
                                mob.heal(1);
                        }
                }
                super.stepOn(level, pos, state, entity);
        }
}
