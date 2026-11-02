package com.nurby.tinkerslegacy.test;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.block.entity.StationBlockEntity;
import com.nurby.tinkerslegacy.library.station.MaterialInputs;
import com.nurby.tinkerslegacy.library.station.StationOperations;
import com.nurby.tinkerslegacy.library.tool.ToolState;
import com.nurby.tinkerslegacy.menu.StationMenu;
import com.nurby.tinkerslegacy.registry.TLBlocks;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLItems;
import com.nurby.tinkerslegacy.registry.TLTools;
import com.nurby.tinkerslegacy.registry.ToolParts;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.nurby.tinkerslegacy.test.ToolAssemblyGameTests.*;

@GameTestHolder(TinkersLegacy.MODID)
@PrefixGameTestTemplate(false)
public final class StationMenuGameTests {

        // Transactions
        @GameTest(template = "empty")
        public static void repeatedOutputClicksConsumeOnlyAvailablePatterns(GameTestHelper helper) {
                StationBlockEntity station = station(helper, TLBlocks.Workstations.Type.STENCIL_TABLE);
                station.setItem(0, blanks(2));
                Player player = player(helper, station);
                StationMenu menu = stencilMenu(player, station);

                menu.broadcastChanges();
                menu.broadcastChanges();
                helper.assertValueEqual(station.getItem(0).getCount(), 2, "Repeated previews consume no blanks");
                for (int click = 0; click < 3; click++) {
                        menu.clicked(menu.resultSlot(), 0, ClickType.PICKUP, player);
                }
                helper.assertValueEqual(menu.getCarried().getCount(), 2, "Two blanks produce exactly two stencils");
                helper.assertTrue(station.getItem(0).isEmpty(), "Each successful click consumes one blank");
                helper.assertTrue(menu.getSlot(menu.resultSlot()).getItem().isEmpty(), "Exhausted input clears the preview");
                helper.assertTrue(!menu.canTakeItemForPickAll(menu.getCarried(), menu.getSlot(menu.resultSlot())),
                                "Double-click collection cannot collect a virtual result");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void shiftClickRequiresRoomBeforeCommitting(GameTestHelper helper) {
                StationBlockEntity station = station(helper, TLBlocks.Workstations.Type.STENCIL_TABLE);
                station.setItem(0, blanks(1));
                Player player = player(helper, station);
                for (int index = 0; index < 36; index++) {
                        player.getInventory().setItem(index, new ItemStack(Items.STONE, 64));
                }
                StationMenu menu = stencilMenu(player, station);
                menu.clicked(menu.resultSlot(), 0, ClickType.QUICK_MOVE, player);
                helper.assertValueEqual(station.getItem(0).getCount(), 1, "Full inventories must not consume ingredients");
                player.getInventory().setItem(17, ItemStack.EMPTY);
                menu.clicked(menu.resultSlot(), 0, ClickType.QUICK_MOVE, player);
                helper.assertTrue(station.getItem(0).isEmpty(), "A free inventory slot permits one craft");
                helper.assertValueEqual(inventoryCount(player, patternItem()), 1, "Shift-click delivers exactly one stencil");
                menu.clicked(menu.resultSlot(), 0, ClickType.QUICK_MOVE, player);
                helper.assertValueEqual(inventoryCount(player, patternItem()), 1, "Repeated shift-click cannot duplicate the result");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void numberKeysAndOffhandDoNotOverwriteExistingStacks(GameTestHelper helper) {
                StationBlockEntity station = station(helper, TLBlocks.Workstations.Type.STENCIL_TABLE);
                station.setItem(0, blanks(2));
                Player player = player(helper, station);
                player.getInventory().setItem(2, new ItemStack(Items.STONE));
                StationMenu menu = stencilMenu(player, station);
                menu.clicked(menu.resultSlot(), 2, ClickType.SWAP, player);
                helper.assertValueEqual(station.getItem(0).getCount(), 2, "Occupied incompatible hotbar slot blocks the craft");
                helper.assertTrue(player.getInventory().getItem(2).is(Items.STONE), "Hotbar item is retained");
                menu.clicked(menu.resultSlot(), 12, ClickType.SWAP, player);
                helper.assertValueEqual(station.getItem(0).getCount(), 2, "Invalid swap buttons cannot craft");
                menu.clicked(menu.resultSlot(), 3, ClickType.SWAP, player);
                helper.assertTrue(player.getInventory().getItem(3).is(patternItem()), "Number key transfers to an empty hotbar slot");
                menu.clicked(menu.resultSlot(), 40, ClickType.SWAP, player);
                helper.assertTrue(player.getInventory().getItem(40).is(patternItem()), "Offhand swap transfers a second output");
                helper.assertTrue(station.getItem(0).isEmpty(), "Both valid swaps consume exactly two blanks");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void stalePreviewCannotDuplicateAnotherPlayersCraft(GameTestHelper helper) {
                StationBlockEntity station = station(helper, TLBlocks.Workstations.Type.STENCIL_TABLE);
                station.setItem(0, blanks(1));
                Player first = player(helper, station);
                Player second = player(helper, station);
                StationMenu firstMenu = stencilMenu(first, station);
                StationMenu secondMenu = stencilMenu(second, station);
                helper.assertTrue(firstMenu.getSlot(firstMenu.resultSlot()).hasItem()
                                && secondMenu.getSlot(secondMenu.resultSlot()).hasItem(), "Both viewers see the available result");
                firstMenu.clicked(firstMenu.resultSlot(), 0, ClickType.PICKUP, first);
                secondMenu.clicked(secondMenu.resultSlot(), 0, ClickType.PICKUP, second);
                helper.assertValueEqual(firstMenu.getCarried().getCount(), 1, "First viewer receives the single result");
                helper.assertTrue(secondMenu.getCarried().isEmpty(), "Stale second preview cannot issue another item");
                helper.assertTrue(!secondMenu.getSlot(secondMenu.resultSlot()).hasItem(), "Stale result is refreshed away");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void craftedToolsReceiveIdentityOnlyWhenTaken(GameTestHelper helper) {
                StationBlockEntity station = station(helper, TLBlocks.Workstations.Type.TOOL_STATION);
                var parts = parts(TLTools.tool(TLTools.Type.PICKAXE), id("wood"));
                for (int index = 0; index < parts.size(); index++) {
                        station.setItem(index, parts.get(index));
                }
                Player player = player(helper, station);
                StationMenu menu = new StationMenu(1, player.getInventory(), station);
                menu.select(StationOperations.ASSEMBLE, id("pickaxe"), "", 0);
                helper.assertTrue(menu.getSlot(menu.resultSlot()).hasItem(), "Complete physical inputs provide a preview");
                helper.assertTrue(!menu.getSlot(menu.resultSlot()).getItem().has(TLDataComponents.TOOL_ID),
                                "Preview stacks have no persistent combat identity");
                menu.clicked(menu.resultSlot(), 0, ClickType.PICKUP, player);
                helper.assertTrue(menu.getCarried().has(TLDataComponents.TOOL_ID), "Committing gives the real tool its identity");
                helper.assertTrue(station.isEmpty(), "Assembly consumes one of each physical part");
                var identity = menu.getCarried().get(TLDataComponents.TOOL_ID);
                menu.clicked(menu.resultSlot(), 0, ClickType.PICKUP, player);
                helper.assertValueEqual(menu.getCarried().get(TLDataComponents.TOOL_ID), identity, "Repeated click keeps the existing tool identity");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void maintenancePrefillsNamesAndKeepsExplicitEdits(GameTestHelper helper) {
                StationBlockEntity station = station(helper, TLBlocks.Workstations.Type.TOOL_STATION);
                ItemStack original = tool(TLTools.Type.PICKAXE, id("wood"));
                original.set(DataComponents.CUSTOM_NAME, Component.literal("Old reliable"));
                ToolState.hurt(original, 10);
                station.setItem(0, original);
                station.setItem(1, new ItemStack(Items.OAK_PLANKS));
                Player player = player(helper, station);
                StationMenu menu = new StationMenu(1, player.getInventory(), station);
                helper.assertValueEqual(menu.name(), "Old reliable", "Maintenance adopts the inserted tool's custom name");
                helper.assertValueEqual(menu.getSlot(menu.resultSlot()).getItem().get(DataComponents.CUSTOM_NAME),
                                Component.literal("Old reliable"), "A repair preview retains the existing name");
                menu.select(StationOperations.MAINTAIN, null, "New reliable", 0);
                menu.broadcastChanges();
                menu.broadcastChanges();
                helper.assertValueEqual(menu.name(), "New reliable", "Repeated refresh does not overwrite a typed name");
                menu.clicked(menu.resultSlot(), 0, ClickType.PICKUP, player);
                helper.assertValueEqual(menu.getCarried().get(DataComponents.CUSTOM_NAME),
                                Component.literal("New reliable"), "Repair and explicit rename commit together");
                helper.assertValueEqual(ToolState.damage(menu.getCarried()), 0, "Named result is also repaired");
                helper.assertValueEqual(original.get(DataComponents.CUSTOM_NAME), Component.literal("Old reliable"),
                                "The original preview input was never renamed in place");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void switchingFromAssemblyToMaintenanceAcceptsNoSelection(GameTestHelper helper) {
                StationBlockEntity station = station(helper, TLBlocks.Workstations.Type.TOOL_STATION);
                Player player = player(helper, station);
                StationMenu menu = new StationMenu(1, player.getInventory(), station);
                menu.select(StationOperations.ASSEMBLE, id("pickaxe"), "", 0);
                helper.assertValueEqual(menu.selection(), id("pickaxe"), "Assembly selects a concrete tool shape");

                for (var operation : List.of(StationOperations.MAINTAIN, StationOperations.REPAIR,
                                StationOperations.REPLACE, StationOperations.MODIFY, StationOperations.RENAME)) {
                        menu.select(operation, null, "", 0);
                        helper.assertValueEqual(menu.operation(), operation, "Maintenance operation selected");
                        helper.assertTrue(menu.selection() == null, "Maintenance operations do not require a shape selection");
                }
                menu.select(StationOperations.ASSEMBLE, null, "", 0);
                helper.assertTrue(menu.selection() != null && menu.selections().contains(menu.selection()),
                                "Switching back to assembly without a shape selects its first available tool");
                helper.succeed();
        }

        // Inventories
        @GameTest(template = "empty")
        public static void closingSavingAndBreakingPreservePartBuilderContents(GameTestHelper helper) {
                StationBlockEntity station = station(helper, TLBlocks.Workstations.Type.PART_BUILDER);
                ItemStack stencil = new ItemStack(patternItem());
                stencil.set(TLDataComponents.PART, ToolParts.get(ToolParts.Type.PICKAXE_HEAD).getId());
                station.setItem(0, stencil);
                station.setItem(1, new ItemStack(Items.OAK_LOG));
                Player player = player(helper, station);
                StationMenu menu = new StationMenu(1, player.getInventory(), station);
                menu.clicked(menu.resultSlot(), 0, ClickType.PICKUP, player);
                helper.assertValueEqual(station.getItem(3).getCount(), 4, "A two-unit head returns four half-unit shards");
                helper.assertTrue(station.getItem(0).is(patternItem()), "The reusable stencil remains in the station");
                menu.removed(player);
                helper.assertValueEqual(station.getItem(3).getCount(), 4, "Closing leaves remainder inventory intact");

                var registries = helper.getLevel().registryAccess();
                var saved = station.saveWithFullMetadata(registries);
                StationBlockEntity loaded = new StationBlockEntity(station.getBlockPos(), station.getBlockState());
                loaded.loadWithComponents(saved, registries);
                for (int slot = 0; slot < station.getContainerSize(); slot++) {
                        helper.assertTrue(ItemStack.matches(station.getItem(slot), loaded.getItem(slot)),
                                        "Persistence retains components and counts in station slot " + slot);
                }
                ItemStack shards = station.getItem(3).copy();
                BlockPos position = station.getBlockPos();
                helper.getLevel().setBlock(position, Blocks.AIR.defaultBlockState(), 3);
                int droppedShards = 0;
                int droppedPatterns = 0;
                for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(position).inflate(1))) {
                        if (ItemStack.isSameItemSameComponents(entity.getItem(), shards)) {
                                droppedShards += entity.getItem().getCount();
                        }
                        if (ItemStack.isSameItemSameComponents(entity.getItem(), stencil)) {
                                droppedPatterns += entity.getItem().getCount();
                        }
                }
                helper.assertValueEqual(droppedShards, 4, "Breaking drops all remainder shards exactly once");
                helper.assertValueEqual(droppedPatterns, 1, "Breaking drops the retained pattern exactly once");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void fullRemainderAndDormantInputsCannotLoseItems(GameTestHelper helper) {
                StationBlockEntity builder = station(helper, TLBlocks.Workstations.Type.PART_BUILDER);
                ItemStack stencil = new ItemStack(patternItem());
                stencil.set(TLDataComponents.PART, ToolParts.get(ToolParts.Type.PICKAXE_HEAD).getId());
                builder.setItem(0, stencil);
                builder.setItem(1, new ItemStack(Items.OAK_LOG));
                builder.setItem(3, MaterialInputs.shard(id("wood"), 61));
                Player player = player(helper, builder);
                StationMenu menu = new StationMenu(1, player.getInventory(), builder);
                menu.clicked(menu.resultSlot(), 0, ClickType.PICKUP, player);
                helper.assertTrue(menu.getCarried().isEmpty(), "A full remainder slot prevents output collection");
                helper.assertValueEqual(builder.getItem(1).getCount(), 1, "Failed remainder transaction retains the log");
                helper.assertValueEqual(builder.getItem(3).getCount(), 61, "Existing remainder remains unchanged");

                helper.setBlock(new BlockPos(3, 1, 2), TLBlocks.block(TLBlocks.Workstations.Type.TOOL_STATION));
                var toolStation = (StationBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(3, 1, 2)));
                toolStation.setItem(5, new ItemStack(Items.DIAMOND));
                StationMenu tools = new StationMenu(2, player.getInventory(), toolStation);
                tools.select(StationOperations.ASSEMBLE, id("pickaxe"), "", 0);
                helper.assertTrue(tools.getSlot(5).isActive(), "A leftover maintenance ingredient remains visible");
                helper.assertTrue(!tools.getSlot(5).mayPlace(new ItemStack(Items.DIAMOND)),
                                "Dormant assembly inputs reject additional items");
                tools.clicked(5, 0, ClickType.PICKUP, player);
                helper.assertTrue(tools.getCarried().is(Items.DIAMOND), "The leftover item can still be recovered");
                helper.assertTrue(!tools.getSlot(5).isActive(), "An empty dormant input hides again");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void outputCannotBeTakenAfterWalkingAway(GameTestHelper helper) {
                StationBlockEntity station = station(helper, TLBlocks.Workstations.Type.STENCIL_TABLE);
                station.setItem(0, blanks(1));
                Player player = player(helper, station);
                StationMenu menu = stencilMenu(player, station);
                player.setPos(station.getBlockPos().getX() + 20, station.getBlockPos().getY(), station.getBlockPos().getZ());
                menu.clicked(menu.resultSlot(), 0, ClickType.PICKUP, player);
                helper.assertTrue(menu.getCarried().isEmpty(), "An out-of-range player cannot collect a result");
                helper.assertValueEqual(station.getItem(0).getCount(), 1, "Out-of-range requests retain ingredients");
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

        private static StationMenu stencilMenu(Player player, StationBlockEntity station) {
                StationMenu menu = new StationMenu(1, player.getInventory(), station);
                menu.select(StationOperations.CUT_PATTERN, ToolParts.get(ToolParts.Type.PICKAXE_HEAD).getId(), "", 0);
                return menu;
        }

        private static ItemStack blanks(int count) {
                return new ItemStack(TLItems.item(TLItems.Items.IntermediaryItems.Type.BLANK_PATTERN), count);
        }

        private static Item patternItem() {
                return TLItems.item(TLItems.Items.IntermediaryItems.Type.PATTERN);
        }

        private static int inventoryCount(Player player, Item item) {
                int count = 0;
                for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                        ItemStack stack = player.getInventory().getItem(slot);
                        if (stack.is(item)) {
                                count += stack.getCount();
                        }
                }
                return count;
        }
}
