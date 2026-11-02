package com.nurby.tinkerslegacy.client.datagen.texture.render_info;

import com.nurby.tinkerslegacy.api.material.datagen.MaterialRenderInfo;
import com.nurby.tinkerslegacy.api.material.datagen.util.ImageColor;

public class TintMaterialRenderInfo extends MaterialRenderInfo {

    private final ImageColor tint;

    public TintMaterialRenderInfo(int tint) {
        this.tint = ImageColor.fromARGB(tint);
    }

    @Override
    protected ImageColor transformPixel(
            ImageColor pixel,
            int x,
            int y
    ) {
        if (pixel.a() == 0) {
            return pixel;
        }

        return new ImageColor(
                multiply(pixel.r(), tint.r()),
                multiply(pixel.g(), tint.g()),
                multiply(pixel.b(), tint.b()),
                pixel.a()
        );
    }

    private static int multiply(int a, int b) {
        return (a * b) / 255;
    }
}