/*package com.nurby.tinkerslegacy.client.model.tool;

import com.mojang.datafixers.util.Either;
import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.library.material.MaterialDefinition;
import com.nurby.tinkerslegacy.library.tool.ToolDefinition;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLRegistries;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;

import java.util.*;
import java.util.function.Function;

public class DynamicToolOverrideHandler_OLD extends ItemOverrides {
    private static final int MIXED_CACHE_LIMIT = 512;

    private final ItemOverrides parent;
    private final ModelBaker baker;
    private final IGeometryBakingContext context;
    private final ModelState modelState;

    private final ResourceLocation toolId;
    private final BlockModel baseModel;
    private final Function<Material, TextureAtlasSprite> spriteGetter;

    private final Map<String, BakedModel> baseCache = new HashMap<>();

    private final Map<String, BakedModel> mixedCache =
            new LinkedHashMap<>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, BakedModel> eldest) {
                    return size() > MIXED_CACHE_LIMIT;
                }
            };

    public DynamicToolOverrideHandler(
            ItemOverrides parent,
            ModelBaker baker,
            IGeometryBakingContext context,
            ModelState modelState,
            ResourceLocation toolId,
            BlockModel baseModel,
            Function<Material, TextureAtlasSprite> spriteGetter
    ) {
        this.parent = parent;
        this.baker = baker;
        this.context = context;
        this.modelState = modelState;
        this.toolId = toolId;
        this.baseModel = baseModel;
        this.spriteGetter = spriteGetter;

        prebakeEqualMaterialModels();
    }

    private void prebakeEqualMaterialModels() {
        ToolDefinition tool = TLRegistries.TOOLS.get(toolId);

        if (tool == null) {
            return;
        }

        for (Map.Entry<ResourceKey<MaterialDefinition>, MaterialDefinition> materialEntry : TLRegistries.MATERIALS.entrySet()) {
            MaterialDefinition material = materialEntry.getValue();

            if (!tool.supports(material)) {
                continue;
            }

            List<ResourceLocation> materials =
                    tool.parts()
                            .stream()
                            .map(part -> material.id())
                            .toList();

            BakedModel model = bakeModel(materials);

            if (model != null) {
                baseCache.put(createCacheKey(materials), model);
            }
        }
    }

    private BakedModel bakeModel(List<ResourceLocation> materials) {
        ItemModelGenerator generator = new ItemModelGenerator();
        List<BlockElement> allElements = new ArrayList<>();
        Map<String, Either<Material, String>> textureMap = new HashMap<>();

        // 1. Generate unbaked 3D elements for EVERY layer, bypassing the 5-layer cap
        for (int i = 0; i < materials.size(); i++) {
            ResourceLocation materialId = materials.get(i);
            String textureKey = "layer" + i;

            // Build the exact texture path directly using the 'material' variable
            ResourceLocation generatedTexture = ResourceLocation.fromNamespaceAndPath(
                    TinkersLegacy.MODID,
                    "item/tools/" + toolId.getPath() + "/" + materialId.getPath() + "/layer" + i
            );

            // Use the modern atlas constant (InventoryMenu.BLOCK_ATLAS)
            Material material = new Material(
                    net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS,
                    generatedTexture
            );

            // Fetch the sprite directly using your class's spriteGetter
            TextureAtlasSprite sprite = spriteGetter.apply(material);

            // Add to the texture map so the synthetic model knows exactly what to render
            textureMap.put(textureKey, Either.left(material));

            // Use vanilla's public method to extrude the 2D sprite into 3D quads
            List<BlockElement> extrudedElements = generator.processFrames(i, textureKey, sprite.contents());

            // Apply NeoForge's anti-seam bleeding fix and add to our master list
            allElements.addAll(ClientHooks.fixItemModelSeams(extrudedElements, sprite));
        }

        // Set the particle texture to the first layer (usually the tool head/handle)
        textureMap.put("particle", textureMap.getOrDefault("layer0", Either.left(
                new Material(
                        net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS,
                        MissingTextureAtlasSprite.getLocation()
                )
        )));

        // 2. Construct a synthetic BlockModel holding our infinite layers
        BlockModel dynamicModel = new BlockModel(
                null,
                allElements,
                textureMap,
                false,
                baseModel.getGuiLight(),
                baseModel.getTransforms(),
                baseModel.getOverrides()
        );

        // Copy necessary properties from your base JSON model
        dynamicModel.name = baseModel.name;
        dynamicModel.customData.copyFrom(baseModel.customData);
        dynamicModel.customData.setGui3d(false);

        // 3. Bake the synthetic model instead of the base model
        // We pass the raw spriteGetter because textureMap already contains our exact resolved materials
        return baker.bakeUncached(
                dynamicModel,
                modelState,
                spriteGetter
        );
    }

    private TextureAtlasSprite resolveTexture(
            Material template,
            List<ResourceLocation> materials
    ) {
        ResourceLocation texture = template.texture();

        String path = texture.getPath();

        if (!path.startsWith("item/tools/")) {
            return spriteGetter.apply(template);
        }

        String layerName =
                path.substring(path.lastIndexOf('/') + 1);

        if (!layerName.startsWith("layer")) {
            return spriteGetter.apply(template);
        }

        int index;

        try {
            index = Integer.parseInt(
                    layerName.substring("layer".length())
            );
        } catch (NumberFormatException ignored) {
            return spriteGetter.apply(template);
        }

        if (index < 0 || index >= materials.size()) {
            return spriteGetter.apply(template);
        }

        ResourceLocation materialId = materials.get(index);

        ResourceLocation generatedTexture =
                ResourceLocation.fromNamespaceAndPath(
                        TinkersLegacy.MODID,
                        "item/tools/"
                                + toolId.getPath()
                                + "/"
                                + materialId.getPath()
                                + "/layer"
                                + index
                );

        return spriteGetter.apply(
                new Material(
                        template.atlasLocation(),
                        generatedTexture
                )
        );
    }

    @Override
    public BakedModel resolve(
            BakedModel original,
            ItemStack stack,
            net.minecraft.client.multiplayer.ClientLevel level,
            net.minecraft.world.entity.LivingEntity entity,
            int seed
    ) {
        BakedModel parentModel =
                parent.resolve(
                        original,
                        stack,
                        level,
                        entity,
                        seed
                );

        List<ResourceLocation> materials =
                stack.get(TLDataComponents.MATERIALS);

        if (materials == null || materials.isEmpty()) {
            return parentModel;
        }

        String key = createCacheKey(materials);

        BakedModel baseCached = baseCache.get(key);

        if (baseCached != null) {
            return baseCached;
        }

        BakedModel mixedCached = mixedCache.get(key);

        if (mixedCached != null) {
            return mixedCached;
        }

        BakedModel baked = bakeModel(materials);

        if (baked == null) {
            return parentModel;
        }

        mixedCache.put(key, baked);

        return baked;
    }

    private String createCacheKey(
            List<ResourceLocation> materials
    ) {
        StringBuilder key = new StringBuilder();

        for (ResourceLocation material : materials) {
            key.append(material)
                    .append(';');
        }

        return key.toString();
    }
}*/