package com.nurby.tinkerslegacy.library.station;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;

public record StationResult(ItemStack output, List<ItemStack> inputsAfter, Component error) {

        public StationResult {
                output = output.copy();
                inputsAfter = inputsAfter.stream().map(ItemStack::copy).toList();
                Objects.requireNonNull(error);
        }

        public boolean valid() {
                return !output.isEmpty();
        }

        public static StationResult invalid(Component error) {
                return new StationResult(ItemStack.EMPTY, List.of(), error);
        }

        public static StationResult invalid(String key) {
                return invalid(Component.translatable(key));
        }
}
