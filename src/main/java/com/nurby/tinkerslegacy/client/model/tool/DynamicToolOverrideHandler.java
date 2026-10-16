package com.nurby.tinkerslegacy.client.model.tool;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.client.model.AbstractDynamicModelHandler;
import com.nurby.tinkerslegacy.library.material.MaterialDefinition;
import com.nurby.tinkerslegacy.library.tool.ToolDefinition;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLRegistries;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;

import java.util.*;
import java.util.function.Function;

public class DynamicToolOverrideHandler extends AbstractDynamicModelHandler {
    private final ResourceLocation toolId;

    public DynamicToolOverrideHandler(
            ItemOverrides nested,
            ModelBaker baker,
            IGeometryBakingContext context,
            ModelState modelState,
            ResourceLocation toolId,
            BlockModel baseModel,
            Function<Material, TextureAtlasSprite> spriteGetter
    ) {
        super(nested, baker, modelState, baseModel, spriteGetter);
        this.toolId = toolId;

        prebakeEqualMaterialModels();
    }

    private void prebakeEqualMaterialModels() {
        ToolDefinition tool = TLRegistries.TOOLS.get(toolId);
        if (tool == null) return;

        for (Map.Entry<ResourceKey<MaterialDefinition>, MaterialDefinition> entry : TLRegistries.MATERIALS.entrySet()) {
            MaterialDefinition material = entry.getValue();
            if (!tool.supports(material)) continue;

            List<ResourceLocation> textureLocations = new ArrayList<>();
            for (int i = 0; i < tool.parts().size(); i++) {
                textureLocations.add(ResourceLocation.fromNamespaceAndPath(
                        TinkersLegacy.MODID,
                        "item/tools/" + toolId.getPath() + "/" + material.id().getPath() + "/layer" + i
                ));
            }

            BakedModel baked = bakeModel(textureLocations);
            if (baked != null) {
                baseCache.put(textureLocations, baked);
            }
        }
    }

    @Override
    protected List<ResourceLocation> resolveTextureLocations(ItemStack stack) {
        ToolDefinition toolDef = TLRegistries.TOOLS.get(toolId);
        if (toolDef == null) return null;

        List<ResourceLocation> materials = stack.get(TLDataComponents.MATERIALS);
        List<ResourceLocation> finalTextures = new ArrayList<>();

        // Unassembled / empty stack fallback -> render template "unknown" textures
        if (materials == null || materials.isEmpty()) {
            for (int i = 0; i < toolDef.parts().size(); i++) {
                finalTextures.add(ResourceLocation.fromNamespaceAndPath(
                        TinkersLegacy.MODID,
                        "item/tools/" + toolId.getPath() + "/unknown/layer" + i
                ));
            }
            return finalTextures;
        }

        // Warning on stack component length mismatch
        if (materials.size() != toolDef.parts().size()) {
            LOGGER.warn("Material count mismatch on tool stack {}: expected {}, got {}", toolId, toolDef.parts().size(), materials.size());
        }

        // 1. Base materials rendered strictly by ToolDefinition positional order
        for (int i = 0; i < toolDef.parts().size(); i++) {
            // Checked bounds AND null element safety for partial stacks
            ResourceLocation matId = (i < materials.size() && materials.get(i) != null)
                    ? materials.get(i)
                    : UNKNOWN_MATERIAL;

            finalTextures.add(ResourceLocation.fromNamespaceAndPath(
                    TinkersLegacy.MODID,
                    "item/tools/" + toolId.getPath() + "/" + matId.getPath() + "/layer" + i
            ));
        }

        // 2. Modifiers gathered, priority-sorted, and appended
        // TODO: Re-enable when modifier registry goes live
        /*List<ResourceLocation> modifierIds = stack.get(TLDataComponents.MODIFIERS);
        if (modifierIds != null && !modifierIds.isEmpty()) {
            List<RenderLayer> modifierLayers = new ArrayList<>();

            for (ResourceLocation modId : modifierIds) {
                ModifierDefinition modifier = TLRegistries.MODIFIERS.get(modId);
                if (modifier != null && modifier.hasTexture()) {
                    ResourceLocation modTexture = ResourceLocation.fromNamespaceAndPath(
                            TinkersLegacy.MODID,
                            "item/tools/" + toolId.getPath() + "/modifiers/" + modId.getPath()
                    );
                    modifierLayers.add(new RenderLayer(modTexture, modifier.renderPriority()));
                }
            }

            Collections.sort(modifierLayers);

            for (RenderLayer layer : modifierLayers) {
                finalTextures.add(layer.texture());
            }
        }*/

        return finalTextures;
    }
}