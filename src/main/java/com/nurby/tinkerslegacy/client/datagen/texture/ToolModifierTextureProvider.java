package com.nurby.tinkerslegacy.client.datagen.texture;

import com.google.common.hash.Hashing;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.registry.TLRegistries;
import com.nurby.tinkerslegacy.registry.tools.ToolStatTypes;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public final class ToolModifierTextureProvider implements DataProvider {
        private final PackOutput output;
        private final ExistingFileHelper files;

        public ToolModifierTextureProvider(PackOutput output, ExistingFileHelper files) {
                this.output = output;
                this.files = files;
        }

        // Generation
        @Override
        public CompletableFuture<?> run(CachedOutput cache) {
                ResourceLocation metadata = ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, "tool_modifiers.json");
                try (var reader = new InputStreamReader(files.getResource(metadata, PackType.CLIENT_RESOURCES).open(), StandardCharsets.UTF_8)) {
                        var fortified = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject(TinkersLegacy.MODID + ":fortify");
                        for (var entry : fortified.entrySet()) {
                                ResourceLocation template = ResourceLocation.parse(entry.getValue().getAsString());
                                ResourceLocation texture = template.withPath(path -> "textures/" + path + ".png");
                                if (!files.exists(texture, PackType.CLIENT_RESOURCES)) continue;
                                try (var stream = files.getResource(texture, PackType.CLIENT_RESOURCES).open();
                                                NativeImage source = NativeImage.read(stream)) {
                                        for (var material : TLRegistries.MATERIALS) {
                                                if (!material.stats().containsKey(ToolStatTypes.HEAD.getId())) continue;
                                                try (NativeImage colored = MaterialTextureGenerator.generate(source, material.renderInfo(), files)) {
                                                        byte[] png = colored.asByteArray();
                                                        var path = output.getOutputFolder().resolve("assets").resolve(TinkersLegacy.MODID)
                                                                        .resolve("textures/item/tools").resolve(entry.getKey()).resolve("modifiers/fortify")
                                                                        .resolve(material.id().getNamespace()).resolve(material.id().getPath() + ".png");
                                                        cache.writeIfNeeded(path, png, Hashing.sha1().hashBytes(png));
                                                }
                                        }
                                }
                        }
                } catch (IOException exception) {
                        return CompletableFuture.failedFuture(exception);
                }
                return CompletableFuture.completedFuture(null);
        }

        @Override
        public String getName() {
                return "Tinkers Legacy modifier overlay textures";
        }
}
