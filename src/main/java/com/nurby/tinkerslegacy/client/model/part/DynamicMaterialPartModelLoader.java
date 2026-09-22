package com.nurby.tinkerslegacy.client.model.part;

import com.nurby.tinkerslegacy.TinkersLegacy;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;

public final class DynamicMaterialPartModelLoader {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(
                    TinkersLegacy.MODID,
                    "dynamic_material_part"
            );

    public static final IGeometryLoader<DynamicMaterialPartGeometry> INSTANCE =
            DynamicMaterialPartGeometry.Loader.INSTANCE;

    private DynamicMaterialPartModelLoader() {
    }
}