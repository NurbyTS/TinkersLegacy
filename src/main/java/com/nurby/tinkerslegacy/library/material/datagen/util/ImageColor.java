package com.nurby.tinkerslegacy.library.material.datagen.util;

public record ImageColor(int r, int g, int b, int a) {

    public ImageColor(int r, int g, int b) {
        this(r, g, b, 255);
    }

    public int toARGB() {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public int toABGR() {
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    public static ImageColor fromARGB(int argb) {
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;

        return new ImageColor(r, g, b, a);
    }

    public static ImageColor fromABGR(int abgr) {
        int a = (abgr >> 24) & 0xFF;
        int b = (abgr >> 16) & 0xFF;
        int g = (abgr >> 8) & 0xFF;
        int r = abgr & 0xFF;

        return new ImageColor(r, g, b, a);
    }
}