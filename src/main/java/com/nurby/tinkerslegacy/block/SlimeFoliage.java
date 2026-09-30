package com.nurby.tinkerslegacy.block;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum SlimeFoliage implements StringRepresentable {
    GREEN("green", 0x69BC5E),
    BLUE("blue", 0x2AEC81),
    PURPLE("purple", 0xA92DFF),
    MAGMA("magma", 0xD09800);

    public static final Codec<SlimeFoliage> CODEC = StringRepresentable.fromEnum(SlimeFoliage::values);

    private final String name;
    private final int color;

    SlimeFoliage(String name, int color) {
        this.name = name;
        this.color = color;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public int color() {
        return color;
    }
}
