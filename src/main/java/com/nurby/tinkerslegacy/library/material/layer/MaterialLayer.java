package com.nurby.tinkerslegacy.library.material.layer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record MaterialLayer(
        ResourceLocation statType,
        ResourceLocation material
) {
    public static final Codec<MaterialLayer> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    ResourceLocation.CODEC.fieldOf("stat_type")
                            .forGetter(MaterialLayer::statType),
                    ResourceLocation.CODEC.fieldOf("material")
                            .forGetter(MaterialLayer::material)
            ).apply(instance, MaterialLayer::new));

    public static final StreamCodec<ByteBuf, MaterialLayer> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC,
                    MaterialLayer::statType,
                    ResourceLocation.STREAM_CODEC,
                    MaterialLayer::material,
                    MaterialLayer::new
            );
}