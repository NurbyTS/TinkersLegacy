package com.nurby.tinkerslegacy.client.model.tool;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.nurby.tinkerslegacy.TinkersLegacy;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.CompositeModel;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import net.neoforged.neoforge.client.model.geometry.StandaloneGeometryBakingContext;

import java.util.function.Function;

public final class DynamicToolGeometry
        implements IUnbakedGeometry<DynamicToolGeometry> {

    private final ResourceLocation tool;
    private final BlockModel baseModel;

    public DynamicToolGeometry(
            ResourceLocation tool,
            BlockModel baseModel
    ) {
        this.tool = tool;
        this.baseModel = baseModel;
    }

    @Override
    public BakedModel bake(
            IGeometryBakingContext context,
            ModelBaker baker,
            Function<Material, net.minecraft.client.renderer.texture.TextureAtlasSprite> spriteGetter,
            ModelState modelState,
            net.minecraft.client.renderer.block.model.ItemOverrides overrides
    ) {
        var particleSprite = spriteGetter.apply(
                new Material(
                        TextureAtlas.LOCATION_BLOCKS,
                        MissingTextureAtlasSprite.getLocation()
                )
        );

        StandaloneGeometryBakingContext itemContext =
                StandaloneGeometryBakingContext.builder(context)
                        .build(
                                ResourceLocation.fromNamespaceAndPath(
                                        TinkersLegacy.MODID,
                                        "dynamic_tool"
                                )
                        );

        DynamicToolOverrideHandler dynamicOverrides =
                new DynamicToolOverrideHandler(
                        overrides,
                        baker,
                        itemContext,
                        modelState,
                        tool,
                        baseModel,
                        spriteGetter
                );

        return CompositeModel.Baked.builder(
                itemContext,
                particleSprite,
                dynamicOverrides,
                context.getTransforms()
        ).build();
    }

    @Override
    public void resolveParents(
            Function<ResourceLocation, net.minecraft.client.resources.model.UnbakedModel> modelGetter,
            IGeometryBakingContext context
    ) {
        baseModel.resolveParents(modelGetter);
    }

    public static final class Loader
            implements IGeometryLoader<DynamicToolGeometry> {

        public static final Loader INSTANCE = new Loader();

        private Loader() {
        }

        @Override
        public DynamicToolGeometry read(
                JsonObject jsonObject,
                JsonDeserializationContext context
        ) throws JsonParseException {

            ResourceLocation tool =
                    ResourceLocation.parse(
                            jsonObject
                                    .get("tool")
                                    .getAsString()
                    );

            jsonObject.remove("loader");
            jsonObject.remove("tool");

            BlockModel baseModel =
                    context.deserialize(
                            jsonObject,
                            BlockModel.class
                    );

            return new DynamicToolGeometry(
                    tool,
                    baseModel
            );
        }
    }
}