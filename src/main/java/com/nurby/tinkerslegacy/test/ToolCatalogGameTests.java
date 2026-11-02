package com.nurby.tinkerslegacy.test;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.block.entity.StationBlockEntity;
import com.nurby.tinkerslegacy.library.station.StationOperations;
import com.nurby.tinkerslegacy.library.station.StationRecipes;
import com.nurby.tinkerslegacy.library.tool.ToolAssembly;
import com.nurby.tinkerslegacy.library.tool.ToolDefinition;
import com.nurby.tinkerslegacy.library.tool.ToolState;
import com.nurby.tinkerslegacy.menu.StationMenu;
import com.nurby.tinkerslegacy.registry.TLBlocks;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLItems;
import com.nurby.tinkerslegacy.registry.TLMaterials;
import com.nurby.tinkerslegacy.registry.TLStations;
import com.nurby.tinkerslegacy.registry.TLTools;
import com.nurby.tinkerslegacy.registry.ToolParts;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.nurby.tinkerslegacy.test.ToolAssemblyGameTests.*;

@GameTestHolder(TinkersLegacy.MODID)
@PrefixGameTestTemplate(false)
public final class ToolCatalogGameTests {
        private static final Set<TLTools.Type> ADVANCED = Set.of(
                        TLTools.Type.HAMMER, TLTools.Type.EXCAVATOR, TLTools.Type.LUMBERAXE,
                        TLTools.Type.SCYTHE, TLTools.Type.CLEAVER, TLTools.Type.LONGBOW,
                        TLTools.Type.CROSSBOW, TLTools.Type.BOLT, TLTools.Type.SHURIKEN);

        // Catalog
        @GameTest(template = "empty")
        public static void originalToolTiersAndEveryCuttablePatternAreAvailable(GameTestHelper helper) {
                helper.assertValueEqual(TLTools.ALL.size(), 21, "The original catalog contains twenty-one tools");
                for (TLTools.Type type : TLTools.Type.values()) {
                        ToolDefinition tool = TLTools.tool(type);
                        List<ItemStack> inputs = padded(example(type));
                        var forge = StationRecipes.evaluate(TLStations.TOOL_FORGE, inputs,
                                        StationOperations.ASSEMBLE, tool.id(), "", 0);
                        helper.assertTrue(forge.valid(), "The Forge assembles " + type);
                        helper.assertTrue(forge.output().is(TLItems.Tools.item(type)), "The Forge produces the selected tool");
                        var station = StationRecipes.evaluate(TLStations.TOOL_STATION, inputs,
                                        StationOperations.ASSEMBLE, tool.id(), "", 0);
                        helper.assertValueEqual(station.valid(), !ADVANCED.contains(type),
                                        "The original basic/advanced station split for " + type);
                }
                var shapes = StationRecipes.shapes();
                helper.assertValueEqual(shapes.size(), 28, "All original stencils plus sharpening kits and material shards are available");
                helper.assertValueEqual(shapes.stream().distinct().count(), 28L, "There are no duplicate stencil choices");
                helper.assertTrue(!shapes.contains(ToolParts.get(ToolParts.Type.BOLT_CORE).getId()),
                                "Bolt cores use casting and never receive a stencil");
                for (var shape : shapes) {
                        ItemStack blank = new ItemStack(TLItems.item(TLItems.Items.IntermediaryItems.Type.BLANK_PATTERN));
                        var result = StationRecipes.evaluate(TLStations.STENCIL_TABLE, List.of(blank),
                                        StationOperations.CUT_PATTERN, shape, "", 0);
                        helper.assertTrue(result.valid(), "The Stencil Table cuts " + shape);
                        helper.assertValueEqual(result.output().get(TLDataComponents.PART), shape, "The stencil remembers its physical part");
                        helper.assertTrue(result.inputsAfter().getFirst().isEmpty(), "Cutting consumes one blank pattern");
                        helper.assertValueEqual(blank.getCount(), 1, "Preview leaves the original blank pattern untouched");
                }
                helper.succeed();
        }

        // Assembly
        @GameTest(template = "empty")
        public static void rangedAssemblyConsumesPhysicalPartsOnceForMultipleStatRoles(GameTestHelper helper) {
                for (var type : List.of(TLTools.Type.SHORTBOW, TLTools.Type.LONGBOW, TLTools.Type.CROSSBOW,
                                TLTools.Type.BOLT, TLTools.Type.SHURIKEN)) {
                        var tool = TLTools.tool(type);
                        List<ItemStack> parts = example(type);
                        parts.forEach(part -> part.setCount(2));
                        List<ItemStack> inputs = padded(parts);
                        List<ItemStack> before = copies(inputs);
                        var result = StationRecipes.evaluate(TLStations.TOOL_FORGE, inputs,
                                        StationOperations.ASSEMBLE, tool.id(), "", 0);
                        helper.assertTrue(result.valid(), "Multi-role assembly succeeds for " + type);
                        helper.assertTrue(tool.parts().size() > parts.size(), "This tool exercises multiple roles per physical component");
                        helper.assertValueEqual(result.output().get(TLDataComponents.MATERIALS).size(), tool.parts().size(),
                                        "Every material-stat role is saved on the finished tool");
                        for (int index = 0; index < parts.size(); index++) {
                                helper.assertValueEqual(result.inputsAfter().get(index).getCount(), 1,
                                                "Only one physical component is spent for " + type + " input " + index);
                        }
                        assertInputs(helper, inputs, before);
                }
                helper.succeed();
        }

        // Maintenance
        @GameTest(template = "empty")
        public static void basicToolStationStillRepairsAdvancedTools(GameTestHelper helper) {
                for (var type : ADVANCED) {
                        ItemStack original = ToolAssembly.assemble(TLTools.tool(type), example(type));
                        ToolState.hurt(original, 1);
                        helper.assertValueEqual(ToolState.damage(original), 1, "The advanced tool has real wear");
                        List<ItemStack> inputs = padded(List.of(original,
                                        new ItemStack(type == TLTools.Type.BOLT ? Items.IRON_INGOT : Items.OAK_PLANKS)));
                        var result = StationRecipes.evaluate(TLStations.TOOL_STATION, inputs,
                                        StationOperations.REPAIR, null, "", 0);
                        helper.assertTrue(result.valid(), "The basic station repairs already-built " + type);
                        helper.assertValueEqual(ToolState.damage(result.output()), 0, "The advanced tool is repaired");
                        helper.assertValueEqual(ToolState.damage(original), 1, "A repair preview leaves its input unchanged");
                }
                helper.succeed();
        }

        // Transfers
        @GameTest(template = "empty")
        public static void rangedShiftClicksUsePhysicalSlotsAndKeepTheOriginalLayouts(GameTestHelper helper) {
                BlockPos relative = new BlockPos(2, 1, 2);
                helper.setBlock(relative, TLBlocks.block(TLBlocks.Workstations.Type.TOOL_FORGE));
                var station = (StationBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(relative));
                Player player = helper.makeMockPlayer(GameType.SURVIVAL);
                player.setPos(station.getBlockPos().getX() + 0.5, station.getBlockPos().getY() + 1,
                                station.getBlockPos().getZ() + 0.5);
                StationMenu menu = new StationMenu(1, player.getInventory(), station);
                menu.select(StationOperations.ASSEMBLE, TLTools.get(TLTools.Type.CROSSBOW).getId(), "", 0);
                var parts = example(TLTools.Type.CROSSBOW);
                int[][] positions = {{38, 47}, {44, 19}, {14, 23}, {18, 51}};
                for (int index = parts.size() - 1; index >= 0; index--) {
                        transfer(player, menu, index, parts.get(index));
                }
                for (int index = 0; index < parts.size(); index++) {
                        helper.assertTrue(ItemStack.isSameItemSameComponents(station.getItem(index), parts.get(index)),
                                        "Crossbow components find their physical input slots");
                        helper.assertValueEqual(menu.getSlot(index).x, positions[index][0], "Original crossbow slot x");
                        helper.assertValueEqual(menu.getSlot(index).y, positions[index][1], "Original crossbow slot y");
                }
                helper.assertTrue(menu.getSlot(menu.resultSlot()).getItem().is(TLItems.Tools.item(TLTools.Type.CROSSBOW)),
                                "Four physical crossbow parts provide all six material layers");
                station.clearContent();
                player.getInventory().clearContent();
                menu = new StationMenu(2, player.getInventory(), station);
                menu.select(StationOperations.ASSEMBLE, TLTools.get(TLTools.Type.SHURIKEN).getId(), "", 0);
                transfer(player, menu, 0, example(TLTools.Type.SHURIKEN).getFirst().copyWithCount(4));
                for (int index = 0; index < 4; index++) {
                        helper.assertValueEqual(station.getItem(index).getCount(), 1,
                                        "One stack supplies each of the four knife-blade slots before stacking spare copies");
                }
                menu.clicked(menu.resultSlot(), 0, ClickType.PICKUP, player);
                helper.assertTrue(menu.getCarried().is(TLItems.Tools.item(TLTools.Type.SHURIKEN)), "Taking the result creates a shuriken tool");
                helper.assertTrue(menu.getCarried().has(TLDataComponents.TOOL_ID), "Only the committed tool gains its identity");
                for (int index = 0; index < station.getContainerSize(); index++) {
                        helper.assertTrue(station.getItem(index).isEmpty(), "Each physical knife blade is consumed exactly once");
                }
                helper.succeed();
        }

        // Inputs
        private static List<ItemStack> example(TLTools.Type type) {
                ToolDefinition tool = TLTools.tool(type);
                return exampleParts(tool, tool.defaultMaterials(type == TLTools.Type.BOLT
                                ? TLMaterials.IRON.get() : TLMaterials.WOOD.get()));
        }

        private static List<ItemStack> padded(List<ItemStack> values) {
                List<ItemStack> inputs = new ArrayList<>(values);
                while (inputs.size() < 6) {
                        inputs.add(ItemStack.EMPTY);
                }
                return inputs;
        }

        private static void transfer(Player player, StationMenu menu, int inventoryIndex, ItemStack stack) {
                player.getInventory().setItem(inventoryIndex, stack.copy());
                menu.clicked(menu.resultSlot() + 28 + inventoryIndex, 0, ClickType.QUICK_MOVE, player);
                // The server's container-click packet handler synchronizes the menu after each click.
                menu.broadcastChanges();
        }
}
