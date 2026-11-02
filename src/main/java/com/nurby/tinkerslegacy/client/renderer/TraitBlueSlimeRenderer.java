package com.nurby.tinkerslegacy.client.renderer;

import com.nurby.tinkerslegacy.TinkersLegacy;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Slime;

public class TraitBlueSlimeRenderer extends SlimeRenderer {
        private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, "textures/entity/blue_slime.png");

        public TraitBlueSlimeRenderer(EntityRendererProvider.Context context) {
                super(context);
        }

        @Override
        public ResourceLocation getTextureLocation(Slime slime) {
                return TEXTURE;
        }
}
