package com.nurby.tinkerslegacy.library.tool;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record ToolStats(
        float attackDamage,
        float attackSpeedMultiplier,
        float miningSpeed,
        int durability,
        int freeModifiers,
        int harvestLevel
) {
    public static final Codec<ToolStats> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.FLOAT.fieldOf("attack_damage")
                            .forGetter(ToolStats::attackDamage),

                    Codec.FLOAT.fieldOf("attack_speed_multiplier")
                            .forGetter(ToolStats::attackSpeedMultiplier),

                    Codec.FLOAT.fieldOf("mining_speed")
                            .forGetter(ToolStats::miningSpeed),

                    Codec.INT.fieldOf("durability")
                            .forGetter(ToolStats::durability),

                    Codec.INT.fieldOf("free_modifiers")
                            .forGetter(ToolStats::freeModifiers),

                    Codec.INT.fieldOf("harvest_level")
                            .forGetter(ToolStats::harvestLevel)
            ).apply(instance, ToolStats::new));

    public static final StreamCodec<ByteBuf, ToolStats> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.FLOAT,
                    ToolStats::attackDamage,

                    ByteBufCodecs.FLOAT,
                    ToolStats::attackSpeedMultiplier,

                    ByteBufCodecs.FLOAT,
                    ToolStats::miningSpeed,

                    ByteBufCodecs.VAR_INT,
                    ToolStats::durability,

                    ByteBufCodecs.VAR_INT,
                    ToolStats::freeModifiers,

                    ByteBufCodecs.VAR_INT,
                    ToolStats::harvestLevel,

                    ToolStats::new
            );
}