package com.nurby.tinkerslegacy.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.nurby.tinkerslegacy.entity.TLToolProjectile;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;

public class TLToolProjectileRenderer extends EntityRenderer<TLToolProjectile> {
        private final ItemRenderer items;

        public TLToolProjectileRenderer(EntityRendererProvider.Context context) {
                super(context);
                items = context.getItemRenderer();
        }

        // Rendering
        @Override
        public void render(TLToolProjectile projectile, float yaw, float partialTick,
                        PoseStack pose, MultiBufferSource buffer, int light) {
                pose.pushPose();
                float direction = Mth.rotLerp(partialTick, projectile.yRotO, projectile.getYRot());
                float pitch = Mth.lerp(partialTick, projectile.xRotO, projectile.getXRot());
                if (projectile.isShuriken()) {
                        pose.scale(0.3F, 0.3F, 0.3F);
                        pose.mulPose(Axis.YP.rotationDegrees(direction));
                        pose.mulPose(Axis.XP.rotationDegrees(-pitch));
                        pose.mulPose(Axis.ZP.rotationDegrees(7 - Math.floorMod(projectile.getUUID().hashCode(), 14)));
                        pose.mulPose(Axis.XP.rotationDegrees(90));
                        float flight = projectile.flightTicks() + (projectile.isEmbedded() ? 0 : partialTick);
                        pose.mulPose(Axis.ZP.rotationDegrees(Math.floorMod(projectile.getUUID().hashCode(), 360) + flight * 40));
                } else {
                        pose.scale(0.5F, 0.5F, 0.5F);
                        pose.mulPose(Axis.YP.rotationDegrees(direction - 90));
                        pose.mulPose(Axis.ZP.rotationDegrees(pitch - 45));
                }
                items.renderStatic(projectile.getItem(), ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY,
                                pose, buffer, projectile.level(), projectile.getId());
                pose.popPose();
                super.render(projectile, yaw, partialTick, pose, buffer, light);
        }

        @Override
        public ResourceLocation getTextureLocation(TLToolProjectile projectile) {
                return TextureAtlas.LOCATION_BLOCKS;
        }
}
