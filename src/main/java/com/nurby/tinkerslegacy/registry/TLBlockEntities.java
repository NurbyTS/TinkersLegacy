package com.nurby.tinkerslegacy.registry;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.content.block.StationBlock;
import com.nurby.tinkerslegacy.content.block.entity.StationBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class TLBlockEntities {
        public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(
                        Registries.BLOCK_ENTITY_TYPE, TinkersLegacy.MODID);

        public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StationBlockEntity>> STATION = BLOCK_ENTITIES.register(
                        "station", () -> BlockEntityType.Builder.of(
                                        StationBlockEntity::new,
                                        BuiltInRegistries.BLOCK.stream().filter(block -> block instanceof StationBlock).toArray(Block[]::new))
                                        .build(null));

        private TLBlockEntities() {
        }

        // Registration
        public static void register(IEventBus modBus) {
                BLOCK_ENTITIES.register(modBus);
        }
}
