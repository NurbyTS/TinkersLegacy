package com.nurby.tinkerslegacy.client.model;

import com.mojang.datafixers.util.Either;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.model.geometry.UnbakedGeometryHelper;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;

public abstract class AbstractDynamicModelHandler extends ItemOverrides {
    protected static final Logger LOGGER = LoggerFactory.getLogger("TinkersLegacy/DynamicModel");
    protected static final int MIXED_CACHE_LIMIT = 512;
    public static final ResourceLocation UNKNOWN_MATERIAL = ResourceLocation.fromNamespaceAndPath("tinkerslegacy", "unknown");

    protected final ItemOverrides nested;
    protected final ModelBaker baker;
    protected final ModelState modelState;
    protected final BlockModel baseModel;
    protected final Function<Material, TextureAtlasSprite> spriteGetter;

    protected final Map<List<ResourceLocation>, BakedModel> baseCache = new HashMap<>();
    protected final Map<List<ResourceLocation>, BakedModel> mixedCache =
            new LinkedHashMap<>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<List<ResourceLocation>, BakedModel> eldest) {
                    return size() > MIXED_CACHE_LIMIT;
                }
            };

    public AbstractDynamicModelHandler(
            ItemOverrides nested,
            ModelBaker baker,
            ModelState modelState,
            BlockModel baseModel,
            Function<Material, TextureAtlasSprite> spriteGetter
    ) {
        this.nested = nested;
        this.baker = baker;
        this.modelState = modelState;
        this.baseModel = baseModel;
        this.spriteGetter = spriteGetter;
    }

    @Override
    public BakedModel resolve(
            BakedModel originalModel,
            ItemStack stack,
            @Nullable ClientLevel level,
            @Nullable LivingEntity entity,
            int seed
    ) {
        BakedModel overridden = nested.resolve(originalModel, stack, level, entity, seed);
        if (overridden != originalModel) {
            return overridden;
        }

        List<ResourceLocation> textureLocations = resolveTextureLocations(stack);
        if (textureLocations == null || textureLocations.isEmpty()) {
            return originalModel;
        }

        BakedModel cached = baseCache.get(textureLocations);
        if (cached != null) return cached;

        cached = mixedCache.get(textureLocations);
        if (cached != null) return cached;

        BakedModel baked = bakeModel(textureLocations);
        if (baked == null) {
            LOGGER.warn("Failed to bake dynamic model for texture combination: {}", textureLocations);
            return originalModel;
        }

        mixedCache.put(textureLocations, baked);
        return baked;
    }

    @Nullable
    protected abstract List<ResourceLocation> resolveTextureLocations(ItemStack stack);

    protected BakedModel bakeModel(List<ResourceLocation> textureLocations) {
        List<BlockElement> allElements = new ArrayList<>();
        Map<String, Either<Material, String>> textureMap = new HashMap<>();
        Material firstValidMaterial = null;

        for (int i = 0; i < textureLocations.size(); i++) {
            ResourceLocation textureLoc = textureLocations.get(i);
            String textureKey = "layer" + i;

            Material material = new Material(InventoryMenu.BLOCK_ATLAS, textureLoc);
            TextureAtlasSprite sprite = spriteGetter.apply(material);

            if (sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation())) {
                LOGGER.warn("Missing texture atlas sprite for location: {}, skipping layer.", textureLoc);
                continue;
            }

            if (firstValidMaterial == null) {
                firstValidMaterial = material;
            }

            textureMap.put(textureKey, Either.left(material));

            List<BlockElement> layerElements = UnbakedGeometryHelper.createUnbakedItemElements(i, sprite, null);
            allElements.addAll(layerElements);
        }

        // Particle fallback uses first successfully resolved layer
        textureMap.put("particle", firstValidMaterial != null ? Either.left(firstValidMaterial) : Either.left(
                new Material(InventoryMenu.BLOCK_ATLAS, MissingTextureAtlasSprite.getLocation())
        ));

        BlockModel dynamicModel = new BlockModel(
                null,
                allElements,
                textureMap,
                baseModel.hasAmbientOcclusion(), // Inherit ambient occlusion
                baseModel.getGuiLight(),
                baseModel.getTransforms(),
                baseModel.getOverrides()
        );

        dynamicModel.name = baseModel.name;
        dynamicModel.customData.copyFrom(baseModel.customData);
        dynamicModel.customData.setGui3d(baseModel.customData.isGui3d()); // Inherit 3D GUI light

        return baker.bakeUncached(dynamicModel, modelState, spriteGetter);
    }
}