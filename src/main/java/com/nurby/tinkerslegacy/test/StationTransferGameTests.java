package com.nurby.tinkerslegacy.test;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.block.entity.StationBlockEntity;
import com.nurby.tinkerslegacy.library.station.MaterialInputs;
import com.nurby.tinkerslegacy.library.station.StationOperations;
import com.nurby.tinkerslegacy.menu.StationMenu;
import com.nurby.tinkerslegacy.registry.TLBlocks;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLItems;
import com.nurby.tinkerslegacy.registry.TLMaterials;
import com.nurby.tinkerslegacy.registry.TLTools;
import com.nurby.tinkerslegacy.registry.ToolParts;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.nurby.tinkerslegacy.test.ToolAssemblyGameTests.*;

@GameTestHolder(TinkersLegacy.MODID)
@PrefixGameTestTemplate(false)
public final class StationTransferGameTests {

        // Patterns
        @GameTest(template = "empty")
        public static void patternsAndMaterialsFindTheirOwnSlots(GameTestHelper helper) {
                StationBlockEntity stencil = station(helper, TLBlocks.Workstations.Type.STENCIL_TABLE);
                Player player = player(helper, stencil);
                StationMenu menu = new StationMenu(1, player.getInventory(), stencil);
                ItemStack blanks = new ItemStack(TLItems.item(TLItems.Items.IntermediaryItems.Type.BLANK_PATTERN), 8);
                transfer(player, menu, 0, blanks);
                helper.assertValueEqual(stencil.getItem(0).getCount(), 8, "Blank patterns enter the stencil input");
                transfer(player, menu, 1, new ItemStack(Items.OAK_PLANKS, 4));
                helper.assertValueEqual(stencil.getItem(0).getCount(), 8, "Unrelated materials cannot enter the stencil slot");
                helper.assertValueEqual(count(player, new ItemStack(Items.OAK_PLANKS)), 4, "Rejected items remain in player inventory");

                StationBlockEntity builder = station(helper, TLBlocks.Workstations.Type.PART_BUILDER);
                player.getInventory().clearContent();
                menu = new StationMenu(2, player.getInventory(), builder);
                transfer(player, menu, 0, pattern());
                transfer(player, menu, 1, new ItemStack(Items.OAK_LOG, 3));
                transfer(player, menu, 2, MaterialInputs.shard(id("wood"), 4));
                helper.assertTrue(builder.getItem(0).is(TLItems.item(TLItems.Items.IntermediaryItems.Type.PATTERN)),
                                "A stencil enters the reusable pattern slot");
                helper.assertTrue(builder.getItem(1).is(Items.OAK_LOG), "A log enters the first material slot");
                helper.assertTrue(builder.getItem(2).is(TLItems.Parts.MATERIAL_SHARD.get()),
                                "Matching shards enter the second material slot");
                helper.assertTrue(builder.getItem(3).isEmpty(), "Shift-clicking never inserts into the remainder output");
                transfer(player, menu, 3, new ItemStack(Items.IRON_INGOT, 2));
                transfer(player, menu, 4, new ItemStack(Items.STONE, 2));
                helper.assertValueEqual(count(player, new ItemStack(Items.IRON_INGOT)), 2, "Metal cannot mix with the occupied wood material inputs");
                helper.assertValueEqual(count(player, new ItemStack(Items.STONE)), 2, "Different materials cannot clog an occupied recipe");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void materialStacksMergeBeforeUsingEmptySlots(GameTestHelper helper) {
                StationBlockEntity station = station(helper, TLBlocks.Workstations.Type.PART_BUILDER);
                station.setItem(2, new ItemStack(Items.OAK_PLANKS, 60));
                Player player = player(helper, station);
                StationMenu menu = new StationMenu(1, player.getInventory(), station);
                transfer(player, menu, 0, new ItemStack(Items.OAK_PLANKS, 8));
                helper.assertValueEqual(station.getItem(2).getCount(), 64, "An existing stack fills before an empty slot");
                helper.assertValueEqual(station.getItem(1).getCount(), 4, "Only the overflow uses the second material slot");
                helper.assertTrue(station.getItem(0).isEmpty() && station.getItem(3).isEmpty(),
                                "Materials do not spill into patterns or remainder outputs");
                helper.assertValueEqual(count(player, new ItemStack(Items.OAK_PLANKS)), 0, "All eight transferred planks are accounted for");
                helper.succeed();
        }

        // Assembly
        @GameTest(template = "empty")
        public static void everyToolRoutesPartsByShapeInsteadOfClickOrder(GameTestHelper helper) {
                StationBlockEntity station = station(helper, TLBlocks.Workstations.Type.TOOL_FORGE);
                Player player = player(helper, station);
                for (TLTools.Type type : TLTools.Type.values()) {
                        station.clearContent();
                        player.getInventory().clearContent();
                        StationMenu menu = new StationMenu(1, player.getInventory(), station);
                        menu.select(StationOperations.ASSEMBLE, TLTools.get(type).getId(), "", 0);
                        var definition = TLTools.tool(type);
                        var parts = exampleParts(definition, definition.defaultMaterials(
                                        type == TLTools.Type.BOLT ? TLMaterials.IRON.get() : TLMaterials.WOOD.get()));
                        for (int index = parts.size() - 1; index >= 0; index--) {
                                transfer(player, menu, index, parts.get(index).copyWithCount(3));
                        }
                        for (int index = 0; index < parts.size(); index++) {
                                helper.assertTrue(ItemStack.isSameItemSameComponents(station.getItem(index), parts.get(index)),
                                                type + " physical part " + index + " enters its matching shape slot");
                                ItemStack expected = parts.get(index);
                                int expectedCount = (int) parts.stream().filter(part ->
                                                ItemStack.isSameItemSameComponents(part, expected)).count() * 3;
                                int storedCount = java.util.stream.IntStream.range(0, station.getContainerSize())
                                                .mapToObj(station::getItem).filter(part ->
                                                                ItemStack.isSameItemSameComponents(part, expected))
                                                .mapToInt(ItemStack::getCount).sum();
                                helper.assertValueEqual(storedCount, expectedCount, "Spare matching parts stack without loss");
                        }
                        menu.broadcastChanges();
                        helper.assertTrue(menu.getSlot(menu.resultSlot()).getItem().is(TLItems.Tools.item(type)),
                                        "Reverse-order shift-clicks provide the selected tool preview");
                }
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void wrongPartsAndFullShapeSlotsDoNotSpillIntoOtherInputs(GameTestHelper helper) {
                StationBlockEntity station = station(helper, TLBlocks.Workstations.Type.TOOL_STATION);
                Player player = player(helper, station);
                StationMenu menu = new StationMenu(1, player.getInventory(), station);
                menu.select(StationOperations.ASSEMBLE, id("pickaxe"), "", 0);
                ItemStack head = part(ToolParts.Type.PICKAXE_HEAD, id("wood"));
                station.setItem(0, head.copyWithCount(64));
                transfer(player, menu, 0, head.copyWithCount(10));
                ItemStack axe = part(ToolParts.Type.AXE_HEAD, id("wood"));
                transfer(player, menu, 1, axe.copyWithCount(2));
                ItemStack invalid = new ItemStack(TLItems.Parts.item(ToolParts.Type.TOOL_ROD));
                transfer(player, menu, 2, invalid.copy());
                helper.assertValueEqual(station.getItem(0).getCount(), 64, "A full head slot is unchanged");
                for (int index = 1; index < station.getContainerSize(); index++) {
                        helper.assertTrue(station.getItem(index).isEmpty(), "Wrong, extra and malformed parts never enter another shape slot");
                }
                helper.assertValueEqual(count(player, head), 10, "Excess heads remain in inventory");
                helper.assertValueEqual(count(player, axe), 2, "Wrong shapes remain in inventory");
                helper.assertValueEqual(count(player, invalid), 1, "Parts without materials remain recoverable");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void repeatedShapeSlotsReceiveOnePartBeforeSpareStacks(GameTestHelper helper) {
                StationBlockEntity station = station(helper, TLBlocks.Workstations.Type.TOOL_STATION);
                Player player = player(helper, station);
                StationMenu menu = new StationMenu(1, player.getInventory(), station);
                menu.select(StationOperations.ASSEMBLE, id("pickaxe"), "", 0);
                ItemStack rod = part(ToolParts.Type.TOOL_ROD, id("wood"));
                // Exercise a future pair of equal physical requirements without registering a production tool.
                for (int index = 0; index < 2; index++) {
                        Slot previous = menu.getSlot(index);
                        Slot repeated = new Slot(station, index, previous.x, previous.y) {
                                @Override
                                public boolean mayPlace(ItemStack stack) {
                                        return ItemStack.isSameItemSameComponents(stack, rod);
                                }
                        };
                        repeated.index = index;
                        menu.slots.set(index, repeated);
                }
                transfer(player, menu, 0, rod.copyWithCount(2));
                helper.assertValueEqual(station.getItem(0).getCount(), 1, "First identical requirement receives one part");
                helper.assertValueEqual(station.getItem(1).getCount(), 1, "Second identical requirement is supplied before stacking");
                helper.assertValueEqual(count(player, rod), 0, "Both copies moved exactly once");
                helper.succeed();
        }

        // Maintenance
        @GameTest(template = "empty")
        public static void maintenanceRoutesToolsAndOnlySupportedIngredients(GameTestHelper helper) {
                StationBlockEntity station = station(helper, TLBlocks.Workstations.Type.TOOL_STATION);
                Player player = player(helper, station);
                StationMenu menu = new StationMenu(1, player.getInventory(), station);
                ItemStack pickaxe = tool(TLTools.Type.PICKAXE, id("wood"));
                transfer(player, menu, 0, pickaxe.copy());
                transfer(player, menu, 1, new ItemStack(Items.OAK_PLANKS, 4));
                transfer(player, menu, 2, new ItemStack(Items.REDSTONE, 8));
                ItemStack head = part(ToolParts.Type.PICKAXE_HEAD, id("stone"));
                transfer(player, menu, 3, head.copy());
                helper.assertTrue(ItemStack.matches(station.getItem(0), pickaxe), "The tool enters the center maintenance slot unchanged");
                helper.assertTrue(station.getItem(1).is(Items.OAK_PLANKS), "Matching repair material enters an ingredient slot");
                helper.assertTrue(station.getItem(2).is(Items.REDSTONE), "Registered modifier ingredients enter ingredient slots");
                helper.assertTrue(ItemStack.matches(station.getItem(3), head), "Compatible replacement parts enter ingredient slots");
                transfer(player, menu, 4, new ItemStack(Items.STONE, 2));
                transfer(player, menu, 5, new ItemStack(Items.APPLE, 2));
                helper.assertTrue(station.getItem(4).isEmpty() && station.getItem(5).isEmpty(),
                                "Unrelated items and nonmatching repair materials stay out of maintenance slots");
                helper.assertValueEqual(count(player, new ItemStack(Items.STONE)), 2, "Nonmatching material is retained");
                helper.assertValueEqual(count(player, new ItemStack(Items.APPLE)), 2, "Unrelated items are retained");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void repairAndModifyModesKeepTheirOwnIngredientRules(GameTestHelper helper) {
                StationBlockEntity station = station(helper, TLBlocks.Workstations.Type.TOOL_STATION);
                station.setItem(0, legacyTool(TLTools.Type.PICKAXE, id("diamond")));
                Player player = player(helper, station);
                StationMenu menu = new StationMenu(1, player.getInventory(), station);
                menu.select(StationOperations.REPAIR, null, "", 0);
                transfer(player, menu, 0, new ItemStack(Items.REDSTONE, 4));
                transfer(player, menu, 1, new ItemStack(Items.DIAMOND, 2));
                helper.assertTrue(station.getItem(1).is(Items.DIAMOND), "Repair mode accepts matching head materials");
                helper.assertTrue(station.getItem(2).isEmpty(), "Repair mode rejects modifier-only inputs");
                helper.assertValueEqual(count(player, new ItemStack(Items.REDSTONE)), 4, "Rejected modifier remains available");
                menu.select(StationOperations.MODIFY, null, "", 0);
                transfer(player, menu, 2, new ItemStack(Items.REDSTONE, 3));
                helper.assertTrue(station.getItem(2).is(Items.REDSTONE), "Modification mode accepts a registered modifier ingredient");
                transfer(player, menu, 3, TLItems.Parts.MATERIAL_SHARD.get().withMaterial(id("diamond"), 2));
                helper.assertTrue(station.getItem(3).isEmpty(), "Modification mode excludes legacy diamond shards");
                helper.assertValueEqual(count(player, TLItems.Parts.MATERIAL_SHARD.get().withMaterial(id("diamond"), 1)), 2, "Rejected shards retain their material component");
                helper.succeed();
        }

        // Inventories
        @GameTest(template = "empty")
        public static void inventoryFallbackAndRemainderTransfersNeverLoseItems(GameTestHelper helper) {
                StationBlockEntity station = station(helper, TLBlocks.Workstations.Type.PART_BUILDER);
                ItemStack shards = MaterialInputs.shard(id("wood"), 5);
                station.setItem(3, shards.copy());
                Player player = player(helper, station);
                StationMenu menu = new StationMenu(1, player.getInventory(), station);
                transfer(player, menu, 9, new ItemStack(Items.APPLE, 4));
                helper.assertTrue(player.getInventory().getItem(0).is(Items.APPLE), "Unrelated main-inventory items move to the hotbar");
                menu.quickMoveStack(player, inventorySlot(menu, 0));
                helper.assertTrue(player.getInventory().getItem(9).is(Items.APPLE), "Unrelated hotbar items move back to main inventory");
                for (int index = 0; index < 36; index++) {
                        player.getInventory().setItem(index, new ItemStack(Items.STONE, 64));
                }
                menu.quickMoveStack(player, 3);
                helper.assertTrue(ItemStack.matches(station.getItem(3), shards), "A full inventory leaves remainder output untouched");
                player.getInventory().setItem(17, ItemStack.EMPTY);
                menu.quickMoveStack(player, 3);
                helper.assertTrue(station.getItem(3).isEmpty(), "Remainders shift out when inventory space exists");
                helper.assertTrue(ItemStack.matches(player.getInventory().getItem(17), shards),
                                "Remainder transfers preserve count and material component");
                helper.succeed();
        }

        // Setup
        private static StationBlockEntity station(GameTestHelper helper, TLBlocks.Workstations.Type type) {
                BlockPos relative = new BlockPos(2, 1, 2);
                helper.setBlock(relative, TLBlocks.block(type));
                return (StationBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(relative));
        }

        private static Player player(GameTestHelper helper, StationBlockEntity station) {
                Player player = helper.makeMockPlayer(GameType.SURVIVAL);
                player.setPos(station.getBlockPos().getX() + 0.5, station.getBlockPos().getY() + 1,
                                station.getBlockPos().getZ() + 0.5);
                return player;
        }

        private static ItemStack pattern() {
                ItemStack pattern = new ItemStack(TLItems.item(TLItems.Items.IntermediaryItems.Type.PATTERN));
                pattern.set(TLDataComponents.PART, ToolParts.get(ToolParts.Type.PICKAXE_HEAD).getId());
                return pattern;
        }

        private static int inventorySlot(StationMenu menu, int index) {
                return menu.resultSlot() + 1 + (index < 9 ? 27 + index : index - 9);
        }

        private static void transfer(Player player, StationMenu menu, int index, ItemStack stack) {
                player.getInventory().setItem(index, stack);
                menu.clicked(inventorySlot(menu, index), 0, ClickType.QUICK_MOVE, player);
        }

        private static int count(Player player, ItemStack target) {
                int count = 0;
                for (int index = 0; index < 36; index++) {
                        ItemStack stack = player.getInventory().getItem(index);
                        if (ItemStack.isSameItemSameComponents(stack, target)) {
                                count += stack.getCount();
                        }
                }
                return count;
        }
}
