package com.nurby.tinkerslegacy.test;

import com.nurby.tinkerslegacy.TLConfig;
import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.library.material.layer.MaterialLayer;
import com.nurby.tinkerslegacy.library.station.MaterialInputs;
import com.nurby.tinkerslegacy.library.station.StationDefinition;
import com.nurby.tinkerslegacy.library.station.StationOperations;
import com.nurby.tinkerslegacy.library.station.StationRecipes;
import com.nurby.tinkerslegacy.library.station.StationRequirement;
import com.nurby.tinkerslegacy.library.station.StationResult;
import com.nurby.tinkerslegacy.library.tool.ToolAssembly;
import com.nurby.tinkerslegacy.library.tool.ToolDefinition;
import com.nurby.tinkerslegacy.library.tool.ToolModifierEntry;
import com.nurby.tinkerslegacy.library.tool.ToolModifiers;
import com.nurby.tinkerslegacy.library.tool.ToolProgress;
import com.nurby.tinkerslegacy.library.tool.ToolState;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLItems;
import com.nurby.tinkerslegacy.registry.TLStations;
import com.nurby.tinkerslegacy.registry.TLTools;
import com.nurby.tinkerslegacy.registry.ToolParts;
import com.nurby.tinkerslegacy.registry.ToolStatTypes;

import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.nurby.tinkerslegacy.test.ToolAssemblyGameTests.*;

@GameTestHolder(TinkersLegacy.MODID)
@PrefixGameTestTemplate(false)
public final class StationRecipeGameTests {

        // Patterns
        @GameTest(template = "empty")
        public static void cuttingPatternsConsumesOneAndHonorsReuse(GameTestHelper helper) {
                ResourceLocation pickHead = ToolParts.get(ToolParts.Type.PICKAXE_HEAD).getId();
                ItemStack blanks = new ItemStack(TLItems.item(TLItems.Items.IntermediaryItems.Type.BLANK_PATTERN), 2);
                List<ItemStack> inputs = List.of(blanks);
                StationResult result = evaluate(TLStations.STENCIL_TABLE, inputs, StationOperations.CUT_PATTERN, pickHead);

                helper.assertTrue(result.valid(), "A blank pattern must be cut into a current tool part");
                helper.assertValueEqual(result.output().get(TLDataComponents.PART), pickHead, "Stored physical shape");
                helper.assertValueEqual(result.inputsAfter().getFirst().getCount(), 1, "One blank consumed per transaction");
                helper.assertValueEqual(blanks.getCount(), 2, "Preview retains original blank stack");
                helper.assertTrue(!evaluate(TLStations.STENCIL_TABLE, inputs, StationOperations.CUT_PATTERN,
                                ToolParts.get(ToolParts.Type.BOLT_CORE).getId()).valid(), "Cast bolt cores have no stencil");

                List<ItemStack> recut = List.of(result.output());
                StationResult same = evaluate(TLStations.STENCIL_TABLE, recut, StationOperations.CUT_PATTERN, pickHead);
                StationResult different = evaluate(TLStations.STENCIL_TABLE, recut, StationOperations.CUT_PATTERN,
                                ToolParts.get(ToolParts.Type.AXE_HEAD).getId());
                helper.assertTrue(!same.valid(), "Recutting the same stencil must not consume it");
                helper.assertValueEqual(different.valid(), TLConfig.REUSE_STENCILS.get(), "Reuse setting controls recutting");
                helper.assertValueEqual(recut.getFirst().get(TLDataComponents.PART), pickHead, "Recutting preview preserves stencil");
                helper.succeed();
        }

        // Materials
        @GameTest(template = "empty")
        public static void materialValuesAndPartCostsUseWholeUnits(GameTestHelper helper) {
                helper.assertValueEqual(MaterialInputs.match(new ItemStack(Items.STICK)).value(), 72, "Stick value");
                helper.assertValueEqual(MaterialInputs.match(new ItemStack(Items.OAK_PLANKS)).value(), 144, "Plank value");
                helper.assertValueEqual(MaterialInputs.match(new ItemStack(Items.OAK_LOG)).value(), 576, "Log value");
                helper.assertValueEqual(MaterialInputs.match(new ItemStack(Items.COBBLESTONE)).value(), 144, "Cobblestone value");
                helper.assertValueEqual(MaterialInputs.match(MaterialInputs.shard(id("wood"), 1)).value(), 72, "Shard value");

                List<ItemStack> inputs = partInputs(ToolParts.Type.PICKAXE_HEAD, new ItemStack(Items.OAK_LOG), ItemStack.EMPTY, ItemStack.EMPTY);
                List<ItemStack> before = copies(inputs);
                StationResult result = evaluate(TLStations.PART_BUILDER, inputs, StationOperations.BUILD_PART, null);
                helper.assertTrue(result.valid(), "A log must supply the two-unit pickaxe head");
                helper.assertValueEqual(result.inputsAfter().get(3).getCount(), 4, "Unused two units return as four shards");
                helper.assertValueEqual(result.inputsAfter().get(3).get(TLDataComponents.MATERIAL_ID), id("wood"), "Shard material");
                helper.assertTrue(result.inputsAfter().get(1).isEmpty(), "One log consumed");
                helper.assertValueEqual(result.inputsAfter().getFirst().getCount(), 1, "Pattern is reusable");
                helper.assertValueEqual(result.output().get(TLDataComponents.MATERIALS),
                                List.of(new MaterialLayer(ToolStatTypes.HEAD.getId(), id("wood"))), "Wood head material");
                assertInputs(helper, inputs, before);

                inputs = partInputs(ToolParts.Type.TOOL_ROD, new ItemStack(Items.STICK, 2), ItemStack.EMPTY, ItemStack.EMPTY);
                result = evaluate(TLStations.PART_BUILDER, inputs, StationOperations.BUILD_PART, null);
                helper.assertTrue(result.valid(), "Two half-unit sticks must build a one-unit rod");
                helper.assertTrue(result.inputsAfter().get(1).isEmpty() && result.inputsAfter().get(3).isEmpty(),
                                "Exact material costs leave no ingredients or shards");
                inputs = partInputs(ToolParts.Type.PICKAXE_HEAD, new ItemStack(Items.STONE),
                                new ItemStack(Items.COBBLESTONE), ItemStack.EMPTY);
                result = evaluate(TLStations.PART_BUILDER, inputs, StationOperations.BUILD_PART, null);
                helper.assertTrue(result.valid(), "Stone and cobblestone can jointly supply a stone part");
                helper.assertValueEqual(result.output().get(TLDataComponents.MATERIALS).getFirst().material(), id("stone"), "Stone head material");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void partBuilderProtectsMaterialsAndRemainderSpace(GameTestHelper helper) {
                List<ItemStack> inputs = partInputs(ToolParts.Type.PICKAXE_HEAD, new ItemStack(Items.OAK_LOG),
                                ItemStack.EMPTY, MaterialInputs.shard(id("wood"), 61));
                List<ItemStack> before = copies(inputs);
                helper.assertTrue(!evaluate(TLStations.PART_BUILDER, inputs, StationOperations.BUILD_PART, null).valid(),
                                "A transaction must fail when all four remainder shards do not fit");
                assertInputs(helper, inputs, before);
                inputs.set(3, MaterialInputs.shard(id("wood"), 60));
                StationResult result = evaluate(TLStations.PART_BUILDER, inputs, StationOperations.BUILD_PART, null);
                helper.assertTrue(result.valid(), "Exactly enough remainder space must be accepted");
                helper.assertValueEqual(result.inputsAfter().get(3).getCount(), 64, "Remainder merges without exceeding maximum");
                inputs.set(3, MaterialInputs.shard(id("stone"), 1));
                helper.assertTrue(!evaluate(TLStations.PART_BUILDER, inputs, StationOperations.BUILD_PART, null).valid(),
                                "Different remainder materials cannot merge");

                inputs = partInputs(ToolParts.Type.PICKAXE_HEAD, new ItemStack(Items.COBBLESTONE), ItemStack.EMPTY, ItemStack.EMPTY);
                helper.assertTrue(!evaluate(TLStations.PART_BUILDER, inputs, StationOperations.BUILD_PART, null).valid(), "Insufficient material must fail");
                inputs = partInputs(ToolParts.Type.PICKAXE_HEAD, new ItemStack(Items.OAK_LOG), new ItemStack(Items.COBBLESTONE), ItemStack.EMPTY);
                helper.assertTrue(!evaluate(TLStations.PART_BUILDER, inputs, StationOperations.BUILD_PART, null).valid(), "Wood and stone cannot be mixed in one part");
                inputs = partInputs(ToolParts.Type.PICKAXE_HEAD, new ItemStack(Items.IRON_INGOT, 2), ItemStack.EMPTY, ItemStack.EMPTY);
                helper.assertTrue(evaluate(TLStations.PART_BUILDER, inputs, StationOperations.BUILD_PART, null).valid(), "The original no-Smeltery fallback shapes metal parts");
                inputs = partInputs(ToolParts.Type.TOOL_ROD, MaterialInputs.shard(id("wood"), 2), ItemStack.EMPTY, ItemStack.EMPTY);
                helper.assertTrue(evaluate(TLStations.PART_BUILDER, inputs, StationOperations.BUILD_PART, null).valid(), "Returned shards must be reusable material");
                helper.succeed();
        }

        // Stations
        @GameTest(template = "empty")
        public static void assemblyUsesOnePartPerSlotAndIsAPurePreview(GameTestHelper helper) {
                List<ItemStack> inputs = new ArrayList<>(parts(TLTools.tool(TLTools.Type.PICKAXE), id("wood")));
                inputs.forEach(stack -> stack.setCount(2));
                while (inputs.size() < 6) inputs.add(ItemStack.EMPTY);
                List<ItemStack> before = copies(inputs);
                StationResult first = evaluate(TLStations.TOOL_STATION, inputs, StationOperations.ASSEMBLE, id("pickaxe"));
                StationResult repeated = evaluate(TLStations.TOOL_STATION, inputs, StationOperations.ASSEMBLE, id("pickaxe"));
                helper.assertTrue(first.valid() && repeated.valid(), "Repeated previews must both succeed");
                helper.assertTrue(ItemStack.matches(first.output(), repeated.output()), "Preview results must be stable");
                for (int index = 0; index < 3; index++) {
                        helper.assertValueEqual(first.inputsAfter().get(index).getCount(), 1, "One physical part per slot consumed");
                }
                assertInputs(helper, inputs, before);
                inputs.set(5, new ItemStack(Items.STICK));
                helper.assertTrue(!evaluate(TLStations.TOOL_STATION, inputs, StationOperations.ASSEMBLE, id("pickaxe")).valid(),
                                "Unexpected assembly inputs must not disappear");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void familiesAndTiersExtendWithoutAnotherStationBlock(GameTestHelper helper) {
                ToolDefinition pickaxe = TLTools.tool(TLTools.Type.PICKAXE);
                StationDefinition forge = new StationDefinition(id("test_forge"), StationOperations.TOOL_FAMILY,
                                3, 9, TLStations.TOOL_STATION.operations());
                StationDefinition fletching = new StationDefinition(id("test_fletching"), id("fletching_family"),
                                3, 6, TLStations.TOOL_STATION.operations());
                ToolDefinition advanced = new ToolDefinition(id("pickaxe"), pickaxe.parts()) {
                        @Override
                        public StationRequirement stationRequirement(ResourceLocation operation) {
                                return new StationRequirement(StationOperations.TOOL_FAMILY, 2);
                        }
                };
                helper.assertTrue(StationRecipes.supports(forge, advanced, StationOperations.ASSEMBLE), "A higher tier of the same family works");
                helper.assertTrue(!StationRecipes.supports(TLStations.TOOL_STATION, advanced, StationOperations.ASSEMBLE), "A lower tier must fail");
                helper.assertTrue(!StationRecipes.supports(fletching, pickaxe, StationOperations.ASSEMBLE), "A different family must fail regardless of tier");
                helper.assertTrue(!StationRecipes.supports(TLStations.STENCIL_TABLE, pickaxe, StationOperations.ASSEMBLE), "Unsupported operations must fail");
                List<ItemStack> inputs = new ArrayList<>(parts(pickaxe, id("wood")));
                while (inputs.size() < forge.inputSlots()) inputs.add(ItemStack.EMPTY);
                helper.assertTrue(evaluate(forge, inputs, StationOperations.ASSEMBLE, id("pickaxe")).valid(), "Assembly must use the extensible definition");
                helper.assertTrue(!evaluate(forge, inputs.subList(0, 6), StationOperations.ASSEMBLE, id("pickaxe")).valid(), "Incorrect inventory capacity must fail");
                helper.succeed();
        }

        // Maintenance
        @GameTest(template = "empty")
        public static void eitherMattockHeadCanSupplyRepairMaterial(GameTestHelper helper) {
                var definition = TLTools.tool(TLTools.Type.MATTOCK);
                List<MaterialLayer> materials = List.of(
                                new MaterialLayer(ToolStatTypes.HEAD.getId(), id("wood")),
                                new MaterialLayer(ToolStatTypes.HEAD.getId(), id("stone")),
                                new MaterialLayer(ToolStatTypes.HANDLE.getId(), id("wood")));
                ItemStack mattock = ToolAssembly.create(definition, materials);
                ToolState.hurt(mattock, 50);
                for (var ingredient : List.of(Items.OAK_PLANKS, Items.COBBLESTONE)) {
                        List<ItemStack> inputs = maintenance(mattock, new ItemStack(ingredient));
                        List<ItemStack> before = copies(inputs);
                        StationResult result = evaluate(TLStations.TOOL_STATION, inputs, StationOperations.REPAIR, null);
                        helper.assertTrue(result.valid(), "Either matching mattock head material must repair it");
                        helper.assertValueEqual(ToolState.damage(result.output()), ingredient == Items.OAK_PLANKS ? 22 : 0,
                                        "Wood restores 28 of 50 damage after Cheapskate scales 35 head durability by 0.8; stone fully repairs");
                        helper.assertValueEqual(result.output().get(TLDataComponents.REPAIR_COUNT), 1, "One repair application tracked");
                        assertInputs(helper, inputs, before);
                }
                helper.assertTrue(!evaluate(TLStations.TOOL_STATION, maintenance(mattock, new ItemStack(Items.IRON_INGOT)),
                                StationOperations.REPAIR, null).valid(), "Unrelated repair materials must fail");
                ItemStack broken = mattock.copy();
                ToolState.hurt(broken, broken.getMaxDamage());
                StationResult repaired = evaluate(TLStations.TOOL_STATION, maintenance(broken, new ItemStack(Items.COBBLESTONE)),
                                StationOperations.REPAIR, null);
                helper.assertTrue(repaired.valid() && !ToolState.broken(repaired.output()), "A material repair must restore a broken tool");
                helper.assertValueEqual(broken.getCount(), 1, "Broken tools and previews must retain their item");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void repairConsumesOnlyOneMatchingIngredientPerMaterial(GameTestHelper helper) {
                ItemStack original = tool(TLTools.Type.PICKAXE, id("wood"));
                ToolState.hurt(original, 1);
                List<ItemStack> inputs = maintenance(original, new ItemStack(Items.OAK_PLANKS));
                inputs.set(2, new ItemStack(Items.OAK_PLANKS));
                StationResult result = evaluate(TLStations.TOOL_STATION, inputs, StationOperations.REPAIR, null);
                helper.assertTrue(result.valid(), "One point of wear must be repairable");
                helper.assertTrue(result.inputsAfter().get(1).isEmpty(), "The first matching ingredient is consumed");
                helper.assertValueEqual(result.inputsAfter().get(2).getCount(), 1, "Unneeded same-material stacks remain");
                helper.assertValueEqual(ToolState.damage(result.output()), 0, "The tool is fully repaired");
                helper.assertValueEqual(result.output().get(TLDataComponents.REPAIR_COUNT), 1, "One application increments repair count once");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void replacementPreservesProgressAndRejectsExcessDamage(GameTestHelper helper) {
                ItemStack original = tool(TLTools.Type.PICKAXE, id("wood"));
                ToolProgress progress = new ToolProgress(2, 123, 2);
                UUID identity = UUID.fromString("10000000-0000-0000-0000-000000000001");
                original.set(TLDataComponents.PROGRESS, progress);
                original.set(TLDataComponents.TOOL_ID, identity);
                original.set(DataComponents.CUSTOM_NAME, Component.literal("My pickaxe"));
                original.set(TLDataComponents.REPAIR_COUNT, 4);
                ToolState.hurt(original, 20);
                ItemStack replacement = part(ToolParts.Type.PICKAXE_HEAD, id("stone"));
                List<ItemStack> inputs = maintenance(original, replacement);
                List<ItemStack> before = copies(inputs);
                StationResult result = StationRecipes.evaluate(TLStations.TOOL_STATION, inputs,
                                StationOperations.REPLACE, null, "", 0);
                helper.assertTrue(result.valid(), "A stronger replacement head must work");
                helper.assertValueEqual(result.output().get(TLDataComponents.PROGRESS), progress, "XP and earned modifiers survive replacement");
                helper.assertValueEqual(result.output().get(TLDataComponents.TOOL_ID), identity, "Combat identity survives replacement");
                helper.assertValueEqual(result.output().get(TLDataComponents.REPAIR_COUNT), 4, "Repair count survives replacement");
                helper.assertValueEqual(result.output().get(DataComponents.CUSTOM_NAME), original.get(DataComponents.CUSTOM_NAME), "Custom name survives replacement");
                helper.assertValueEqual(ToolState.damage(result.output()), 20, "Replacement preserves absolute damage");
                helper.assertTrue(result.inputsAfter().getFirst().isEmpty() && result.inputsAfter().get(1).isEmpty(), "Replacement consumes one tool and one part");
                assertInputs(helper, inputs, before);

                ItemStack diamond = legacyTool(TLTools.Type.PICKAXE, id("diamond"));
                ToolState.hurt(diamond, 900);
                helper.assertTrue(!StationRecipes.evaluate(TLStations.TOOL_STATION,
                                maintenance(diamond, part(ToolParts.Type.PICKAXE_HEAD, id("wood"))),
                                StationOperations.REPLACE, null, "", 0).valid(), "A weak replacement cannot erase damage beyond its maximum");
                replacement.set(TLDataComponents.MATERIALS, List.of(
                                new MaterialLayer(ToolStatTypes.HEAD.getId(), id("stone")),
                                new MaterialLayer(ToolStatTypes.HEAD.getId(), id("wood"))));
                helper.assertTrue(!StationRecipes.evaluate(TLStations.TOOL_STATION, maintenance(original, replacement),
                                StationOperations.REPLACE, null, "", 0).valid(), "Malformed part layers must fail replacement too");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void modifierBlocksMustFitInFullAndDoNotAlterInputs(GameTestHelper helper) {
                ItemStack original = tool(TLTools.Type.PICKAXE, id("wood"));
                original.set(TLDataComponents.STARTING_MODIFIERS, 1);
                original.set(TLDataComponents.MODIFIERS, List.of(new ToolModifierEntry(ToolModifiers.HASTE, 45)));
                List<ItemStack> inputs = maintenance(original, new ItemStack(Items.REDSTONE_BLOCK));
                List<ItemStack> before = copies(inputs);
                helper.assertTrue(!evaluate(TLStations.TOOL_STATION, inputs, StationOperations.MODIFY, null).valid(),
                                "Nine redstone points must not fit a five-point space with no spare slot");
                assertInputs(helper, inputs, before);
                inputs = maintenance(original, new ItemStack(Items.REDSTONE, 6));
                StationResult result = evaluate(TLStations.TOOL_STATION, inputs, StationOperations.MODIFY, null);
                helper.assertTrue(result.valid(), "Five dust must finish the already purchased modifier level");
                helper.assertValueEqual(result.output().get(TLDataComponents.MODIFIERS).getFirst().points(), 50, "Haste level completed");
                helper.assertValueEqual(result.inputsAfter().get(1).getCount(), 1, "Excess dust remains");
                helper.assertValueEqual(ToolState.freeModifiers(result.output()), 0, "Completing a level spends no extra slot");

                original.set(TLDataComponents.MODIFIERS, List.of(new ToolModifierEntry(ToolModifiers.HASTE, 41)));
                result = evaluate(TLStations.TOOL_STATION, maintenance(original, new ItemStack(Items.REDSTONE_BLOCK)), StationOperations.MODIFY, null);
                helper.assertTrue(result.valid() && result.inputsAfter().get(1).isEmpty(), "A whole ingredient block must fit exactly");
                helper.assertValueEqual(result.output().get(TLDataComponents.MODIFIERS).getFirst().points(), 50, "Whole-block value applied");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void excessModifierStacksRemainWhenAnotherSlotAdvancesTheSameModifier(GameTestHelper helper) {
                ItemStack original = tool(TLTools.Type.PICKAXE, id("wood"));
                List<ItemStack> inputs = maintenance(original, new ItemStack(Items.DIAMOND));
                inputs.set(2, new ItemStack(Items.DIAMOND));
                List<ItemStack> before = copies(inputs);
                StationResult result = evaluate(TLStations.TOOL_STATION, inputs, StationOperations.MODIFY, null);
                helper.assertTrue(result.valid(), "A second diamond slot must not invalidate the first application");
                helper.assertTrue(result.inputsAfter().get(1).isEmpty(), "The first diamond is applied once");
                helper.assertValueEqual(result.inputsAfter().get(2).getCount(), 1, "The unused diamond remains in its original slot");
                helper.assertValueEqual(result.output().get(TLDataComponents.MODIFIERS).size(), 1, "Only one modifier is installed");
                assertInputs(helper, inputs, before);

                original.set(TLDataComponents.STARTING_MODIFIERS, 1);
                original.set(TLDataComponents.MODIFIERS, List.of(new ToolModifierEntry(ToolModifiers.HASTE, 45)));
                inputs = maintenance(original, new ItemStack(Items.REDSTONE, 5));
                inputs.set(2, new ItemStack(Items.REDSTONE, 3));
                before = copies(inputs);
                result = evaluate(TLStations.TOOL_STATION, inputs, StationOperations.MODIFY, null);
                helper.assertTrue(result.valid(), "A later dust stack remains when the first stack finishes the available level");
                helper.assertValueEqual(result.output().get(TLDataComponents.MODIFIERS).getFirst().points(), 50, "Only available progress is applied");
                helper.assertValueEqual(result.inputsAfter().get(2).getCount(), 3, "All excess dust remains");
                assertInputs(helper, inputs, before);

                inputs = maintenance(original, new ItemStack(Items.REDSTONE_BLOCK));
                inputs.set(2, new ItemStack(Items.REDSTONE, 5));
                result = evaluate(TLStations.TOOL_STATION, inputs, StationOperations.MODIFY, null);
                helper.assertTrue(result.valid(), "An oversized block must not prevent a later fitting dust application");
                helper.assertTrue(result.inputsAfter().get(1).is(Items.REDSTONE_BLOCK), "The whole oversized block remains intact");
                helper.assertTrue(result.inputsAfter().get(2).isEmpty(), "Only the five fitting dust are consumed");
                helper.assertValueEqual(result.output().get(TLDataComponents.MODIFIERS).getFirst().points(), 50, "Block value is never truncated");

                inputs = maintenance(original, new ItemStack(Items.REDSTONE, 5));
                inputs.set(2, new ItemStack(Items.DIAMOND));
                helper.assertTrue(!evaluate(TLStations.TOOL_STATION, inputs, StationOperations.MODIFY, null).valid(),
                                "A different modifier that cannot start must still invalidate the combined operation");
                inputs.set(2, new ItemStack(Items.DIRT));
                helper.assertTrue(!evaluate(TLStations.TOOL_STATION, inputs, StationOperations.MODIFY, null).valid(),
                                "Unrelated unused items must never be silently ignored");
                helper.succeed();
        }

        // Inputs
        private static List<ItemStack> partInputs(ToolParts.Type part, ItemStack first, ItemStack second, ItemStack remainder) {
                ItemStack pattern = new ItemStack(TLItems.item(TLItems.Items.IntermediaryItems.Type.PATTERN));
                pattern.set(TLDataComponents.PART, ToolParts.get(part).getId());
                return new ArrayList<>(List.of(pattern, first, second, remainder));
        }

        private static List<ItemStack> maintenance(ItemStack tool, ItemStack ingredient) {
                return new ArrayList<>(List.of(tool, ingredient, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY));
        }

        private static StationResult evaluate(StationDefinition station, List<ItemStack> inputs,
                        ResourceLocation operation, ResourceLocation selection) {
                return StationRecipes.evaluate(station, inputs, operation, selection, "", 0);
        }
}
