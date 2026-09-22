package com.nurby.tinkerslegacy.library.material.datagen.util;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.io.IOException;
import java.io.InputStream;

public class ImageLoader {
    private final ExistingFileHelper existingFileHelper;

    public ImageLoader(ExistingFileHelper existingFileHelper) {
        this.existingFileHelper = existingFileHelper;
    }

    public NativeImage load(ResourceLocation location) {
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(
                location.getNamespace(),
                "textures/" + location.getPath() + ".png"
        );

        try {
            Resource resource = existingFileHelper.getResource(
                    texture,
                    PackType.CLIENT_RESOURCES
            );

            try (InputStream stream = resource.open()) {
                return NativeImage.read(stream);
            }
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to load texture " + location,
                    e
            );
        }
    }
}