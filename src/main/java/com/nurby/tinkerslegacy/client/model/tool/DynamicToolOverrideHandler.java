package com.nurby.tinkerslegacy.client.model.tool;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.library.material.MaterialDefinition;
import com.nurby.tinkerslegacy.library.material.layer.MaterialLayer;
import com.nurby.tinkerslegacy.library.tool.ToolDefinition;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLRegistries;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class DynamicToolOverrideHandler extends ItemOverrides {

    private final ItemOverrides parent;
    private final ModelBaker baker;
    private final IGeometryBakingContext context;
    private final ModelState modelState;

    private final ResourceLocation toolId;
    private final BlockModel baseModel;
    private final Function<Material, TextureAtlasSprite> spriteGetter;

    private final Map<String, BakedModel> cache =
            new LinkedHashMap<>();

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

            List<MaterialLayer> layers =
                    tool.parts()
                            .stream()
                            .map(part -> new MaterialLayer(
                                    part.statType(),
                                    material.id()
                            ))
                            .toList();

            BakedModel model = bakeModel(layers);

            if (model != null) {
                cache.put(createCacheKey(layers), model);
            }
        }
    }

    private BakedModel bakeModel(
            List<MaterialLayer> layers
    ) {
        return baker.bakeUncached(
                baseModel,
                modelState,
                material -> resolveTexture(
                        material,
                        layers
                )
        );
    }

    private TextureAtlasSprite resolveTexture(
            Material template,
            List<MaterialLayer> layers
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

        if (index < 0 || index >= layers.size()) {
            return spriteGetter.apply(template);
        }

        MaterialLayer layer = layers.get(index);

        ResourceLocation generatedTexture =
                ResourceLocation.fromNamespaceAndPath(
                        TinkersLegacy.MODID,
                        "item/tools/"
                                + toolId.getPath()
                                + "/"
                                + layer.material().getPath()
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

        List<MaterialLayer> layers =
                stack.get(TLDataComponents.MATERIALS);

        if (layers == null || layers.isEmpty()) {
            return parentModel;
        }

        String key = createCacheKey(layers);

        BakedModel cached = cache.get(key);

        if (cached != null) {
            return cached;
        }

        BakedModel baked = bakeModel(layers);

        if (baked == null) {
            return parentModel;
        }

        cache.put(key, baked);

        return baked;
    }

    private String createCacheKey(
            List<MaterialLayer> layers
    ) {
        StringBuilder key = new StringBuilder();

        for (MaterialLayer layer : layers) {
            key.append(layer.statType())
                    .append('=')
                    .append(layer.material())
                    .append(';');
        }

        return key.toString();
    }
}