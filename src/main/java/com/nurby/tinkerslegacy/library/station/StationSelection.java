package com.nurby.tinkerslegacy.library.station;

import net.minecraft.resources.ResourceLocation;
import javax.annotation.Nullable;

public record StationSelection(
                ResourceLocation operation, @Nullable ResourceLocation selection, String name, int replacementIndex) {
}
