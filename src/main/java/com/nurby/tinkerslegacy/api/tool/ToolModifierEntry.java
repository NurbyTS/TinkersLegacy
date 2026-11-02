package com.nurby.tinkerslegacy.api.tool;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record ToolModifierEntry(ResourceLocation id, int points) {

        public static final Codec<ToolModifierEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        ResourceLocation.CODEC.fieldOf("id").forGetter(ToolModifierEntry::id),
                        Codec.intRange(1, Integer.MAX_VALUE).fieldOf("points").forGetter(ToolModifierEntry::points)
        ).apply(instance, ToolModifierEntry::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, ToolModifierEntry> STREAM_CODEC = StreamCodec.composite(
                        ResourceLocation.STREAM_CODEC, ToolModifierEntry::id,
                        ByteBufCodecs.VAR_INT, ToolModifierEntry::points,
                        ToolModifierEntry::new);

        public ToolModifierEntry {
                if (id == null || points < 1) {
                        throw new IllegalArgumentException("A modifier needs an ID and positive progress");
                }
        }
}
