package com.nurby.tinkerslegacy.client.model.part;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.nurby.tinkerslegacy.TinkersLegacy;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.model.CompositeModel;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import net.neoforged.neoforge.client.model.geometry.StandaloneGeometryBakingContext;

import java.util.function.Function;

public final class DynamicMaterialPartGeometry
        implements IUnbakedGeometry<DynamicMaterialPartGeometry> {

    private final ResourceLocation part;
    private final BlockModel baseModel;

    public DynamicMaterialPartGeometry(
            ResourceLocation part,
            BlockModel baseModel
    ) {
        this.part = part;
        this.baseModel = baseModel;
    }

    @Override
    public BakedModel bake(
            IGeometryBakingContext context,
            ModelBaker baker,
            Function<Material, TextureAtlasSprite> spriteGetter,
            ModelState modelState,
            ItemOverrides overrides
    ) {
        TextureAtlasSprite particleSprite = spriteGetter.apply(
                new Material(
                        InventoryMenu.BLOCK_ATLAS,
                        MissingTextureAtlasSprite.getLocation()
                )
        );

        StandaloneGeometryBakingContext itemContext =
                StandaloneGeometryBakingContext.builder(context)
                        .build(
                                ResourceLocation.fromNamespaceAndPath(
                                        TinkersLegacy.MODID,
                                        "dynamic_material_part"
                                )
                        );

        ItemOverrides dynamicOverrides =
                new DynamicMaterialPartOverrideHandler(
                        overrides,
                        baker,
                        itemContext,
                        modelState,
                        part,
                        baseModel
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
            Function<ResourceLocation, UnbakedModel> modelGetter,
            IGeometryBakingContext context
    ) {
        baseModel.resolveParents(modelGetter);
    }

    public static final class Loader
            implements IGeometryLoader<DynamicMaterialPartGeometry> {

        public static final Loader INSTANCE = new Loader();

        private Loader() {
        }

        @Override
        public DynamicMaterialPartGeometry read(
                JsonObject jsonObject,
                JsonDeserializationContext context
        ) throws JsonParseException {

            String partString = jsonObject
                    .get("part")
                    .getAsString();

            ResourceLocation part =
                    ResourceLocation.parse(partString);

            jsonObject.remove("loader");
            jsonObject.remove("part");

            BlockModel baseModel =
                    context.deserialize(
                            jsonObject,
                            BlockModel.class
                    );

            return new DynamicMaterialPartGeometry(
                    part,
                    baseModel
            );
        }
    }
}