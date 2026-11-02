package com.nurby.tinkerslegacy.client.model.tool;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.api.material.MaterialDefinition;
import com.nurby.tinkerslegacy.api.tool.ToolDefinition;
import com.nurby.tinkerslegacy.api.tool.ToolModifierEntry;
import com.nurby.tinkerslegacy.client.model.AbstractDynamicModelHandler;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.model.CompositeModel;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class DynamicToolOverrideHandler extends AbstractDynamicModelHandler {

    private final IGeometryBakingContext context;
    private final ResourceLocation toolId;
    private final ToolModifierModels modifiers;
    private final ToolAmmunitionModels ammunition = new ToolAmmunitionModels();

    private final Map<ResourceLocation, BakedModel> overlayCache = new LinkedHashMap<>();

    public DynamicToolOverrideHandler(ItemOverrides nested, ModelBaker baker, IGeometryBakingContext context, ModelState modelState, ResourceLocation toolId, BlockModel baseModel, Function<Material, TextureAtlasSprite> spriteGetter) {
        super(nested, baker, modelState, baseModel, spriteGetter);
        this.context = context;
        this.toolId = toolId;
        this.modifiers = new ToolModifierModels(toolId, Minecraft.getInstance().getResourceManager());

        prebakeEqualMaterialModels();
    }

    private void prebakeEqualMaterialModels() {
        ToolDefinition tool = TLRegistries.TOOLS.get(toolId);
        if (tool == null)
            return;

        for (Map.Entry<ResourceKey<MaterialDefinition>, MaterialDefinition> entry : TLRegistries.MATERIALS.entrySet()) {
            MaterialDefinition material = entry.getValue();
            if (!tool.supports(material))
                continue;

            List<ResourceLocation> textureLocations = new ArrayList<>();
            for (int i = 0; i < tool.parts().size(); i++) {
                textureLocations.add(ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, "item/tools/" + toolId.getPath() + "/" + material.id().getPath() + "/layer" + i));
            }

            BakedModel baked = bakeModel(textureLocations);
            if (baked != null) {
                baseCache.put(textureLocations, baked);
            }
        }
    }

    @Override
    public BakedModel resolve(BakedModel originalModel, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
        BakedModel overridden = nested.resolve(originalModel, stack, level, entity, seed);
        if (overridden != originalModel) {
            return overridden;
        }

        List<ResourceLocation> layers = stack.get(TLDataComponents.MATERIALS);
        if (layers == null || layers.isEmpty()) {
            return originalModel;
        }

        String pose = pose(stack, entity);
        List<ToolModifierEntry> entries = stack.getOrDefault(TLDataComponents.MODIFIERS, List.of());

        List<ResourceLocation> textureLocations = resolveTextureLocations(stack, layers, pose);
        if (textureLocations == null || textureLocations.isEmpty()) {
            return originalModel;
        }

        // 1. Check base prebaked cache
        BakedModel baked = baseCache.get(textureLocations);

        // 2. Check LRU mixed cache
        if (baked == null) {
            baked = mixedCache.get(textureLocations);
        }

        // 3. Bake base layered model if not cached
        if (baked == null) {
            baked = bakeModel(textureLocations);
            if (baked == null) {
                LOGGER.warn("Failed to bake dynamic model for tool {}", toolId);
                return originalModel;
            }
            mixedCache.put(textureLocations, baked);
        }

        // 4. Apply modifier overlays (if any)
        if (!entries.isEmpty()) {
            baked = overlays(baked, entries, pose);
        }

        // 5. Apply ammunition / bow models
        return ammunition(baked, stack, pose, level, entity, seed);
    }

    @Override
    protected List<ResourceLocation> resolveTextureLocations(ItemStack stack) {
        List<ResourceLocation> layers = stack.get(TLDataComponents.MATERIALS);
        if (layers == null || layers.isEmpty())
            return null;
        return resolveTextureLocations(stack, layers, "");
    }

    private List<ResourceLocation> resolveTextureLocations(ItemStack stack, List<ResourceLocation> layers, String pose) {
        ToolDefinition toolDef = TLRegistries.TOOLS.get(toolId);
        if (toolDef == null)
            return null;

        List<ResourceLocation> finalTextures = new ArrayList<>();

        if (layers.size() != toolDef.parts().size()) {
            LOGGER.warn("Material count mismatch on tool stack {}: expected {}, got {}", toolId, toolDef.parts().size(), layers.size());
        }

        for (int i = 0; i < toolDef.parts().size(); i++) {
            ResourceLocation matId = (i < layers.size() && layers.get(i) != null) ? layers.get(i) : UNKNOWN_MATERIAL;

            finalTextures.add(ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, "item/tools/" + toolId.getPath() + "/" + matId.getPath() + "/layer" + i + pose));
        }

        return finalTextures;
    }

    private BakedModel ammunition(BakedModel model, ItemStack stack, String pose, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
        ToolDefinition definition = TLRegistries.TOOLS.get(toolId);
        return definition instanceof com.nurby.tinkerslegacy.content.tools.RangedToolDefinition ranged ? ammunition.attach(model, stack, ranged, pose, level, entity, seed) : model;
    }

    private String pose(ItemStack stack, @Nullable LivingEntity entity) {
        if (com.nurby.tinkerslegacy.api.tool.ToolState.broken(stack))
            return "_broken";
        ToolDefinition tool = TLRegistries.TOOLS.get(toolId);
        if (!(tool instanceof com.nurby.tinkerslegacy.content.tools.RangedToolDefinition ranged))
            return "";
        if (com.nurby.tinkerslegacy.api.tool.ToolRangedActions.isLoaded(stack))
            return "_pull3";
        if (entity == null || !entity.isUsingItem() || entity.getUseItem() != stack)
            return "";

        float progress = ranged.drawProgress(stack, stack.getUseDuration(entity) - entity.getUseItemRemainingTicks());
        boolean crossbow = ranged.kind() == com.nurby.tinkerslegacy.content.tools.RangedToolDefinition.Kind.CROSSBOW;
        return progress >= (crossbow ? 0.999F : 0.9F) ? "_pull3" : progress >= (crossbow ? 0.5F : 0.65F) ? "_pull2" : "_pull1";
    }

    private BakedModel overlays(BakedModel base, List<ToolModifierEntry> entries, String pose) {
        List<ResourceLocation> textures = modifiers.textures(entries, pose);
        if (textures.isEmpty())
            return base;

        var composite = CompositeModel.Baked.builder(context, base.getParticleIcon(), ItemOverrides.EMPTY, base.getTransforms());
        composite.addLayer(base);

        for (ResourceLocation texture : textures) {
            BakedModel overlay = overlayCache.get(texture);
            if (overlay == null) {
                TextureAtlasSprite sprite = spriteGetter.apply(new Material(TextureAtlas.LOCATION_BLOCKS, texture));
                if (sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation()))
                    continue;

                BlockModel template = ToolModifierModels.template(texture);
                template.resolveParents(baker::getModel);
                overlay = baker.bakeUncached(template, modelState, spriteGetter);
                if (overlay == null)
                    continue;

                overlayCache.put(texture, overlay);
            }
            composite.addLayer(overlay);
        }

        return composite.build();
    }
}