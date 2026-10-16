package com.nurby.tinkerslegacy.client.model.part;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.client.model.AbstractDynamicModelHandler;
import com.nurby.tinkerslegacy.library.material.MaterialDefinition;
import com.nurby.tinkerslegacy.library.part.PartDefinition;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class DynamicMaterialPartOverrideHandler extends AbstractDynamicModelHandler {
    private final ResourceLocation partId;

    public DynamicMaterialPartOverrideHandler(
            ItemOverrides nested,
            ModelBaker baker,
            IGeometryBakingContext owner,
            ModelState modelState,
            ResourceLocation partId,
            BlockModel baseModel,
            Function<Material, TextureAtlasSprite> spriteGetter
    ) {
        super(nested, baker, modelState, baseModel, spriteGetter);
        this.partId = partId;

        prebakePartModels();
    }

    private void prebakePartModels() {
        PartDefinition definition = TLRegistries.PARTS.get(partId);
        if (definition == null) return;

        baseModel.resolveParents(baker::getModel);

        // Prebake iterates generically over all registered materials for variable slots
        for (Map.Entry<ResourceKey<MaterialDefinition>, MaterialDefinition> entry : TLRegistries.MATERIALS.entrySet()) {
            MaterialDefinition material = entry.getValue();
            if (!material.parts().contains(partId)) continue;

            List<ResourceLocation> textureLocations = new ArrayList<>();
            for (int i = 0; i < definition.textureNames().size(); i++) {
                String textureName = definition.textureNames().get(i);
                ResourceLocation defaultMat = definition.defaultSlotMaterials().get(i);

                // If slot has a fixed default, use it; otherwise use the variable material
                ResourceLocation matId = (defaultMat != null) ? defaultMat : material.id();

                textureLocations.add(ResourceLocation.fromNamespaceAndPath(
                        TinkersLegacy.MODID,
                        "item/parts/" + matId.getPath() + "/" + textureName
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
        PartDefinition definition = TLRegistries.PARTS.get(partId);
        if (definition == null) return null;

        List<ResourceLocation> materials = stack.get(TLDataComponents.MATERIALS);
        List<ResourceLocation> textures = new ArrayList<>();

        // Unassembled / empty part fallback
        if (materials == null || materials.isEmpty()) {
            for (int i = 0; i < definition.textureNames().size(); i++) {
                String textureName = definition.textureNames().get(i);
                ResourceLocation defaultMat = definition.defaultSlotMaterials().get(i);
                ResourceLocation matId = (defaultMat != null) ? defaultMat : UNKNOWN_MATERIAL;

                textures.add(ResourceLocation.fromNamespaceAndPath(
                        TinkersLegacy.MODID,
                        "item/parts/" + matId.getPath() + "/" + textureName
                ));
            }
            return textures;
        }

        // Generic slot resolution: stack material -> slot default -> UNKNOWN_MATERIAL fallback
        for (int i = 0; i < definition.textureNames().size(); i++) {
            String textureName = definition.textureNames().get(i);
            ResourceLocation defaultMat = definition.defaultSlotMaterials().get(i);

            ResourceLocation matId;
            if (i < materials.size() && materials.get(i) != null) {
                matId = materials.get(i);
            } else
                matId = Objects.requireNonNullElse(defaultMat, UNKNOWN_MATERIAL);

            textures.add(ResourceLocation.fromNamespaceAndPath(
                    TinkersLegacy.MODID,
                    "item/parts/" + matId.getPath() + "/" + textureName
            ));
        }

        return textures;
    }
}