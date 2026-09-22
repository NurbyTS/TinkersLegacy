package com.nurby.tinkerslegacy.library.material.datagen.util;

import com.mojang.blaze3d.platform.NativeImage;

public class ImageInfo {
    private final NativeImage image;

    public ImageInfo(NativeImage image) {
        this.image = image;
    }

    public NativeImage output() {
        return image;
    }

    public int width() {
        return image.getWidth();
    }

    public int height() {
        return image.getHeight();
    }

    public ImageColor getPixel(int x, int y) {
        return ImageColor.fromABGR(image.getPixelRGBA(x, y));
    }

    public void setPixel(int x, int y, ImageColor color) {
        image.setPixelRGBA(x, y, color.toABGR());
    }
}