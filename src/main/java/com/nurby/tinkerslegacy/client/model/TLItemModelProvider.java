package com.nurby.tinkerslegacy.client.model;

import com.google.gson.JsonObject;

import com.nurby.tinkerslegacy.TinkersLegacy;
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
                flatItem(TLItems.Items.IntermediaryItems.SILKY_CLOTH, "item/material/intermediary");

                flatItem(TLItems.Items.Miscellaneous.CREATIVE_MODIFIER, "item/misc");
                flatItem(TLItems.Items.Miscellaneous.BALL_OF_MOSS, "item/misc");
                flatItem(TLItems.Items.Miscellaneous.MENDING_MOSS, "item/misc");
                flatItem(TLItems.Items.Miscellaneous.EXPANDER_VERTICAL, "item/misc");
                flatItem(TLItems.Items.Miscellaneous.EXPANDER_HORIZONTAL, "item/misc");

                flatItem(TLItems.Items.RawOres.RAW_COBALT, "item/material/ore");
                flatItem(TLItems.Items.RawOres.RAW_ARDITE, "item/material/ore");

                flatItem(TLItems.Items.Nuggets.COBALT_NUGGET, "item/material/nugget");
                flatItem(TLItems.Items.Nuggets.ARDITE_NUGGET, "item/material/nugget");
                flatItem(TLItems.Items.Nuggets.ALUBRASS_NUGGET, "item/material/nugget");
                flatItem(TLItems.Items.Nuggets.KNIGHTSLIME_NUGGET, "item/material/nugget");
                flatItem(TLItems.Items.Nuggets.MANYULLYN_NUGGET, "item/material/nugget");
                flatItem(TLItems.Items.Nuggets.PIGIRON_NUGGET, "item/material/nugget");

                flatItem(TLItems.Items.Ingots.COBALT_INGOT, "item/material/ingot");
                flatItem(TLItems.Items.Ingots.ARDITE_INGOT, "item/material/ingot");
                flatItem(TLItems.Items.Ingots.ALUBRASS_INGOT, "item/material/ingot");
                flatItem(TLItems.Items.Ingots.KNIGHTSLIME_INGOT, "item/material/ingot");
                flatItem(TLItems.Items.Ingots.MANYULLYN_INGOT, "item/material/ingot");
                flatItem(TLItems.Items.Ingots.PIGIRON_INGOT, "item/material/ingot");

                flatItem(TLItems.Items.Gems.SILKY_JEWEL, "item/material/gem");
                flatItem(TLItems.Items.Gems.GREEN_SLIME_CRYSTAL, "item/material/gem");
                flatItem(TLItems.Items.Gems.BLUE_SLIME_CRYSTAL, "item/material/gem");
                flatItem(TLItems.Items.Gems.MAGMA_SLIME_CRYSTAL, "item/material/gem");

                flatItem(TLItems.Items.Bricks.SEARED_BRICK, "item/material/brick");
                flatItem(TLItems.Items.Bricks.MUD_BRICK, "item/material/brick");
                flatItem(TLItems.Items.Bricks.DRIED_BRICK, "item/material/brick");

                flatItem(TLItems.Items.Slimeballs.BLUE_SLIMEBALL, "item/slime");
                flatItem(TLItems.Items.Slimeballs.PURPLE_SLIMEBALL, "item/slime");
                flatItem(TLItems.Items.Slimeballs.MAGMA_SLIMEBALL, "item/slime");
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
                        DeferredHolder<?, ?> partHolder) {
                ResourceLocation id = partHolder.getId();

                ItemModelBuilder builder = getBuilder(id.getPath())
                                .parent(
                                                getExistingFile(
                                                                mcLoc("item/generated")));

                DynamicMaterialPartModelBuilder loader = builder.customLoader(
                                DynamicMaterialPartModelBuilder::new);

                loader.part(id);

                if (id.equals(
                                ResourceLocation.fromNamespaceAndPath(
                                                TinkersLegacy.MODID,
                                                "bolt_core"))) {
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