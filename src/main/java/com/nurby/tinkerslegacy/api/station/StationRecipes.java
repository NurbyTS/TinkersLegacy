package com.nurby.tinkerslegacy.api.station;

import com.nurby.tinkerslegacy.TLConfig;
import com.nurby.tinkerslegacy.api.tool.*;
import com.nurby.tinkerslegacy.content.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLItems;
import com.nurby.tinkerslegacy.registry.TLRegistries;
import com.nurby.tinkerslegacy.registry.tools.ToolParts;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public final class StationRecipes {
    @FunctionalInterface
    public interface Operation {
        StationResult evaluate(StationDefinition station, List<ItemStack> inputs, ResourceLocation selection, String name, int replacementIndex);
    }

    private static final Map<ResourceLocation, Operation> OPERATIONS = new LinkedHashMap<>();

    static {
        add(StationOperations.CUT_PATTERN, StationRecipes::cutPattern);
        add(StationOperations.BUILD_PART, StationRecipes::buildPart);
        add(StationOperations.ASSEMBLE, StationRecipes::assemble);
        add(StationOperations.MAINTAIN, StationRecipes::maintain);
        add(StationOperations.REPAIR, StationRecipes::repair);
        add(StationOperations.REPLACE, StationRecipes::replace);
        add(StationOperations.MODIFY, StationRecipes::modify);
        add(StationOperations.RENAME, StationRecipes::rename);
    }

    private StationRecipes() {
    }

    // Registration
    public static void add(ResourceLocation id, Operation operation) {
        if (OPERATIONS.putIfAbsent(id, operation) != null) {
            throw new IllegalArgumentException("Duplicate station operation: " + id);
        }
    }

    // Getters
    public static List<ResourceLocation> shapes() {
        var result = new LinkedHashSet<ResourceLocation>();
        for (var part : TLRegistries.PARTS) {
            // Bolt cores are cast around a shaft and had no stencil in Tinkers 2.
            if (!part.id().equals(ToolParts.get(ToolParts.Type.BOLT_CORE).getId())) {
                result.add(part.id());
            }
        }
        result.add(TLItems.Parts.MATERIAL_SHARD.getId());
        result.add(ToolParts.get(ToolParts.Type.SHARPENING_KIT).getId());
        return List.copyOf(result);
    }

    public static int partCost(ResourceLocation part) {
        if (TLItems.Parts.MATERIAL_SHARD.getId().equals(part)) {
            return MaterialInputs.SHARD;
        }
        var definition = TLRegistries.PARTS.get(part);
        return definition == null ? 0 : Math.multiplyExact(definition.castingCost(), MaterialInputs.UNIT);
    }

    public static StationResult evaluate(StationDefinition station, List<ItemStack> inputs, ResourceLocation operation, ResourceLocation selection, String name, int replacementIndex) {
        if (inputs.size() != station.inputSlots() || !station.operations().contains(operation)) {
            return invalid("operation");
        }
        var handler = OPERATIONS.get(operation);
        if (handler == null) {
            return invalid("operation");
        }
        return handler.evaluate(station, copy(inputs), selection, name == null ? "" : name, replacementIndex);
    }

    // Patterns
    private static StationResult cutPattern(StationDefinition station, List<ItemStack> inputs, ResourceLocation selection, String name, int replacementIndex) {
        if (selection == null || !shapes().contains(selection)) {
            return invalid("select_part");
        }
        ItemStack input = inputs.getFirst();
        boolean blank = input.is(TLItems.item(TLItems.Items.IntermediaryItems.Type.BLANK_PATTERN));
        boolean reusable = TLConfig.REUSE_STENCILS.get() && input.is(TLItems.item(TLItems.Items.IntermediaryItems.Type.PATTERN)) && !selection.equals(input.get(TLDataComponents.PART));
        if (!blank && !reusable) {
            return invalid("pattern");
        }
        ItemStack result = new ItemStack(TLItems.item(TLItems.Items.IntermediaryItems.Type.PATTERN));
        result.set(TLDataComponents.PART, selection);
        input.shrink(1);
        return success(result, inputs);
    }

    // Parts
    private static StationResult buildPart(StationDefinition station, List<ItemStack> inputs, ResourceLocation selection, String name, int replacementIndex) {
        if (inputs.size() < 4 || !inputs.getFirst().is(TLItems.item(TLItems.Items.IntermediaryItems.Type.PATTERN))) {
            return invalid("pattern");
        }
        var partId = inputs.getFirst().get(TLDataComponents.PART);
        if (partId == null || !shapes().contains(partId)) {
            return invalid("pattern");
        }
        boolean shard = TLItems.Parts.MATERIAL_SHARD.getId().equals(partId);
        var definition = TLRegistries.PARTS.get(partId);
        var item = TLItems.Parts.get(partId);
        if (!shard && (item == null || definition == null)) {
            return invalid("part");
        }
        ResourceLocation material = null;
        int value = 0;
        int cost = partCost(partId);
        for (int slot = 1; slot <= 2; slot++) {
            ItemStack stack = inputs.get(slot);
            if (stack.isEmpty()) {
                continue;
            }
            var match = MaterialInputs.match(stack);
            if (match == null || !match.craftable() || (material != null && !material.equals(match.material()))) {
                return invalid("material");
            }
            material = match.material();
            while (value < cost && !stack.isEmpty()) {
                value += match.value();
                stack.shrink(1);
            }
        }
        if (material == null || value < cost || !TLRegistries.MATERIALS.containsKey(material)) {
            return invalid("material_count");
        }
        var materialDefinition = TLRegistries.MATERIALS.get(material);
        if (materialDefinition == null || !MaterialInputs.craftable(materialDefinition) || (shard && !materialDefinition.hasShard()) || (!shard && !materialDefinition.stats().keySet().containsAll(definition.statTypes()))) {
            return invalid("material");
        }
        if (value > cost) {
            ItemStack remainder = MaterialInputs.remainder(material, value - cost, inputs.get(3));
            if (remainder.isEmpty())
                return invalid("remainder");
            inputs.set(3, remainder);
        }
        if (shard) {
            return success(MaterialInputs.shard(material, 1), inputs);
        }
        List<ResourceLocation> layers = new ArrayList<>();
        for (var statType : definition.statTypes()) {
            layers.add(material);
        }
        ItemStack result = new ItemStack(item.get());
        result.set(TLDataComponents.MATERIALS, List.copyOf(layers));
        return success(result, inputs);
    }

    // Assembly
    private static StationResult assemble(StationDefinition station, List<ItemStack> inputs, ResourceLocation selection, String name, int replacementIndex) {
        if (selection == null || !TLRegistries.TOOLS.containsKey(selection)) {
            return invalid("select_tool");
        }
        ToolDefinition definition = TLRegistries.TOOLS.get(selection);
        if (!supports(station, definition, StationOperations.ASSEMBLE)) {
            return invalid("tier");
        }
        int count = definition.partRequirements().size();
        if (count > inputs.size() || !emptyAfter(inputs, count)) {
            return invalid("part");
        }
        ItemStack result = ToolAssembly.assemble(definition, inputs.subList(0, count));
        if (result.isEmpty()) {
            return invalid("part");
        }
        for (int i = 0; i < count; i++) {
            inputs.get(i).shrink(1);
        }
        if (!name.isBlank() && name.length() <= 50 && name.chars().noneMatch(Character::isISOControl)) {
            result.set(DataComponents.CUSTOM_NAME, Component.literal(name.strip()));
        }
        return success(result, inputs);
    }

    // Maintenance
    private static StationResult maintain(StationDefinition station, List<ItemStack> inputs, ResourceLocation selection, String name, int replacementIndex) {
        if (!validTool(station, inputs, StationOperations.MAINTAIN)) {
            return invalid("tool");
        }
        if (name.length() > 50 || name.chars().anyMatch(Character::isISOControl)) {
            return invalid("name");
        }

        // The original repair screen tries each operation without consuming a failed preview.
        StationResult result = repair(station, copy(inputs), selection, name, replacementIndex);
        if (!result.valid()) {
            result = replaceMatchingParts(station, copy(inputs));
        }
        if (!result.valid()) {
            result = modify(station, copy(inputs), selection, name, replacementIndex);
        }
        if (!result.valid()) {
            result = rename(station, copy(inputs), selection, name, replacementIndex);
        }
        if (result.valid()) {
            ItemStack named = result.output().copy();
            if (name.isBlank()) {
                named.remove(DataComponents.CUSTOM_NAME);
            } else {
                named.set(DataComponents.CUSTOM_NAME, Component.literal(name.strip()));
            }
            return success(named, result.inputsAfter());
        }
        return result;
    }

    private static StationResult replaceMatchingParts(StationDefinition station, List<ItemStack> inputs) {
        if (!validTool(station, inputs, StationOperations.REPLACE)) {
            return invalid("tool");
        }
        ItemStack original = inputs.getFirst();
        var definition = ((DynamicTool) original.getItem()).getDefinition();
        List<ResourceLocation> layers = new ArrayList<>(original.get(TLDataComponents.MATERIALS));
        var assigned = new LinkedHashSet<Integer>();
        for (int slot = 1; slot < inputs.size(); slot++) {
            ItemStack ingredient = inputs.get(slot);
            if (ingredient.isEmpty()) {
                continue;
            }
            int candidate = -1;
            for (int index = 0; index < definition.partRequirements().size(); index++) {
                if (assigned.contains(index) || !ToolAssembly.matches(ingredient, definition.partRequirements().get(index))) {
                    continue;
                }
                List<ResourceLocation> replaced = replaceLayers(definition, layers, ingredient, index);
                if (!replaced.equals(layers)) {
                    candidate = index;
                    // Input zero is the tool; later ingredient slots target repeated physical parts.
                    if (slot - 1 <= index)
                        break;
                }
            }
            if (candidate < 0) {
                return invalid("replacement");
            }
            layers = replaceLayers(definition, layers, ingredient, candidate);
            assigned.add(candidate);
            ingredient.shrink(1);
        }
        if (assigned.isEmpty()) {
            return invalid("replacement");
        }
        // Apply all parts together; a physical slot can only be replaced once.
        return finishReplacement(original, layers, inputs);
    }

    private static StationResult repair(StationDefinition station, List<ItemStack> inputs, ResourceLocation selection, String name, int replacementIndex) {
        if (!validTool(station, inputs, StationOperations.REPAIR)) {
            return invalid("tool");
        }
        ItemStack result = ToolRepair.repair(inputs.getFirst(), inputs.subList(1, inputs.size()));
        if (result.isEmpty()) {
            return invalid("repair_material");
        }
        inputs.getFirst().shrink(1);
        return success(result, inputs);
    }

    private static StationResult replace(StationDefinition station, List<ItemStack> inputs, ResourceLocation selection, String name, int replacementIndex) {
        if (!validTool(station, inputs, StationOperations.REPLACE) || inputs.size() < 2 || !emptyAfter(inputs, 2)) {
            return invalid("tool");
        }
        ItemStack original = inputs.getFirst();
        ToolDefinition definition = ((DynamicTool) original.getItem()).getDefinition();
        if (replacementIndex < 0 || replacementIndex >= definition.partRequirements().size()) {
            return invalid("select_part");
        }
        var requirement = definition.partRequirements().get(replacementIndex);
        ItemStack replacement = inputs.get(1);
        if (!ToolAssembly.matches(replacement, requirement)) {
            return invalid("part");
        }
        List<ResourceLocation> layers = replaceLayers(definition, original.get(TLDataComponents.MATERIALS), replacement, replacementIndex);
        replacement.shrink(1);
        return finishReplacement(original, layers, inputs);
    }

    private static List<ResourceLocation> replaceLayers(ToolDefinition definition, List<ResourceLocation> current, ItemStack replacement, int replacementIndex) {
        List<ResourceLocation> layers = new ArrayList<>(current);
        int offset = 0;
        for (int index = 0; index < replacementIndex; index++) {
            offset += definition.partRequirements().get(index).statTypes().size();
        }
        List<ResourceLocation> partLayers = replacement.getOrDefault(TLDataComponents.MATERIALS, List.of());
        int statCount = definition.partRequirements().get(replacementIndex).statTypes().size();
        for (int i = 0; i < statCount && i < partLayers.size(); i++) {
            layers.set(offset + i, partLayers.get(i));
        }
        return layers;
    }

    private static StationResult finishReplacement(ItemStack original, List<ResourceLocation> layers, List<ItemStack> inputs) {
        var definition = ((DynamicTool) original.getItem()).getDefinition();
        if (layers.equals(original.get(TLDataComponents.MATERIALS)) || !ToolAssembly.supportsMaterials(definition, layers)) {
            return invalid("replacement");
        }
        ItemStack result = original.copyWithCount(1);
        int damage = ToolState.damage(original);
        result.set(TLDataComponents.MATERIALS, List.copyOf(layers));
        ((DynamicTool) result.getItem()).calculateStats(result);
        if (!ToolModifiers.compatible(result)) {
            return invalid("replacement_traits");
        }
        if (ToolState.availableModifiers(result) < 0) {
            return invalid("replacement_modifiers");
        }
        if (damage > result.getMaxDamage()) {
            return invalid("replacement_damage");
        }
        if (damage == result.getMaxDamage() && damage > 0) {
            result.set(TLDataComponents.BROKEN, true);
            result.set(TLDataComponents.BROKEN_DAMAGE, damage);
        }
        result.setDamageValue(Math.min(damage, Math.max(0, result.getMaxDamage() - 1)));
        inputs.getFirst().shrink(1);
        return success(result, inputs);
    }

    private static StationResult modify(StationDefinition station, List<ItemStack> inputs, ResourceLocation selection, String name, int replacementIndex) {
        if (!validTool(station, inputs, StationOperations.MODIFY)) {
            return invalid("tool");
        }
        ItemStack result = ToolModifiers.modify(inputs.getFirst(), inputs.subList(1, inputs.size()));
        if (result.isEmpty())
            return invalid("modifier");
        inputs.getFirst().shrink(1);
        return success(result, inputs);
    }

    private static StationResult rename(StationDefinition station, List<ItemStack> inputs, ResourceLocation selection, String name, int replacementIndex) {
        if (!validTool(station, inputs, StationOperations.RENAME) || !emptyAfter(inputs, 1)) {
            return invalid("tool");
        }
        if (name.length() > 50 || name.chars().anyMatch(Character::isISOControl)) {
            return invalid("name");
        }
        ItemStack result = inputs.getFirst().copyWithCount(1);
        if (name.isBlank()) {
            result.remove(DataComponents.CUSTOM_NAME);
        } else {
            result.set(DataComponents.CUSTOM_NAME, Component.literal(name.strip()));
        }
        if (ItemStack.isSameItemSameComponents(result, inputs.getFirst())) {
            return invalid("name");
        }
        inputs.getFirst().shrink(1);
        return success(result, inputs);
    }

    // Validation
    public static boolean supports(StationDefinition station, ToolDefinition tool, ResourceLocation operation) {
        var requirement = tool.stationRequirement(operation);
        return station.operations().contains(operation) && station.family().equals(requirement.family()) && station.tier() >= requirement.tier();
    }

    private static boolean validTool(StationDefinition station, List<ItemStack> inputs, ResourceLocation operation) {
        if (inputs.isEmpty() || !(inputs.getFirst().getItem() instanceof DynamicTool tool) || inputs.getFirst().getCount() != 1) {
            return false;
        }
        var layers = inputs.getFirst().get(TLDataComponents.MATERIALS);
        return layers != null && ToolAssembly.supportsMaterials(tool.getDefinition(), layers) && supports(station, tool.getDefinition(), operation);
    }

    private static boolean emptyAfter(List<ItemStack> inputs, int start) {
        return inputs.subList(start, inputs.size()).stream().allMatch(ItemStack::isEmpty);
    }

    private static List<ItemStack> copy(List<ItemStack> inputs) {
        return new ArrayList<>(inputs.stream().map(ItemStack::copy).toList());
    }

    private static StationResult success(ItemStack result, List<ItemStack> remaining) {
        return new StationResult(result, remaining, Component.empty());
    }

    private static StationResult invalid(String reason) {
        return StationResult.invalid(Component.translatable("station.tinkerslegacy.error." + reason));
    }
}