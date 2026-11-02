package com.nurby.tinkerslegacy.api.tool;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record ToolProgress(int promotions, long xp, int earnedSlots) {

        public static final ToolProgress EMPTY = new ToolProgress(0, 0, 0);

        public static final Codec<ToolProgress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        Codec.intRange(0, Integer.MAX_VALUE).fieldOf("promotions").forGetter(ToolProgress::promotions),
                        Codec.LONG.validate(value -> value >= 0 ? DataResult.success(value)
                                        : DataResult.error(() -> "Tool XP cannot be negative")).fieldOf("xp").forGetter(ToolProgress::xp),
                        Codec.intRange(0, Integer.MAX_VALUE).fieldOf("earned_slots").forGetter(ToolProgress::earnedSlots)
        ).apply(instance, ToolProgress::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, ToolProgress> STREAM_CODEC = StreamCodec.composite(
                        ByteBufCodecs.VAR_INT, ToolProgress::promotions,
                        ByteBufCodecs.VAR_LONG, ToolProgress::xp,
                        ByteBufCodecs.VAR_INT, ToolProgress::earnedSlots,
                        ToolProgress::new);

        public ToolProgress {
                if (promotions < 0 || xp < 0 || earnedSlots < 0) {
                        throw new IllegalArgumentException("Tool progress cannot be negative");
                }
        }
}
