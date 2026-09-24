package com.nurby.tinkerslegacy.registry;

import com.nurby.tinkerslegacy.TinkersLegacy;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class TLBlocks {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(
            BuiltInRegistries.BLOCK,
            TinkersLegacy.MODID);

    public static final DeferredHolder<Block, Block> GROUT = BLOCKS.register(
            "grout",
            () -> new Block(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.CLAY)));

    public static final DeferredHolder<Block, Block> SEARED_BRICKS = BLOCKS.register(
            "seared_bricks",
            () -> new Block(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.BRICKS)));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }

    private TLBlocks() {
    }
}
