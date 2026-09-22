package com.nurby.tinkerslegacy.client.model.tool;

import com.nurby.tinkerslegacy.TinkersLegacy;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;

public final class DynamicToolModelLoader {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(
                    TinkersLegacy.MODID,
                    "dynamic_tool"
            );

    public static final IGeometryLoader<DynamicToolGeometry> INSTANCE =
            DynamicToolGeometry.Loader.INSTANCE;

    private DynamicToolModelLoader() {
    }
}