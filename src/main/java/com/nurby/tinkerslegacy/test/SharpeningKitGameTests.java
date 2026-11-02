package com.nurby.tinkerslegacy.test;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.library.station.StationOperations;
import com.nurby.tinkerslegacy.library.station.StationRecipes;
import com.nurby.tinkerslegacy.library.station.StationResult;
import com.nurby.tinkerslegacy.library.tool.ToolModifiers;
import com.nurby.tinkerslegacy.library.tool.ToolProgress;
import com.nurby.tinkerslegacy.library.tool.ToolState;
import com.nurby.tinkerslegacy.registry.TLBlocks;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLStations;
import com.nurby.tinkerslegacy.registry.TLTools;
import com.nurby.tinkerslegacy.registry.ToolParts;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import static com.nurby.tinkerslegacy.test.ToolAssemblyGameTests.id;
import static com.nurby.tinkerslegacy.test.ToolAssemblyGameTests.part;
import static com.nurby.tinkerslegacy.test.ToolAssemblyGameTests.tool;

@GameTestHolder(TinkersLegacy.MODID)
@PrefixGameTestTemplate(false)
public final class SharpeningKitGameTests {

        // Fortification
        @GameTest(template = "empty")
        public static void firstKitsAcceptEqualAndLowerMiningLevels(GameTestHelper helper) {
                ItemStack stack = tool(TLTools.Type.PICKAXE, id("iron"));
                ItemStack before = stack.copy();
                for (String material : List.of("wood", "stone", "iron")) {
                        List<ItemStack> inputs = kit(material);
                        ItemStack fortified = ToolModifiers.modify(stack, inputs);
                        helper.assertTrue(!fortified.isEmpty(), "Any valid first kit is accepted: " + material);
                        helper.assertValueEqual(harvest(fortified), List.of("wood", "stone", "iron").indexOf(material),
                                        "The kit sets mining level even when equal to or below the head");
                        helper.assertTrue(inputs.stream().allMatch(ItemStack::isEmpty), "One kit and one flint are consumed");
                        helper.assertTrue(ItemStack.matches(stack, before), "Fortifying leaves the original tool unchanged");
                        helper.assertValueEqual(ToolState.freeModifiers(fortified), ToolState.freeModifiers(stack),
                                        "Fortification consumes no modifier slot");
                }
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void kitsReplaceHigherAndLowerTiersAndCanSwitchBack(GameTestHelper helper) {
                ItemStack stack = tool(TLTools.Type.PICKAXE, id("wood"));
                for (String material : List.of("iron", "obsidian", "iron", "obsidian")) {
                        List<ItemStack> inputs = kit(material);
                        stack = ToolModifiers.modify(stack, inputs);
                        helper.assertTrue(!stack.isEmpty(), "A different material can replace or restore any kit tier");
                        helper.assertValueEqual(harvest(stack), material.equals("iron") ? 2 : 4,
                                        "Mining level follows the selected kit");
                        helper.assertTrue(inputs.stream().allMatch(ItemStack::isEmpty), "Each replacement consumes one kit and flint");
                        helper.assertValueEqual(stack.get(TLDataComponents.MODIFIERS).stream()
                                        .filter(entry -> ToolModifiers.isFortify(entry.id())).count(), 1L,
                                        "Only the current fortification is retained");
                        List<ItemStack> duplicate = kit(material);
                        helper.assertTrue(ToolModifiers.modify(stack, duplicate).isEmpty(), "The already installed material is rejected");
                        helper.assertTrue(duplicate.stream().allMatch(input -> input.getCount() == 1),
                                        "An unchanged kit consumes no ingredients");
                }
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void equalTierKitsSwitchMaterialAndPreserveToolState(GameTestHelper helper) {
                ItemStack stack = tool(TLTools.Type.PICKAXE, id("stone"));
                ToolModifiers.setPoints(stack, ToolModifiers.HASTE, 17);
                stack = ToolModifiers.modify(stack, kit("obsidian"));
                ToolModifiers.setPoints(stack, ToolModifiers.DIAMOND, 1);
                ((DynamicTool) stack.getItem()).calculateStats(stack);
                stack.set(TLDataComponents.PROGRESS, new ToolProgress(2, 73, 2));
                stack.set(TLDataComponents.REPAIR_COUNT, 4);
                stack.set(DataComponents.CUSTOM_NAME, Component.literal("Old Stone Pick"));
                stack.setDamageValue(5);
                ToolState.initialize(stack, true);
                ((DynamicTool) stack.getItem()).calculateStats(stack);
                ItemStack before = stack.copy();
                int slots = ToolState.freeModifiers(stack);
                for (String material : List.of("cobalt", "obsidian")) {
                        ItemStack inputBefore = stack.copy();
                        List<ItemStack> ingredients = kit(material);
                        ItemStack switched = ToolModifiers.modify(stack, ingredients);
                        helper.assertTrue(!switched.isEmpty(), "A different equal-tier kit replaces existing fortification");
                        helper.assertValueEqual(harvest(switched), 4, "Switching preserves Cobalt mining level");
                        helper.assertTrue(ingredients.stream().allMatch(ItemStack::isEmpty), "Switching costs one kit and one flint");
                        helper.assertValueEqual(switched.get(TLDataComponents.MODIFIERS).stream().map(entry -> entry.id()).toList(),
                                        List.of(ToolModifiers.HASTE, ToolModifiers.id("fortify/tinkerslegacy/" + material), ToolModifiers.DIAMOND),
                                        "Only the kit entry changes, in its original acquisition position");
                        helper.assertValueEqual(ToolModifiers.points(switched, ToolModifiers.HASTE), 17, "Partial modifier progress survives");
                        helper.assertValueEqual(ToolState.freeModifiers(switched), slots, "Switching costs no modifier slot");
                        helper.assertValueEqual(switched.get(TLDataComponents.PROGRESS), before.get(TLDataComponents.PROGRESS), "Tool experience survives");
                        helper.assertValueEqual(switched.get(TLDataComponents.TOOL_ID), before.get(TLDataComponents.TOOL_ID), "Combat identity survives");
                        helper.assertValueEqual(switched.get(TLDataComponents.MATERIALS), before.get(TLDataComponents.MATERIALS), "Head materials stay unchanged");
                        helper.assertValueEqual(switched.get(TLDataComponents.REPAIR_COUNT), 4, "Repair count survives");
                        helper.assertValueEqual(switched.get(DataComponents.CUSTOM_NAME), before.get(DataComponents.CUSTOM_NAME), "Custom name survives");
                        helper.assertValueEqual(switched.getDamageValue(), 5, "Switching never repairs the tool");
                        ((DynamicTool) switched.getItem()).calculateStats(switched);
                        helper.assertValueEqual(switched.get(TLDataComponents.MODIFIED_STATS), before.get(TLDataComponents.MODIFIED_STATS),
                                        "Repeated calculation preserves all stats when only the kit's material changes");
                        helper.assertTrue(ItemStack.matches(stack, inputBefore), "The original input tool is untouched");
                        stack = switched;
                }
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void equalKitTiersPreserveLaterDiamondBoost(GameTestHelper helper) {
                ItemStack stack = ToolModifiers.modify(tool(TLTools.Type.PICKAXE, id("wood")), kit("iron"));
                stack = ToolModifiers.applyIngredient(stack, new ItemStack(Items.DIAMOND));
                helper.assertValueEqual(harvest(stack), 3, "Diamond raises the installed Iron kit's final tier");
                ItemStack before = stack.copy();
                List<ItemStack> ingredients = kit("bronze");
                ItemStack swapped = ToolModifiers.modify(stack, ingredients);
                helper.assertTrue(!swapped.isEmpty(), "Bronze can replace Iron even when Diamond raises the final tier");
                helper.assertValueEqual(harvest(swapped), 3, "The later Diamond boost remains active after the swap");
                helper.assertValueEqual(swapped.get(TLDataComponents.MODIFIERS).stream().map(entry -> entry.id()).toList(),
                                List.of(ToolModifiers.id("fortify/tinkerslegacy/bronze"), ToolModifiers.DIAMOND),
                                "The kit stays before Diamond in acquisition order");
                helper.assertTrue(ingredients.stream().allMatch(ItemStack::isEmpty), "Equal installed-tier swap consumes one kit and flint");
                helper.assertTrue(ItemStack.matches(stack, before), "Swapping leaves the original tool untouched");
                ((DynamicTool) swapped.getItem()).calculateStats(swapped);
                helper.assertValueEqual(harvest(swapped), 3, "Recalculation retains the Diamond-boosted mining tier");
                List<ItemStack> lower = kit("stone");
                ItemStack downgraded = ToolModifiers.modify(swapped, lower);
                helper.assertTrue(!downgraded.isEmpty(), "A lower kit can replace the installed kit despite Diamond");
                helper.assertValueEqual(harvest(downgraded), 2, "The lower Stone kit is still followed by Diamond's one-level boost");
                helper.assertTrue(lower.stream().allMatch(ItemStack::isEmpty), "A lower kit consumes one kit and flint");
                ((DynamicTool) downgraded.getItem()).calculateStats(downgraded);
                helper.assertValueEqual(harvest(downgraded), 2, "Recalculation preserves the original modifier order after downgrading");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void kitTierOverridesStrongerReplacementHead(GameTestHelper helper) {
                ItemStack stack = ToolModifiers.modify(tool(TLTools.Type.PICKAXE, id("wood")), kit("iron"));
                StationResult replacement = StationRecipes.evaluate(TLStations.TOOL_STATION,
                                inputs(stack, part(ToolParts.Type.PICKAXE_HEAD, id("obsidian"))),
                                StationOperations.REPLACE, null, "", 0);
                helper.assertTrue(replacement.valid(), "Obsidian head replacement is supported");
                stack = replacement.output();
                helper.assertValueEqual(harvest(stack), 2, "The installed Iron kit overrides the stronger replacement head");
                List<ItemStack> ingredients = kit("bronze");
                ItemStack swapped = ToolModifiers.modify(stack, ingredients);
                helper.assertTrue(!swapped.isEmpty(), "An equal Iron-to-Bronze kit swap remains valid");
                helper.assertValueEqual(harvest(swapped), 2, "The stronger head does not raise the kit's mining level");
                helper.assertTrue(ingredients.stream().allMatch(ItemStack::isEmpty), "One kit and flint are consumed");
                List<ItemStack> lower = kit("stone");
                ItemStack downgraded = ToolModifiers.modify(swapped, lower);
                helper.assertTrue(!downgraded.isEmpty(), "A lower kit can replace a kit over a stronger head");
                helper.assertValueEqual(harvest(downgraded), 1, "Stone sets mining level below both the former kit and the head");
                helper.assertTrue(lower.stream().allMatch(ItemStack::isEmpty), "The lower kit consumes one kit and flint");
                StationResult weaker = StationRecipes.evaluate(TLStations.TOOL_STATION,
                                inputs(downgraded, part(ToolParts.Type.PICKAXE_HEAD, id("wood"))),
                                StationOperations.REPLACE, null, "", 0);
                helper.assertTrue(weaker.valid(), "Later wood-head replacement remains supported");
                helper.assertValueEqual(harvest(weaker.output()), 1, "The Stone kit also overrides a weaker head");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void kitsReplayAfterEarlierDiamondRegardlessOfCachedStats(GameTestHelper helper) {
                ItemStack stack = tool(TLTools.Type.PICKAXE, id("stone"));
                ToolModifiers.setPoints(stack, ToolModifiers.DIAMOND, 1);
                // Leave the cached stats unchanged; replay must use the saved acquisition order.
                List<ItemStack> ingredients = kit("wood");
                ItemStack result = ToolModifiers.modify(stack, ingredients);
                helper.assertTrue(!result.isEmpty(), "A lower first kit is accepted after Diamond");
                helper.assertValueEqual(harvest(result), 0, "The later Wood kit overrides the earlier Diamond mining boost");
                helper.assertTrue(ingredients.stream().allMatch(ItemStack::isEmpty), "One kit and flint are consumed");
                helper.assertValueEqual(result.get(TLDataComponents.MODIFIERS).stream().map(entry -> entry.id()).toList(),
                                List.of(ToolModifiers.DIAMOND, ToolModifiers.id("fortify/tinkerslegacy/wood")),
                                "The kit follows the earlier Diamond modifier");
                for (int repeat = 0; repeat < 3; repeat++) {
                        ((DynamicTool) result.getItem()).calculateStats(result);
                        helper.assertValueEqual(harvest(result), 0, "Repeated calculation preserves the kit's assigned tier");
                }
                result = ToolModifiers.modify(result, kit("obsidian"));
                helper.assertValueEqual(harvest(result), 4, "A later kit swap still occupies the same position after Diamond");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void headReplacementsKeepTheInstalledKitTier(GameTestHelper helper) {
                ItemStack stack = ToolModifiers.modify(tool(TLTools.Type.PICKAXE, id("wood")), kit("iron"));
                stack.set(TLDataComponents.PROGRESS, new ToolProgress(2, 73, 2));
                stack.setDamageValue(5);
                ToolState.initialize(stack, true);
                var identity = stack.get(TLDataComponents.TOOL_ID);
                for (String material : List.of("obsidian", "wood")) {
                        StationResult replacement = StationRecipes.evaluate(TLStations.TOOL_STATION,
                                        inputs(stack, part(ToolParts.Type.PICKAXE_HEAD, id(material))),
                                        StationOperations.REPLACE, null, "", 0);
                        helper.assertTrue(replacement.valid(), "Head replacement remains supported");
                        stack = replacement.output();
                        helper.assertValueEqual(harvest(stack), 2,
                                        "The installed kit sets the same tier with either a stronger or weaker head");
                        helper.assertValueEqual(stack.get(TLDataComponents.PROGRESS), new ToolProgress(2, 73, 2),
                                        "Head replacement preserves experience");
                        helper.assertValueEqual(stack.get(TLDataComponents.TOOL_ID), identity,
                                        "Head replacement preserves combat identity");
                        helper.assertValueEqual(stack.getDamageValue(), 5, "Head replacement preserves absolute wear");
                }
                helper.succeed();
        }

        // Stations
        @GameTest(template = "empty")
        public static void maintenanceDistinguishesKitRepairsFromUpgrades(GameTestHelper helper) {
                ItemStack stack = tool(TLTools.Type.PICKAXE, id("iron"));
                stack.setDamageValue(20);
                List<ItemStack> inputs = inputs(stack, part(ToolParts.Type.SHARPENING_KIT, id("obsidian")),
                                new ItemStack(Items.FLINT));
                StationResult upgraded = StationRecipes.evaluate(TLStations.TOOL_STATION, inputs,
                                StationOperations.MAINTAIN, null, "", 0);
                helper.assertTrue(upgraded.valid(), "Kit plus flint upgrades through Repair & Modify");
                helper.assertValueEqual(harvest(upgraded.output()), 4, "Station produces Cobalt-tier tool");
                helper.assertValueEqual(upgraded.output().getDamageValue(), 20, "Upgrade does not silently repair");
                helper.assertTrue(upgraded.inputsAfter().stream().allMatch(ItemStack::isEmpty),
                                "Exactly one tool, kit, and flint are consumed on commit");
                helper.assertTrue(inputs.subList(0, 3).stream().allMatch(input -> input.getCount() == 1),
                                "The preview leaves the original inventory untouched");
                StationResult repaired = StationRecipes.evaluate(TLStations.TOOL_STATION,
                                inputs(stack, part(ToolParts.Type.SHARPENING_KIT, id("iron"))),
                                StationOperations.MAINTAIN, null, "", 0);
                helper.assertTrue(repaired.valid(), "A matching kit without flint still repairs");
                helper.assertValueEqual(harvest(repaired.output()), 2, "Repair does not change mining level");
                helper.assertTrue(repaired.output().getDamageValue() < 20, "Matching kit restores durability");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void stationSwapsLowerKitsOnBrokenToolsOnceAndRejectsDuplicateResults(GameTestHelper helper) {
                ItemStack stack = ToolModifiers.modify(tool(TLTools.Type.PICKAXE, id("iron")), kit("obsidian"));
                stack.set(TLDataComponents.BROKEN, true);
                stack.set(TLDataComponents.BROKEN_DAMAGE, stack.getMaxDamage());
                stack.setDamageValue(stack.getMaxDamage() - 1);
                ItemStack kit = part(ToolParts.Type.SHARPENING_KIT, id("wood"));
                kit.setCount(2);
                List<ItemStack> inventory = inputs(stack, kit, new ItemStack(Items.FLINT, 3));
                for (var operation : List.of(StationOperations.MAINTAIN, StationOperations.MODIFY)) {
                        StationResult swapped = StationRecipes.evaluate(TLStations.TOOL_STATION, inventory,
                                        operation, null, "", 0);
                        helper.assertTrue(swapped.valid(), "Lower-tier swaps are available in both maintenance and modification");
                        helper.assertValueEqual(harvest(swapped.output()), 0, "Station swap lowers the mining tier even on a broken tool");
                        helper.assertTrue(ToolState.broken(swapped.output()), "A kit swap cannot repair a broken tool");
                        helper.assertValueEqual(ToolState.damage(swapped.output()), ToolState.damage(stack), "Broken damage is preserved");
                        helper.assertTrue(swapped.inputsAfter().getFirst().isEmpty(), "Commit consumes the original tool");
                        helper.assertValueEqual(swapped.inputsAfter().get(1).getCount(), 1, "Commit spends exactly one kit");
                        helper.assertValueEqual(swapped.inputsAfter().get(2).getCount(), 2, "Commit spends exactly one flint");
                        helper.assertValueEqual(inventory.get(1).getCount(), 2, "Preview does not spend the input kit");
                        helper.assertValueEqual(inventory.get(2).getCount(), 3, "Preview does not spend the input flint");
                        List<ItemStack> after = new ArrayList<>(swapped.inputsAfter());
                        after.set(0, swapped.output());
                        StationResult duplicate = StationRecipes.evaluate(TLStations.TOOL_STATION, after,
                                        operation, null, "", 0);
                        helper.assertTrue(!duplicate.valid(), "Reapplying the same material is a no-op");
                        helper.assertValueEqual(after.get(1).getCount(), 1, "Duplicate preserves remaining kits");
                        helper.assertValueEqual(after.get(2).getCount(), 2, "Duplicate preserves remaining flint");
                }
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void stationDowngradeRebuildsMiningRulesAndPreservesProgress(GameTestHelper helper) {
                ItemStack stack = tool(TLTools.Type.PICKAXE, id("iron"));
                ToolModifiers.setPoints(stack, ToolModifiers.HASTE, 17);
                stack = ToolModifiers.modify(stack, kit("obsidian"));
                stack.set(TLDataComponents.PROGRESS, new ToolProgress(2, 73, 2));
                stack.set(TLDataComponents.REPAIR_COUNT, 4);
                stack.set(DataComponents.CUSTOM_NAME, Component.literal("Old Iron Pick"));
                stack.setDamageValue(20);
                ToolState.initialize(stack, true);
                ItemStack before = stack.copy();
                for (var ore : TLBlocks.OreBlocks.ALL) {
                        helper.assertTrue(stack.isCorrectToolForDrops(ore.get().defaultBlockState()),
                                        "The original Obsidian kit permits Nether ore drops");
                }
                ItemStack kits = part(ToolParts.Type.SHARPENING_KIT, id("wood"));
                kits.setCount(2);
                List<ItemStack> inventory = inputs(stack, kits, new ItemStack(Items.FLINT, 3));
                StationResult preview = StationRecipes.evaluate(TLStations.TOOL_STATION, inventory,
                                StationOperations.MAINTAIN, null, "Old Iron Pick", 0);
                helper.assertTrue(preview.valid(), "Repair & Modify offers the weaker kit");
                ItemStack downgraded = preview.output();
                helper.assertValueEqual(harvest(downgraded), 0, "Wood replaces the Obsidian kit's mining level");
                for (int repeat = 0; repeat < 3; repeat++) {
                        ((DynamicTool) downgraded.getItem()).calculateStats(downgraded);
                        for (var ore : TLBlocks.OreBlocks.ALL) {
                                helper.assertTrue(!downgraded.isCorrectToolForDrops(ore.get().defaultBlockState()),
                                                "Rebuilt Tool rules reject Nether ore drops after downgrading");
                        }
                        helper.assertTrue(!downgraded.isCorrectToolForDrops(Blocks.DIAMOND_ORE.defaultBlockState()),
                                        "Rebuilt Tool rules also reject vanilla ores above the new tier");
                        helper.assertTrue(downgraded.isCorrectToolForDrops(Blocks.STONE.defaultBlockState()),
                                        "The downgraded pickaxe still mines blocks within its tier");
                }
                helper.assertValueEqual(downgraded.get(TLDataComponents.PROGRESS), before.get(TLDataComponents.PROGRESS), "XP and earned slots survive");
                helper.assertValueEqual(downgraded.get(TLDataComponents.TOOL_ID), before.get(TLDataComponents.TOOL_ID), "Combat identity survives");
                helper.assertValueEqual(downgraded.get(TLDataComponents.MATERIALS), before.get(TLDataComponents.MATERIALS), "The head material is not replaced");
                helper.assertValueEqual(downgraded.get(TLDataComponents.REPAIR_COUNT), 4, "Fortification does not count as repair");
                helper.assertValueEqual(downgraded.getDamageValue(), 20, "Fortification does not restore damage");
                helper.assertValueEqual(downgraded.get(DataComponents.CUSTOM_NAME), before.get(DataComponents.CUSTOM_NAME), "The custom name survives");
                helper.assertValueEqual(ToolModifiers.points(downgraded, ToolModifiers.HASTE), 17, "Partial Haste survives");
                helper.assertValueEqual(ToolState.freeModifiers(downgraded), ToolState.freeModifiers(before), "No free slot is consumed");
                helper.assertValueEqual(downgraded.get(TLDataComponents.MODIFIERS).stream().map(entry -> entry.id()).toList(),
                                List.of(ToolModifiers.HASTE, ToolModifiers.id("fortify/tinkerslegacy/wood")),
                                "The kit stays in its original acquisition position");
                helper.assertTrue(preview.inputsAfter().getFirst().isEmpty(), "Commit consumes the original tool once");
                helper.assertValueEqual(preview.inputsAfter().get(1).getCount(), 1, "Commit spends one kit");
                helper.assertValueEqual(preview.inputsAfter().get(2).getCount(), 2, "Commit spends one flint");
                helper.assertTrue(ItemStack.matches(stack, before), "Preview preserves the original tool");
                helper.assertValueEqual(kits.getCount(), 2, "Preview preserves input kits");
                helper.assertValueEqual(inventory.get(2).getCount(), 3, "Preview preserves input flint");
                ItemStack restored = ToolModifiers.modify(downgraded, kit("obsidian"));
                for (var ore : TLBlocks.OreBlocks.ALL) {
                        helper.assertTrue(restored.isCorrectToolForDrops(ore.get().defaultBlockState()),
                                        "Switching back restores Nether ore eligibility");
                }
                helper.succeed();
        }

        // Migration
        @GameTest(template = "empty")
        public static void savedProtectedKitStatsRefreshOnServerInventoryTick(GameTestHelper helper) {
                ItemStack iron = tool(TLTools.Type.PICKAXE, id("iron"));
                ItemStack stack = ToolModifiers.modify(iron, kit("stone"));
                ToolState.initialize(stack, true);
                stack.set(TLDataComponents.PROGRESS, new ToolProgress(3, 123, 3));
                stack.set(TLDataComponents.REPAIR_COUNT, 4);
                stack.set(DataComponents.CUSTOM_NAME, Component.literal("Old protected pick"));
                stack.setDamageValue(20);
                // Version 1 kept the stronger Iron head's tier over an installed Stone kit.
                var cached = new HashMap<>(stack.get(TLDataComponents.MODIFIED_STATS));
                cached.put("harvest_level", 2);
                stack.set(TLDataComponents.MODIFIED_STATS, Map.copyOf(cached));
                stack.set(DataComponents.TOOL, iron.get(DataComponents.TOOL));
                stack.set(TLDataComponents.TOOL_DATA_VERSION, 1);
                ItemStack before = stack.copy();
                helper.assertTrue(stack.isCorrectToolForDrops(Blocks.DIAMOND_ORE.defaultBlockState()),
                                "The saved version 1 cache still permits Diamond ore drops");
                var player = helper.makeMockServerPlayerInLevel();
                try {
                        ((DynamicTool) stack.getItem()).inventoryTick(stack, helper.getLevel(), player, 0, false);
                        helper.assertValueEqual(harvest(stack), 1, "The server tick restores the installed kit's tier");
                        helper.assertTrue(!stack.isCorrectToolForDrops(Blocks.DIAMOND_ORE.defaultBlockState()),
                                        "The server tick replaces stale mining rules with the lower kit's rules");
                        helper.assertTrue(stack.isCorrectToolForDrops(Blocks.IRON_ORE.defaultBlockState()),
                                        "The refreshed tool still mines ores within its tier");
                        helper.assertValueEqual(stack.get(TLDataComponents.TOOL_DATA_VERSION), ToolState.DATA_VERSION,
                                        "Version 2 marks the protected-tier cache as migrated");
                        helper.assertValueEqual(stack.get(TLDataComponents.PROGRESS), before.get(TLDataComponents.PROGRESS), "Migration retains XP and earned slots");
                        helper.assertValueEqual(stack.get(TLDataComponents.TOOL_ID), before.get(TLDataComponents.TOOL_ID), "Migration retains combat identity");
                        helper.assertValueEqual(stack.get(TLDataComponents.MATERIALS), before.get(TLDataComponents.MATERIALS), "Migration retains physical materials");
                        helper.assertValueEqual(stack.get(TLDataComponents.MODIFIERS), before.get(TLDataComponents.MODIFIERS), "Migration retains the selected kit and modifier order");
                        helper.assertValueEqual(stack.get(DataComponents.CUSTOM_NAME), before.get(DataComponents.CUSTOM_NAME), "Migration retains the custom name");
                        helper.assertValueEqual(stack.getDamageValue(), 20, "Migration retains absolute damage");
                        helper.assertValueEqual(stack.get(TLDataComponents.REPAIR_COUNT), 4, "Migration does not count as a repair");
                        helper.assertValueEqual(ToolState.freeModifiers(stack), ToolState.freeModifiers(before), "Migration retains modifier capacity");
                } finally {
                        helper.getLevel().getServer().getPlayerList().remove(player);
                }
                helper.succeed();
        }

        // Inputs
        private static List<ItemStack> kit(String material) {
                return List.of(part(ToolParts.Type.SHARPENING_KIT, id(material)), new ItemStack(Items.FLINT));
        }

        private static int harvest(ItemStack stack) {
                return stack.get(TLDataComponents.MODIFIED_STATS).get("harvest_level").intValue();
        }

        private static List<ItemStack> inputs(ItemStack... stacks) {
                List<ItemStack> result = new ArrayList<>(List.of(stacks));
                while (result.size() < TLStations.TOOL_STATION.inputSlots()) result.add(ItemStack.EMPTY);
                return result;
        }
}
