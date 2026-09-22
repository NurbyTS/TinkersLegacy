package com.nurby.tinkerslegacy.client.datagen.render_info;

import com.mojang.blaze3d.platform.NativeImage;
import com.nurby.tinkerslegacy.library.material.datagen.util.ImageColor;
import com.nurby.tinkerslegacy.library.material.datagen.util.ImageInfo;
import com.nurby.tinkerslegacy.library.material.datagen.util.ImageLoader;
import com.nurby.tinkerslegacy.library.material.datagen.MaterialRenderInfo;
import net.minecraft.resources.ResourceLocation;

public class CompositeRenderInfo extends MaterialRenderInfo {

    private final ResourceLocation texture;

    public CompositeRenderInfo(ResourceLocation texture) {
        this.texture = texture;
    }

    @Override
    public void transform(
            ImageInfo image,
            ImageLoader imageLoader
    ) {
        try (NativeImage overlayImage = imageLoader.load(texture)) {
            ImageInfo overlay = new ImageInfo(overlayImage);

            for (int x = 0; x < image.width(); x++) {
                for (int y = 0; y < image.height(); y++) {
                    if (x >= overlay.width() || y >= overlay.height()) {
                        continue;
                    }

                    ImageColor baseColor = image.getPixel(x, y);
                    ImageColor overlayColor = overlay.getPixel(x, y);

                    image.setPixel(
                            x,
                            y,
                            blend(baseColor, overlayColor)
                    );
                }
            }
        }
    }

    private ImageColor blend(
            ImageColor base,
            ImageColor overlay
    ) {
        int brightness =
                (base.r() * 299 +
                        base.g() * 587 +
                        base.b() * 114) / 1000;

        double normalized = brightness / 255.0;
        int shade = (int) (
                Math.pow(normalized, 2) * 255.0
        );

        return new ImageColor(
                overlay.r() * shade / 255,
                overlay.g() * shade / 255,
                overlay.b() * shade / 255,
                base.a()
        );
    }
}