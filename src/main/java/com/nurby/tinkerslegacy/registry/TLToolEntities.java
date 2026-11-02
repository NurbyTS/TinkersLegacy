package com.nurby.tinkerslegacy.registry;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.entity.TLToolProjectile;
import com.nurby.tinkerslegacy.entity.TraitBlueSlime;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Monster;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class TLToolEntities {
        public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(
                        Registries.ENTITY_TYPE, TinkersLegacy.MODID);
        public static final DeferredHolder<EntityType<?>, EntityType<TLToolProjectile>> PROJECTILE = ENTITIES.register(
                        "tool_projectile", () -> EntityType.Builder.<TLToolProjectile>of(TLToolProjectile::new, MobCategory.MISC)
                                        .sized(0.5F, 0.5F).clientTrackingRange(8).updateInterval(1)
                                        .build(TinkersLegacy.MODID + ":tool_projectile"));

        public static final DeferredHolder<EntityType<?>, EntityType<TraitBlueSlime>> BLUE_SLIME = ENTITIES.register(
                        "blue_slime", () -> EntityType.Builder.<TraitBlueSlime>of(TraitBlueSlime::new, MobCategory.MONSTER)
                                        .sized(0.52F, 0.52F).eyeHeight(0.325F).spawnDimensionsScale(4.0F)
                                        .clientTrackingRange(10).build(TinkersLegacy.MODID + ":blue_slime"));

        private TLToolEntities() {
        }

        // Registration
        public static void register(IEventBus modBus) {
                ENTITIES.register(modBus);
                modBus.addListener(TLToolEntities::attributes);
        }

        private static void attributes(EntityAttributeCreationEvent event) {
                event.put(BLUE_SLIME.get(), Monster.createMonsterAttributes().build());
        }
}
