package com.nurby.tinkerslegacy.client.datagen;

import com.google.common.hash.Hashing;
import com.mojang.blaze3d.platform.NativeImage;
import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.client.model.PartModelOffsets;
import com.nurby.tinkerslegacy.library.station.StationRecipes;
import com.nurby.tinkerslegacy.registry.TLItems;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;

import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public final class PatternTextureProvider implements DataProvider {

        private final PackOutput output;
        private final ExistingFileHelper helper;

        public PatternTextureProvider(PackOutput output, ExistingFileHelper helper) {
                this.output = output;
                this.helper = helper;
        }

        // Generation
        @Override
        public CompletableFuture<?> run(CachedOutput cache) {
                ResourceLocation blank = ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID,
                                "textures/item/material/intermediary/blank_pattern.png");
                try (var stream = helper.getResource(blank, PackType.CLIENT_RESOURCES).open();
                                NativeImage pattern = NativeImage.read(stream)) {
                        for (ResourceLocation part : StationRecipes.shapes()) {
                                generate(cache, pattern, part);
                        }
                } catch (IOException exception) {
                        throw new IllegalStateException("Failed to generate stencil textures", exception);
                }
                return CompletableFuture.completedFuture(null);
        }

        private void generate(CachedOutput cache, NativeImage pattern, ResourceLocation part) throws IOException {
                String name = part.equals(TLItems.Parts.MATERIAL_SHARD.getId()) ? "shard" : part.getPath();
                ResourceLocation source = ResourceLocation.fromNamespaceAndPath(part.getNamespace(),
                                "textures/item/part_templates/" + name + ".png");
                ResourceLocation texture = texture(part);
                ResourceLocation stencilTemplate = template(part, "");
                ResourceLocation outlineTemplate = template(part, "_outline");
                var offset = PartModelOffsets.get(part);
                try (NativeImage shape = read(source);
                                NativeImage stencil = helper.exists(stencilTemplate, PackType.CLIENT_RESOURCES)
                                                ? read(stencilTemplate) : engrave(pattern, shape, offset);
                                NativeImage outline = helper.exists(outlineTemplate, PackType.CLIENT_RESOURCES)
                                                ? read(outlineTemplate) : outline(shape, offset)) {
                        writeTexture(cache, texture, stencil);
                        writeTexture(cache, ResourceLocation.fromNamespaceAndPath(part.getNamespace(),
                                        "gui/pattern/" + part.getPath()), outline);
                }
        }

        private NativeImage read(ResourceLocation texture) throws IOException {
                try (var stream = helper.getResource(texture, PackType.CLIENT_RESOURCES).open()) {
                        return NativeImage.read(stream);
                }
        }

        private static ResourceLocation template(ResourceLocation part, String suffix) {
                return ResourceLocation.fromNamespaceAndPath(part.getNamespace(),
                                "textures/item/pattern_templates/" + part.getPath() + suffix + ".png");
        }

        private void writeTexture(CachedOutput cache, ResourceLocation texture, NativeImage image) throws IOException {
                byte[] png = image.asByteArray();
                var path = output.getOutputFolder().resolve("assets").resolve(texture.getNamespace())
                                .resolve("textures").resolve(texture.getPath() + ".png");
                cache.writeIfNeeded(path, png, Hashing.sha1().hashBytes(png));
                helper.trackGenerated(texture, PackType.CLIENT_RESOURCES, ".png", "textures");
        }

        private static NativeImage outline(NativeImage shape, PartModelOffsets.Offset offset) {
                NativeImage outline = new NativeImage(shape.getWidth(), shape.getHeight(), true);
                int shiftX = offset.x() * shape.getWidth() / 16;
                int shiftY = offset.y() * shape.getHeight() / 16;
                for (int y = 0; y < shape.getHeight(); y++) {
                        for (int x = 0; x < shape.getWidth(); x++) {
                                int sourceX = x - shiftX;
                                int sourceY = y - shiftY;
                                if (opaque(shape, sourceX, sourceY) && edge(shape, sourceX, sourceY)) {
                                        outline.setPixelRGBA(x, y, 0xFF323232);
                                }
                        }
                }
                return outline;
        }

        private static NativeImage engrave(NativeImage pattern, NativeImage shape, PartModelOffsets.Offset offset) {
                NativeImage stencil = new NativeImage(pattern.getWidth(), pattern.getHeight(), true);
                stencil.copyFrom(pattern);
                int borderX = pattern.getWidth() / 8;
                int borderY = pattern.getHeight() / 8;
                int shiftX = offset.x() * shape.getWidth() / 16;
                int shiftY = offset.y() * shape.getHeight() / 16;
                // Use the original model offset and pixel scale; fitting the bounds changes the part's shape.
                for (int y = borderY; y <= pattern.getHeight() - borderY && y < pattern.getHeight(); y++) {
                        for (int x = borderX; x <= pattern.getWidth() - borderX && x < pattern.getWidth(); x++) {
                                int shapeX = x * shape.getWidth() / pattern.getWidth() - shiftX;
                                int shapeY = y * shape.getHeight() / pattern.getHeight() - shiftY;
                                int pixel = pattern.getPixelRGBA(x, y);
                                if ((pixel >>> 24) != 0 && opaque(shape, shapeX, shapeY)) {
                                        float shade = edge(shape, shapeX, shapeY) ? 0.6F : 0.5F;
                                        int red = (int) ((pixel & 255) * shade);
                                        int green = (int) ((pixel >> 8 & 255) * shade);
                                        int blue = (int) ((pixel >> 16 & 255) * shade);
                                        stencil.setPixelRGBA(x, y,
                                                        pixel & 0xFF000000 | blue << 16 | green << 8 | red);
                                }
                        }
                }
                return stencil;
        }

        private static boolean edge(NativeImage image, int x, int y) {
                return !opaque(image, x - 1, y) || !opaque(image, x + 1, y)
                                || !opaque(image, x, y - 1) || !opaque(image, x, y + 1);
        }

        private static boolean opaque(NativeImage image, int x, int y) {
                return x >= 0 && y >= 0 && x < image.getWidth() && y < image.getHeight()
                                && (image.getPixelRGBA(x, y) >>> 24) >= 64;
        }

        // Getters
        public static ResourceLocation texture(ResourceLocation part) {
                return ResourceLocation.fromNamespaceAndPath(part.getNamespace(), "item/pattern/" + part.getPath());
        }

        @Override
        public String getName() {
                return "Stencil Textures";
        }

}
