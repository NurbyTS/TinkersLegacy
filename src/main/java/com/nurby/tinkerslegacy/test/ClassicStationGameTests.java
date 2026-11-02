package com.nurby.tinkerslegacy.test;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.library.station.StationConnections;
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
import com.nurby.tinkerslegacy.registry.ToolStatTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

import static com.nurby.tinkerslegacy.test.ToolAssemblyGameTests.*;

@GameTestHolder(TinkersLegacy.MODID)
@PrefixGameTestTemplate(false)
public final class ClassicStationGameTests {

        // Maintenance
        @GameTest(template = "empty")
        public static void legacyDiamondToolsStillRepairBeforeModifying(GameTestHelper helper) {
                ItemStack original = legacyTool(TLTools.Type.PICKAXE, id("diamond"));
                ToolState.hurt(original, 10);
                List<ItemStack> inputs = maintenance(original, new ItemStack(Items.DIAMOND));
                List<ItemStack> before = copies(inputs);
                StationResult result = maintain(inputs, "Repaired pickaxe");

                helper.assertTrue(result.valid(), "The classic maintenance screen must infer material repair");
                helper.assertValueEqual(ToolState.damage(result.output()), 0, "Matching diamond repairs first");
                helper.assertTrue(result.output().get(TLDataComponents.MODIFIERS).isEmpty(),
                                "A matching repair ingredient must not also install a modifier");
                helper.assertValueEqual(result.output().get(DataComponents.CUSTOM_NAME),
                                Component.literal("Repaired pickaxe"), "Naming applies alongside repair");
                helper.assertTrue(result.inputsAfter().get(1).isEmpty(), "Only one repair diamond is consumed");
                assertInputs(helper, inputs, before);

                inputs = maintenance(legacyTool(TLTools.Type.PICKAXE, id("diamond")), new ItemStack(Items.DIAMOND));
                result = maintain(inputs, "Upgraded pickaxe");
                helper.assertTrue(result.valid(), "An intact tool skips repair and tries modification");
                helper.assertValueEqual(result.output().get(TLDataComponents.MODIFIERS).getFirst().id(),
                                ToolModifiers.DIAMOND, "The same ingredient modifies an intact tool");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void matchingReplacementPartsWorkInAnyMaintenanceSlot(GameTestHelper helper) {
                ItemStack original = tool(TLTools.Type.PICKAXE, id("wood"));
                ToolProgress progress = new ToolProgress(2, 123, 2);
                original.set(TLDataComponents.PROGRESS, progress);
                List<ItemStack> inputs = maintenance(original, ItemStack.EMPTY);
                inputs.set(3, part(ToolParts.Type.BINDING, id("stone")));
                inputs.set(5, part(ToolParts.Type.PICKAXE_HEAD, id("stone")));
                List<ItemStack> before = copies(inputs);
                StationResult result = maintain(inputs, "New stone parts");

                helper.assertTrue(result.valid(), "Parts in any of the five maintenance slots must be matched by shape");
                var materials = result.output().get(TLDataComponents.MATERIALS);
                helper.assertValueEqual(materials.get(0).material(), id("stone"), "Head inferred from part shape");
                helper.assertValueEqual(materials.get(1).material(), id("wood"), "Unreplaced handle retained");
                helper.assertValueEqual(materials.get(2).material(), id("stone"), "Binding inferred independently");
                helper.assertValueEqual(result.output().get(TLDataComponents.PROGRESS), progress, "Replacement retains progress");
                helper.assertTrue(result.inputsAfter().get(3).isEmpty() && result.inputsAfter().get(5).isEmpty(),
                                "Each replacement consumes exactly one physical part");
                assertInputs(helper, inputs, before);
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void replacementCannotConsumeTwoPartsForTheSamePhysicalSlot(GameTestHelper helper) {
                ItemStack original = tool(TLTools.Type.PICKAXE, id("wood"));
                List<ItemStack> inputs = maintenance(original, part(ToolParts.Type.PICKAXE_HEAD, id("stone")));
                inputs.set(2, part(ToolParts.Type.PICKAXE_HEAD, id("iron")));
                List<ItemStack> before = copies(inputs);
                helper.assertTrue(!maintain(inputs, "").valid(),
                                "One pickaxe head cannot consume two different replacement heads in the same transaction");
                assertInputs(helper, inputs, before);
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void multipleReplacementsValidateTheFinalToolTogether(GameTestHelper helper) {
                ItemStack original = com.nurby.tinkerslegacy.library.tool.ToolAssembly.create(
                                TLTools.tool(TLTools.Type.PICKAXE), List.of(
                                                new com.nurby.tinkerslegacy.library.material.layer.MaterialLayer(ToolStatTypes.HEAD.getId(), id("stone")),
                                                new com.nurby.tinkerslegacy.library.material.layer.MaterialLayer(ToolStatTypes.HANDLE.getId(), id("wood")),
                                                new com.nurby.tinkerslegacy.library.material.layer.MaterialLayer(ToolStatTypes.EXTRA.getId(), id("wood"))));
                ToolState.hurt(original, 100);
                List<ItemStack> inputs = maintenance(original, part(ToolParts.Type.PICKAXE_HEAD, id("wood")));
                inputs.set(2, part(ToolParts.Type.TOOL_ROD, id("iron")));
                List<ItemStack> before = copies(inputs);
                var result = maintain(inputs, "");
                helper.assertTrue(result.valid(),
                                "A lower durability head is valid when the simultaneous replacement handle makes the final tool strong enough");
                helper.assertValueEqual(ToolState.damage(result.output()), 100, "Final replacements preserve absolute damage");
                helper.assertTrue(result.output().getMaxDamage() > 100, "Combined parts accommodate existing damage");
                helper.assertValueEqual(result.output().get(TLDataComponents.MATERIALS).get(0).material(), id("wood"), "Head replaced");
                helper.assertValueEqual(result.output().get(TLDataComponents.MATERIALS).get(1).material(), id("iron"), "Handle replaced");
                helper.assertTrue(result.inputsAfter().get(1).isEmpty() && result.inputsAfter().get(2).isEmpty(),
                                "Exactly one of each physical part is consumed");
                assertInputs(helper, inputs, before);
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void invalidMaintenanceDoesNotFallBackToConsumingRename(GameTestHelper helper) {
                ItemStack original = tool(TLTools.Type.PICKAXE, id("wood"));
                List<ItemStack> inputs = maintenance(original, new ItemStack(Items.DIRT));
                List<ItemStack> before = copies(inputs);
                helper.assertTrue(!maintain(inputs, "Named tool").valid(),
                                "A name must not silently consume a tool with unrelated maintenance ingredients");
                assertInputs(helper, inputs, before);

                inputs = maintenance(original, ItemStack.EMPTY);
                StationResult result = maintain(inputs, "Named tool");
                helper.assertTrue(result.valid(), "Tool-only input supports renaming in the same maintenance view");
                helper.assertValueEqual(result.output().get(DataComponents.CUSTOM_NAME),
                                Component.literal("Named tool"), "Maintenance rename");
                helper.assertTrue(maintain(maintenance(result.output(), ItemStack.EMPTY), "").valid(),
                                "Clearing the naming field removes an existing custom name");
                helper.succeed();
        }

        // Connections
        @GameTest(template = "empty")
        public static void stationTabsFollowHorizontalConnectionsOnly(GameTestHelper helper) {
                BlockPos start = new BlockPos(2, 1, 2);
                BlockPos north = start.north();
                BlockPos east = start.east();
                BlockPos above = start.above();
                BlockPos diagonal = start.south().west();
                helper.setBlock(start, TLBlocks.block(TLBlocks.Workstations.Type.TOOL_STATION));
                helper.setBlock(north, TLBlocks.block(TLBlocks.Workstations.Type.STENCIL_TABLE));
                helper.setBlock(east, TLBlocks.block(TLBlocks.Workstations.Type.PART_BUILDER));
                helper.setBlock(above, TLBlocks.block(TLBlocks.Workstations.Type.PART_BUILDER));
                helper.setBlock(diagonal, TLBlocks.block(TLBlocks.Workstations.Type.STENCIL_TABLE));

                List<BlockPos> expected = List.of(helper.absolutePos(start), helper.absolutePos(north), helper.absolutePos(east));
                helper.assertValueEqual(StationConnections.find(helper.getLevel(), helper.absolutePos(start)), expected,
                                "Tabs connect horizontally in stable north/east/south/west order without diagonals or vertical links");
                helper.setBlock(north, Blocks.AIR);
                helper.assertValueEqual(StationConnections.find(helper.getLevel(), helper.absolutePos(start)),
                                List.of(helper.absolutePos(start), helper.absolutePos(east)), "Removed tables leave the connection set");
                helper.assertTrue(StationConnections.find(helper.getLevel(), helper.absolutePos(north)).isEmpty(),
                                "An ordinary block cannot act as a station connection origin");
                helper.succeed();
        }

        // Inputs
        private static List<ItemStack> maintenance(ItemStack tool, ItemStack ingredient) {
                List<ItemStack> inputs = new ArrayList<>(List.of(tool, ingredient));
                while (inputs.size() < TLStations.TOOL_STATION.inputSlots()) {
                        inputs.add(ItemStack.EMPTY);
                }
                return inputs;
        }

        private static StationResult maintain(List<ItemStack> inputs, String name) {
                return StationRecipes.evaluate(TLStations.TOOL_STATION, inputs,
                                StationOperations.MAINTAIN, null, name, 0);
        }
}
