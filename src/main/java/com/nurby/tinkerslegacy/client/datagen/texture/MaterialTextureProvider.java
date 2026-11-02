package com.nurby.tinkerslegacy.client.datagen.texture;

import com.google.common.hash.Hashing;
import com.mojang.blaze3d.platform.NativeImage;
import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.api.material.MaterialDefinition;
import com.nurby.tinkerslegacy.api.part.PartDefinition;
import com.nurby.tinkerslegacy.api.tool.ToolDefinition;
import com.nurby.tinkerslegacy.registry.TLRegistries;
import com.nurby.tinkerslegacy.registry.tools.ToolStatTypes;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class MaterialTextureProvider implements DataProvider {

        private final PackOutput output;
        private final ExistingFileHelper existingFileHelper;

        public MaterialTextureProvider(
                        PackOutput output,
                        ExistingFileHelper existingFileHelper
        ) {
                this.output = output;
                this.existingFileHelper = existingFileHelper;
        }

        @Override
        public CompletableFuture<?> run(CachedOutput cache) {
                try {
                        for (Map.Entry<ResourceKey<MaterialDefinition>, MaterialDefinition> materialEntry : TLRegistries.MATERIALS.entrySet()) {
                                generatePartTextures(cache, materialEntry.getValue());
                                generateToolTextures(cache, materialEntry.getValue());
                        }
                } catch (IOException e) {
                        throw new RuntimeException(
                                        "Failed to generate material textures",
                                        e
                        );
                }

                return CompletableFuture.completedFuture(null);
        }

        private void generatePartTextures(
                        CachedOutput cache,
                        MaterialDefinition material
        ) throws IOException {
                for (ResourceLocation partId : material.parts()) {
                        if (partId.getPath().equals("bolt_core")) {
                                continue;
                        }

                        generatePart(cache, material, partId);
                }

                generateBoltTextures(cache, material);
        }

        private void generatePart(
                        CachedOutput cache,
                        MaterialDefinition material,
                        ResourceLocation partId
        ) throws IOException {
                PartDefinition def = TLRegistries.PARTS.get(partId);

                if (def == null) {
                        throw new IllegalArgumentException(
                                        "Unknown part " + partId +
                                                        " referenced by material " + material.id()
                        );
                }

                generateTexture(
                                cache,
                                material,
                                "part_templates/" + partId.getPath(),
                                "parts/" + material.id().getPath(),
                                partId.getPath()
                );
        }

        private void generateToolTextures(
                        CachedOutput cache,
                        MaterialDefinition material
        ) throws IOException {
                for (Map.Entry<ResourceKey<ToolDefinition>, ToolDefinition> toolEntry : TLRegistries.TOOLS.entrySet()) {
                        ToolDefinition tool = toolEntry.getValue();
                        for (int layer : tool.renderLayers()) {
                                if (!material.stats().containsKey(tool.parts().get(layer).statType())) continue;
                                generateToolTexture(cache, material, tool, layer, "");
                                generateToolTexture(cache, material, tool, layer, "_broken");
                            if (tool instanceof com.nurby.tinkerslegacy.content.tools.RangedToolDefinition) {
                                        for (int pose = 1; pose <= 3; pose++) {
                                                generateToolTexture(cache, material, tool, layer, "_pull" + pose);
                                        }
                                }
                        }
                }
        }

        private void generateToolTexture(CachedOutput cache, MaterialDefinition material,
                        ToolDefinition tool, int layer, String suffix) throws IOException {
                String folder = "tool_templates/" + tool.id().getPath() + "/layer" + layer;
                ResourceLocation variant = ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID,
                                "textures/item/" + folder + suffix + ".png");
                String source = existingFileHelper.exists(variant, PackType.CLIENT_RESOURCES) ? folder + suffix : folder;
                generateTexture(cache, material, source, "tools/" + tool.id().getPath() + "/" + material.id().getPath(),
                                "layer" + layer + suffix);
        }

        private void generateTexture(
                        CachedOutput cache,
                        MaterialDefinition material,
                        String sourcePath,
                        String outputPath,
                        String textureName
        ) throws IOException {
                ResourceLocation sourceId =
                                ResourceLocation.fromNamespaceAndPath(
                                                TinkersLegacy.MODID,
                                                "textures/item/" +
                                                                sourcePath +
                                                                ".png"
                                );

                Resource resource = existingFileHelper.getResource(
                                sourceId,
                                PackType.CLIENT_RESOURCES
                );

                NativeImage source;

                try (InputStream stream = resource.open()) {
                        source = NativeImage.read(stream);
                }

                NativeImage generated = MaterialTextureGenerator.generate(
                                source,
                                material.renderInfo(),
                                existingFileHelper
                );

                Path path = output.getOutputFolder()
                                .resolve("assets")
                                .resolve(TinkersLegacy.MODID)
                                .resolve("textures")
                                .resolve("item")
                                .resolve(outputPath)
                                .resolve(textureName + ".png");

                byte[] png = generated.asByteArray();

                cache.writeIfNeeded(
                                path,
                                png,
                                Hashing.sha1().hashBytes(png)
                );

                source.close();
                generated.close();
        }

        private void generateBoltTextures(
                        CachedOutput cache,
                        MaterialDefinition material
        ) throws IOException {
                boolean hasHead = material.stats()
                                .containsKey(ToolStatTypes.HEAD.getId());

                boolean hasArrowShaft = material.stats()
                                .containsKey(ToolStatTypes.ARROW_SHAFT.getId());

                if (hasHead) {
                        generateTexture(
                                        cache,
                                        material,
                                        "part_templates/bolt_tip",
                                        "parts/" + material.id().getPath(),
                                        "bolt_tip"
                        );
                }

                if (hasArrowShaft) {
                        generateTexture(
                                        cache,
                                        material,
                                        "part_templates/bolt_shaft",
                                        "parts/" + material.id().getPath(),
                                        "bolt_shaft"
                        );
                }
        }

        @Override
        public String getName() {
                return "Material Textures";
        }
}
