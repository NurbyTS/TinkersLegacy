package com.nurby.tinkerslegacy.client.model.tool;

import com.mojang.math.Transformation;
import com.nurby.tinkerslegacy.api.tool.ToolRangedActions;
import com.nurby.tinkerslegacy.content.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.content.tools.RangedToolDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.neoforged.neoforge.client.RenderTypeGroup;
import net.neoforged.neoforge.client.model.CompositeModel;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.QuadTransformers;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ToolAmmunitionModels {
        private final Map<CacheKey, BakedModel> cache = new LinkedHashMap<>();

        // Models
        public BakedModel attach(BakedModel base, ItemStack launcher, RangedToolDefinition definition,
                        String pose, ClientLevel level, LivingEntity entity, int seed) {
                ItemStack ammunition = ammunition(launcher, definition, entity, pose);
                if (ammunition.isEmpty()) return base;
                // Do not recurse through a launcher supplied as charged ammunition by a command or another mod.
                if (ammunition.getItem() instanceof ProjectileWeaponItem
                                || ammunition.getItem() instanceof DynamicTool item && item.getDefinition() instanceof RangedToolDefinition) return base;
                BakedModel model = Minecraft.getInstance().getItemRenderer().getModel(ammunition, level, entity, seed);
                CacheKey key = new CacheKey(base, model, ammunition.getItem(), ammunition.getComponentsPatch(), pose);
                BakedModel cached = cache.get(key);
                if (cached != null) return cached;

                IQuadTransformer transform = QuadTransformers.applying(transformation(definition, pose));
                List<BakedQuad> quads = new ArrayList<>();
                for (BakedModel pass : model.getRenderPasses(ammunition, false)) {
                        addQuads(quads, pass, ammunition, null, transform);
                        for (Direction side : Direction.values()) addQuads(quads, pass, ammunition, side, transform);
                }
                var combined = CompositeModel.Baked.builder(base.useAmbientOcclusion(), base.isGui3d(),
                                base.usesBlockLight(), base.getParticleIcon(), ItemOverrides.EMPTY, base.getTransforms());
                combined.addLayer(base);
                combined.addQuads(RenderTypeGroup.EMPTY, quads);
                BakedModel result = combined.build();
                if (cache.size() >= 128) cache.remove(cache.keySet().iterator().next());
                cache.put(key, result);
                return result;
        }

        private void addQuads(List<BakedQuad> result, BakedModel model, ItemStack ammunition,
                        Direction side, IQuadTransformer transform) {
                for (BakedQuad original : model.getQuads(null, side, RandomSource.create(42), ModelData.EMPTY, null)) {
                        BakedQuad quad = transform.process(original);
                        if (quad.isTinted()) {
                                int color = Minecraft.getInstance().getItemColors().getColor(ammunition, quad.getTintIndex());
                                QuadTransformers.applyingColor(0xFF000000 | color).processInPlace(quad);
                                // The launcher is the rendered stack, so resolve ammunition tint before combining its faces.
                                quad = new BakedQuad(quad.getVertices(), -1, quad.getDirection(), quad.getSprite(),
                                                quad.isShade(), quad.hasAmbientOcclusion());
                        }
                        result.add(quad);
                }
        }

        // Ammunition
        private ItemStack ammunition(ItemStack launcher, RangedToolDefinition definition, LivingEntity entity, String pose) {
                if (!pose.startsWith("_pull")) return ItemStack.EMPTY;
                if (definition.kind() == RangedToolDefinition.Kind.CROSSBOW) {
                        var projectiles = launcher.getOrDefault(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY).getItems();
                        return projectiles.isEmpty() ? ItemStack.EMPTY : projectiles.getFirst();
                }
                if (!(entity instanceof Player player) || !entity.isUsingItem() || entity.getUseItem() != launcher) {
                        return ItemStack.EMPTY;
                }
                ItemStack result = ToolRangedActions.findAmmo(definition, player);
                return result.isEmpty() && player.hasInfiniteMaterials() ? ToolRangedActions.creativeAmmo(definition) : result;
        }

        private Transformation transformation(RangedToolDefinition definition, String pose) {
                Matrix4f matrix = new Matrix4f();
                if (definition.kind() == RangedToolDefinition.Kind.CROSSBOW) {
                        matrix.translation(0.5625F, 0.4375F, 0.5625F).rotateZ((float) Math.PI / 2);
                } else {
                        float offset = switch (pose) {
                                case "_pull1" -> 0.1880F;
                                case "_pull2" -> 0.1255F;
                                default -> 0.0630F;
                        };
                        matrix.translation(0.5F - offset, 0.5F + offset, 0.51F).rotateY((float) Math.PI);
                }
                return new Transformation(matrix.translate(-0.5F, -0.5F, -0.5F));
        }

        private record CacheKey(BakedModel launcher, BakedModel ammunition, Item item, DataComponentPatch components, String pose) {
        }
}
