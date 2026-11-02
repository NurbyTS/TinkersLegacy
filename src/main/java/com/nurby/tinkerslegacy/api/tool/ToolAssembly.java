package com.nurby.tinkerslegacy.api.tool;

import com.nurby.tinkerslegacy.content.item.dynamic.DynamicPart;
import com.nurby.tinkerslegacy.content.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLItems;
import com.nurby.tinkerslegacy.registry.TLRegistries;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class ToolAssembly {

    private ToolAssembly() {
    }

    // Assembly
    public static ItemStack assemble(ToolDefinition definition, List<ItemStack> inputs) {
        List<ToolPartRequirement> requirements = definition.partRequirements();

        if (inputs.size() != requirements.size()) {
            return ItemStack.EMPTY;
        }

        List<ResourceLocation> materials = new ArrayList<>();

        for (int index = 0; index < requirements.size(); index++) {
            ToolPartRequirement requirement = requirements.get(index);
            ItemStack input = inputs.get(index);

            if (!matches(input, requirement)) {
                return ItemStack.EMPTY;
            }

            List<ResourceLocation> partMaterials = input.get(TLDataComponents.MATERIALS);
            if (partMaterials == null) {
                return ItemStack.EMPTY;
            }

            for (int statIndex = 0; statIndex < requirement.statTypes().size(); statIndex++) {
                if (statIndex >= partMaterials.size()) {
                    return ItemStack.EMPTY;
                }
                materials.add(partMaterials.get(statIndex));
            }
        }

        return create(definition, materials);
    }

    public static ItemStack create(ToolDefinition definition, List<ResourceLocation> materials) {
        return create(definition, materials, null);
    }

    public static ItemStack create(ToolDefinition definition, List<ResourceLocation> materials, @Nullable HolderLookup.Provider registries) {
        if (!supportsMaterials(definition, materials) || materials.stream().anyMatch(materialId -> {
            var mat = TLRegistries.MATERIALS.get(materialId);
            return mat == null || !mat.available();
        })) {
            return ItemStack.EMPTY;
        }

        DynamicTool item = TLItems.Tools.get(definition);

        if (item == null) {
            return ItemStack.EMPTY;
        }

        ItemStack result = new ItemStack(item);
        result.set(TLDataComponents.MATERIALS, List.copyOf(materials));
        ToolState.initialize(result, false);

        if (!ToolModifiers.compatible(result)) {
            return ItemStack.EMPTY;
        }

        if (registries == null)
            item.calculateStats(result);
        else
            item.calculateStats(result, registries);
        return result;
    }

    // Validation
    public static boolean supportsMaterials(ToolDefinition definition, List<ResourceLocation> materials) {
        if (materials.size() != definition.parts().size()) {
            return false;
        }

        for (int index = 0; index < materials.size(); index++) {
            ResourceLocation materialId = materials.get(index);
            ToolPart requirement = definition.parts().get(index);

            if (!validMaterial(materialId, requirement.statType()) || !TLRegistries.PARTS.containsKey(requirement.part()) || !TLRegistries.PARTS.get(requirement.part()).statTypes().contains(requirement.statType())) {
                return false;
            }
        }

        return definition.acceptsMaterials(materials);
    }

    public static boolean matches(ItemStack input, ToolPartRequirement requirement) {
        if (input.isEmpty() || !(input.getItem() instanceof DynamicPart part) || !part.definition().id().equals(requirement.part())) {
            return false;
        }

        List<ResourceLocation> partMaterials = input.get(TLDataComponents.MATERIALS);

        if (partMaterials == null || partMaterials.size() != part.definition().statTypes().size()) {
            return false;
        }

        for (int i = 0; i < partMaterials.size(); i++) {
            ResourceLocation materialId = partMaterials.get(i);
            ResourceLocation statType = part.definition().statTypes().get(i);

            if (!validMaterial(materialId, statType)) {
                return false;
            }

            var mat = TLRegistries.MATERIALS.get(materialId);
            if (mat == null || !mat.available()) {
                return false;
            }
        }

        return true;
    }

    private static boolean validMaterial(ResourceLocation materialId, ResourceLocation statType) {
        return TLRegistries.MATERIALS.containsKey(materialId) && TLRegistries.MATERIALS.get(materialId).stats().containsKey(statType);
    }
}