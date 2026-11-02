package com.nurby.tinkerslegacy.api.tool;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.api.tool.modifier.ModifierRuntime;
import com.nurby.tinkerslegacy.api.tool.traits.ToolTraits;
import com.nurby.tinkerslegacy.content.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.content.tools.CleaverDefinition;
import com.nurby.tinkerslegacy.content.tools.ProjectileToolDefinition;
import com.nurby.tinkerslegacy.content.tools.RangedToolDefinition;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLRegistries;
import com.nurby.tinkerslegacy.registry.tools.ToolStatTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

public final class ToolModifiers {

    private static final Map<ResourceLocation, Definition> MODIFIERS = new LinkedHashMap<>();

    // Variants

    public static final ResourceLocation DIAMOND = register("diamond", 0x8CF4E2, 1, 1, stack -> tagged(stack, "gems/diamond") || stack.is(Items.DIAMOND) ? 1 : 0, (tool, points, base, stats) -> {
        add(stats, "durability", 500);
        improveHarvest(stats, 3);
        add(stats, "attack_damage", 1);
        add(stats, "mining_speed", 0.5);
    });

    public static final ResourceLocation EMERALD = register("emerald", 0x41F384, 1, 1, stack -> tagged(stack, "gems/emerald") || stack.is(Items.EMERALD) ? 1 : 0, (tool, points, base, stats) -> {
        add(stats, "durability", base.getOrDefault("durability", 0).intValue() / 2);
        improveHarvest(stats, 2);
    });

    public static final ResourceLocation HASTE = register("haste", 0x910000, 50, 5, stack -> tagged(stack, "storage_blocks/redstone") || stack.is(Items.REDSTONE_BLOCK) ? 9 : tagged(stack, "dusts/redstone") || stack.is(Items.REDSTONE) ? 1 : 0, (tool, points, base, stats) -> {
        if (tool.isHarvestTool()) {
            double speed = stats.getOrDefault("mining_speed", 1).doubleValue();
            for (int i = 0; i < points; i++) {
                speed += speed <= 15 ? 0.15 - 0.05 * speed / 15 : speed <= 25 ? 0.1 - 0.05 * (speed - 15) / 10 : 0.05;
            }
            stats.put("mining_speed", speed + (points / 50) * 0.5);
        }
        if (tool.isWeapon()) {
            add(stats, "attack_speed", base.getOrDefault("attack_speed", 1).doubleValue() * 0.004 * points);
        }
        if (tool instanceof RangedToolDefinition) {
            add(stats, "draw_speed", stats.getOrDefault("draw_speed", 1).doubleValue() * 0.002 * points);
        }
    });

    public static final ResourceLocation SHARPNESS = register("sharpness", 0xFFF6F6, 72, 5, stack -> tagged(stack, "storage_blocks/quartz") || stack.is(Items.QUARTZ_BLOCK) ? 4 : tagged(stack, "gems/quartz") || stack.is(Items.QUARTZ) ? 1 : 0, (tool, points, base, stats) -> {
        double original = base.getOrDefault("attack_damage", 0).doubleValue();
        double attack = original;
        for (int i = 0; i < points; i++) {
            attack += attack <= 10 ? 0.05 - 0.025 * attack / 10 : attack <= 20 ? 0.025 - 0.01 * attack / 20 : 0.015;
        }
        add(stats, "attack_damage", attack - original + (points / 72) * 0.25);
    });

    public static final ResourceLocation BANE = simple("bane_of_arthopods", 0x61BA49, 24, 5, stack -> stack.is(Items.FERMENTED_SPIDER_EYE));
    public static final ResourceLocation SMITE = simple("smite", 0xE8D500, 24, 5, stack -> named(stack, "consecrated_soil"));
    public static final ResourceLocation BEHEADING = combination("beheading", 0x10574B, 10, part(stack -> stack.is(Items.ENDER_PEARL), 1), part(stack -> stack.is(Items.OBSIDIAN), 1));
    public static final ResourceLocation BLASTING = combination("blasting", 0xFFAA23, 3, part(stack -> stack.is(Items.TNT), 3));
    public static final ResourceLocation FIERY = simple("fiery", 0xEA9E32, 25, 5, stack -> stack.is(Items.BLAZE_POWDER));
    public static final ResourceLocation FINS = combination("fins", 0xABCDEF, 1, part(stack -> stack.is(Items.COD) || stack.is(Items.SALMON) || stack.is(Items.TROPICAL_FISH) || stack.is(Items.PUFFERFISH), 2));
    public static final ResourceLocation GLOWING = combination("glowing", 0xFFFFAA, 1, part(stack -> stack.is(Items.GLOWSTONE_DUST), 2), part(stack -> stack.is(Items.ENDER_EYE), 1));
    public static final ResourceLocation HARVEST_WIDTH = simple("harvestwidth", 0xCAF6A2, 1, 1, stack -> named(stack, "expander_horizontal"));
    public static final ResourceLocation HARVEST_HEIGHT = simple("harvestheight", 0xCAF6A2, 1, 1, stack -> named(stack, "expander_vertical"));
    public static final ResourceLocation KNOCKBACK = simple("knockback", 0x9F9F9F, 10, 99, stack -> stack.is(Items.PISTON) || stack.is(Items.STICKY_PISTON));
    public static final ResourceLocation LUCK = register("luck", 0x5A82E2, 120, 3, stack -> stack.is(Items.LAPIS_BLOCK) || tagged(stack, "storage_blocks/lapis") ? 9 : stack.is(Items.LAPIS_LAZULI) || tagged(stack, "gems/lapis") ? 1 : 0, (tool, points, base, stats) -> {
    });
    public static final ResourceLocation MENDING_MOSS = simple("mending_moss", 0x43AB32, 1, 3, stack -> named(stack, "mending_moss"));
    public static final ResourceLocation NECROTIC = simple("necrotic", 0x5E0000, 1, 10, stack -> named(stack, "necrotic_bone") || tagged(stack, "bones/wither"));
    public static final ResourceLocation REINFORCED = simple("reinforced", 0x502E83, 1, 5, stack -> named(stack, "reinforced_plate"));
    public static final ResourceLocation SHULKING = simple("shulking", 0xAACCFF, 50, 1, stack -> stack.is(Items.POPPED_CHORUS_FRUIT));
    public static final ResourceLocation SILKTOUCH = register("silktouch", 0xFBE28B, 1, 1, stack -> named(stack, "silky_jewel") ? 1 : 0, (tool, points, base, stats) -> {
        stats.put("mining_speed", Math.max(1, stats.getOrDefault("mining_speed", 1).doubleValue() - 3));
        stats.put("attack_damage", Math.max(1, stats.getOrDefault("attack_damage", 1).doubleValue() - 3));
    });
    public static final ResourceLocation WEBBED = simple("webbed", 0xFFFFFF, 1, 3, stack -> stack.is(Items.COBWEB));
    public static final ResourceLocation SOULBOUND = simple("soulbound", 0xF5FBAC, 1, 1, stack -> stack.is(Items.NETHER_STAR));
    public static final ResourceLocation CREATIVE = simple("creative", 0xFFFFFF, 1, Integer.MAX_VALUE, stack -> named(stack, "creative_modifier"));
    public static final ResourceLocation FORTIFY = id("fortify");
    public static final ResourceLocation EMBOSSMENT = simple("extratrait", 0xDDDDDD, 1, 1, stack -> false);

    static {
        get(BLASTING).policy = SlotPolicy.FIRST_LEVEL;
        get(LUCK).policy = SlotPolicy.FIRST_LEVEL;
        get(LUCK).thresholds = new int[]{60, 180, 360};
        get(SOULBOUND).policy = SlotPolicy.FREE;
        get(CREATIVE).policy = SlotPolicy.FREE;
        get(EMBOSSMENT).policy = SlotPolicy.FREE;
    }

    private ToolModifiers() {
    }

    // Registration

    public static void register(ResourceLocation id, Definition definition) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(definition);
        if (MODIFIERS.putIfAbsent(id, definition) != null) {
            throw new IllegalArgumentException("Duplicate modifier: " + id);
        }
    }

    private static ResourceLocation register(String name, int color, int points, int levels, ToIntFunction<ItemStack> ingredient, Effect effect) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, name);
        register(id, new Definition(points, levels, ingredient, effect).color(color));
        return id;
    }

    private static ResourceLocation simple(String name, int color, int points, int levels, Predicate<ItemStack> ingredient) {
        return register(name, color, points, levels, stack -> ingredient.test(stack) ? 1 : 0, (tool, count, base, stats) -> {
        });
    }

    private static ResourceLocation combination(String name, int color, int levels, Ingredient... ingredients) {
        ResourceLocation id = simple(name, color, 1, levels, stack -> false);
        get(id).recipe = List.of(ingredients);
        return id;
    }

    private static Ingredient part(Predicate<ItemStack> predicate, int count) {
        return new Ingredient(predicate, count);
    }

    // Getters

    public static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, name);
    }

    public static boolean isIngredient(ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem() instanceof com.nurby.tinkerslegacy.content.item.dynamic.DynamicPart || ToolRepair.isKit(stack) || stack.is(Items.FLINT) || stack.is(Items.GOLD_BLOCK) || named(stack, "green_slime_crystal") || named(stack, "blue_slime_crystal") || named(stack, "magma_slime_crystal") || MODIFIERS.values().stream().anyMatch(definition -> definition.matches(stack)));
    }

    public static Definition get(ResourceLocation id) {
        if (isFortify(id))
            return FORTIFICATION;
        return MODIFIERS.get(id);
    }

    public static int color(ItemStack stack, ResourceLocation id) {
        if (isFortify(id)) {
            ResourceLocation material = ResourceLocation.tryParse(id.getPath().substring(8).replaceFirst("/", ":"));
            var definition = material == null || !TLRegistries.MATERIALS.containsKey(material) ? null : TLRegistries.MATERIALS.get(material);
            return definition == null ? 0xDDDDDD : definition.color();
        }
        if (id.equals(EMBOSSMENT)) {
            List<ResourceLocation> materials = stack.getOrDefault(TLDataComponents.EMBOSSMENT, List.of());
            if (!materials.isEmpty()) {
                ResourceLocation material = materials.getFirst();
                if (TLRegistries.MATERIALS.containsKey(material))
                    return TLRegistries.MATERIALS.get(material).color();
            }
        }
        if (id.equals(FORTIFY))
            return 0xDDDDDD;
        Definition definition = get(id);
        return definition == null ? 0xFFFFFF : definition.color();
    }

    public static int points(ItemStack stack, ResourceLocation id) {
        return stack.getOrDefault(TLDataComponents.MODIFIERS, List.<ToolModifierEntry>of()).stream().filter(entry -> entry.id().equals(id)).mapToInt(ToolModifierEntry::points).sum();
    }

    public static int level(ItemStack stack, ResourceLocation id) {
        Definition definition = get(id);
        return definition == null ? 0 : definition.level(points(stack, id));
    }

    public static boolean sameModifiers(ItemStack first, ItemStack second) {
        List<ToolModifierEntry> left = first.getOrDefault(TLDataComponents.MODIFIERS, List.of());
        List<ToolModifierEntry> right = second.getOrDefault(TLDataComponents.MODIFIERS, List.of());
        if (left.size() != right.size() || !Objects.equals(first.get(TLDataComponents.EMBOSSMENT), second.get(TLDataComponents.EMBOSSMENT)))
            return false;
        for (int index = 0; index < left.size(); index++) {
            ToolModifierEntry a = left.get(index);
            ToolModifierEntry b = right.get(index);
            if (!a.id().equals(b.id()))
                return false;
            Definition definition = get(a.id());
            if (definition == null ? a.points() != b.points() : definition.startedLevels(a.points()) != definition.startedLevels(b.points()))
                return false;
        }
        return true;
    }

    public static int displayLevel(ItemStack stack, ResourceLocation id) {
        int level = level(stack, id);
        if (id.equals(BEHEADING) && stack.getItem() instanceof DynamicTool item && item.getDefinition() instanceof CleaverDefinition)
            level += 2;
        return Math.min(id.equals(BEHEADING) ? 10 : Integer.MAX_VALUE, level);
    }

    public static int bonusSlots(ItemStack stack) {
        return points(stack, CREATIVE);
    }

    public static boolean isFortify(ResourceLocation id) {
        return id.getNamespace().equals(TinkersLegacy.MODID) && id.getPath().startsWith("fortify/");
    }

    private static int fortifyHarvest(ResourceLocation id) {
        String[] material = id.getPath().substring(8).split("/", 2);
        if (material.length != 2)
            return -1;
        ResourceLocation materialId = ResourceLocation.fromNamespaceAndPath(material[0], material[1]);
        if (!TLRegistries.MATERIALS.containsKey(materialId))
            return -1;
        var head = TLRegistries.MATERIALS.get(materialId).stats().get(ToolStatTypes.HEAD.getId());
        return head == null ? -1 : head.statMap().getInt("harvest_level");
    }

    public static Map<String, Number> apply(ToolDefinition tool, ItemStack stack, Map<String, Number> base) {
        return apply(tool, stack, base, base);
    }

    public static Map<String, Number> apply(ToolDefinition tool, ItemStack stack, Map<String, Number> base, Map<String, Number> initialStats) {
        Map<String, Number> stats = new HashMap<>(initialStats);
        for (ToolModifierEntry entry : stack.getOrDefault(TLDataComponents.MODIFIERS, List.<ToolModifierEntry>of())) {
            if (isFortify(entry.id())) {
                // The kit sets the tier; later modifiers still apply in acquisition order.
                int harvest = fortifyHarvest(entry.id());
                for (String key : HARVEST_KEYS) {
                    if (harvest >= 0 && stats.containsKey(key)) {
                        stats.put(key, harvest);
                    }
                }
                continue;
            }
            Definition definition = get(entry.id());
            if (definition != null) {
                definition.effect().apply(tool, Math.min(entry.points(), definition.maxPoints()), base, stats);
            }
        }
        stats.put("durability", Math.max(1, stats.getOrDefault("durability", 1).intValue()));
        stats.put("free_modifiers", ToolState.freeModifiers(stack));
        return Map.copyOf(stats);
    }

    // Modification

    public static ItemStack applyIngredient(ItemStack tool, ItemStack ingredient) {
        return ingredient.isEmpty() ? ItemStack.EMPTY : modify(tool, List.of(ingredient.copyWithCount(1)));
    }

    public static ItemStack modify(ItemStack original, List<ItemStack> ingredients) {
        if (!(original.getItem() instanceof DynamicTool item))
            return ItemStack.EMPTY;
        ItemStack result = original.copyWithCount(1);
        ToolState.initialize(result, false);
        List<ItemStack> remaining = ingredients.stream().map(ItemStack::copy).toList();
        Set<ResourceLocation> advanced = new HashSet<>();
        Set<Integer> touched = new HashSet<>();
        if (fortify(result, remaining, touched))
            advanced.add(FORTIFY);
        if (emboss(result, remaining, touched))
            advanced.add(EMBOSSMENT);
        for (var registration : MODIFIERS.entrySet()) {
            ResourceLocation id = registration.getKey();
            Definition definition = registration.getValue();
            if (!canApply(result, id))
                continue;
            int initial = points(original, id);
            while (true) {
                int[] consumption = definition.consumption(remaining);
                if (consumption == null)
                    break;
                int increment = definition.recipe.isEmpty() ? 0 : 1;
                if (definition.recipe.isEmpty()) {
                    // An oversized block must not hide a smaller ingredient in a later slot.
                    boolean found = false;
                    for (int index = 0; index < remaining.size(); index++) {
                        int value = definition.ingredient.applyAsInt(remaining.get(index));
                        if (!remaining.get(index).isEmpty() && value > 0 && fits(result, id, value, initial)) {
                            consumption = new int[remaining.size()];
                            consumption[index] = 1;
                            increment = value;
                            found = true;
                            break;
                        }
                    }
                    if (!found)
                        break;
                }
                if (!fits(result, id, increment, initial))
                    break;
                setPoints(result, id, points(result, id) + increment);
                for (int index = 0; index < consumption.length; index++) {
                    if (consumption[index] > 0) {
                        remaining.get(index).shrink(consumption[index]);
                        touched.add(index);
                    }
                }
                advanced.add(id);
            }
        }
        if (advanced.isEmpty())
            return ItemStack.EMPTY;
        for (int index = 0; index < ingredients.size(); index++) {
            ItemStack input = ingredients.get(index);
            if (input.isEmpty() || touched.contains(index))
                continue;
            if (advanced.stream().noneMatch(id -> recognized(id, input)))
                return ItemStack.EMPTY;
        }
        if (!compatible(result))
            return ItemStack.EMPTY;
        item.calculateStats(result);
        for (int index = 0; index < ingredients.size(); index++) {
            ingredients.get(index).setCount(remaining.get(index).getCount());
        }
        return result;
    }

    private static boolean fits(ItemStack stack, ResourceLocation id, int addition, int initial) {
        Definition definition = get(id);
        int before = points(stack, id);
        long after = (long) before + addition;
        int maximum = definition.maxPoints();
        if (id.equals(BEHEADING) && ((DynamicTool) stack.getItem()).getDefinition() instanceof CleaverDefinition)
            maximum -= 2;
        if (after > maximum || after > Integer.MAX_VALUE)
            return false;
        // The original crafting operation starts at most one new level of each modifier.
        if (!id.equals(CREATIVE) && definition.startedLevels((int) after) - definition.startedLevels(initial) > 1)
            return false;
        return definition.slots((int) after) - definition.slots(before) <= ToolState.freeModifiers(stack);
    }

    public static void setPoints(ItemStack stack, ResourceLocation id, int points) {
        List<ToolModifierEntry> entries = new ArrayList<>(stack.getOrDefault(TLDataComponents.MODIFIERS, List.of()));
        for (int index = 0; index < entries.size(); index++) {
            if (entries.get(index).id().equals(id)) {
                entries.set(index, new ToolModifierEntry(id, points));
                stack.set(TLDataComponents.MODIFIERS, List.copyOf(entries));
                return;
            }
        }
        entries.add(new ToolModifierEntry(id, points));
        stack.set(TLDataComponents.MODIFIERS, List.copyOf(entries));
    }

    private static boolean canApply(ItemStack stack, ResourceLocation id) {
        ToolDefinition tool = ((DynamicTool) stack.getItem()).getDefinition();
        if (id.equals(HASTE) && tool instanceof ProjectileToolDefinition)
            return false;
        if (id.equals(FINS) && !(tool instanceof ProjectileToolDefinition))
            return false;
        if (id.equals(BLASTING) && !tool.isHarvestTool())
            return false;
        if (id.equals(LUCK) && !tool.isHarvestTool() && !tool.isWeapon() && !(tool instanceof ProjectileToolDefinition))
            return false;
        if ((id.equals(HARVEST_WIDTH) || id.equals(HARVEST_HEIGHT)) && !tool.isHarvestTool())
            return false;
        if (id.equals(LUCK) && (enchanted(stack, Enchantments.SILK_TOUCH) || has(stack, SILKTOUCH) || has(stack, BLASTING) || trait(stack, "squeaky")))
            return false;
        if (id.equals(SILKTOUCH) && (enchanted(stack, Enchantments.FORTUNE) || enchanted(stack, Enchantments.LOOTING) || has(stack, LUCK) || has(stack, BLASTING) || trait(stack, "squeaky") || trait(stack, "autosmelt")))
            return false;
        if (id.equals(BLASTING) && (enchanted(stack, Enchantments.SILK_TOUCH) || enchanted(stack, Enchantments.FORTUNE) || enchanted(stack, Enchantments.LOOTING) || has(stack, SILKTOUCH) || has(stack, LUCK) || trait(stack, "squeaky") || trait(stack, "autosmelt")))
            return false;
        return get(id).accepts.test(stack);
    }

    public static boolean compatible(ItemStack stack) {
        return (!has(stack, BLASTING) || !has(stack, SILKTOUCH) && !has(stack, LUCK) && !trait(stack, "squeaky") && !trait(stack, "autosmelt")) && (!has(stack, SILKTOUCH) || !has(stack, LUCK) && !trait(stack, "squeaky") && !trait(stack, "autosmelt")) && (!has(stack, LUCK) || !trait(stack, "squeaky")) && (!trait(stack, "autosmelt") || !trait(stack, "squeaky"));
    }

    private static boolean enchanted(ItemStack stack, ResourceKey<Enchantment> enchantment) {
        return stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).entrySet().stream().anyMatch(entry -> entry.getKey().is(enchantment) && entry.getIntValue() > 0);
    }

    public static boolean has(ItemStack stack, ResourceLocation id) {
        return points(stack, id) > 0;
    }

    private static boolean trait(ItemStack stack, String name) {
        return ToolTraits.isPresent(stack, id(name));
    }

    private static boolean fortify(ItemStack stack, List<ItemStack> ingredients, Set<Integer> touched) {
        DynamicTool item = (DynamicTool) stack.getItem();
        if (!item.getDefinition().isHarvestTool() || ingredients.stream().noneMatch(ToolRepair::isKit) || ingredients.stream().noneMatch(ingredient -> ingredient.is(Items.FLINT)))
            return false;
        List<ToolModifierEntry> entries = new ArrayList<>(stack.getOrDefault(TLDataComponents.MODIFIERS, List.of()));
        int previous = -1;
        for (int index = 0; index < entries.size(); index++) {
            if (isFortify(entries.get(index).id())) {
                previous = index;
                break;
            }
        }
        for (int index = 0; index < ingredients.size(); index++) {
            ResourceLocation material = ToolRepair.kitMaterial(ingredients.get(index));
            if (material == null)
                continue;
            ResourceLocation id = id("fortify/" + material.getNamespace() + "/" + material.getPath());
            if (has(stack, id))
                continue;
            // Preserve acquisition order when changing the kit material.
            List<ToolModifierEntry> changed = new ArrayList<>(entries);
            ToolModifierEntry entry = new ToolModifierEntry(id, 1);
            if (previous >= 0)
                changed.set(previous, entry);
            else
                changed.add(entry);
            for (int flint = 0; flint < ingredients.size(); flint++) {
                if (!ingredients.get(flint).is(Items.FLINT))
                    continue;
                stack.set(TLDataComponents.MODIFIERS, List.copyOf(changed));
                ingredients.get(index).shrink(1);
                ingredients.get(flint).shrink(1);
                touched.add(index);
                touched.add(flint);
                return true;
            }
        }
        return false;
    }

    private static boolean emboss(ItemStack stack, List<ItemStack> ingredients, Set<Integer> touched) {
        if (has(stack, EMBOSSMENT) || stack.has(TLDataComponents.EMBOSSMENT))
            return false;
        ToolDefinition tool = ((DynamicTool) stack.getItem()).getDefinition();
        for (int index = 0; index < ingredients.size(); index++) {
            ItemStack part = ingredients.get(index);
            for (ToolPartRequirement requirement : tool.partRequirements()) {
                if (!ToolAssembly.matches(part, requirement))
                    continue;
                List<ResourceLocation> layers = part.getOrDefault(TLDataComponents.MATERIALS, List.of());
                if (layers.isEmpty())
                    continue;

                Definition recipe = new Definition(1, 1, ignored -> 0, (definition, points, base, stats) -> {
                });
                int selected = index;
                recipe.recipe = List.of(part(input -> input == ingredients.get(selected), 1), part(input -> named(input, "green_slime_crystal"), 1), part(input -> named(input, "blue_slime_crystal"), 1), part(input -> named(input, "magma_slime_crystal"), 1), part(input -> input.is(Items.GOLD_BLOCK), 1));

                int[] consumed = recipe.consumption(ingredients);
                if (consumed == null)
                    continue;

                stack.set(TLDataComponents.EMBOSSMENT, List.copyOf(layers));
                if (!compatible(stack)) {
                    stack.remove(TLDataComponents.EMBOSSMENT);
                    continue;
                }

                setPoints(stack, EMBOSSMENT, 1);
                for (int slot = 0; slot < consumed.length; slot++) {
                    if (consumed[slot] > 0) {
                        ingredients.get(slot).shrink(consumed[slot]);
                        touched.add(slot);
                    }
                }
                return true;
            }
        }
        return false;
    }

    private static boolean recognized(ResourceLocation id, ItemStack stack) {
        if (id.equals(FORTIFY))
            return ToolRepair.isKit(stack) || stack.is(Items.FLINT);
        if (id.equals(EMBOSSMENT))
            return stack.getItem() instanceof com.nurby.tinkerslegacy.content.item.dynamic.DynamicPart || named(stack, "green_slime_crystal") || named(stack, "blue_slime_crystal") || named(stack, "magma_slime_crystal") || stack.is(Items.GOLD_BLOCK);
        return get(id).matches(stack);
    }

    private static boolean named(ItemStack stack, String name) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(id(name));
    }

    private static boolean tagged(ItemStack stack, String path) {
        TagKey<Item> tag = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
        return stack.is(tag);
    }

    private static void add(Map<String, Number> stats, String key, double value) {
        stats.put(key, stats.getOrDefault(key, 0).doubleValue() + value);
    }

    private static void improveHarvest(Map<String, Number> stats, int maximum) {
        for (String key : HARVEST_KEYS) {
            if (stats.containsKey(key)) {
                int current = stats.get(key).intValue();
                if (current < maximum)
                    stats.put(key, current + 1);
            }
        }
    }

    private static final List<String> HARVEST_KEYS = List.of("harvest_level", "harvest_level_axe", "harvest_level_shovel", "harvest_level_pickaxe", "harvest_level_pick");
    private static final Definition FORTIFICATION = new Definition(1, 1, stack -> 0, (tool, points, base, stats) -> {
    });

    static {
        FORTIFICATION.policy = SlotPolicy.FREE;
    }

    private enum SlotPolicy {
        EVERY_LEVEL, FIRST_LEVEL, FREE
    }

    public record Ingredient(Predicate<ItemStack> predicate, int count) {
        public Ingredient {
            Objects.requireNonNull(predicate);
            if (count < 1)
                throw new IllegalArgumentException("Modifier ingredient count must be positive");
        }
    }

    public static final class Definition {
        private int color = 0xFFFFFF;
        private final int pointsPerLevel;
        private final int maxLevels;
        private final ToIntFunction<ItemStack> ingredient;
        private final Effect effect;
        private SlotPolicy policy = SlotPolicy.EVERY_LEVEL;
        private List<Ingredient> recipe = List.of();
        private int[] thresholds;
        private Predicate<ItemStack> accepts = stack -> true;
        private ModifierRuntime runtime = new ModifierRuntime() {
        };

        public Definition(int pointsPerLevel, int maxLevels, ToIntFunction<ItemStack> ingredient, Effect effect) {
            if (pointsPerLevel < 1 || maxLevels < 1 || (long) pointsPerLevel * maxLevels > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Invalid modifier limits");
            }
            this.pointsPerLevel = pointsPerLevel;
            this.maxLevels = maxLevels;
            this.ingredient = Objects.requireNonNull(ingredient);
            this.effect = Objects.requireNonNull(effect);
        }

        public Definition color(int color) {
            this.color = color & 0xFFFFFF;
            return this;
        }

        public int color() {
            return color;
        }

        public Definition withoutSlotCost() {
            policy = SlotPolicy.FREE;
            return this;
        }

        public Definition firstLevelCostsSlot() {
            policy = SlotPolicy.FIRST_LEVEL;
            return this;
        }

        public Definition when(Predicate<ItemStack> predicate) {
            accepts = Objects.requireNonNull(predicate);
            return this;
        }

        public Definition recipe(Ingredient... ingredients) {
            recipe = List.of(ingredients);
            return this;
        }

        public Definition runtime(ModifierRuntime effects) {
            runtime = Objects.requireNonNull(effects);
            return this;
        }

        public ModifierRuntime runtime() {
            return runtime;
        }

        public int pointsPerLevel() {
            return pointsPerLevel;
        }

        public int maxLevels() {
            return maxLevels;
        }

        public ToIntFunction<ItemStack> ingredient() {
            return ingredient;
        }

        public Effect effect() {
            return effect;
        }

        public int maxPoints() {
            return thresholds == null ? pointsPerLevel * maxLevels : thresholds[thresholds.length - 1];
        }

        public int level(int points) {
            if (thresholds != null) {
                int level = 0;
                for (int threshold : thresholds)
                    if (points >= threshold)
                        level++;
                return level;
            }
            return points <= 0 ? 0 : Math.min(maxLevels, 1 + (points - 1) / pointsPerLevel);
        }

        public int nextThreshold(int points) {
            if (thresholds != null) {
                for (int threshold : thresholds)
                    if (points < threshold)
                        return threshold;
                return maxPoints();
            }
            return (int) Math.min(maxPoints(), ((long) points / pointsPerLevel + 1) * pointsPerLevel);
        }

        private int startedLevels(int points) {
            if (points <= 0)
                return 0;
            if (thresholds != null) {
                for (int index = 0; index < thresholds.length; index++) {
                    if (points <= thresholds[index])
                        return index + 1;
                }
                return thresholds.length;
            }
            return 1 + (points - 1) / pointsPerLevel;
        }

        public int slots(int points) {
            if (points <= 0 || policy == SlotPolicy.FREE)
                return 0;
            return policy == SlotPolicy.FIRST_LEVEL ? 1 : 1 + (points - 1) / pointsPerLevel;
        }

        private boolean matches(ItemStack stack) {
            return !stack.isEmpty() && (ingredient.applyAsInt(stack) > 0 || recipe.stream().anyMatch(part -> part.predicate.test(stack)));
        }

        private int[] consumption(List<ItemStack> inputs) {
            int[] used = new int[inputs.size()];
            if (recipe.isEmpty())
                return inputs.stream().anyMatch(this::matches) ? used : null;
            for (Ingredient part : recipe) {
                int needed = part.count;
                for (int index = 0; index < inputs.size() && needed > 0; index++) {
                    ItemStack stack = inputs.get(index);
                    if (!part.predicate.test(stack))
                        continue;
                    int count = Math.min(needed, stack.getCount() - used[index]);
                    used[index] += count;
                    needed -= count;
                }
                if (needed > 0)
                    return null;
            }
            return used;
        }
    }

    @FunctionalInterface
    public interface Effect {
        void apply(ToolDefinition tool, int points, Map<String, Number> base, Map<String, Number> stats);
    }
}