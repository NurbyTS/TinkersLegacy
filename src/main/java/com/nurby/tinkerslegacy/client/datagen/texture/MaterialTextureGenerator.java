package com.nurby.tinkerslegacy.client.datagen.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.nurby.tinkerslegacy.api.material.datagen.MaterialRenderInfo;
import com.nurby.tinkerslegacy.api.material.datagen.util.ImageInfo;
import com.nurby.tinkerslegacy.api.material.datagen.util.ImageLoader;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class MaterialTextureGenerator {

    public static NativeImage generate(
            NativeImage source,
            MaterialRenderInfo renderInfo,
            ExistingFileHelper existingFileHelper
    ) {
        NativeImage output = new NativeImage(
                source.getWidth(),
                source.getHeight(),
                true
        );

        output.copyFrom(source);

        renderInfo.transform(
                new ImageInfo(output),
                new ImageLoader(existingFileHelper)
        );

        return output;
    }
}