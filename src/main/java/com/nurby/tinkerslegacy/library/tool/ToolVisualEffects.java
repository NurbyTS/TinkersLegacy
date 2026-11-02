package com.nurby.tinkerslegacy.library.tool;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class ToolVisualEffects {
        private static final DustParticleOptions THORNS = new DustParticleOptions(Vec3.fromRGB24(0x5A8A28).toVector3f(), 1);
        private static final DustParticleOptions BLOOD = new DustParticleOptions(Vec3.fromRGB24(0xB51E27).toVector3f(), 1);
        private static final int MAX_TRAIL_PARTICLES = 8;
        private static final ThreadLocal<RandomSource> RANDOM = ThreadLocal.withInitial(RandomSource::create);

        private ToolVisualEffects() {
        }

        // Impacts
        public static void shocking(LivingEntity target) {
                around(target, ParticleTypes.ELECTRIC_SPARK, 5);
        }

        public static void thorns(LivingEntity target) {
                around(target, THORNS, 1);
        }

        public static void bleeding(LivingEntity target) {
                around(target, BLOOD, 1);
        }

        private static void around(Entity target, ParticleOptions particle, int count) {
                if (!(target.level() instanceof ServerLevel level)) return;
                level.sendParticles(particle, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                                count, target.getBbWidth() * 0.3, target.getBbHeight() * 0.3, target.getBbWidth() * 0.3, 0.02);
        }

        public static void sweep(LivingEntity attacker, double distance) {
                if (!(attacker.level() instanceof ServerLevel level)) return;
                Vec3 look = attacker.getLookAngle();
                level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                                attacker.getX() + look.x * distance, attacker.getY() + 1, attacker.getZ() + look.z * distance,
                                1, 0, 0, 0, 0);
        }

        // Mining
        public static void autosmelt(ServerLevel level, BlockPos pos) {
                RandomSource random = RANDOM.get();
                for (int particle = 0; particle < 3; particle++) {
                        level.sendParticles(ParticleTypes.FLAME,
                                        pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(), pos.getZ() + random.nextDouble(),
                                        0, 0, 0, 0, 0);
                }
        }

        // Trails
        public static void hovering(Entity projectile) {
                if (!(projectile.level() instanceof ServerLevel level)) return;
                RandomSource random = RANDOM.get();
                if (random.nextInt(2) != 0) return;
                level.sendParticles(ParticleTypes.FLAME, projectile.getX(), projectile.getY(), projectile.getZ(),
                                0, (random.nextFloat() - 0.5F) / 15, random.nextFloat() / 15, (random.nextFloat() - 0.5F) / 15, 1);
        }

        public static void endspeed(Entity projectile, Vec3 start, double traveled) {
                if (!(projectile.level() instanceof ServerLevel level) || traveled <= 0.3) return;
                int count = Math.min(MAX_TRAIL_PARTICLES, (int) (traveled / 0.3));
                // Accelerated collision steps can number in the thousands; sample a bounded trail once per tick.
                for (int particle = 1; particle <= count; particle++) {
                        Vec3 position = start.lerp(projectile.position(), (double) particle / count);
                        level.sendParticles(ParticleTypes.END_ROD, position.x, position.y, position.z,
                                        0, 0, 0, 0, 0);
                }
        }
}
