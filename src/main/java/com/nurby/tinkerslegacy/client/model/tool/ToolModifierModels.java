package com.nurby.tinkerslegacy.client.model.tool;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.library.tool.ToolModifierEntry;
import com.nurby.tinkerslegacy.library.tool.ToolModifiers;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ToolModifierModels {
        private final ResourceLocation tool;
        private final Map<ResourceLocation, Map<String, ResourceLocation>> textures;
        private final Set<ResourceLocation> available;

        public ToolModifierModels(ResourceLocation tool, ResourceManager resources) {
                this.tool = tool;
                Map<ResourceLocation, Map<String, ResourceLocation>> definitions = new LinkedHashMap<>();
                ResourceLocation metadata = ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, "tool_modifiers.json");
                resources.getResource(metadata).ifPresent(resource -> {
                        try (var reader = resource.openAsReader()) {
                                for (var modifier : JsonParser.parseReader(reader).getAsJsonObject().entrySet()) {
                                        Map<String, ResourceLocation> poses = new LinkedHashMap<>();
                                        for (var pose : modifier.getValue().getAsJsonObject().entrySet()) {
                                                poses.put(pose.getKey(), ResourceLocation.parse(pose.getValue().getAsString()));
                                        }
                                        definitions.put(ResourceLocation.parse(modifier.getKey()), Map.copyOf(poses));
                                }
                        } catch (IOException exception) {
                                throw new IllegalStateException("Cannot read modifier textures for " + tool, exception);
                        }
                });
                textures = Map.copyOf(definitions);
                available = Set.copyOf(resources.listResources("textures/item/tools/" + tool.getPath() + "/modifiers",
                                location -> location.getPath().endsWith(".png")).keySet());
        }

        // Textures
        public List<ResourceLocation> textures(List<ToolModifierEntry> modifiers, String pose) {
                List<ResourceLocation> result = new ArrayList<>();
                for (ToolModifierEntry modifier : modifiers) {
                        ResourceLocation modifierId = modifier.id();
                        boolean fortified = ToolModifiers.isFortify(modifierId);
                        Map<String, ResourceLocation> mapping = textures.get(fortified ? ToolModifiers.FORTIFY : modifierId);
                        if (mapping == null) continue;
                        String drawing = pose.startsWith("_pull") ? pose.substring(5) : "";
                        ResourceLocation texture = mapping.getOrDefault(tool.getPath() + drawing, mapping.get(tool.getPath()));
                        if (texture == null) continue;
                        if (fortified) {
                                texture = ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, "item/tools/" + tool.getPath()
                                                + "/modifiers/" + modifierId.getPath());
                        }
                        ResourceLocation file = texture.withPath(path -> "textures/" + path + ".png");
                        if (available.contains(file) && !result.contains(texture)) result.add(texture);
                }
                return List.copyOf(result);
        }

        public static BlockModel template(ResourceLocation texture) {
                JsonObject json = new JsonObject();
                json.addProperty("parent", "minecraft:item/generated");
                JsonObject textures = new JsonObject();
                textures.addProperty("layer0", texture.toString());
                json.add("textures", textures);
                return BlockModel.fromString(json.toString());
        }
}
