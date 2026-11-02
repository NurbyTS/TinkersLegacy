package com.nurby.tinkerslegacy.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.nurby.tinkerslegacy.TLConfig;
import com.nurby.tinkerslegacy.api.station.StationLayout;
import com.nurby.tinkerslegacy.content.block.StationBlock;
import com.nurby.tinkerslegacy.content.block.entity.StationBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class StationRenderer implements BlockEntityRenderer<StationBlockEntity> {
        private final ItemRenderer items;

        public StationRenderer(BlockEntityRendererProvider.Context context) {
                items = context.getItemRenderer();
        }

        @Override
        public void render(StationBlockEntity station, float partialTick, PoseStack poses,
                        MultiBufferSource buffers, int light, int overlay) {
                if (!TLConfig.RENDER_STATION_CONTENTS.get()) return;
                poses.pushPose();
                poses.translate(0.5D, 0, 0.5D);
                poses.mulPose(Axis.YP.rotationDegrees(180 - station.getBlockState().getValue(StationBlock.FACING).toYRot()));
                for (int slot = 0; slot < station.getContainerSize(); slot++) {
                        ItemStack stack = station.getItem(slot);
                        if (stack.isEmpty()) continue;
                        var display = StationLayout.tabletop(station.definition(), slot, stack);
                        int seed = (int) station.getBlockPos().asLong() + slot;
                        var model = items.getModel(stack, station.getLevel(), null, seed);
                        poses.pushPose();
                        poses.translate(display.x(), display.y(), display.z());
                        if (!display.upright()) poses.mulPose(Axis.XP.rotationDegrees(90));
                        poses.mulPose(Axis.YP.rotationDegrees(180));
                        poses.scale(display.scale(), display.scale(), display.scale());
                        // Original tables transform raw models; FIXED applies an unwanted second scale/rotation.
                        items.render(stack, ItemDisplayContext.NONE, false, poses, buffers, light, overlay, model);
                        poses.popPose();
                }
                poses.popPose();
        }
}
