package com.nurby.tinkerslegacy.client.datagen.render_info;

import com.nurby.tinkerslegacy.library.material.datagen.util.ImageColor;
import com.nurby.tinkerslegacy.library.material.datagen.MaterialRenderInfo;

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