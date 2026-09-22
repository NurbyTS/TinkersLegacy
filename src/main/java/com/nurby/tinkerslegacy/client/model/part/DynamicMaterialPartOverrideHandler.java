package com.nurby.tinkerslegacy.client.model.part;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.library.material.MaterialDefinition;
import com.nurby.tinkerslegacy.library.material.layer.MaterialLayer;
import com.nurby.tinkerslegacy.library.part.PartDefinition;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLMaterials;
import com.nurby.tinkerslegacy.registry.TLRegistries;
import com.nurby.tinkerslegacy.registry.ToolStatTypes;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class DynamicMaterialPartOverrideHandler extends ItemOverrides {

    private static final Logger LOGGER =
            LoggerFactory.getLogger("TinkersLegacy/DynamicPart");

    private static final ResourceLocation BOLT_CORE =
            ResourceLocation.fromNamespaceAndPath(
                    TinkersLegacy.MODID,
                    "bolt_core"
            );

    private final Map<String, BakedModel> models =
            new HashMap<>();

    private final ItemOverrides nested;
    private final ModelBaker baker;
    private final ModelState modelState;
    private final BlockModel baseModel;
    private final ResourceLocation part;

    public DynamicMaterialPartOverrideHandler(
            ItemOverrides nested,
            ModelBaker baker,
            IGeometryBakingContext owner,
            ModelState modelState,
            ResourceLocation part,
            BlockModel baseModel
    ) {
        this.nested = nested;
        this.baker = baker;
        this.modelState = modelState;
        this.baseModel = baseModel;
        this.part = part;

        bakeModels(
                baker,
                owner,
                modelState,
                part,
                baseModel
        );
    }

    @Override
    public BakedModel resolve(
            BakedModel originalModel,
            ItemStack stack,
            @Nullable ClientLevel level,
            @Nullable LivingEntity entity,
            int seed
    ) {
        BakedModel overridden =
                nested.resolve(
                        originalModel,
                        stack,
                        level,
                        entity,
                        seed
                );

        if (overridden != originalModel) {
            return overridden;
        }

        List<MaterialLayer> materials =
                stack.get(TLDataComponents.MATERIALS);

        if (materials == null || materials.isEmpty()) {
            return originalModel;
        }

        String cacheKey = createCacheKey(materials);

        BakedModel model = models.get(cacheKey);

        if (model != null) {
            return model;
        }

        return bakeRuntimeModel(materials);
    }

    private void bakeModels(
            ModelBaker baker,
            IGeometryBakingContext owner,
            ModelState modelState,
            ResourceLocation part,
            BlockModel baseModel
    ) {
        PartDefinition definition = TLRegistries.PARTS.get(part);

        if (definition == null) {
            LOGGER.warn(
                    "Unable to bake dynamic part {}: no PartDefinition",
                    part
            );
            return;
        }

        baseModel.resolveParents(baker::getModel);

        if (part.equals(BOLT_CORE)) {
            bakeBoltCoreModels(
                    baker,
                    modelState,
                    baseModel,
                    part
            );
            return;
        }

        for (Map.Entry<ResourceKey<MaterialDefinition>, MaterialDefinition> materialEntry : TLRegistries.MATERIALS.entrySet()) {
            MaterialDefinition material = materialEntry.getValue();

            if (!material.parts().contains(part)) {
                continue;
            }

            List<MaterialLayer> layers = definition.statTypes()
                    .stream()
                    .map(statType -> new MaterialLayer(
                            statType,
                            material.id()
                    ))
                    .toList();

            bakeModel(
                    baker,
                    modelState,
                    baseModel,
                    part,
                    layers
            );
        }
    }

    private void bakeBoltCoreModels(
            ModelBaker baker,
            ModelState modelState,
            BlockModel baseModel,
            ResourceLocation part
    ) {
        MaterialDefinition iron = TLMaterials.IRON.value();

        ResourceLocation shaftType =
                ToolStatTypes.ARROW_SHAFT.getId();

        ResourceLocation headType =
                ToolStatTypes.HEAD.getId();


        for (Map.Entry<ResourceKey<MaterialDefinition>, MaterialDefinition> materialEntry : TLRegistries.MATERIALS.entrySet()) {
            MaterialDefinition material = materialEntry.getValue();

            if (!material.stats().containsKey(shaftType)) {
                continue;
            }

            List<MaterialLayer> layers = List.of(
                    new MaterialLayer(
                            shaftType,
                            material.id()
                    ),
                    new MaterialLayer(
                            headType,
                            iron.id()
                    )
            );

            bakeModel(
                    baker,
                    modelState,
                    baseModel,
                    part,
                    layers
            );
        }
    }

    private BakedModel bakeRuntimeModel(
            List<MaterialLayer> materials
    ) {
        BakedModel baked = bakeModel(
                baker,
                modelState,
                baseModel,
                part,
                materials
        );

        models.put(
                createCacheKey(materials),
                baked
        );

        return baked;
    }

    private BakedModel bakeModel(
            ModelBaker baker,
            ModelState modelState,
            BlockModel baseModel,
            ResourceLocation part,
            List<MaterialLayer> materials
    ) {
        Function<Material, TextureAtlasSprite> spriteGetter =
                material -> {
                    ResourceLocation texture =
                            material.texture();

                    ResourceLocation generated =
                            resolveGeneratedTexture(
                                    texture,
                                    part,
                                    materials
                            );

                    return baker.getModelTextureGetter().apply(
                            new Material(
                                    material.atlasLocation(),
                                    generated
                            )
                    );
                };

        return baker.bakeUncached(
                baseModel,
                modelState,
                spriteGetter
        );
    }

    private ResourceLocation resolveGeneratedTexture(
            ResourceLocation texture,
            ResourceLocation part,
            List<MaterialLayer> materials
    ) {
        String path = texture.getPath();

        String textureName;

        if (path.startsWith("item/part_silhouette/")) {
            textureName = path.substring(
                    "item/part_silhouette/".length()
            );
        }
        else if (path.startsWith("item/tool_part/")) {
            textureName = path.substring(
                    "item/tool_part/".length()
            );
        }
        else {
            return texture;
        }

        ResourceLocation statType =
                findStatType(
                        part,
                        textureName,
                        materials
                );

        if (statType == null) {
            return texture;
        }

        for (MaterialLayer layer : materials) {
            if (!layer.statType().equals(statType)) {
                continue;
            }

            return ResourceLocation.fromNamespaceAndPath(
                    TinkersLegacy.MODID,
                    "item/parts/"
                            + layer.material().getPath()
                            + "/"
                            + textureName
            );
        }

        return texture;
    }

    private ResourceLocation findStatType(
            ResourceLocation part,
            String textureName,
            List<MaterialLayer> materials
    ) {
        if (textureName.equals("bolt_shaft")) {
            return ToolStatTypes.ARROW_SHAFT.getId();
        }

        if (textureName.equals("bolt_tip")) {
            return ToolStatTypes.HEAD.getId();
        }

        PartDefinition definition = TLRegistries.PARTS.get(part);

        if (definition == null) {
            return null;
        }

        for (ResourceLocation type : definition.statTypes()) {
            for (MaterialLayer layer : materials) {
                if (layer.statType().equals(type)) {
                    return type;
                }
            }
        }

        return null;
    }

    private String createCacheKey(
            List<MaterialLayer> materials
    ) {
        StringBuilder key = new StringBuilder();

        for (MaterialLayer layer : materials) {
            if (!key.isEmpty()) {
                key.append('|');
            }

            key.append(layer.statType())
                    .append('=')
                    .append(layer.material());
        }

        return key.toString();
    }
}