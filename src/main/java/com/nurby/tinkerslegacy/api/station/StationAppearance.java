package com.nurby.tinkerslegacy.api.station;

import com.nurby.tinkerslegacy.content.block.StationBlock;
import com.nurby.tinkerslegacy.registry.TLStations;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.model.data.ModelProperty;

import javax.annotation.Nullable;

public final class StationAppearance {
        public static final ModelProperty<ResourceLocation> BASE = new ModelProperty<>();

        private StationAppearance() {
        }

        // Getters
        public static ResourceLocation defaultBase(ResourceLocation station) {
                return BuiltInRegistries.BLOCK.getKey(station.equals(TLStations.TOOL_FORGE.id()) ? Blocks.IRON_BLOCK
                                : station.equals(TLStations.PART_BUILDER.id()) ? Blocks.OAK_LOG : Blocks.OAK_PLANKS);
        }

        public static ResourceLocation validBase(@Nullable ResourceLocation base, ResourceLocation station) {
                if (base == null || !BuiltInRegistries.BLOCK.containsKey(base)) return defaultBase(station);
                var block = BuiltInRegistries.BLOCK.get(base);
                return block == Blocks.AIR || block instanceof StationBlock ? defaultBase(station) : base;
        }
}
