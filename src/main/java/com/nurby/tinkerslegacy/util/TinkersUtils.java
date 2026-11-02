package com.nurby.tinkerslegacy.util;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.api.material.MaterialDefinition;
import com.nurby.tinkerslegacy.api.material.stat.MaterialStat;
import com.nurby.tinkerslegacy.api.material.stat.MaterialStatMap;
import com.nurby.tinkerslegacy.api.material.stat.PartStatDefinition;
import com.nurby.tinkerslegacy.api.part.PartDefinition;
import com.nurby.tinkerslegacy.api.tool.ToolDefinition;
import com.nurby.tinkerslegacy.api.tool.ToolPart;
import com.nurby.tinkerslegacy.api.tool.ToolState;
import com.nurby.tinkerslegacy.content.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.text.DecimalFormat;
import java.util.*;

public final class TinkersUtils {
    private static final int COLOR_DURABILITY = 0x48CC48;
    private static final int COLOR_MINING_SPEED = 0x78A0CD;
    private static final int COLOR_ATTACK = 0xD76464;
    private static final int COLOR_MODIFIER = 0xB9B95A;
    private static final int COLOR_DRAWSPEED = 0x808080;
    private static final int COLOR_RANGE = 0x8CAFAF;

    private static final String STAT_DURABILITY = "durability";
    private static final String STAT_MINING_SPEED = "mining_speed";
    private static final String STAT_ATTACK_DAMAGE = "attack_damage";
    private static final String STAT_MODIFIER = "modifier";
    private static final String STAT_DRAWSPEED = "drawspeed";
    private static final String STAT_RANGE = "range";
    private static final String STAT_HARVEST_LEVEL = "harvest_level";
    private static final String STAT_FREE_MODIFIERS = "free_modifiers";

    public static final String STAT_HARVEST_LEVEL_PICK = "harvest_level_pickaxe";
    public static final String STAT_HARVEST_LEVEL_AXE = "harvest_level_axe";
    public static final String STAT_HARVEST_LEVEL_SHOVEL = "harvest_level_shovel";

    private TinkersUtils() {
    }

    public static List<Component> getStatsForLayer(ResourceLocation materialId, ResourceLocation statType,
            Component heading
    ) {
        List<Component> components = new ArrayList<>();

        MaterialDefinition material = TLRegistries.MATERIALS.get(materialId);

        if (material == null) {
            components.add(
                    Component.literal("Unknown material: " + materialId
                    ).withStyle(ChatFormatting.UNDERLINE)
            );

            return components;
        }

        components.add(
                heading.copy().withStyle(ChatFormatting.UNDERLINE)
        );

        MaterialStat stat = material.stats().get(statType);

        if (stat != null) {
            stat.statMap().values().forEach(
                    (name, value) -> addStat(components, name, value)
            );
        }

        return components;
    }

    public static List<Component> getStatsForLayer(ResourceLocation materialId, ResourceLocation statType
    ) {
        String key = statType.getNamespace()
                + ".material_stat_type."
                + statType.getPath();

        return getStatsForLayer(materialId, statType,
                translationOrFallback(key)
        );
    }

    public static void addToolPartTooltips(
            ToolDefinition definition,
            ItemStack stack,
            List<Component> tooltipComponents
    ) {
        List<ResourceLocation> materials =
                stack.get(TLDataComponents.MATERIALS);

        if (materials == null || materials.isEmpty()) {
            return;
        }

        boolean first = true;

        for (int index = 0; index < definition.parts().size(); index++) {
            ToolPart part = definition.parts().get(index);
            ResourceLocation materialId = index < materials.size() ? materials.get(index) : null;

            MaterialDefinition material = TLRegistries.MATERIALS.get(materialId);

            MutableComponent partName =
                    getMaterialPartName(
                            part.part(), materialId != null ? List.of(materialId) : List.of()
                    ).withColor(
                            material == null
                                    ? 0xFFFFFF
                                    : material.color()
                    );

            if (!first) {
                tooltipComponents.add(Component.empty());
            }

            first = false;

            if (materialId != null) {
                tooltipComponents.addAll(getStatsForLayer(materialId, part.statType(), partName));
            }
        }
    }

    public static void addStat(
            List<Component> tooltipComponents,
            String stat,
            Number value
    ) {
        Component valueComponent = formatStatValue(stat, value);

        MutableComponent statName =
                translationOrFallback(
                        TinkersLegacy.MODID + ".stat." + stat
                );

        tooltipComponents.add(
                statName
                        .append(": ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(valueComponent)
        );
    }

    private static Component formatStatValue(
            String stat,
            Number value
    ) {
        return switch (stat) {
            case STAT_DURABILITY ->
                    coloredValue(value, COLOR_DURABILITY);

            case STAT_MINING_SPEED ->
                    coloredValueRounded(value, COLOR_MINING_SPEED);

            case STAT_ATTACK_DAMAGE ->
                    coloredValueRounded(value, COLOR_ATTACK);

            case STAT_MODIFIER ->
                    coloredValue(value, COLOR_MODIFIER);

            case STAT_DRAWSPEED ->
                    coloredValueRounded(value, COLOR_DRAWSPEED);

            case STAT_RANGE ->
                    coloredValueRounded(value, COLOR_RANGE);

            case STAT_HARVEST_LEVEL, STAT_HARVEST_LEVEL_PICK, STAT_HARVEST_LEVEL_AXE, STAT_HARVEST_LEVEL_SHOVEL ->
                    formatHarvestLevel(value.intValue());

            case STAT_FREE_MODIFIERS ->
                    Component.literal(
                            String.valueOf(value.intValue())
                    );

            default ->
                    Component.literal(String.valueOf(value));
        };
    }

    private static Component formatHarvestLevel(int level) {
        ChatFormatting color = switch (level) {
            case 1 -> ChatFormatting.WHITE;
            case 2 -> ChatFormatting.AQUA;
            case 3 -> ChatFormatting.DARK_PURPLE;
            default -> ChatFormatting.GRAY;
        };

        String key =
                TinkersLegacy.MODID + ".mininglevel." + level;

        return translationOrFallback(key)
                .copy()
                .withStyle(color);
    }

    private static MutableComponent coloredValueRounded(
            Number value,
            int color
    ) {
        DecimalFormat format = new DecimalFormat("0.##");

        return Component.literal(
                format.format(value.doubleValue())
        ).withStyle(style -> style.withColor(color));
    }


    private static MutableComponent coloredValue(
            Number value,
            int color
    ) {
        return Component.literal(String.valueOf(value))
                .withStyle(style -> style.withColor(color));
    }

    public static Component formatMaterialName(List<ResourceLocation> materials
    ) {
        if (materials == null || materials.isEmpty()) {
            return translationOrFallback(
                    TinkersLegacy.MODID + ".material.unknown"
            );
        }

        List<MutableComponent> materialNames = materials.stream()
                .distinct()
                .map(material -> materialName(material, materials.stream().distinct().count() > 1))
                .toList();

        Component result = materialNames.getFirst();

        for (int i = 1; i < materialNames.size(); i++) {
            result = result.copy()
                    .append("-")
                    .append(materialNames.get(i));
        }

        return result;
    }

    // Names
    private static MutableComponent materialName(ResourceLocation material, boolean combined) {
        String key = material.getNamespace() + ".material." + material.getPath();
        return combined ? Component.translatableWithFallback(key + ".name", "%s", Component.translatable(key)) : Component.translatable(key);
    }

    public static MutableComponent getMaterialPartName(ResourceLocation part, List<ResourceLocation> materials) {
        return Component.translatable("item." + part.getNamespace() + ".parts." + part.getPath(), formatMaterialName(materials));
    }

    public static Component getToolName(ToolDefinition tool, List<ResourceLocation> materials) {
        Component name = Component.translatable("item." + tool.id().getNamespace() + ".tools." + tool.id().getPath());
        if (materials == null || materials.isEmpty()) {
            return name;
        }
        List<ResourceLocation> namingParts = tool.repairParts().stream().filter(index -> index >= 0 && index < materials.size()).map(materials::get).filter(TLRegistries.MATERIALS::containsKey).toList();
        if (namingParts.isEmpty()) {
            return name;
        }
        return Component.translatable("tinkerslegacy.tool.material_name", formatMaterialName(namingParts), name);
    }

    public static void addToolStatsTooltip(
            ItemStack stack,
            List<Component> tooltipComponents
    ) {
        Map<String, Number> stats = stack.getOrDefault(TLDataComponents.MODIFIED_STATS, Map.of());
        if (!(stack.getItem() instanceof DynamicTool dynTool)) {
            return;
        }
        ToolDefinition toolDef = dynTool.getDefinition();

        if (stats.isEmpty()) {
            return;
        }

        addDurabilityStat(
                stack,
                tooltipComponents, stats.getOrDefault(STAT_DURABILITY, 0)
        );

        if (toolDef.isHarvestTool()) {
            if (stats.containsKey(STAT_HARVEST_LEVEL)) {
                addStat(
                        tooltipComponents,
                        STAT_HARVEST_LEVEL,
                        stats.get(STAT_HARVEST_LEVEL)
                );
            }
            else {
                if (stats.containsKey(STAT_HARVEST_LEVEL_PICK)) {
                    addStat(
                            tooltipComponents,
                            STAT_HARVEST_LEVEL_PICK,
                            stats.get(STAT_HARVEST_LEVEL_PICK)
                    );
                }

                if (stats.containsKey(STAT_HARVEST_LEVEL_AXE)) {
                    addStat(
                            tooltipComponents,
                            STAT_HARVEST_LEVEL_AXE,
                            stats.get(STAT_HARVEST_LEVEL_AXE)
                    );
                }

                if (stats.containsKey(STAT_HARVEST_LEVEL_SHOVEL)) {
                    addStat(
                            tooltipComponents,
                            STAT_HARVEST_LEVEL_SHOVEL,
                            stats.get(STAT_HARVEST_LEVEL_SHOVEL)
                    );
                }
            }

            addStat(
                    tooltipComponents,
                    STAT_MINING_SPEED, stats.getOrDefault(STAT_MINING_SPEED, 1)
            );
        }

        addStat(
                tooltipComponents,
                STAT_ATTACK_DAMAGE,
                getActualDamage(stack)
        );

        addStat(
                tooltipComponents,
                STAT_FREE_MODIFIERS, stats.getOrDefault(STAT_FREE_MODIFIERS, 0)
        );
    }

    private static void addDurabilityStat(
            ItemStack stack,
            List<Component> tooltipComponents,
            Number maximumValue
    ) {
        int maximum = maximumValue.intValue();

        int current = ToolState.broken(stack) ? 0 : Math.max(
                maximum - ToolState.damage(stack),
                0
        );

        float ratio = maximum <= 0
                ? 0.0F
                : Mth.clamp(
                (float) current / maximum,
                0.0F,
                1.0F
        );

        int color = lerpColor(
                0xFF0000,
                COLOR_DURABILITY,
                ratio
        );

        Component value = Component.literal(
                current + "/" + maximum
        ).withStyle(
                style -> style.withColor(color)
        );

        MutableComponent statName =
                translationOrFallback(
                        TinkersLegacy.MODID
                                + ".stat."
                                + STAT_DURABILITY
                );

        tooltipComponents.add(
                statName
                        .append(": ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(value)
        );
    }

    private static int lerpColor(
            int from,
            int to,
            float ratio
    ) {
        ratio = Mth.clamp(
                ratio,
                0.0F,
                1.0F
        );

        int fromR = (from >> 16) & 0xFF;
        int fromG = (from >> 8) & 0xFF;
        int fromB = from & 0xFF;

        int toR = (to >> 16) & 0xFF;
        int toG = (to >> 8) & 0xFF;
        int toB = to & 0xFF;

        int r = Mth.floor(
                Mth.lerp(ratio, fromR, toR)
        );

        int g = Mth.floor(
                Mth.lerp(ratio, fromG, toG)
        );

        int b = Mth.floor(
                Mth.lerp(ratio, fromB, toB)
        );

        return (r << 16) | (g << 8) | b;
    }

    public static MaterialStat createMaterialStat(
            DeferredHolder<PartStatDefinition, PartStatDefinition> definition,
            Object... values
    ) {
        LinkedHashMap<String, Object> parsed =
                parseValues(
                        definition,
                        values
                );

        Map<String, Number> numericValues =
                new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : parsed.entrySet()) {
            if (!(entry.getValue() instanceof Number number)) {
                throw new IllegalArgumentException(
                        "Stat value for field '"
                                + entry.getKey()
                                + "' must be a Number"
                );
            }

            numericValues.put(
                    entry.getKey(),
                    number
            );
        }

        return new MaterialStat(
                definition.getId(),
                new MaterialStatMap(numericValues)
        );
    }

    private static LinkedHashMap<String, Object> parseValues(
            DeferredHolder<PartStatDefinition, PartStatDefinition> definition,
            Object[] values
    ) {
        if (values.length % 2 != 0) {
            throw new IllegalArgumentException(
                    "Stat values must be provided as field/value pairs"
            );
        }

        LinkedHashMap<String, Object> result =
                new LinkedHashMap<>();

        for (int i = 0; i < values.length; i += 2) {
            if (!(values[i] instanceof String name)) {
                throw new IllegalArgumentException(
                        "Stat field must be a String"
                );
            }

            Object value = values[i + 1];

            if (result.putIfAbsent(name, value) != null) {
                throw new IllegalArgumentException(
                        "Field '"
                                + name
                                + "' was provided more than once"
                );
            }
        }

        return result;
    }

    public static List<PartDefinition> getCompatibleParts(
            Collection<ResourceLocation> statTypes
    ) {
        return findCompatibleParts(statTypes);
    }

    public static List<ResourceLocation> getCompatiblePartIds(
            Collection<ResourceLocation> statTypes
    ) {
        return findCompatibleParts(statTypes).stream()
                .map(PartDefinition::id)
                .toList();
    }

    private static List<PartDefinition> findCompatibleParts(
            Collection<ResourceLocation> statTypes
    ) {
        List<PartDefinition> compatible =
                new ArrayList<>();

        for (PartDefinition part : TLRegistries.PARTS) {
            if (statTypes.containsAll(part.statTypes())) {
                compatible.add(part);
            }
        }

        return compatible;
    }

    private static MutableComponent translationOrFallback(String key) {
        return Component.translatable(key);
    }

    public static double getActualDamage(ItemStack stack) {
        Map<String, Number> stats = stack.getOrDefault(TLDataComponents.MODIFIED_STATS, Map.of());
        if (stats.isEmpty() || !(stack.getItem() instanceof DynamicTool tool)) {
            return 0.0;
        }
        double attackDamage = stats.getOrDefault(STAT_ATTACK_DAMAGE, 0).doubleValue();
        return tool.getDefinition().baseDamage() + attackDamage * tool.getDefinition().damagePotential();
    }

    public static double calcCutoffDamage(
            double damage,
            double cutoff
    ) {
        double multiplier = 1.0;
        double remaining = damage;
        double result = 0.0;

        while (remaining > cutoff) {
            result += multiplier * cutoff;

            if (multiplier > 0.001F) {
                multiplier *= 0.9F;
            } else {
                result += multiplier * cutoff * (
                        (remaining / cutoff) - 1.0F
                );
                return result;
            }

            remaining -= cutoff;
        }

        result += multiplier * remaining;

        return result;
    }
}