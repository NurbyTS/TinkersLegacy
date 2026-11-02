package com.nurby.tinkerslegacy.test;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.block.entity.StationBlockEntity;
import com.nurby.tinkerslegacy.library.station.StationConnections;
import com.nurby.tinkerslegacy.menu.CraftingStationMenu;
import com.nurby.tinkerslegacy.menu.StationMenu;
import com.nurby.tinkerslegacy.menu.StationMenuAccess;
import com.nurby.tinkerslegacy.registry.TLBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(TinkersLegacy.MODID)
@PrefixGameTestTemplate(false)
public final class CraftingStationGameTests {

        // Crafting
        @GameTest(template = "empty")
        public static void shapelessCraftingConsumesOnlyWhenTaken(GameTestHelper helper) {
                StationBlockEntity station = station(helper);
                station.setItem(8, new ItemStack(Items.OAK_LOG, 3));
                ServerPlayer player = player(helper, station);
                CraftingStationMenu menu = menu(player, station);

                for (int preview = 0; preview < 3; preview++) menu.broadcastChanges();
                helper.assertTrue(menu.getSlot(0).getItem().is(Items.OAK_PLANKS), "A log anywhere in the grid previews planks");
                helper.assertValueEqual(menu.getSlot(0).getItem().getCount(), 4, "Vanilla recipe output count is retained");
                helper.assertValueEqual(station.getItem(8).getCount(), 3, "Preview refresh does not consume logs");
                for (int click = 0; click < 4; click++) menu.clicked(0, 0, ClickType.PICKUP, player);
                helper.assertValueEqual(menu.getCarried().getCount(), 12, "Three logs yield twelve planks after repeated clicks");
                helper.assertTrue(station.isEmpty() && menu.getSlot(0).getItem().isEmpty(), "Exhaustion clears inputs and output");
                helper.assertTrue(!menu.canTakeItemForPickAll(menu.getCarried(), menu.getSlot(0)),
                                "Double-click collection excludes the virtual result");
                succeed(helper, player);
        }

        @GameTest(template = "empty")
        public static void shapedRecipesWorkAtAnOffset(GameTestHelper helper) {
                StationBlockEntity station = station(helper);
                for (int slot : new int[] { 4, 5, 7, 8 }) station.setItem(slot, new ItemStack(Items.OAK_PLANKS));
                ServerPlayer player = player(helper, station);
                CraftingStationMenu menu = menu(player, station);
                helper.assertTrue(menu.getSlot(0).getItem().is(Items.CRAFTING_TABLE), "A bottom-right two-by-two square matches the vanilla recipe");
                menu.clicked(0, 0, ClickType.PICKUP, player);
                helper.assertTrue(menu.getCarried().is(Items.CRAFTING_TABLE), "Taking the offset recipe gives a crafting table");
                helper.assertTrue(station.isEmpty(), "Only the four positioned ingredients are consumed");

                menu.setCarried(ItemStack.EMPTY);
                menu.grid().setItem(4, new ItemStack(Items.OAK_PLANKS));
                menu.grid().setItem(5, new ItemStack(Items.OAK_PLANKS));
                menu.grid().setItem(7, new ItemStack(Items.OAK_PLANKS));
                menu.grid().setItem(6, new ItemStack(Items.OAK_PLANKS));
                helper.assertTrue(menu.getSlot(0).getItem().isEmpty(), "An incorrect arrangement is not a crafting table recipe");
                succeed(helper, player);
        }

        @GameTest(template = "empty")
        public static void shiftCraftingHonorsFullInventoryAndOutputStackLimits(GameTestHelper helper) {
                StationBlockEntity station = station(helper);
                station.setItem(0, new ItemStack(Items.OAK_LOG, 64));
                ServerPlayer player = player(helper, station);
                fillInventory(player, Items.STONE);
                CraftingStationMenu menu = menu(player, station);
                menu.clicked(0, 0, ClickType.QUICK_MOVE, player);
                helper.assertValueEqual(station.getItem(0).getCount(), 64, "A completely full inventory blocks crafting");
                helper.assertValueEqual(dropped(helper, station.getBlockPos(), Items.OAK_PLANKS), 0,
                                "A completely blocked transfer does not create dropped outputs");

                player.getInventory().clearContent();
                menu.clicked(0, 0, ClickType.QUICK_MOVE, player);
                helper.assertValueEqual(count(player.getInventory(), Items.OAK_PLANKS), 256,
                                "Shift-click crafts all sixty-four logs into four stacks of planks");
                helper.assertTrue(station.isEmpty(), "Each shift-crafted output consumes its ingredient once");
                menu.clicked(0, 0, ClickType.QUICK_MOVE, player);
                helper.assertValueEqual(count(player.getInventory(), Items.OAK_PLANKS), 256, "Repeated shift-click cannot duplicate exhausted output");
                assertStackLimits(helper, player.getInventory());
                succeed(helper, player);
        }

        @GameTest(template = "empty")
        public static void partiallyFittingShiftOutputIsNeverLostOrOverstacked(GameTestHelper helper) {
                StationBlockEntity station = station(helper);
                station.setItem(0, new ItemStack(Items.OAK_LOG));
                ServerPlayer player = player(helper, station);
                fillInventory(player, Items.STONE);
                player.getInventory().setItem(0, new ItemStack(Items.OAK_PLANKS, 61));
                CraftingStationMenu menu = menu(player, station);
                menu.clicked(0, 0, ClickType.QUICK_MOVE, player);
                helper.assertTrue(station.isEmpty(), "Vanilla partial transfer crafts once when some output fits");
                helper.assertValueEqual(count(player.getInventory(), Items.OAK_PLANKS), 64, "Only three planks fill the remaining inventory space");
                helper.assertValueEqual(dropped(helper, station.getBlockPos(), Items.OAK_PLANKS), 1,
                                "The fourth plank is dropped intact like vanilla crafting");
                assertStackLimits(helper, player.getInventory());
                succeed(helper, player);
        }

        @GameTest(template = "empty")
        public static void craftingReturnsBucketsToTheirOriginalSlots(GameTestHelper helper) {
                StationBlockEntity station = station(helper);
                for (int slot = 0; slot < 3; slot++) station.setItem(slot, new ItemStack(Items.MILK_BUCKET));
                station.setItem(3, new ItemStack(Items.SUGAR));
                station.setItem(4, new ItemStack(Items.EGG));
                station.setItem(5, new ItemStack(Items.SUGAR));
                for (int slot = 6; slot < 9; slot++) station.setItem(slot, new ItemStack(Items.WHEAT));
                ServerPlayer player = player(helper, station);
                CraftingStationMenu menu = menu(player, station);
                helper.assertTrue(menu.getSlot(0).getItem().is(Items.CAKE), "The complete vanilla cake recipe is recognized");
                menu.clicked(0, 0, ClickType.PICKUP, player);
                helper.assertTrue(menu.getCarried().is(Items.CAKE), "The result is one cake");
                for (int slot = 0; slot < 3; slot++) {
                        helper.assertTrue(station.getItem(slot).is(Items.BUCKET), "Milk bucket remainder returns to grid slot " + slot);
                        helper.assertValueEqual(station.getItem(slot).getCount(), 1, "Each milk bucket returns one empty bucket");
                }
                for (int slot = 3; slot < 9; slot++) helper.assertTrue(station.getItem(slot).isEmpty(), "Non-container ingredients are consumed");
                helper.assertTrue(menu.getSlot(0).getItem().isEmpty(), "Bucket remainders do not retain the old cake preview");
                succeed(helper, player);
        }

        // Transactions
        @GameTest(template = "empty")
        public static void staleViewerCannotTakeAnotherPlayersResult(GameTestHelper helper) {
                StationBlockEntity station = station(helper);
                station.setItem(0, new ItemStack(Items.OAK_LOG));
                ServerPlayer first = player(helper, station);
                ServerPlayer second = player(helper, station);
                CraftingStationMenu firstMenu = menu(first, station);
                CraftingStationMenu secondMenu = menu(second, station);
                helper.assertTrue(firstMenu.getSlot(0).hasItem() && secondMenu.getSlot(0).hasItem(), "Both viewers initially see the same recipe");
                firstMenu.clicked(0, 0, ClickType.PICKUP, first);
                secondMenu.clicked(0, 0, ClickType.PICKUP, second);
                helper.assertValueEqual(firstMenu.getCarried().getCount(), 4, "The first viewer receives the only craft");
                helper.assertTrue(secondMenu.getCarried().isEmpty(), "A stale second viewer receives nothing");
                helper.assertTrue(secondMenu.getSlot(0).getItem().isEmpty(), "The stale preview is cleared");
                succeed(helper, first, second);
        }

        @GameTest(template = "empty")
        public static void changedIngredientsRequireANewOutputPreview(GameTestHelper helper) {
                StationBlockEntity station = station(helper);
                station.setItem(0, new ItemStack(Items.OAK_LOG));
                ServerPlayer player = player(helper, station);
                CraftingStationMenu menu = menu(player, station);
                station.setItem(0, new ItemStack(Items.BIRCH_LOG));
                menu.clicked(0, 0, ClickType.PICKUP, player);
                helper.assertTrue(menu.getCarried().isEmpty(), "An old oak-plank preview cannot commit changed ingredients");
                helper.assertTrue(station.getItem(0).is(Items.BIRCH_LOG), "Rejecting the stale click leaves the replacement log");
                helper.assertTrue(menu.getSlot(0).getItem().is(Items.BIRCH_PLANKS), "The replacement recipe is now displayed");
                menu.clicked(0, 0, ClickType.PICKUP, player);
                helper.assertTrue(menu.getCarried().is(Items.BIRCH_PLANKS), "Taking the refreshed preview commits the new recipe");
                succeed(helper, player);
        }

        @GameTest(template = "empty")
        public static void walkingAwayCannotCraftFromAStoredPreview(GameTestHelper helper) {
                StationBlockEntity station = station(helper);
                station.setItem(0, new ItemStack(Items.OAK_LOG));
                ServerPlayer player = player(helper, station);
                CraftingStationMenu menu = menu(player, station);
                player.setPos(station.getBlockPos().getX() + 20, station.getBlockPos().getY(), station.getBlockPos().getZ());
                menu.clicked(0, 0, ClickType.PICKUP, player);
                helper.assertTrue(menu.getCarried().isEmpty(), "Out-of-range output clicks are rejected");
                helper.assertValueEqual(station.getItem(0).getCount(), 1, "Rejected out-of-range clicks retain the log");
                succeed(helper, player);
        }

        // Inventories
        @GameTest(template = "empty")
        public static void closingSavingAndBreakingPreserveOnlyRealInputs(GameTestHelper helper) {
                StationBlockEntity station = station(helper);
                ItemStack logs = new ItemStack(Items.OAK_LOG, 3);
                logs.set(DataComponents.CUSTOM_NAME, Component.literal("Stored ingredients"));
                station.setItem(8, logs.copy());
                ServerPlayer player = player(helper, station);
                CraftingStationMenu menu = menu(player, station);
                helper.assertTrue(menu.getSlot(0).hasItem(), "A real recipe preview exists before closing");
                menu.removed(player);
                helper.assertTrue(ItemStack.matches(station.getItem(8), logs), "Closing retains the original grid and components");
                helper.assertValueEqual(count(player.getInventory(), Items.OAK_LOG), 0, "Closing does not return persistent inputs to the player");

                var registries = helper.getLevel().registryAccess();
                StationBlockEntity loaded = new StationBlockEntity(station.getBlockPos(), station.getBlockState());
                loaded.loadWithComponents(station.saveWithFullMetadata(registries), registries);
                for (int slot = 0; slot < 9; slot++) {
                        helper.assertTrue(ItemStack.matches(station.getItem(slot), loaded.getItem(slot)),
                                        "Saving and loading retain grid slot " + slot);
                }
                BlockPos position = station.getBlockPos();
                helper.getLevel().setBlock(position, Blocks.AIR.defaultBlockState(), 3);
                helper.assertValueEqual(dropped(helper, position, Items.OAK_LOG), 3, "Breaking drops the three saved logs once");
                helper.assertValueEqual(dropped(helper, position, Items.OAK_PLANKS), 0, "Breaking never drops an uncrafted preview");
                succeed(helper, player);
        }

        @GameTest(template = "empty")
        public static void shiftClickRoutesPlayerItemsToGridAndSideItemsToPlayer(GameTestHelper helper) {
                StationBlockEntity station = station(helper);
                BlockPos chestPosition = station.getBlockPos().east();
                helper.getLevel().setBlock(chestPosition, Blocks.CHEST.defaultBlockState(), 3);
                Container chest = (Container) helper.getLevel().getBlockEntity(chestPosition);
                chest.setItem(0, new ItemStack(Items.APPLE, 5));
                station.setItem(4, new ItemStack(Items.OAK_LOG, 60));
                ServerPlayer player = player(helper, station);
                player.getInventory().setItem(0, new ItemStack(Items.OAK_LOG, 8));
                CraftingStationMenu menu = menu(player, station);
                helper.assertValueEqual(menu.sideSlotCount(), 27, "The adjacent chest exposes all twenty-seven slots");
                menu.clicked(37, 0, ClickType.QUICK_MOVE, player);
                helper.assertValueEqual(station.getItem(4).getCount(), 64, "Shift-click fills a matching grid stack first");
                helper.assertValueEqual(count(station, Items.OAK_LOG), 68, "Remaining shifted logs occupy another grid slot without loss");
                helper.assertValueEqual(count(player.getInventory(), Items.OAK_LOG), 0, "All eight player logs moved into the grid");
                menu.clicked(CraftingStationMenu.SIDE_START, 0, ClickType.QUICK_MOVE, player);
                helper.assertTrue(chest.getItem(0).isEmpty(), "Side-inventory extraction updates the real chest");
                helper.assertValueEqual(count(player.getInventory(), Items.APPLE), 5, "Side-inventory shift-click returns items to the player");
                helper.assertValueEqual(count(station, Items.APPLE), 0, "Chest shift-click does not scatter unrelated items into the recipe");
                assertStackLimits(helper, station);
                succeed(helper, player);
        }

        @GameTest(template = "empty")
        public static void removedNeighborCannotBeAccessedThroughOldSlots(GameTestHelper helper) {
                StationBlockEntity station = station(helper);
                BlockPos chestPosition = station.getBlockPos().east();
                helper.getLevel().setBlock(chestPosition, Blocks.CHEST.defaultBlockState(), 3);
                Container chest = (Container) helper.getLevel().getBlockEntity(chestPosition);
                chest.setItem(0, new ItemStack(Items.APPLE, 5));
                ServerPlayer player = player(helper, station);
                CraftingStationMenu menu = menu(player, station);
                helper.assertValueEqual(menu.sideSlotCount(), 27, "A chest was attached when the menu opened");
                helper.getLevel().setBlock(chestPosition, Blocks.AIR.defaultBlockState(), 3);
                menu.clicked(CraftingStationMenu.SIDE_START, 0, ClickType.QUICK_MOVE, player);
                helper.assertTrue(!menu.stillValid(player), "Destroying the attached chest invalidates its open menu");
                helper.assertValueEqual(count(player.getInventory(), Items.APPLE), 0, "Old chest slots cannot duplicate dropped contents");
                helper.assertValueEqual(dropped(helper, chestPosition, Items.APPLE), 5, "The chest contents remain recoverable as drops");
                succeed(helper, player);
        }

        @GameTest(template = "empty")
        public static void blockedChestIsNotExposed(GameTestHelper helper) {
                StationBlockEntity station = station(helper);
                BlockPos chestPosition = station.getBlockPos().east();
                helper.getLevel().setBlock(chestPosition, Blocks.CHEST.defaultBlockState(), 3);
                helper.getLevel().setBlock(chestPosition.above(), Blocks.STONE.defaultBlockState(), 3);
                ServerPlayer player = player(helper, station);
                CraftingStationMenu menu = menu(player, station);
                helper.assertValueEqual(menu.sideSlotCount(), 0, "A chest blocked by a solid block is not accessible through the station");
                helper.assertTrue(menu.stillValid(player), "The crafting grid remains usable without an accessible neighbor");
                succeed(helper, player);
        }

        // Navigation
        @GameTest(template = "empty")
        public static void craftingAndToolStationsShareConnectionsAndKeepSeparateMenus(GameTestHelper helper) {
                StationBlockEntity station = station(helper);
                BlockPos toolPosition = station.getBlockPos().east();
                helper.getLevel().setBlock(toolPosition, TLBlocks.block(TLBlocks.Workstations.Type.TOOL_STATION).defaultBlockState(), 3);
                StationBlockEntity tools = (StationBlockEntity) helper.getLevel().getBlockEntity(toolPosition);
                ServerPlayer player = player(helper, station);
                var craftingMenu = station.createMenu(1, player.getInventory(), player);
                var toolMenu = tools.createMenu(2, player.getInventory(), player);
                helper.assertTrue(craftingMenu instanceof CraftingStationMenu, "Craft operations open a dedicated crafting menu");
                helper.assertTrue(toolMenu instanceof StationMenu, "Tool operations retain their existing menu");
                helper.assertValueEqual(((StationMenuAccess) craftingMenu).position(), station.getBlockPos(), "Crafting menu exposes its navigation position");
                helper.assertValueEqual(((StationMenuAccess) toolMenu).position(), toolPosition, "Tool menu exposes the same navigation contract");
                helper.assertTrue(StationConnections.find(helper.getLevel(), station.getBlockPos()).contains(toolPosition),
                                "Crafting tabs discover the adjacent tool station");
                helper.assertTrue(StationConnections.find(helper.getLevel(), toolPosition).contains(station.getBlockPos()),
                                "Tool tabs discover the adjacent crafting station");
                helper.assertValueEqual(((CraftingStationMenu) craftingMenu).sideSlotCount(), 0,
                                "A connected tool station is not also exposed as a storage inventory");
                helper.assertTrue(!station.shouldTriggerClientSideContainerClosingOnOpen(), "Switching station screens retains the mouse position");
                craftingMenu.removed(player);
                toolMenu.removed(player);
                succeed(helper, player);
        }

        // Setup
        private static StationBlockEntity station(GameTestHelper helper) {
                BlockPos relative = new BlockPos(2, 1, 2);
                helper.setBlock(relative, TLBlocks.block(TLBlocks.Workstations.Type.CRAFTING_STATION));
                return (StationBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(relative));
        }

        private static ServerPlayer player(GameTestHelper helper, StationBlockEntity station) {
                ServerPlayer player = helper.makeMockServerPlayerInLevel();
                player.getInventory().clearContent();
                player.setPos(station.getBlockPos().getX() + 0.5, station.getBlockPos().getY() + 1,
                                station.getBlockPos().getZ() + 0.5);
                return player;
        }

        private static CraftingStationMenu menu(ServerPlayer player, StationBlockEntity station) {
                CraftingStationMenu menu = new CraftingStationMenu(1, player.getInventory(), station);
                player.containerMenu = menu;
                return menu;
        }

        private static void fillInventory(ServerPlayer player, Item item) {
                for (int slot = 0; slot < 36; slot++) player.getInventory().setItem(slot, new ItemStack(item, 64));
        }

        private static int count(Container container, Item item) {
                int count = 0;
                for (int slot = 0; slot < container.getContainerSize(); slot++) {
                        if (container.getItem(slot).is(item)) count += container.getItem(slot).getCount();
                }
                return count;
        }

        private static int dropped(GameTestHelper helper, BlockPos position, Item item) {
                return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(position).inflate(2))
                                .stream().map(ItemEntity::getItem).filter(stack -> stack.is(item))
                                .mapToInt(ItemStack::getCount).sum();
        }

        private static void assertStackLimits(GameTestHelper helper, Container container) {
                for (int slot = 0; slot < container.getContainerSize(); slot++) {
                        ItemStack stack = container.getItem(slot);
                        helper.assertTrue(stack.isEmpty() || stack.getCount() <= stack.getMaxStackSize(),
                                        "No transfer produces an oversized stack in slot " + slot);
                }
        }

        private static void succeed(GameTestHelper helper, ServerPlayer... players) {
                for (ServerPlayer player : players) {
                        player.closeContainer();
                        helper.getLevel().getServer().getPlayerList().remove(player);
                }
                helper.succeed();
        }
}
