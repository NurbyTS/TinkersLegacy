package com.nurby.tinkerslegacy.registry;

import com.nurby.tinkerslegacy.TinkersLegacy;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class TLSounds {
        public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(
                        Registries.SOUND_EVENT,
                        TinkersLegacy.MODID);

        // Sounds
        public static final DeferredHolder<SoundEvent, SoundEvent> TOY_SQUEAK = add("toy_squeak");
        public static final DeferredHolder<SoundEvent, SoundEvent> TOOL_STATION = add("tool_station");
        public static final DeferredHolder<SoundEvent, SoundEvent> TOOL_FORGE = add("tool_forge");
        public static final DeferredHolder<SoundEvent, SoundEvent> SHOCKING_CHARGED = add("shocking_charged");
        public static final DeferredHolder<SoundEvent, SoundEvent> SHOCKING_DISCHARGE = add("shocking_discharge");
        public static final DeferredHolder<SoundEvent, SoundEvent> FRYPAN_BOING = add("frypan_boing");

        private TLSounds() {
        }

        // Adders
        private static DeferredHolder<SoundEvent, SoundEvent> add(String name) {
                return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                                ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, name)));
        }

        // Registration
        public static void register(IEventBus bus) {
                SOUNDS.register(bus);
        }
}
