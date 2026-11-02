package com.nurby.tinkerslegacy.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class SlimyMudBlock extends Block {
        public static final MapCodec<SlimyMudBlock> CODEC = simpleCodec(SlimyMudBlock::new);

        public SlimyMudBlock(Properties properties) {
                super(properties);
        }

        @Override
        public MapCodec<SlimyMudBlock> codec() {
                return CODEC;
        }

        @Override
        public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
                entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.4, 1, 0.4));
                if (!level.isClientSide && entity instanceof LivingEntity living) {
                        living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 1));
                }
                super.stepOn(level, pos, state, entity);
        }
}
