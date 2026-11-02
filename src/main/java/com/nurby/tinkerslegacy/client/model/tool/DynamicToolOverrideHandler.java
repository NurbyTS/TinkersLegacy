package com.nurby.tinkerslegacy.client.model.tool;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.library.material.MaterialDefinition;
import com.nurby.tinkerslegacy.library.material.layer.MaterialLayer;
import com.nurby.tinkerslegacy.library.tool.ToolDefinition;
import com.nurby.tinkerslegacy.library.tool.ToolModifierEntry;
import com.nurby.tinkerslegacy.library.tool.ToolModifiers;
import net.minecraft.client.Minecraft;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLRegistries;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.neoforged.neoforge.client.model.CompositeModel;
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

        private final Map<CacheKey, BakedModel> cache = new LinkedHashMap<>();
        private final Map<ResourceLocation, BakedModel> overlayCache = new LinkedHashMap<>();
        private final ToolModifierModels modifiers;
        private final ToolAmmunitionModels ammunition = new ToolAmmunitionModels();

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
                modifiers = new ToolModifierModels(toolId, Minecraft.getInstance().getResourceManager());

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
                                cache.put(new CacheKey(layers, "", List.of()), model);
                        }
                }
        }

        private BakedModel bakeModel(List<MaterialLayer> layers) {
                return bakeModel(layers, "");
        }

        private BakedModel bakeModel(List<MaterialLayer> layers, String pose) {
                return baker.bakeUncached(
                                baseModel,
                                modelState,
                                material -> resolveTexture(
                                                material,
                                                layers,
                                                pose
                                )
                );
        }

        private TextureAtlasSprite resolveTexture(
                        Material template,
                        List<MaterialLayer> layers,
                        String pose
        ) {
                ResourceLocation texture = template.texture();

                String path = texture.getPath();

                if (!path.startsWith("item/tool_templates/") && !path.startsWith("item/tools/")) {
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
                                                                + index + pose
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

                String pose = pose(stack, entity);
                List<ToolModifierEntry> entries = stack.getOrDefault(TLDataComponents.MODIFIERS, List.of());
                List<ModifierKey> modifierKeys = entries.stream().map(entry -> new ModifierKey(entry.id(),
                                ToolModifiers.get(entry.id()) == null ? entry.points() : ToolModifiers.get(entry.id()).level(entry.points()))).toList();
                CacheKey key = new CacheKey(layers, pose, modifierKeys);

                BakedModel cached = cache.get(key);

                if (cached != null) {
                        return ammunition(cached, stack, pose, level, entity, seed);
                }

                BakedModel baked = bakeModel(layers, pose);
                if (baked != null) baked = overlays(baked, entries, pose);

                if (baked == null) {
                        return parentModel;
                }

                if (cache.size() >= 512) cache.remove(cache.keySet().iterator().next());
                cache.put(key, baked);

                return ammunition(baked, stack, pose, level, entity, seed);
        }

        private BakedModel ammunition(BakedModel model, ItemStack stack, String pose,
                        net.minecraft.client.multiplayer.ClientLevel level, net.minecraft.world.entity.LivingEntity entity, int seed) {
                ToolDefinition definition = TLRegistries.TOOLS.get(toolId);
                return definition instanceof com.nurby.tinkerslegacy.tools.RangedToolDefinition ranged
                                ? ammunition.attach(model, stack, ranged, pose, level, entity, seed) : model;
        }

        private String pose(ItemStack stack, net.minecraft.world.entity.LivingEntity entity) {
                if (com.nurby.tinkerslegacy.library.tool.ToolState.broken(stack)) return "_broken";
                ToolDefinition tool = TLRegistries.TOOLS.get(toolId);
                if (!(tool instanceof com.nurby.tinkerslegacy.tools.RangedToolDefinition ranged)) return "";
                if (com.nurby.tinkerslegacy.library.tool.ToolRangedActions.isLoaded(stack)) return "_pull3";
                if (entity == null || !entity.isUsingItem() || entity.getUseItem() != stack) return "";
                float progress = ranged.drawProgress(stack, stack.getUseDuration(entity) - entity.getUseItemRemainingTicks());
                boolean crossbow = ranged.kind() == com.nurby.tinkerslegacy.tools.RangedToolDefinition.Kind.CROSSBOW;
                return progress >= (crossbow ? 0.999F : 0.9F) ? "_pull3"
                                : progress >= (crossbow ? 0.5F : 0.65F) ? "_pull2" : "_pull1";
        }

        // Modifiers
        private BakedModel overlays(BakedModel base, List<ToolModifierEntry> entries, String pose) {
                List<ResourceLocation> textures = modifiers.textures(entries, pose);
                if (textures.isEmpty()) return base;
                var composite = CompositeModel.Baked.builder(context, base.getParticleIcon(), ItemOverrides.EMPTY, base.getTransforms());
                composite.addLayer(base);
                for (ResourceLocation texture : textures) {
                        BakedModel overlay = overlayCache.get(texture);
                        if (overlay == null) {
                                TextureAtlasSprite sprite = spriteGetter.apply(new Material(TextureAtlas.LOCATION_BLOCKS, texture));
                                if (sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation())) continue;
                                BlockModel template = ToolModifierModels.template(texture);
                                template.resolveParents(baker::getModel);
                                overlay = baker.bakeUncached(template, modelState, spriteGetter);
                                if (overlay == null) continue;
                                overlayCache.put(texture, overlay);
                        }
                        composite.addLayer(overlay);
                }
                return composite.build();
        }

        private record ModifierKey(ResourceLocation id, int level) {
        }

        private record CacheKey(List<MaterialLayer> layers, String pose, List<ModifierKey> modifiers) {
                private CacheKey {
                        layers = List.copyOf(layers);
                        modifiers = List.copyOf(modifiers);
                }
        }
}
