package com.nurby.tinkerslegacy.api.material.stat;

import net.minecraft.resources.ResourceLocation;

public record MaterialStat(
        ResourceLocation type,
        MaterialStatMap statMap
) {
}