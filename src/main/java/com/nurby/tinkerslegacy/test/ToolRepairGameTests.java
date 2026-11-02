package com.nurby.tinkerslegacy.test;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.block.entity.StationBlockEntity;
import com.nurby.tinkerslegacy.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.library.station.StationOperations;
import com.nurby.tinkerslegacy.library.station.StationRecipes;
import com.nurby.tinkerslegacy.library.tool.ToolModifierEntry;
import com.nurby.tinkerslegacy.library.tool.ToolModifiers;
import com.nurby.tinkerslegacy.library.tool.ToolProgress;
import com.nurby.tinkerslegacy.library.tool.ToolRepair;
import com.nurby.tinkerslegacy.library.tool.ToolState;
import com.nurby.tinkerslegacy.menu.CraftingStationMenu;
import com.nurby.tinkerslegacy.registry.TLBlocks;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLItems;
import com.nurby.tinkerslegacy.registry.TLStations;
import com.nurby.tinkerslegacy.registry.TLTools;
import com.nurby.tinkerslegacy.registry.ToolParts;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.nurby.tinkerslegacy.test.ToolAssemblyGameTests.*;

@GameTestHolder(TinkersLegacy.MODID)
@PrefixGameTestTemplate(false)
public final class ToolRepairGameTests {

        // Kits
        @GameTest(template = "empty")
        public static void sharpeningKitHasAReusableTwoUnitStencil(GameTestHelper helper) {
                var shape = ToolParts.get(ToolParts.Type.SHARPENING_KIT).getId();
                helper.assertTrue(StationRecipes.shapes().contains(shape), "Sharpening kits are selectable stencil shapes");
                helper.assertValueEqual(StationRecipes.partCost(shape), 288, "The original kit costs two whole material units");
                ItemStack blank = new ItemStack(TLItems.item(TLItems.Items.IntermediaryItems.Type.BLANK_PATTERN));
                var cut = StationRecipes.evaluate(TLStations.STENCIL_TABLE, List.of(blank),
                                StationOperations.CUT_PATTERN, shape, "", 0);
                helper.assertTrue(cut.valid(), "A kit stencil can be cut in survival");
                var built = StationRecipes.evaluate(TLStations.PART_BUILDER,
                                List.of(cut.output(), new ItemStack(Items.COBBLESTONE, 3), ItemStack.EMPTY, ItemStack.EMPTY),
                                StationOperations.BUILD_PART, null, "", 0);
                helper.assertTrue(built.valid(), "Two stone units build a sharpening kit");
                helper.assertValueEqual(ToolRepair.kitMaterial(built.output()), id("stone"), "Kit retains its head material");
                helper.assertValueEqual(built.inputsAfter().get(1).getCount(), 1, "Only the two required units are consumed");
                helper.assertTrue(ItemStack.matches(cut.output(), built.inputsAfter().getFirst()), "The stencil is reusable");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void craftingRepairPreservesEveryToolComponent(GameTestHelper helper) {
                ItemStack original = tool(TLTools.Type.PICKAXE, id("stone"));
                original.set(TLDataComponents.PROGRESS, new ToolProgress(3, 123, 3));
                original.set(TLDataComponents.MODIFIERS, List.of(new ToolModifierEntry(ToolModifiers.DIAMOND, 1)));
                original.set(DataComponents.CUSTOM_NAME, Component.literal("Old reliable"));
                ((DynamicTool) original.getItem()).calculateStats(original);
                ToolState.initialize(original, true);
                ToolState.hurt(original, original.getMaxDamage());
                ItemStack snapshot = original.copy();
                CraftingInput input = grid(original, part(ToolParts.Type.SHARPENING_KIT, id("stone")));
                ItemStack result = result(helper, input);
                helper.assertTrue(!result.isEmpty() && !ToolState.broken(result), "Matching kit repairs a broken tool");
                helper.assertTrue(ToolState.damage(result) < ToolState.damage(original), "Repair restores durability");
                helper.assertValueEqual(result.get(TLDataComponents.PROGRESS), original.get(TLDataComponents.PROGRESS), "XP and earned slots survive");
                helper.assertValueEqual(result.get(TLDataComponents.MODIFIERS), original.get(TLDataComponents.MODIFIERS), "Modifiers survive");
                helper.assertValueEqual(result.get(TLDataComponents.MATERIALS), original.get(TLDataComponents.MATERIALS), "Material layers survive");
                helper.assertValueEqual(result.get(TLDataComponents.TOOL_ID), original.get(TLDataComponents.TOOL_ID), "Combat identity survives");
                helper.assertValueEqual(result.get(DataComponents.CUSTOM_NAME), original.get(DataComponents.CUSTOM_NAME), "Custom name survives");
                helper.assertValueEqual(result.get(TLDataComponents.REPAIR_COUNT), 1, "One repair round is recorded");
                helper.assertTrue(ItemStack.matches(original, snapshot), "Recipe preview does not mutate the original tool");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void craftingRepairRejectsWrongMaterialsAndLooseIngredients(GameTestHelper helper) {
                ItemStack tool = tool(TLTools.Type.PICKAXE, id("stone"));
                ToolState.hurt(tool, 20);
                helper.assertTrue(result(helper, grid(tool, part(ToolParts.Type.SHARPENING_KIT, id("wood")))).isEmpty(),
                                "Kit must match a head material");
                helper.assertTrue(result(helper, grid(tool, part(ToolParts.Type.SHARPENING_KIT, id("stone")),
                                part(ToolParts.Type.SHARPENING_KIT, id("stone")))).isEmpty(),
                                "Like the original, a single-head tool accepts one separate sharpening-kit slot");
                helper.assertTrue(result(helper, grid(tool, new ItemStack(Items.COBBLESTONE))).isEmpty(),
                                "Loose repair material belongs in the Tool Station");
                helper.assertTrue(result(helper, grid(tool, part(ToolParts.Type.SHARPENING_KIT, id("stone")),
                                new ItemStack(Items.DIRT))).isEmpty(), "Unrelated ingredients invalidate the recipe");
                helper.assertTrue(result(helper, grid(tool(TLTools.Type.PICKAXE, id("stone")),
                                part(ToolParts.Type.SHARPENING_KIT, id("stone")))).isEmpty(), "An undamaged tool cannot consume a kit");
                helper.assertTrue(result(helper, grid(tool, tool.copy())).isEmpty(),
                                "Vanilla two-tool repair cannot erase dynamic material components");
                ItemStack malformed = tool.copy();
                malformed.set(TLDataComponents.MATERIALS, List.of());
                helper.assertTrue(result(helper, grid(malformed, part(ToolParts.Type.SHARPENING_KIT, id("stone")))).isEmpty(),
                                "Malformed tool components are rejected before stat calculation");
                ItemStack iron = new ItemStack(Items.IRON_PICKAXE);
                iron.setDamageValue(100);
                helper.assertTrue(result(helper, grid(iron, iron.copy())).is(Items.IRON_PICKAXE),
                                "Ordinary vanilla tool repair remains available");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void kitsAlsoRepairAtTheToolStation(GameTestHelper helper) {
                ItemStack original = tool(TLTools.Type.PICKAXE, id("stone"));
                ToolState.hurt(original, 30);
                ItemStack kits = part(ToolParts.Type.SHARPENING_KIT, id("stone"));
                kits.setCount(3);
                var result = StationRecipes.evaluate(TLStations.TOOL_STATION,
                                List.of(original, kits, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY),
                                StationOperations.REPAIR, null, "", 0);
                helper.assertTrue(result.valid(), "Tool Station accepts matching kits as well as raw materials");
                helper.assertValueEqual(ToolState.damage(result.output()), 0, "Matching kit restores the damaged pickaxe");
                helper.assertValueEqual(result.inputsAfter().get(1).getCount(), 2, "Unused kits remain in the station");
                helper.assertValueEqual(kits.getCount(), 3, "Preview leaves the original kit stack alone");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void takingRepairedToolConsumesOneKitAndRetainsIdentity(GameTestHelper helper) {
                BlockPos position = new BlockPos(2, 1, 2);
                helper.setBlock(position, TLBlocks.block(TLBlocks.Workstations.Type.CRAFTING_STATION));
                StationBlockEntity station = (StationBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(position));
                ItemStack original = tool(TLTools.Type.PICKAXE, id("stone"));
                ToolState.initialize(original, true);
                ToolState.hurt(original, 20);
                var identity = original.get(TLDataComponents.TOOL_ID);
                station.setItem(4, original);
                ItemStack kits = part(ToolParts.Type.SHARPENING_KIT, id("stone"));
                kits.setCount(4);
                station.setItem(8, kits);
                var player = helper.makeMockServerPlayerInLevel();
                player.setPos(station.getBlockPos().getX() + 0.5, station.getBlockPos().getY() + 1,
                                station.getBlockPos().getZ() + 0.5);
                CraftingStationMenu menu = new CraftingStationMenu(1, player.getInventory(), station);
                menu.clicked(0, 0, ClickType.PICKUP, player);
                helper.assertTrue(!menu.getCarried().isEmpty(), "Crafting Station delivers the repair");
                helper.assertValueEqual(ToolState.damage(menu.getCarried()), 0, "Delivered tool is repaired");
                helper.assertValueEqual(menu.getCarried().get(TLDataComponents.TOOL_ID), identity,
                                "The same tool identity survives the transaction");
                helper.assertTrue(station.getItem(4).isEmpty(), "Original tool leaves the grid exactly once");
                helper.assertValueEqual(station.getItem(8).getCount(), 3, "A stack of kits spends exactly one kit");
                menu.clicked(0, 0, ClickType.PICKUP, player);
                helper.assertValueEqual(station.getItem(8).getCount(), 3, "A repeated click cannot spend more kits");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void mattockKitRepairsRetainOriginalPerHeadRounds(GameTestHelper helper) {
                ItemStack original = tool(TLTools.Type.MATTOCK, id("stone"));
                ToolState.hurt(original, original.getMaxDamage());
                ItemStack kits = part(ToolParts.Type.SHARPENING_KIT, id("stone"));
                kits.setCount(3);
                var repaired = StationRecipes.evaluate(TLStations.TOOL_STATION,
                                List.of(original, kits, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY),
                                StationOperations.REPAIR, null, "", 0);
                helper.assertTrue(repaired.valid() && !ToolState.broken(repaired.output()), "Two-head kit repair restores the mattock");
                helper.assertValueEqual(repaired.inputsAfter().get(1).getCount(), 1, "Each repair head consumes one kit in the round");
                helper.assertValueEqual(repaired.output().get(TLDataComponents.REPAIR_COUNT), 1, "Both heads contribute to the same repair round");
                helper.assertTrue(!result(helper, grid(original, part(ToolParts.Type.SHARPENING_KIT, id("stone")),
                                part(ToolParts.Type.SHARPENING_KIT, id("stone")))).isEmpty(),
                                "The original two-head validation permits two separate kit slots for a mattock");
                helper.succeed();
        }

        // Setup
        private static CraftingInput grid(ItemStack... values) {
                NonNullList<ItemStack> inputs = NonNullList.withSize(9, ItemStack.EMPTY);
                for (int slot = 0; slot < values.length; slot++) {
                        inputs.set(slot, values[slot]);
                }
                return CraftingInput.of(3, 3, inputs);
        }

        private static ItemStack result(GameTestHelper helper, CraftingInput input) {
                return helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
                                .map(recipe -> recipe.value().assemble(input, helper.getLevel().registryAccess()))
                                .orElse(ItemStack.EMPTY);
        }
}
