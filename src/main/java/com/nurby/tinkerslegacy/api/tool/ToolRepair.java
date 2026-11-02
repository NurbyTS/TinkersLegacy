package com.nurby.tinkerslegacy.api.tool;

import com.nurby.tinkerslegacy.api.station.MaterialInputs;
import com.nurby.tinkerslegacy.content.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLItems;
import com.nurby.tinkerslegacy.registry.TLRegistries;
import com.nurby.tinkerslegacy.registry.tools.ToolParts;
import com.nurby.tinkerslegacy.registry.tools.ToolStatTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ToolRepair {
    private ToolRepair() {
    }

    // Getters
    public static boolean isKit(ItemStack stack) {
        return stack.is(TLItems.Parts.item(ToolParts.Type.SHARPENING_KIT));
    }

    public static ResourceLocation kitMaterial(ItemStack stack) {
        if (!isKit(stack)) {
            return null;
        }
        List<ResourceLocation> layers = stack.get(TLDataComponents.MATERIALS);
        if (layers == null || layers.size() != 1) {
            return null;
        }
        ResourceLocation material = layers.getFirst();
        return TLRegistries.MATERIALS.containsKey(material) && TLRegistries.MATERIALS.get(material).stats().containsKey(ToolStatTypes.HEAD.getId()) ? material : null;
    }

    // Repair
    public static ItemStack repair(ItemStack original, List<ItemStack> ingredients) {
        if (!(original.getItem() instanceof DynamicTool tool)) {
            return ItemStack.EMPTY;
        }
        List<ResourceLocation> layers = original.get(TLDataComponents.MATERIALS);
        if (layers == null || !ToolAssembly.supportsMaterials(tool.getDefinition(), layers)) {
            return ItemStack.EMPTY;
        }
        ItemStack result = original.copyWithCount(1);
        ToolState.initialize(result, false);
        tool.calculateStats(result);
        if (ToolState.damage(result) <= 0 || !result.has(TLDataComponents.BASE_STATS)) {
            return ItemStack.EMPTY;
        }
        List<Integer> heads = tool.getDefinition().repairParts().stream().filter(index -> index >= 0 && index < layers.size()).toList();
        if (heads.isEmpty()) {
            return ItemStack.EMPTY;
        }
        List<ItemStack> checked = ingredients.stream().map(ItemStack::copy).toList();
        for (int index : heads) {
            ResourceLocation material = layers.get(index);
            consume(checked, material, true);
            while (consume(checked, material, false) > 0) {
            }
        }
        for (int index = 0; index < ingredients.size(); index++) {
            ItemStack stack = ingredients.get(index);
            if (!stack.isEmpty() && ItemStack.matches(stack, checked.get(index))) {
                return ItemStack.EMPTY;
            }
        }

        boolean repaired = false;
        while (ToolState.damage(result) > 0) {
            double raw = 0;
            Set<ResourceLocation> normalMaterials = new LinkedHashSet<>();
            boolean consumed = false;
            for (int index : heads) {
                ResourceLocation material = layers.get(index);
                if (normalMaterials.contains(material)) {
                    continue;
                }
                if (!TLRegistries.MATERIALS.containsKey(material) || !TLRegistries.MATERIALS.get(material).stats().containsKey(ToolStatTypes.HEAD.getId())) {
                    return ItemStack.EMPTY;
                }
                double durability = TLRegistries.MATERIALS.get(material).stats().get(ToolStatTypes.HEAD.getId()).statMap().getFloat("durability");
                // Each repair head tries a kit before the ordinary material match, as in Tinkers 2.
                int kit = consume(ingredients, material, true);
                int normal = consume(ingredients, material, false);
                consumed |= kit > 0 || normal > 0;
                raw += durability * (kit + normal) / MaterialInputs.UNIT * tool.getDefinition().repairModifierForPart(index);
                if (normal > 0) {
                    normalMaterials.add(material);
                }
            }
            if (!consumed) {
                break;
            }
            // Original custom kit repairs do not count toward the ordinary-material bonus.
            raw = Math.floor(raw * (1 + (normalMaterials.size() - 1) / 9.0));
            ToolState.repair(result, com.nurby.tinkerslegacy.api.tool.traits.ToolTraits.heal(result, repairAmount(result, raw)));
            repaired = true;
        }
        return repaired ? result : ItemStack.EMPTY;
    }

    private static int consume(List<ItemStack> ingredients, ResourceLocation material, boolean kit) {
        for (ItemStack stack : ingredients) {
            if (stack.isEmpty() || isKit(stack) != kit) {
                continue;
            }
            var match = MaterialInputs.match(stack);
            if (match != null && match.material().equals(material)) {
                stack.shrink(1);
                return match.value();
            }
            if (kit) {
                // Original kit matching examines the first kit stack before its material.
                return 0;
            }
        }
        return 0;
    }

    private static int repairAmount(ItemStack stack, double raw) {
        double original = stack.get(TLDataComponents.BASE_STATS).get("durability").doubleValue();
        int maximum = stack.getMaxDamage();
        int used = ToolState.usedModifiers(stack);
        double slots = 1.0 - Math.min(3, Math.max(0, used)) * 0.05;
        int repairs = stack.getOrDefault(TLDataComponents.REPAIR_COUNT, 0);
        double fatigue = Math.max(0.5, (100 - repairs / 2) / 100.0);
        double restore = Math.max(raw * Math.min(10, maximum / Math.max(1, original)), maximum / 64.0);
        return Math.max(1, (int) Math.ceil(restore * slots * fatigue));
    }
}