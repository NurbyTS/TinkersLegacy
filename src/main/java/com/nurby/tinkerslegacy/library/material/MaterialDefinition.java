package com.nurby.tinkerslegacy.library.material;

import com.nurby.tinkerslegacy.util.TinkersUtils;
import com.nurby.tinkerslegacy.library.material.recipe.MaterialRecipeInfo;
import com.nurby.tinkerslegacy.library.material.stat.MaterialStat;
import com.nurby.tinkerslegacy.library.material.stat.PartStatDefinition;
import com.nurby.tinkerslegacy.library.material.datagen.MaterialRenderInfo;
import com.nurby.tinkerslegacy.registry.TLStatTypes;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record MaterialDefinition(
        ResourceLocation id,
        Map<ResourceLocation, MaterialStat> stats,
        Map<ResourceLocation, List<ResourceLocation>> traits,
        MaterialRecipeInfo recipeInfo,
        MaterialRenderInfo renderInfo,
        Optional<ResourceLocation> representativeItem,
        int color
) {
    public MaterialDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(recipeInfo, "recipeInfo");
        Objects.requireNonNull(renderInfo, "renderInfo");

        stats = Collections.unmodifiableMap(new LinkedHashMap<>(stats));

        Map<ResourceLocation, List<ResourceLocation>> copiedTraits = new LinkedHashMap<>();
        traits.forEach((type, values) -> copiedTraits.put(type, List.copyOf(values)));
        traits = Collections.unmodifiableMap(copiedTraits);
    }

    public Optional<MaterialStat> stats(PartStatDefinition definition) {
        return Optional.ofNullable(stats.get(definition.id()));
    }

    public List<ResourceLocation> traits(PartStatDefinition definition) {
        LinkedHashSet<ResourceLocation> combined = new LinkedHashSet<>(
                traits.getOrDefault(TLStatTypes.GENERAL.getId(), List.of())
        );

        combined.addAll(traits.getOrDefault(definition.id(), List.of()));

        return List.copyOf(combined);
    }

    public List<ResourceLocation> traitsForType(PartStatDefinition definition) {
        return traits.getOrDefault(definition.id(), List.of());
    }

    public List<ResourceLocation> parts() {
        return TinkersUtils.getCompatiblePartIds(stats.keySet());
    }
}