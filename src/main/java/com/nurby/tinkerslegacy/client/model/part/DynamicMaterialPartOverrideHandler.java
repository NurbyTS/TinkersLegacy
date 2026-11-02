package com.nurby.tinkerslegacy.client.model.part;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.api.material.MaterialDefinition;
import com.nurby.tinkerslegacy.api.part.PartDefinition;
import com.nurby.tinkerslegacy.client.model.AbstractDynamicModelHandler;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLMaterials;
import com.nurby.tinkerslegacy.registry.TLRegistries;
import com.nurby.tinkerslegacy.registry.tools.ToolStatTypes;
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

    private static final ResourceLocation BOLT_CORE = ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, "bolt_core");

    private final ResourceLocation partId;

    public DynamicMaterialPartOverrideHandler(
            ItemOverrides nested,
            ModelBaker baker,
            IGeometryBakingContext owner,
            ModelState modelState, ResourceLocation partId, BlockModel baseModel, Function<Material, TextureAtlasSprite> spriteGetter
    ) {
        super(nested, baker, modelState, baseModel, spriteGetter);
        this.partId = partId;

        prebakePartModels();
    }

    private void prebakePartModels() {
        PartDefinition definition = TLRegistries.PARTS.get(partId);
        if (definition == null) {
            LOGGER.warn("Unable to bake dynamic part {}: no PartDefinition", partId);
            return;
        }

        baseModel.resolveParents(baker::getModel);

        if (partId.equals(BOLT_CORE)) {
            prebakeBoltCoreModels();
            return;
        }

        for (Map.Entry<ResourceKey<MaterialDefinition>, MaterialDefinition> entry : TLRegistries.MATERIALS.entrySet()) {
            MaterialDefinition material = entry.getValue();
            if (!material.parts().contains(partId))
                continue;

            List<ResourceLocation> textureLocations = resolveTexturesForPart(definition, List.of(material.id()));
            BakedModel baked = bakeModel(textureLocations);
            if (baked != null) {
                baseCache.put(textureLocations, baked);
            }
        }
    }

    private void prebakeBoltCoreModels() {
        MaterialDefinition iron = TLMaterials.IRON.value();
        ResourceLocation shaftType = ToolStatTypes.ARROW_SHAFT.getId();

        for (Map.Entry<ResourceKey<MaterialDefinition>, MaterialDefinition> entry : TLRegistries.MATERIALS.entrySet()) {
            MaterialDefinition material = entry.getValue();
            if (!material.stats().containsKey(shaftType))
                continue;

            List<ResourceLocation> textureLocations = List.of(ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, "item/parts/" + material.id().getPath() + "/bolt_shaft"), ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, "item/parts/" + iron.id().getPath() + "/bolt_tip")
            );

            BakedModel baked = bakeModel(textureLocations);
            if (baked != null) {
                baseCache.put(textureLocations, baked);
            }
        }
    }

    @Override
    protected List<ResourceLocation> resolveTextureLocations(ItemStack stack) {
        PartDefinition definition = TLRegistries.PARTS.get(partId);
        if (definition == null)
            return null;

        List<ResourceLocation> layers = stack.get(TLDataComponents.MATERIALS);
        layers = (layers != null) ? layers : List.of();

        // Special case logic for Bolt Core
        if (partId.equals(BOLT_CORE)) {
            return resolveBoltCoreTextures(layers);
        }

        return resolveTexturesForPart(definition, layers);
    }

    private List<ResourceLocation> resolveTexturesForPart(PartDefinition definition, List<ResourceLocation> materialIds) {
        List<ResourceLocation> textures = new ArrayList<>();

        // 1. Empty / Unassembled fallback handling
        if (materialIds.isEmpty()) {
            for (int i = 0; i < definition.textureNames().size(); i++) {
                String textureName = definition.textureNames().get(i);
                ResourceLocation defaultMat = definition.defaultSlotMaterials().get(i);
                ResourceLocation matId = Objects.requireNonNullElse(defaultMat, UNKNOWN_MATERIAL);

                textures.add(ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, "item/parts/" + matId.getPath() + "/" + textureName));
            }
            return textures;
        }

        // 2. Generic slot resolution: input material -> slot default -> UNKNOWN_MATERIAL fallback
        for (int i = 0; i < definition.textureNames().size(); i++) {
            String textureName = definition.textureNames().get(i);
            ResourceLocation defaultMat = definition.defaultSlotMaterials().get(i);

            ResourceLocation matId;
            if (i < materialIds.size() && materialIds.get(i) != null) {
                matId = materialIds.get(i);
            } else {
                matId = Objects.requireNonNullElse(defaultMat, UNKNOWN_MATERIAL);
            }

            textures.add(ResourceLocation.fromNamespaceAndPath(
                    TinkersLegacy.MODID, "item/parts/" + matId.getPath() + "/" + textureName));
        }

        return textures;
    }

    private List<ResourceLocation> resolveBoltCoreTextures(List<ResourceLocation> materialIds) {
        ResourceLocation shaftMat = (materialIds.size() > 0 && materialIds.get(0) != null) ? materialIds.get(0) : UNKNOWN_MATERIAL;

        ResourceLocation headMat = (materialIds.size() > 1 && materialIds.get(1) != null) ? materialIds.get(1) : TLMaterials.IRON.getId();

        return List.of(ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, "item/parts/" + shaftMat.getPath() + "/bolt_shaft"), ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, "item/parts/" + headMat.getPath() + "/bolt_tip"));
    }
}