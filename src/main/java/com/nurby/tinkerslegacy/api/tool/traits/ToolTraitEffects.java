package com.nurby.tinkerslegacy.api.tool.traits;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.api.tool.ToolVisualEffects;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ToolTraitEffects {
        public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, TinkersLegacy.MODID);
        public static final DeferredHolder<MobEffect, MobEffect> MOMENTUM = add("momentum", false);
        public static final DeferredHolder<MobEffect, MobEffect> INSATIABLE = add("insatiable", true);
        public static final DeferredHolder<MobEffect, MobEffect> SPLINTER = add("splinter", true);
        public static final DeferredHolder<MobEffect, MobEffect> ENDERFERENCE = add("enderference", true);
        public static final DeferredHolder<MobEffect, MobEffect> MAGNETIC = EFFECTS.register("magnetic", Magnetic::new);
        public static final DeferredHolder<MobEffect, MobEffect> BLEEDING = EFFECTS.register("bleeding", Bleeding::new);

        private ToolTraitEffects() {
        }

        // Getters
        public static int level(LivingEntity entity, Holder<MobEffect> effect) {
                MobEffectInstance active = entity.getEffect(effect);
                return active == null ? 0 : active.getAmplifier();
        }

        public static void apply(LivingEntity entity, Holder<MobEffect> effect, int duration, int level) {
                entity.addEffect(new MobEffectInstance(effect, duration, level, false, false, false));
        }

        // Registration
        private static DeferredHolder<MobEffect, MobEffect> add(String name, boolean harmful) {
                return EFFECTS.register(name, () -> new Status(harmful));
        }

        public static void register(IEventBus bus) {
                EFFECTS.register(bus);
        }

        private static class Status extends MobEffect {
                private Status(boolean harmful) {
                        super(harmful ? MobEffectCategory.HARMFUL : MobEffectCategory.BENEFICIAL, 0xFFFFFF);
                }
        }

        private static final class Magnetic extends Status {
                private Magnetic() {
                        super(false);
                }

                @Override
                public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
                        return (duration & 1) == 0;
                }

                @Override
                public boolean applyEffectTick(LivingEntity entity, int amplifier) {
                        if (entity.level().isClientSide) return true;
                        double range = 1.8 + amplifier * 0.3;
                        int pulled = 0;
                        for (ItemEntity item : entity.level().getEntitiesOfClass(ItemEntity.class, entity.getBoundingBox().inflate(range))) {
                                if (!item.isAlive() || item.getItem().isEmpty()) continue;
                                if (pulled++ > 200) break;
                                Vec3 force = entity.position().subtract(item.position()).normalize().scale(0.07);
                                item.setDeltaMovement(item.getDeltaMovement().add(force));
                                item.hasImpulse = true;
                        }
                        return true;
                }
        }

        private static final class Bleeding extends Status {
                private Bleeding() {
                        super(true);
                }

                @Override
                public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
                        return duration > 0 && duration % 15 == 0;
                }

                @Override
                public boolean applyEffectTick(LivingEntity entity, int amplifier) {
                        if (!entity.level().isClientSide) {
                                ToolTraits.bleedDamage(entity, entity.getLastHurtByMob(), (amplifier + 1F) / 3F);
                                ToolVisualEffects.bleeding(entity);
                        }
                        return true;
                }
        }
}
