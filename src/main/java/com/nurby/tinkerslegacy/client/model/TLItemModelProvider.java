package com.nurby.tinkerslegacy.client.model;

import com.google.gson.JsonObject;
import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.library.tool.ToolDefinition;
import com.nurby.tinkerslegacy.registry.TLItems;
import com.nurby.tinkerslegacy.registry.TLTools;
import com.nurby.tinkerslegacy.registry.ToolParts;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.CustomLoaderBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.LinkedHashMap;
import java.util.Map;

public class TLItemModelProvider extends ItemModelProvider {
    private static final ResourceLocation DYNAMIC_MATERIAL_PART =
            ResourceLocation.fromNamespaceAndPath(
                    TinkersLegacy.MODID,
                    "dynamic_material_part"
            );

    private static final ResourceLocation DYNAMIC_TOOL =
            ResourceLocation.fromNamespaceAndPath(
                    TinkersLegacy.MODID,
                    "dynamic_tool"
            );

    public TLItemModelProvider(
            PackOutput output,
            ExistingFileHelper existingFileHelper
    ) {
        super(
                output,
                TinkersLegacy.MODID,
                existingFileHelper
        );
    }

    @Override
    protected void registerModels() {
        registerItems();
        registerParts();
        registerTools();
    }

    private void registerItems() {
        withExistingParent("cobalt_nugget", mcLoc("item/generated")).texture("layer0", modLoc("item/material/nugget/cobalt_nugget"));
        withExistingParent("ardite_nugget", mcLoc("item/generated")).texture("layer0", modLoc("item/material/nugget/ardite_nugget"));
        withExistingParent("alubrass_nugget", mcLoc("item/generated")).texture("layer0", modLoc("item/material/nugget/alubrass_nugget"));
        withExistingParent("knightslime_nugget", mcLoc("item/generated")).texture("layer0", modLoc("item/material/nugget/knightslime_nugget"));
        withExistingParent("manyullyn_nugget", mcLoc("item/generated")).texture("layer0", modLoc("item/material/nugget/manyullyn_nugget"));
        withExistingParent("pigiron_nugget", mcLoc("item/generated")).texture("layer0", modLoc("item/material/nugget/pigiron_nugget"));

        withExistingParent("cobalt_ingot", mcLoc("item/generated")).texture("layer0", modLoc("item/material/ingot/cobalt_ingot"));
        withExistingParent("ardite_ingot", mcLoc("item/generated")).texture("layer0", modLoc("item/material/ingot/ardite_ingot"));
        withExistingParent("alubrass_ingot", mcLoc("item/generated")).texture("layer0", modLoc("item/material/ingot/alubrass_ingot"));
        withExistingParent("knightslime_ingot", mcLoc("item/generated")).texture("layer0", modLoc("item/material/ingot/knightslime_ingot"));
        withExistingParent("manyullyn_ingot", mcLoc("item/generated")).texture("layer0", modLoc("item/material/ingot/manyullyn_ingot"));
        withExistingParent("pigiron_ingot", mcLoc("item/generated")).texture("layer0", modLoc("item/material/ingot/pigiron_ingot"));

        withExistingParent("seared_brick", mcLoc("item/generated")).texture("layer0", modLoc("item/material/brick/seared_brick"));
        withExistingParent("mud_brick", mcLoc("item/generated")).texture("layer0", modLoc("item/material/brick/mud_brick"));
        withExistingParent("dried_brick", mcLoc("item/generated")).texture("layer0", modLoc("item/material/brick/dried_brick"));
    }

    private void registerParts() {
        registerPart(ToolParts.PICKAXE_HEAD);
        registerPart(ToolParts.AXE_HEAD);
        registerPart(ToolParts.SHOVEL_HEAD);
        registerPart(ToolParts.KAMA_HEAD);
        registerPart(ToolParts.SWORD_BLADE);
        registerPart(ToolParts.HAMMER_HEAD);
        registerPart(ToolParts.BROAD_AXE_HEAD);
        registerPart(ToolParts.LARGE_SWORD_BLADE);
        registerPart(ToolParts.EXCAVATOR_HEAD);
        registerPart(ToolParts.SCYTHE_HEAD);
        registerPart(ToolParts.PAN_HEAD);
        registerPart(ToolParts.SIGN_HEAD);
        registerPart(ToolParts.LARGE_PLATE);
        registerPart(ToolParts.KNIFE_BLADE);
        registerPart(ToolParts.BOW_LIMB);
        registerPart(ToolParts.BOW_STRING);
        registerPart(ToolParts.ARROW_HEAD);
        registerPart(ToolParts.ARROW_SHAFT);
        registerPart(ToolParts.FLETCHING);
        registerPart(ToolParts.TOOL_ROD);
        registerPart(ToolParts.TOUGH_TOOL_ROD);
        registerPart(ToolParts.BINDING);
        registerPart(ToolParts.TOUGH_BINDING);
        registerPart(ToolParts.WIDE_GUARD);
        registerPart(ToolParts.CROSS_GUARD);
        registerPart(ToolParts.BOLT_CORE);
    }

    private void registerPart(
            DeferredHolder<?, ?> partHolder
    ) {
        ResourceLocation id = partHolder.getId();

        ItemModelBuilder builder =
                getBuilder(id.getPath())
                        .parent(
                                getExistingFile(
                                        mcLoc("item/generated")
                                )
                        );

        DynamicMaterialPartModelBuilder loader =
                builder.customLoader(
                        DynamicMaterialPartModelBuilder::new
                );

        loader.part(id);

        if (id.equals(
                ResourceLocation.fromNamespaceAndPath(
                        TinkersLegacy.MODID,
                        "bolt_core"
                )
        )) {
            loader.texture(
                    "layer0",
                    ResourceLocation.fromNamespaceAndPath(
                            TinkersLegacy.MODID,
                            "item/tool_part/bolt_shaft"
                    )
            );

            loader.texture(
                    "layer1",
                    ResourceLocation.fromNamespaceAndPath(
                            TinkersLegacy.MODID,
                            "item/tool_part/bolt_tip"
                    )
            );

            return;
        }

        loader.texture(
                "layer0",
                ResourceLocation.fromNamespaceAndPath(
                        TinkersLegacy.MODID,
                        "item/tool_part/" + id.getPath()
                )
        );
    }

    private void registerTools() {
        for (DeferredHolder<ToolDefinition, ? extends ToolDefinition> holder :
                TLTools.TOOLS.getEntries()) {

            ToolDefinition definition = holder.get();

            registerTool(
                    holder.getId(),
                    definition
            );
        }
    }

    private void registerTool(
            ResourceLocation id,
            ToolDefinition definition
    ) {
        ItemModelBuilder builder =
                getBuilder(id.getPath())
                        .parent(
                                getExistingFile(
                                        mcLoc("item/handheld")
                                )
                        );

        DynamicToolModelBuilder loader =
                builder.customLoader(
                        DynamicToolModelBuilder::new
                );

        loader.tool(id);

        for (int index = 0;
             index < definition.parts().size();
             index++) {

            loader.texture(
                    "layer" + index,
                    ResourceLocation.fromNamespaceAndPath(
                            TinkersLegacy.MODID,
                            "item/tools/"
                                    + id.getPath()
                                    + "/layer"
                                    + index
                    )
            );
        }
    }

    private static class DynamicMaterialPartModelBuilder
            extends CustomLoaderBuilder<ItemModelBuilder> {

        private ResourceLocation part;
        private final Map<String, ResourceLocation> textures =
                new LinkedHashMap<>();

        private DynamicMaterialPartModelBuilder(
                ItemModelBuilder parent,
                ExistingFileHelper existingFileHelper
        ) {
            super(
                    DYNAMIC_MATERIAL_PART,
                    parent,
                    existingFileHelper,
                    false
            );
        }

        private DynamicMaterialPartModelBuilder part(
                ResourceLocation part
        ) {
            this.part = part;
            return this;
        }

        private DynamicMaterialPartModelBuilder texture(
                String layer,
                ResourceLocation texture
        ) {
            textures.put(layer, texture);
            return this;
        }

        @Override
        public JsonObject toJson(JsonObject json) {
            JsonObject texturesJson = new JsonObject();

            for (Map.Entry<String, ResourceLocation> entry :
                    textures.entrySet()) {

                texturesJson.addProperty(
                        entry.getKey(),
                        entry.getValue().toString()
                );
            }

            json.addProperty(
                    "part",
                    part.toString()
            );

            json.add(
                    "textures",
                    texturesJson
            );

            return super.toJson(json);
        }
    }

    private static class DynamicToolModelBuilder
            extends CustomLoaderBuilder<ItemModelBuilder> {

        private ResourceLocation tool;
        private final Map<String, ResourceLocation> textures =
                new LinkedHashMap<>();

        private DynamicToolModelBuilder(
                ItemModelBuilder parent,
                ExistingFileHelper existingFileHelper
        ) {
            super(
                    DYNAMIC_TOOL,
                    parent,
                    existingFileHelper,
                    false
            );
        }

        private DynamicToolModelBuilder tool(
                ResourceLocation tool
        ) {
            this.tool = tool;
            return this;
        }

        private DynamicToolModelBuilder texture(
                String layer,
                ResourceLocation texture
        ) {
            textures.put(layer, texture);
            return this;
        }

        @Override
        public JsonObject toJson(JsonObject json) {
            JsonObject texturesJson = new JsonObject();

            for (Map.Entry<String, ResourceLocation> entry :
                    textures.entrySet()) {

                texturesJson.addProperty(
                        entry.getKey(),
                        entry.getValue().toString()
                );
            }

            json.addProperty(
                    "tool",
                    tool.toString()
            );

            json.add(
                    "textures",
                    texturesJson
            );

            return super.toJson(json);
        }
    }
}