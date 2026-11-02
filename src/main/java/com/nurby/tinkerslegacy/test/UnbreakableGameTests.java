package com.nurby.tinkerslegacy.test;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.library.station.StationOperations;
import com.nurby.tinkerslegacy.library.station.StationRecipes;
import com.nurby.tinkerslegacy.library.station.StationResult;
import com.nurby.tinkerslegacy.library.tool.ToolModifiers;
import com.nurby.tinkerslegacy.library.tool.ToolProgress;
import com.nurby.tinkerslegacy.library.tool.ToolState;
import com.nurby.tinkerslegacy.library.tool.modifier.ModifierEffects;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLItems;
import com.nurby.tinkerslegacy.registry.TLStations;
import com.nurby.tinkerslegacy.registry.TLTools;

import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Unbreakable;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

import static com.nurby.tinkerslegacy.test.ToolAssemblyGameTests.id;
import static com.nurby.tinkerslegacy.test.ToolAssemblyGameTests.tool;

@GameTestHolder(TinkersLegacy.MODID)
@PrefixGameTestTemplate(false)
public final class UnbreakableGameTests {

        // Stations
        @GameTest(template = "empty")
        public static void fiveReinforcementsConsumeOnePlateAndSlotEach(GameTestHelper helper) {
                ItemStack stack = tool(TLTools.Type.PICKAXE, id("iron"));
                stack.set(TLDataComponents.STARTING_MODIFIERS, 5);
                for (int level = 1; level <= 5; level++) {
                        List<ItemStack> inputs = inputs(stack, plates(2));
                        ItemStack before = stack.copy();
                        StationResult result = StationRecipes.evaluate(TLStations.TOOL_STATION, inputs,
                                        StationOperations.MAINTAIN, null, "", 0);
                        helper.assertTrue(result.valid(), "Repair & Modify accepts Reinforced level " + level);
                        helper.assertTrue(ItemStack.matches(inputs.getFirst(), before), "A preview never mutates the input tool");
                        helper.assertValueEqual(inputs.get(1).getCount(), 2, "A preview never consumes the original plates");
                        helper.assertTrue(result.inputsAfter().getFirst().isEmpty(), "The output replaces exactly one tool");
                        helper.assertValueEqual(result.inputsAfter().get(1).getCount(), 1,
                                        "One craft consumes one reinforcement, even with extra plates available");
                        stack = result.output();
                        helper.assertValueEqual(ToolModifiers.level(stack, ToolModifiers.REINFORCED), level,
                                        "Each plate adds exactly one Reinforced level");
                        helper.assertValueEqual(ToolState.freeModifiers(stack), 5 - level,
                                        "Each Reinforced level consumes one free modifier slot");
                        helper.assertTrue(stack.has(DataComponents.UNBREAKABLE) == (level == 5),
                                        "Only the fifth reinforcement supplies native Unbreakable");
                        helper.assertTrue(!stack.hasFoil(), "Reinforced never creates enchantment glint");
                }
                stack.set(TLDataComponents.STARTING_MODIFIERS, 6);
                List<ItemStack> capped = inputs(stack, plates(2));
                ItemStack before = stack.copy();
                helper.assertTrue(!StationRecipes.evaluate(TLStations.TOOL_STATION, capped,
                                StationOperations.MAINTAIN, null, "", 0).valid(), "A sixth level is rejected despite a spare slot");
                helper.assertTrue(ItemStack.matches(capped.getFirst(), before), "The capped tool is unchanged");
                helper.assertValueEqual(capped.get(1).getCount(), 2, "The cap consumes no plates");
                helper.assertValueEqual(ToolState.freeModifiers(capped.getFirst()), 1, "The cap consumes no slot");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void exhaustedSlotsRejectReinforcementWithoutSpending(GameTestHelper helper) {
                for (int current : new int[] {0, 4}) {
                        ItemStack stack = tool(TLTools.Type.PICKAXE, id("iron"));
                        stack.set(TLDataComponents.STARTING_MODIFIERS, current);
                        if (current > 0) ToolModifiers.setPoints(stack, ToolModifiers.REINFORCED, current);
                        ((DynamicTool) stack.getItem()).calculateStats(stack);
                        List<ItemStack> inputs = inputs(stack, plates(2));
                        ItemStack before = stack.copy();
                        helper.assertTrue(!StationRecipes.evaluate(TLStations.TOOL_STATION, inputs,
                                        StationOperations.MAINTAIN, null, "", 0).valid(),
                                        "Starting Reinforced and reaching Unbreakable both require a slot");
                        helper.assertTrue(ItemStack.matches(inputs.getFirst(), before), "Slot exhaustion preserves the tool");
                        helper.assertValueEqual(inputs.get(1).getCount(), 2, "Slot exhaustion consumes no plates");
                        helper.assertValueEqual(ToolState.freeModifiers(inputs.getFirst()), 0, "No modifier slot is created");
                        helper.assertTrue(!inputs.getFirst().has(DataComponents.UNBREAKABLE), "Failure cannot grant Unbreakable");
                }
                helper.succeed();
        }

        // Properties
        @GameTest(template = "empty")
        public static void unbreakablePropertiesDoNotRequireAnEnchantmentRegistry(GameTestHelper helper) {
                for (TLTools.Type type : TLTools.Type.values()) {
                        ItemStack stack = tool(type, id("iron"));
                        ToolModifiers.setPoints(stack, ToolModifiers.REINFORCED, 4);
                        ((DynamicTool) stack.getItem()).calculateStats(stack, null);
                        helper.assertTrue(!stack.has(DataComponents.UNBREAKABLE), "Four levels remain breakable: " + type);
                        ToolModifiers.setPoints(stack, ToolModifiers.REINFORCED, 5);
                        ((DynamicTool) stack.getItem()).calculateStats(stack, null);
                        helper.assertValueEqual(stack.get(DataComponents.UNBREAKABLE), new Unbreakable(false),
                                        "The fifth level supplies the component without registry access: " + type);
                        helper.assertTrue(!stack.hasFoil(), "Unbreakable retains the ordinary material appearance: " + type);
                        for (int repeat = 0; repeat < 3; repeat++) {
                                ((DynamicTool) stack.getItem()).calculateStats(stack, null);
                                helper.assertValueEqual(stack.get(DataComponents.UNBREAKABLE), new Unbreakable(false),
                                                "Repeated stat calculation retains native Unbreakable: " + type);
                        }
                }
                ItemStack explicit = tool(TLTools.Type.PICKAXE, id("iron"));
                explicit.set(DataComponents.UNBREAKABLE, new Unbreakable(true));
                ToolModifiers.setPoints(explicit, ToolModifiers.REINFORCED, 2);
                ((DynamicTool) explicit.getItem()).calculateStats(explicit, null);
                helper.assertValueEqual(explicit.get(DataComponents.UNBREAKABLE), new Unbreakable(true),
                                "Lower Reinforced levels do not erase an explicit native component or its visibility");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void unbreakablePreservesWearProgressAndBrokenState(GameTestHelper helper) {
                for (boolean broken : new boolean[] {false, true}) {
                        ItemStack stack = tool(TLTools.Type.PICKAXE, id("iron"));
                        stack.set(TLDataComponents.STARTING_MODIFIERS, 5);
                        stack.set(TLDataComponents.PROGRESS, new ToolProgress(2, 73, 2));
                        stack.set(TLDataComponents.REPAIR_COUNT, 4);
                        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Old Iron Pick"));
                        ToolState.initialize(stack, true);
                        ToolState.hurt(stack, broken ? stack.getMaxDamage() : 7);
                        ToolModifiers.setPoints(stack, ToolModifiers.REINFORCED, 4);
                        ((DynamicTool) stack.getItem()).calculateStats(stack);
                        ItemStack before = stack.copy();
                        StationResult result = StationRecipes.evaluate(TLStations.TOOL_STATION,
                                        inputs(stack, plates(1)), StationOperations.MODIFY, null, "", 0);
                        helper.assertTrue(result.valid(), "The fifth reinforcement accepts an existing worn or broken tool");
                        stack = result.output();
                        for (int repeat = 0; repeat < 3; repeat++) {
                                ((DynamicTool) stack.getItem()).calculateStats(stack, null);
                                ToolState.hurt(stack, Integer.MAX_VALUE);
                                helper.assertValueEqual(ToolState.damage(stack), ToolState.damage(before),
                                                "Unbreakable grants immunity without repairing existing wear");
                                helper.assertTrue(ToolState.broken(stack) == broken, "Unbreakable cannot revive an already broken tool");
                                helper.assertValueEqual(stack.get(TLDataComponents.MATERIALS), before.get(TLDataComponents.MATERIALS),
                                                "Original physical materials survive");
                                helper.assertValueEqual(stack.get(TLDataComponents.PROGRESS), before.get(TLDataComponents.PROGRESS),
                                                "Experience and earned modifier slots survive");
                                helper.assertValueEqual(stack.get(TLDataComponents.TOOL_ID), before.get(TLDataComponents.TOOL_ID),
                                                "Combat identity survives");
                                helper.assertValueEqual(stack.get(TLDataComponents.REPAIR_COUNT), 4, "No repair is counted");
                                helper.assertValueEqual(stack.get(DataComponents.CUSTOM_NAME), before.get(DataComponents.CUSTOM_NAME),
                                                "The custom name survives");
                                helper.assertTrue(!stack.hasFoil(), "Unbreakable does not make a worn tool glow");
                        }
                        ItemStack saved = ItemStack.parse(helper.getLevel().registryAccess(),
                                        stack.save(helper.getLevel().registryAccess())).orElseThrow();
                        helper.assertTrue(ItemStack.matches(saved, stack), "All Unbreakable state survives saving and loading");
                }
                helper.succeed();
        }

        // Durability
        @GameTest(template = "empty")
        public static void nativeUnbreakableAlsoStopsCentralToolWear(GameTestHelper helper) {
                ItemStack stack = tool(TLTools.Type.PICKAXE, id("iron"));
                ToolState.hurt(stack, 7);
                stack.set(DataComponents.UNBREAKABLE, new Unbreakable(true));
                ItemStack before = stack.copy();
                for (int amount : new int[] {-5, 0, 1, Integer.MAX_VALUE}) {
                        ToolState.hurt(stack, amount);
                        helper.assertTrue(ItemStack.matches(stack, before),
                                        "A native Unbreakable component stops central wear without a Reinforced modifier");
                }
                stack.remove(DataComponents.UNBREAKABLE);
                ToolState.hurt(stack, 1);
                helper.assertValueEqual(ToolState.damage(stack), 8, "Removing explicit Unbreakable restores normal wear");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void reinforcementRollsOnceAndSubtractsOriginalWear(GameTestHelper helper) {
                var entity = helper.spawnWithNoFreeWill(EntityType.HUSK, 1, 1, 1);
                for (int level = 1; level <= 4; level++) {
                        ItemStack stack = tool(TLTools.Type.PICKAXE, id("iron"));
                        ToolModifiers.setPoints(stack, ToolModifiers.REINFORCED, level);
                        int prevented = 0;
                        for (int sample = 0; sample < 128; sample++) {
                                long seed = sample * 0x9E3779B97F4A7C15L;
                                RandomSource expected = RandomSource.create(seed);
                                boolean prevent = expected.nextFloat() <= level * 0.2F;
                                if (prevent) prevented++;
                                entity.getRandom().setSeed(seed);
                                helper.assertValueEqual(ModifierEffects.adjustDamage(stack, 3, 7, entity), prevent ? 4 : 7,
                                                "A successful 20%-per-level roll subtracts original wear, preserving trait-added wear");
                                helper.assertValueEqual(entity.getRandom().nextFloat(), expected.nextFloat(),
                                                "Reinforced makes one roll for the event rather than one per damage point");
                                entity.getRandom().setSeed(seed);
                                helper.assertValueEqual(ModifierEffects.adjustDamage(stack, 3, 1, entity), prevent ? 0 : 1,
                                                "Preventing original wear cannot create negative damage after trait reduction");
                                entity.getRandom().setSeed(seed);
                                helper.assertValueEqual(ModifierEffects.adjustDamage(stack, 3, entity), prevent ? 0 : 3,
                                                "The compatibility overload treats its input as the original damage");
                        }
                        helper.assertTrue(prevented > 0 && prevented < 128, "Partial levels demonstrate both outcomes deterministically");
                        helper.assertValueEqual(ModifierEffects.adjustDamage(stack, 0, 0, entity), 0, "Zero wear stays zero");
                        helper.assertValueEqual(ModifierEffects.adjustDamage(stack, 3, -2, entity), 0, "Negative adjusted wear is clamped");
                }
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void unbreakableProtectsTheLastPointWhileFourLevelsCanBreak(GameTestHelper helper) {
                var entity = helper.spawnWithNoFreeWill(EntityType.HUSK, 1, 1, 1);
                long failingSeed = 0;
                while (RandomSource.create(failingSeed).nextFloat() <= 0.8F) failingSeed++;
                for (int level : new int[] {4, 5}) {
                        ItemStack stack = tool(TLTools.Type.PICKAXE, id("iron"));
                        ToolModifiers.setPoints(stack, ToolModifiers.REINFORCED, level);
                        ((DynamicTool) stack.getItem()).calculateStats(stack, null);
                        stack.setDamageValue(stack.getMaxDamage() - 1);
                        entity.getRandom().setSeed(failingSeed);
                        ToolState.hurt(stack, 1, entity);
                        helper.assertTrue(ToolState.broken(stack) == (level == 4),
                                        "Four levels can still break, whereas the fifth always preserves the last point");
                        helper.assertValueEqual(stack.getCount(), 1, "Neither case destroys the repairable tool");
                        helper.assertValueEqual(stack.getDamageValue(), stack.getMaxDamage() - 1,
                                        "Vanilla damage stays below the destruction threshold");
                        if (level == 5) {
                                helper.assertValueEqual(ModifierEffects.adjustDamage(stack, 3, 7, entity), 0,
                                                "The fifth level prevents all wear, including trait-added wear");
                        }
                }
                helper.succeed();
        }

        // Helpers
        private static ItemStack plates(int count) {
                return new ItemStack(TLItems.item(TLItems.Items.Miscellaneous.Type.REINFORCED_PLATE), count);
        }

        private static List<ItemStack> inputs(ItemStack... stacks) {
                List<ItemStack> result = new ArrayList<>();
                for (ItemStack stack : stacks) result.add(stack.copy());
                while (result.size() < 6) result.add(ItemStack.EMPTY);
                return result;
        }
}
