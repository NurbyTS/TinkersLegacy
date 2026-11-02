package com.nurby.tinkerslegacy.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nurby.tinkerslegacy.api.station.StationAppearance;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLStations;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public final class StationModel extends BakedModelWrapper<BakedModel> {
        private final ResourceLocation station;
        @Nullable
        private final ResourceLocation itemBase;
        private final ItemOverrides overrides = new ItemOverrides() {
                @Override
                public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level,
                                @Nullable LivingEntity entity, int seed) {
                        return new StationModel(originalModel, station,
                                        StationAppearance.validBase(stack.get(TLDataComponents.STATION_BASE), station));
                }
        };

        public StationModel(BakedModel model, ResourceLocation station, @Nullable ResourceLocation itemBase) {
                super(model);
                this.station = station;
                this.itemBase = itemBase;
        }

        // Rendering
        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random) {
                return getQuads(state, side, random, ModelData.EMPTY, null);
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random,
                        ModelData data, @Nullable RenderType renderType) {
                ResourceLocation base = StationAppearance.validBase(itemBase == null ? data.get(StationAppearance.BASE) : itemBase, station);
                BlockState baseState = BuiltInRegistries.BLOCK.get(base).defaultBlockState();
                BakedModel baseModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(baseState);
                List<BakedQuad> result = new ArrayList<>();
                for (BakedQuad quad : originalModel.getQuads(state, side, random, data, renderType)) {
                        if (!baseFace(quad)) {
                                result.add(quad);
                                continue;
                        }
                        List<BakedQuad> faces = baseModel.getQuads(baseState, quad.getDirection(), RandomSource.create(42), ModelData.EMPTY, null);
                        TextureAtlasSprite sprite = faces.isEmpty() ? baseModel.getParticleIcon(ModelData.EMPTY) : faces.getFirst().getSprite();
                        result.add(retexture(quad, sprite));
                }
                return result;
        }

        private boolean baseFace(BakedQuad quad) {
                if (station.equals(TLStations.TOOL_FORGE.id())) {
                        return !quad.getSprite().contents().name().getPath().equals("block/station/tool_forge_top");
                }
                // Keep the engraved tabletop. Legs and the underside use the actual crafting block.
                int[] vertices = quad.getVertices();
                int stride = vertices.length / 4;
                for (int index = 0; index < 4; index++) {
                        if (Float.intBitsToFloat(vertices[index * stride + 1]) > 0.7501F) return false;
                }
                return true;
        }

        private static BakedQuad retexture(BakedQuad quad, TextureAtlasSprite sprite) {
                int[] vertices = quad.getVertices().clone();
                int stride = vertices.length / 4;
                TextureAtlasSprite original = quad.getSprite();
                for (int index = 0; index < 4; index++) {
                        int offset = index * stride;
                        float u = (Float.intBitsToFloat(vertices[offset + 4]) - original.getU0()) / (original.getU1() - original.getU0());
                        float v = (Float.intBitsToFloat(vertices[offset + 5]) - original.getV0()) / (original.getV1() - original.getV0());
                        vertices[offset + 4] = Float.floatToRawIntBits(sprite.getU(u));
                        vertices[offset + 5] = Float.floatToRawIntBits(sprite.getV(v));
                }
                return new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(), sprite,
                                quad.isShade(), quad.hasAmbientOcclusion());
        }

        @Override
        public ItemOverrides getOverrides() {
                return overrides;
        }

        @Override
        public BakedModel applyTransform(ItemDisplayContext context, PoseStack poses, boolean leftHand) {
                originalModel.applyTransform(context, poses, leftHand);
                return this;
        }

        @Override
        public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
                return List.of(this);
        }
}
