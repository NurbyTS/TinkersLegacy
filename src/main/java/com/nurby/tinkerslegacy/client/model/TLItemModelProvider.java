package com.nurby.tinkerslegacy.client.model;

import com.google.gson.JsonObject;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.library.part.PartDefinition;
import com.nurby.tinkerslegacy.library.tool.ToolDefinition;
import com.nurby.tinkerslegacy.registry.TLItems;
import com.nurby.tinkerslegacy.registry.TLTools;
import com.nurby.tinkerslegacy.registry.ToolParts;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import net.neoforged.neoforge.client.model.generators.CustomLoaderBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.LinkedHashMap;
import java.util.Map;

public class TLItemModelProvider extends ItemModelProvider {
        private static final ResourceLocation DYNAMIC_MATERIAL_PART = ResourceLocation.fromNamespaceAndPath(
                        TinkersLegacy.MODID,
                        "dynamic_material_part");

        private static final ResourceLocation DYNAMIC_TOOL = ResourceLocation.fromNamespaceAndPath(
                        TinkersLegacy.MODID,
                        "dynamic_tool");

        public TLItemModelProvider(
                        PackOutput output,
                        ExistingFileHelper existingFileHelper) {
                super(
                                output,
                                TinkersLegacy.MODID,
                                existingFileHelper);
        }

        @Override
        protected void registerModels() {
                registerItems();
                registerParts();
                registerTools();
        }

        private void registerItems() {
                flatItems(TLItems.Items.IntermediaryItems.ALL, "item/material/intermediary");

                flatItems(TLItems.Items.Miscellaneous.ALL, "item/misc");

                flatItems(TLItems.Items.RawOres.ALL, "item/material/ore");

                flatItems(TLItems.Items.Nuggets.ALL, "item/material/nugget");

                flatItems(TLItems.Items.Ingots.ALL, "item/material/ingot");

                flatItems(TLItems.Items.Gems.ALL, "item/material/gem");

                flatItems(TLItems.Items.Bricks.ALL, "item/material/brick");

                flatItems(TLItems.Items.Slimeballs.ALL, "item/slime");
        }

        private void registerParts() {
                for (DeferredHolder<PartDefinition, ? extends PartDefinition> holder : ToolParts.PARTS.getEntries()) {
                        registerPart(holder);
                }
        }

        private void registerPart(
                        DeferredHolder<PartDefinition, ? extends PartDefinition> partHolder) {
                ResourceLocation id = partHolder.getId();

                ItemModelBuilder builder = getBuilder(id.getPath())
                                .parent(
                                                getExistingFile(
                                                                mcLoc("item/generated")));

                DynamicMaterialPartModelBuilder loader = builder.customLoader(
                                DynamicMaterialPartModelBuilder::new);

                loader.part(id);

                if (id.equals(ToolParts.get(ToolParts.Type.BOLT_CORE).getId())) {
                        loader.texture(
                                        "layer0",
                                        ResourceLocation.fromNamespaceAndPath(
                                                        TinkersLegacy.MODID,
                                                        "item/tool_part/bolt_shaft"));

                        loader.texture(
                                        "layer1",
                                        ResourceLocation.fromNamespaceAndPath(
                                                        TinkersLegacy.MODID,
                                                        "item/tool_part/bolt_tip"));

                        return;
                }

                loader.texture(
                                "layer0",
                                ResourceLocation.fromNamespaceAndPath(
                                                TinkersLegacy.MODID,
                                                "item/tool_part/" + id.getPath()));
        }

        private void registerTools() {
                for (DeferredHolder<ToolDefinition, ? extends ToolDefinition> holder : TLTools.TOOLS.getEntries()) {

                        ToolDefinition definition = holder.get();

                        registerTool(
                                        holder.getId(),
                                        definition);
                }
        }

        private void registerTool(
                        ResourceLocation id,
                        ToolDefinition definition) {
                ItemModelBuilder builder = getBuilder(id.getPath())
                                .parent(
                                                getExistingFile(
                                                                mcLoc("item/handheld")));

                DynamicToolModelBuilder loader = builder.customLoader(
                                DynamicToolModelBuilder::new);

                loader.tool(id);

                for (int index = 0; index < definition.parts().size(); index++) {

                        loader.texture(
                                        "layer" + index,
                                        ResourceLocation.fromNamespaceAndPath(
                                                        TinkersLegacy.MODID,
                                                        "item/tools/"
                                                                        + id.getPath()
                                                                        + "/layer"
                                                                        + index));
                }
        }

        private void flatItems(
                        Iterable<? extends DeferredHolder<Item, ? extends Item>> items,
                        String folder) {
                for (var item : items) {
                        flatItem(item, folder);
                }
        }

        private void flatItem(
                        DeferredHolder<Item, ? extends Item> item,
                        String folder) {
                String name = item.getId().getPath();

                withExistingParent(name, mcLoc("item/generated")).texture("layer0", modLoc(folder + "/" + name));
        }

        private static class DynamicMaterialPartModelBuilder
                        extends CustomLoaderBuilder<ItemModelBuilder> {

                private ResourceLocation part;
                private final Map<String, ResourceLocation> textures = new LinkedHashMap<>();

                private DynamicMaterialPartModelBuilder(
                                ItemModelBuilder parent,
                                ExistingFileHelper existingFileHelper) {
                        super(
                                        DYNAMIC_MATERIAL_PART,
                                        parent,
                                        existingFileHelper,
                                        false);
                }

                private DynamicMaterialPartModelBuilder part(
                                ResourceLocation part) {
                        this.part = part;
                        return this;
                }

                private DynamicMaterialPartModelBuilder texture(
                                String layer,
                                ResourceLocation texture) {
                        textures.put(layer, texture);
                        return this;
                }

                @Override
                public JsonObject toJson(JsonObject json) {
                        JsonObject texturesJson = new JsonObject();

                        for (Map.Entry<String, ResourceLocation> entry : textures.entrySet()) {

                                texturesJson.addProperty(
                                                entry.getKey(),
                                                entry.getValue().toString());
                        }

                        json.addProperty(
                                        "part",
                                        part.toString());

                        json.add(
                                        "textures",
                                        texturesJson);

                        return super.toJson(json);
                }
        }

        private static class DynamicToolModelBuilder
                        extends CustomLoaderBuilder<ItemModelBuilder> {

                private ResourceLocation tool;
                private final Map<String, ResourceLocation> textures = new LinkedHashMap<>();

                private DynamicToolModelBuilder(
                                ItemModelBuilder parent,
                                ExistingFileHelper existingFileHelper) {
                        super(
                                        DYNAMIC_TOOL,
                                        parent,
                                        existingFileHelper,
                                        false);
                }

                private DynamicToolModelBuilder tool(
                                ResourceLocation tool) {
                        this.tool = tool;
                        return this;
                }

                private DynamicToolModelBuilder texture(
                                String layer,
                                ResourceLocation texture) {
                        textures.put(layer, texture);
                        return this;
                }

                @Override
                public JsonObject toJson(JsonObject json) {
                        JsonObject texturesJson = new JsonObject();

                        for (Map.Entry<String, ResourceLocation> entry : textures.entrySet()) {

                                texturesJson.addProperty(
                                                entry.getKey(),
                                                entry.getValue().toString());
                        }

                        json.addProperty(
                                        "tool",
                                        tool.toString());

                        json.add(
                                        "textures",
                                        texturesJson);

                        return super.toJson(json);
                }
        }
}