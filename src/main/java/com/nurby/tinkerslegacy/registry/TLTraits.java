package com.nurby.tinkerslegacy.registry;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.api.trait.TraitDefinition;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class TLTraits {
    public static final DeferredRegister<TraitDefinition> TRAITS = DeferredRegister.create(TLRegistries.TRAIT_KEY, TinkersLegacy.MODID);

    public static final DeferredHolder<TraitDefinition, TraitDefinition>

    public static void register(IEventBus bus) {
        TRAITS.register(bus);
    }
}
