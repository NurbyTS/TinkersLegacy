package com.nurby.tinkerslegacy.api.material;

import com.nurby.tinkerslegacy.api.material.datagen.MaterialRenderInfo;
import com.nurby.tinkerslegacy.api.material.recipe.MaterialRecipeInfo;
import com.nurby.tinkerslegacy.api.material.recipe.StencilRecipeInfo;
import com.nurby.tinkerslegacy.api.material.stat.MaterialStat;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public final class MaterialBuilder {
        private final ResourceLocation id;

        private MaterialRecipeInfo recipeInfo;
        private MaterialRenderInfo renderInfo;
        private ResourceLocation representativeItem;
        private int color;

        private final Map<ResourceLocation, MaterialStat> stats = new LinkedHashMap<>();
        private final Map<ResourceLocation, List<ResourceLocation>> traits = new LinkedHashMap<>();

        private MaterialBuilder(ResourceLocation id) {
                this.id = id;
        }

        public static MaterialBuilder material(ResourceLocation id) {
                return new MaterialBuilder(id);
        }

        // Properties
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

        public MaterialBuilder color(int color) {
                this.color = color;
                return this;
        }

        // Stats
        public MaterialBuilder stats(MaterialStat... additions) {
                for (MaterialStat stat : additions) {
                        if (stats.putIfAbsent(stat.type(), stat) != null) {
                                throw new IllegalArgumentException("Stats " + stat.type() + " are already registered for " + id);
                        }
                }
                return this;
        }

        // Traits
        public MaterialBuilder trait(ResourceLocation trait, ResourceLocation statType) {
                traits.computeIfAbsent(statType, key -> new ArrayList<>()).add(trait);
                return this;
        }

        // Registration
        public MaterialDefinition build() {
                if (recipeInfo == null) {
                        recipeInfo = new StencilRecipeInfo();
                }

                return new MaterialDefinition(id, stats, traits, recipeInfo, renderInfo,
                                Optional.ofNullable(representativeItem), color);
        }
}
