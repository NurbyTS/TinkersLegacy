package com.nurby.tinkerslegacy.api.material.datagen;

import com.nurby.tinkerslegacy.api.material.datagen.util.ImageColor;
import com.nurby.tinkerslegacy.api.material.datagen.util.ImageInfo;
import com.nurby.tinkerslegacy.api.material.datagen.util.ImageLoader;

public abstract class MaterialRenderInfo {

    public void transform(
            ImageInfo image,
            ImageLoader loader
    ) {
        for (int x = 0; x < image.width(); x++) {
            for (int y = 0; y < image.height(); y++) {
                ImageColor pixel = image.getPixel(x, y);

                image.setPixel(x, y, transformPixel(pixel, x, y));
            }
        }
    }


    protected ImageColor transformPixel(
            ImageColor pixel,
            int x,
            int y
    ) {
        return pixel;
    }
}