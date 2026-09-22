package com.nurby.tinkerslegacy.library.material.stat;

import net.minecraft.resources.ResourceLocation;

public record MaterialStat(
        ResourceLocation type,
        MaterialStatMap statMap
) {
}