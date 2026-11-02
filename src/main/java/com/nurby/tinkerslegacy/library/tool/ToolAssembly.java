package com.nurby.tinkerslegacy.library.tool;

import com.nurby.tinkerslegacy.item.dynamic.DynamicPart;
import com.nurby.tinkerslegacy.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.library.material.layer.MaterialLayer;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLItems;
import com.nurby.tinkerslegacy.registry.TLRegistries;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class ToolAssembly {

        private ToolAssembly() {
        }

        // Assembly
        public static ItemStack assemble(
                        ToolDefinition definition,
                        List<ItemStack> inputs) {
                List<ToolPartRequirement> requirements = definition.partRequirements();

                if (inputs.size() != requirements.size()) {
                        return ItemStack.EMPTY;
                }

                List<MaterialLayer> materials = new ArrayList<>();

                for (int index = 0; index < requirements.size(); index++) {
                        ToolPartRequirement requirement = requirements.get(index);
                        ItemStack input = inputs.get(index);

                        if (!matches(input, requirement)) {
                                return ItemStack.EMPTY;
                        }

                        List<MaterialLayer> layers = input.get(TLDataComponents.MATERIALS);

                        for (var statType : requirement.statTypes()) {
                                MaterialLayer layer = layers.stream()
                                                .filter(material -> material.statType().equals(statType))
                                                .findFirst()
                                                .orElse(null);

                                if (layer == null) {
                                        return ItemStack.EMPTY;
                                }

                                materials.add(layer);
                        }
                }

                return create(definition, materials);
        }

        public static ItemStack create(
                        ToolDefinition definition,
                        List<MaterialLayer> materials) {
                return create(definition, materials, null);
        }

        public static ItemStack create(
                        ToolDefinition definition,
                        List<MaterialLayer> materials,
                        @Nullable HolderLookup.Provider registries) {
                if (!supportsMaterials(definition, materials)
                                || materials.stream().anyMatch(layer -> !TLRegistries.MATERIALS.get(layer.material()).available())) {
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

                if (registries == null) item.calculateStats(result);
                else item.calculateStats(result, registries);
                return result;
        }

        // Validation
        public static boolean supportsMaterials(
                        ToolDefinition definition,
                        List<MaterialLayer> materials) {
                if (materials.size() != definition.parts().size()) {
                        return false;
                }

                for (int index = 0; index < materials.size(); index++) {
                        MaterialLayer material = materials.get(index);
                        ToolPart requirement = definition.parts().get(index);

                        if (!material.statType().equals(requirement.statType()) || !validMaterial(material)
                                        || !TLRegistries.PARTS.containsKey(requirement.part())
                                        || !TLRegistries.PARTS.get(requirement.part()).statTypes().contains(requirement.statType())) {
                                return false;
                        }
                }

                return definition.acceptsMaterials(materials);
        }

        public static boolean matches(ItemStack input, ToolPartRequirement requirement) {
                if (input.isEmpty() || !(input.getItem() instanceof DynamicPart part)
                                || !part.definition().id().equals(requirement.part())) {
                        return false;
                }

                List<MaterialLayer> layers = input.get(TLDataComponents.MATERIALS);

                if (layers == null || layers.size() != part.definition().statTypes().size()
                                || layers.stream().map(MaterialLayer::statType).distinct().count() != layers.size()) {
                        return false;
                }

                for (MaterialLayer layer : layers) {
                        if (!part.definition().statTypes().contains(layer.statType()) || !validMaterial(layer)
                                        || !TLRegistries.MATERIALS.get(layer.material()).available()) {
                                return false;
                        }
                }

                return requirement.statTypes().stream()
                                .allMatch(role -> layers.stream().anyMatch(layer -> layer.statType().equals(role)));
        }

        private static boolean validMaterial(MaterialLayer layer) {
                return TLRegistries.MATERIALS.containsKey(layer.material())
                                && TLRegistries.MATERIALS.get(layer.material()).stats().containsKey(layer.statType());
        }
}
