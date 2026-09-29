package com.nurby.tinkerslegacy.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Soft, non-sticky slime: entities sink into the full-size visual cube before bouncing. */
public class CongealedSlimeBlock extends Block {
    public static final MapCodec<CongealedSlimeBlock> CODEC = simpleCodec(CongealedSlimeBlock::new);
    private static final VoxelShape COLLISION = Block.box(0, 0, 0, 16, 10, 16);

    public CongealedSlimeBlock(Properties properties) { super(properties); }

    @Override
    public MapCodec<CongealedSlimeBlock> codec() { return CODEC; }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION;
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float distance) {
        entity.causeFallDamage(distance, 0.0F, level.damageSources().fall());
    }

    @Override
    public void updateEntityAfterFallOn(BlockGetter level, Entity entity) {
        Vec3 motion = entity.getDeltaMovement();
        if ((entity instanceof LivingEntity || entity instanceof ItemEntity) && motion.y < -0.25) {
            entity.setDeltaMovement(motion.x, -motion.y * 1.2, motion.z);
            entity.fallDistance = 0;
            if (entity instanceof ItemEntity) entity.setOnGround(false);
        } else {
            super.updateEntityAfterFallOn(level, entity);
        }
    }
}
