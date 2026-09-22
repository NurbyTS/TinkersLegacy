package com.nurby.tinkerslegacy;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TLConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLE_TOOLS;
    public static final ModConfigSpec.BooleanValue ENABLE_ARMOR;
    public static final ModConfigSpec.BooleanValue ENABLE_LEVELING;
    public static final ModConfigSpec.BooleanValue ENABLE_PLANNER;

    public static final ModConfigSpec.BooleanValue REUSE_STENCILS;
    public static final ModConfigSpec.BooleanValue CRAFT_CASTABLE_MATERIALS;
    public static final ModConfigSpec.BooleanValue ENABLE_CLAY_CASTS;
    public static final ModConfigSpec.BooleanValue ALLOW_BRICK_CASTING;
    public static final ModConfigSpec.BooleanValue ENABLE_OBSIDIAN_ALLOY;
    public static final ModConfigSpec.DoubleValue ORE_TO_INGOT_RATIO;
    public static final ModConfigSpec.BooleanValue ADD_LEATHER_DRYING_RECIPE;
    public static final ModConfigSpec.BooleanValue ADD_FLINT_RECIPE;
    public static final ModConfigSpec.BooleanValue CHESTS_KEEP_INVENTORY;
    public static final ModConfigSpec.BooleanValue RENDER_STATION_CONTENTS;
    public static final ModConfigSpec.BooleanValue GENERATE_SLIME_ISLANDS;
    public static final ModConfigSpec.BooleanValue GENERATE_ISLANDS_IN_SUPERFLAT;
    public static final ModConfigSpec.BooleanValue SLIME_ISLANDS_SURFACE_ONLY;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> SLIME_ISLAND_DIMENSION_BLACKLIST;
    public static final ModConfigSpec.IntValue SLIME_ISLAND_RATE;
    public static final ModConfigSpec.IntValue MAGMA_ISLAND_RATE;
    public static final ModConfigSpec.BooleanValue GENERATE_COBALT_ORE;
    public static final ModConfigSpec.BooleanValue GENERATE_ARDITE_ORE;
    public static final ModConfigSpec.IntValue COBALT_ORE_RATE;
    public static final ModConfigSpec.IntValue ARDITE_ORE_RATE;

    public static final ModConfigSpec.IntValue NEW_TOOL_MIN_MODIFIERS;
    public static final ModConfigSpec.IntValue TOOL_MAXIMUM_LEVELS;
    public static final ModConfigSpec.IntValue TOOL_DEFAULT_BASE_XP;
    public static final ModConfigSpec.IntValue TOOL_LEVEL_XP_INCREASE;
    public static final Map<String, ModConfigSpec.IntValue> TOOL_BASE_XP;
    public static final ModConfigSpec.IntValue NEW_ARMOR_MIN_MODIFIERS;
    public static final ModConfigSpec.IntValue ARMOR_MAXIMUM_LEVELS;
    public static final ModConfigSpec.IntValue ARMOR_BASE_XP;
    public static final ModConfigSpec.IntValue ARMOR_LEVEL_XP_INCREASE;
    public static final ModConfigSpec.IntValue ARMOR_XP_GAIN_CAP;
    public static final ModConfigSpec.DoubleValue ARMOR_DAMAGE_TO_XP;

    public static final ModConfigSpec.BooleanValue ARMOR_COMPACT_VIEW;
    public static final ModConfigSpec.BooleanValue BOUNCY_USES_DURABILITY;
    public static final ModConfigSpec.BooleanValue SPAWN_WITH_ARMORY_BOOK;

    public static final ModConfigSpec.BooleanValue AUTOSMELT_FORTUNE_INTERACTION;
    public static final ModConfigSpec.BooleanValue MATCH_VANILLA_SLIMEBLOCK;
    public static final ModConfigSpec.BooleanValue LIMIT_PIGGYBACK_PACK;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> CRAFTING_STATION_BLACKLIST;

    static {
        BUILDER.push("modules");

        ENABLE_TOOLS = BUILDER
                .comment("Whether to enable the tools module")
                .define("enableTools", true);

        ENABLE_ARMOR = BUILDER
                .comment("Whether to enable the armor module")
                .define("enableArmor", true);

        ENABLE_LEVELING = BUILDER
                .comment("Whether to enable the leveling module")
                .define("enableLeveling", true);

        ENABLE_PLANNER = BUILDER
                .comment("Whether to enable the planner module")
                .define("enablePlanner", true);

        BUILDER.pop();

        BUILDER.push("core");

        REUSE_STENCILS = BUILDER
                .comment("Allows an existing stencil to be recut into another stencil.")
                .define("reuseStencils", true);

        CRAFT_CASTABLE_MATERIALS = BUILDER
                .comment("Allows normally cast-only materials to be shaped in the Part Builder.")
                .define("craftCastableMaterials", false);

        ENABLE_CLAY_CASTS = BUILDER
                .comment("Enables single-use clay casts.")
                .define("enableClayCasts", true);

        ALLOW_BRICK_CASTING = BUILDER
                .comment("Allows bricks to be cast from molten clay.")
                .define("allowBrickCasting", true);

        ENABLE_OBSIDIAN_ALLOY = BUILDER
                .comment("Allows water and lava to alloy into molten obsidian.")
                .define("enableObsidianAlloy", true);

        ORE_TO_INGOT_RATIO = BUILDER
                .comment("Ingots produced when an ore or raw material is melted.")
                .defineInRange("oreToIngotRatio", 2.0, 1.0, 64.0);

        ADD_LEATHER_DRYING_RECIPE = BUILDER
                .comment("Adds the cooked meat to leather drying recipes.")
                .define("addLeatherDryingRecipe", true);

        ADD_FLINT_RECIPE = BUILDER
                .comment("Adds the classic recipe converting three gravel into one flint.")
                .define("addFlintRecipe", true);

        CHESTS_KEEP_INVENTORY = BUILDER
                .comment("Pattern and Part Chests retain their contents when harvested.")
                .define("chestsKeepInventory", true);

        RENDER_STATION_CONTENTS = BUILDER
                .comment("Renders stored patterns, parts, and crafting ingredients on station tabletops.")
                .define("renderStationContents", true);

        GENERATE_SLIME_ISLANDS = BUILDER
                .comment("Generates classic slime islands in the Overworld and magma islands in the Nether.")
                .define("generateSlimeIslands", true);

        GENERATE_ISLANDS_IN_SUPERFLAT = BUILDER
                .comment("Allows slime islands in superflat worlds.")
                .define("generateIslandsInSuperflat", false);

        SLIME_ISLANDS_SURFACE_ONLY = BUILDER
                .comment("Restricts ordinary slime islands to surface dimensions.")
                .define("slimeIslandsSurfaceOnly", true);

        SLIME_ISLAND_DIMENSION_BLACKLIST = BUILDER
                .comment("Dimensions where ordinary slime islands cannot generate.")
                .defineListAllowEmpty(
                        "slimeIslandDimensionBlacklist",
                        List.of("minecraft:the_nether", "minecraft:the_end"),
                        value -> value instanceof String string && !string.isBlank()
                );

        SLIME_ISLAND_RATE = BUILDER
                .comment("Average chunks per ordinary slime island.")
                .defineInRange("slimeIslandRate", 730, 1, Integer.MAX_VALUE);

        MAGMA_ISLAND_RATE = BUILDER
                .comment("Average chunks per Nether magma island.")
                .defineInRange("magmaIslandRate", 100, 1, Integer.MAX_VALUE);

        GENERATE_COBALT_ORE = BUILDER
                .comment("Generates cobalt ore in Nether biomes.")
                .define("generateCobaltOre", true);

        GENERATE_ARDITE_ORE = BUILDER
                .comment("Generates ardite ore in Nether biomes.")
                .define("generateArditeOre", true);

        COBALT_ORE_RATE = BUILDER
                .comment("Cobalt generation attempts per chunk.")
                .defineInRange("cobaltOreRate", 20, 1, 256);

        ARDITE_ORE_RATE = BUILDER
                .comment("Ardite generation attempts per chunk.")
                .defineInRange("arditeOreRate", 20, 1, 256);

        BUILDER.pop();

        BUILDER.push("tools");

        BUILDER.pop();

        BUILDER.push("armory");

        ARMOR_COMPACT_VIEW = BUILDER
                .comment("Disables the armor preview panel in the Armor Station and Armor Forge.")
                .define("compactArmorStationView", false);

        BOUNCY_USES_DURABILITY = BUILDER
                .comment("Makes the Bouncy armor trait consume durability for each bounce.")
                .define("bouncyUsesDurability", true);

        SPAWN_WITH_ARMORY_BOOK = BUILDER
                .comment("Gives the Armory book once when a player first enters a world.")
                .define("spawnWithArmoryBook", true);

        BUILDER.pop();

        BUILDER.push("leveling");

        NEW_TOOL_MIN_MODIFIERS = BUILDER
                .comment("Starting modifier slots for newly built tools.")
                .defineInRange("newToolMinModifiers", 3, 0, Integer.MAX_VALUE);

        TOOL_MAXIMUM_LEVELS = BUILDER
                .comment("Maximum tool levels. A negative value means unlimited.")
                .defineInRange("toolMaximumLevels", -1, -1, Integer.MAX_VALUE);

        TOOL_DEFAULT_BASE_XP = BUILDER
                .comment("Base XP requirement for tools without a tool-specific override.")
                .defineInRange("toolDefaultBaseXp", 500, 1, Integer.MAX_VALUE);

        TOOL_LEVEL_XP_INCREASE = BUILDER
                .comment("Cumulative XP scaling per tool level.")
                .defineInRange("toolLevelXpIncrease", 50, 0, Integer.MAX_VALUE);

        BUILDER.push("toolBaseXp");

        Map<String, ModConfigSpec.IntValue> toolBaseXp = new LinkedHashMap<>();

        for (String tool : List.of(
                "pickaxe", "shovel", "hatchet", "mattock", "kama",
                "broadsword", "longsword", "rapier", "frypan", "battlesign",
                "shortbow", "arrow", "hammer", "excavator", "lumberaxe",
                "scythe", "cleaver", "longbow", "crossbow", "bolt",
                "shuriken", "mace"
        )) {
            boolean aoe = tool.equals("hammer")
                    || tool.equals("excavator")
                    || tool.equals("lumberaxe")
                    || tool.equals("scythe");

            toolBaseXp.put(
                    tool,
                    BUILDER.defineInRange(
                            tool,
                            aoe ? 4500 : 500,
                            1,
                            Integer.MAX_VALUE
                    )
            );
        }

        BUILDER.pop();

        TOOL_BASE_XP = Map.copyOf(toolBaseXp);

        NEW_ARMOR_MIN_MODIFIERS = BUILDER
                .comment("Starting modifier slots for newly built armor.")
                .defineInRange("newArmorMinModifiers", 3, 0, Integer.MAX_VALUE);

        ARMOR_MAXIMUM_LEVELS = BUILDER
                .comment("Maximum armor levels. A negative value means unlimited.")
                .defineInRange("armorMaximumLevels", -1, -1, Integer.MAX_VALUE);

        ARMOR_BASE_XP = BUILDER
                .comment("Base XP requirement for armor.")
                .defineInRange("armorBaseXp", 100, 1, Integer.MAX_VALUE);

        ARMOR_LEVEL_XP_INCREASE = BUILDER
                .comment("Cumulative XP scaling per armor level.")
                .defineInRange("armorLevelXpIncrease", 50, 0, Integer.MAX_VALUE);

        ARMOR_XP_GAIN_CAP = BUILDER
                .comment("Maximum XP a single damage event can grant to armor.")
                .defineInRange("armorXpGainCap", 100, 1, Integer.MAX_VALUE);

        ARMOR_DAMAGE_TO_XP = BUILDER
                .comment("Multiplier converting incoming damage to armor XP.")
                .defineInRange("armorDamageToXp", 0.25, 0.0, Double.MAX_VALUE);

        BUILDER.pop();

        BUILDER.push("tools");

        AUTOSMELT_FORTUNE_INTERACTION = BUILDER
                .comment("Luck/Fortune increases drops after Autosmelt.")
                .define("autosmeltFortuneInteraction", true);

        MATCH_VANILLA_SLIMEBLOCK = BUILDER
                .comment("Restricts the vanilla slime block to green slimeballs.")
                .define("matchVanillaSlimeblock", false);

        LIMIT_PIGGYBACK_PACK = BUILDER
                .comment("Limits piggyback packs to players and mobs that vanilla allows to be leashed.")
                .define("limitPiggybackPack", false);

        CRAFTING_STATION_BLACKLIST = BUILDER
                .comment("Block entity registry names or fully qualified class names the Crafting Station must not connect to.")
                .defineListAllowEmpty(
                        "craftingStationBlacklist",
                        List.of("de.ellpeck.actuallyadditions.mod.tile.TileEntityItemViewer"),
                        value -> value instanceof String string && !string.isBlank()
                );

        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    public static final ModConfigSpec SPEC;

    private TLConfig() {
    }
}