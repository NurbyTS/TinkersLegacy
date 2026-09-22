package com.nurby.tinkerslegacy.client.datagen.render_info;

import com.nurby.tinkerslegacy.library.material.datagen.util.ImageColor;
import com.nurby.tinkerslegacy.library.material.datagen.MaterialRenderInfo;
import net.minecraft.util.Mth;

import java.awt.Color;

public class MetalRenderInfo extends MaterialRenderInfo {

    private final int baseColor;
    private final float shinyness;
    private final float brightness;
    private final float hueshift;

    public MetalRenderInfo(
            int baseColor,
            float shinyness,
            float brightness,
            float hueshift
    ) {
        this.baseColor = baseColor;
        this.shinyness = shinyness;
        this.brightness = brightness;
        this.hueshift = hueshift;
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

        float l = getPerceptualBrightness(pixel) / 255.0f;

        ImageColor base = ImageColor.fromARGB(baseColor);

        int r = multiply(base.r(), pixel.r());
        int g = multiply(base.g(), pixel.g());
        int b = multiply(base.b(), pixel.b());

        float[] hsb = Color.RGBtoHSB(r, g, b, null);

        hsb[0] -= (0.5f - l * l) * hueshift;

        if (l > 0.9f) {
            hsb[1] = Mth.clamp(
                    hsb[1] - (l * l * shinyness),
                    0.0f,
                    1.0f
            );
        }

        if (l > 0.8f) {
            hsb[2] = Mth.clamp(
                    hsb[2] + (l * l * brightness),
                    0.0f,
                    1.0f
            );
        }

        int color = Color.HSBtoRGB(
                hsb[0],
                hsb[1],
                hsb[2]
        );

        return new ImageColor(
                (color >> 16) & 0xFF,
                (color >> 8) & 0xFF,
                color & 0xFF,
                pixel.a()
        );
    }

    private static int multiply(int a, int b) {
        return (a * b) / 255;
    }

    private static int getPerceptualBrightness(ImageColor pixel) {
        return (int) Math.sqrt(
                0.241 * pixel.r() * pixel.r() +
                        0.691 * pixel.g() * pixel.g() +
                        0.068 * pixel.b() * pixel.b()
        );
    }
}