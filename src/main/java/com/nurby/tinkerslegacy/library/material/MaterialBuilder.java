package com.nurby.tinkerslegacy.library.material;

import com.nurby.tinkerslegacy.library.material.recipe.MaterialRecipeInfo;
import com.nurby.tinkerslegacy.library.material.recipe.StencilRecipeInfo;
import com.nurby.tinkerslegacy.library.material.stat.MaterialStat;
import com.nurby.tinkerslegacy.library.material.datagen.MaterialRenderInfo;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class MaterialBuilder {
    private final ResourceLocation id;

    private MaterialRecipeInfo recipeInfo;
    private MaterialRenderInfo renderInfo;
    private ResourceLocation representativeItem;

    private final Map<ResourceLocation, MaterialStat> stats =
            new LinkedHashMap<>();

    private final Map<ResourceLocation, List<ResourceLocation>> traits =
            new LinkedHashMap<>();
    private int color;

    private MaterialBuilder(ResourceLocation id) {
        this.id = id;
    }

    public static MaterialBuilder material(ResourceLocation id) {
        return new MaterialBuilder(id);
    }

    public MaterialBuilder renderInfo(MaterialRenderInfo renderInfo) {
        this.renderInfo = renderInfo;
        return this;
    }

    public MaterialBuilder recipeInfo(MaterialRecipeInfo recipeInfo) {
        this.recipeInfo = recipeInfo;
        return this;
    }

    public MaterialBuilder representativeItem(ResourceLocation item) {
        this.representativeItem = item;
        return this;
    }

    public MaterialBuilder stats(MaterialStat... additions) {
        for (MaterialStat stat : additions) {
            if (stats.putIfAbsent(stat.type(), stat) != null) {
                throw new IllegalArgumentException(
                        "Stats " + stat.type() +
                                " are already registered for " + id
                );
            }
        }

        return this;
    }

    public MaterialBuilder color(int color) {
        this.color = color;
        return this;
    }

    public MaterialBuilder trait(
            ResourceLocation trait,
            ResourceLocation statType
    ) {
        traits.computeIfAbsent(
                statType,
                key -> new ArrayList<>()
        ).add(trait);

        return this;
    }

    public MaterialDefinition build() {
        if (recipeInfo == null) {
            recipeInfo = new StencilRecipeInfo();
        }

        return new MaterialDefinition(
                id,
                stats,
                traits,
                recipeInfo,
                renderInfo,
                Optional.ofNullable(representativeItem),
                color
        );
    }
}