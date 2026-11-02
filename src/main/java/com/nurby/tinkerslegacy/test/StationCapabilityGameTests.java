package com.nurby.tinkerslegacy.test;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.block.entity.StationBlockEntity;
import com.nurby.tinkerslegacy.menu.CraftingStationMenu;
import com.nurby.tinkerslegacy.registry.TLBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.Map;
import java.util.IdentityHashMap;

@EventBusSubscriber(modid = TinkersLegacy.MODID)
@GameTestHolder(TinkersLegacy.MODID)
@PrefixGameTestTemplate(false)
public final class StationCapabilityGameTests {
        private static final Map<BlockEntity, ItemStackHandler> INVENTORIES = new IdentityHashMap<>();

        @SubscribeEvent
        public static void capabilities(RegisterCapabilitiesEvent event) {
                if (!net.neoforged.neoforge.gametest.GameTestHooks.isGametestEnabled()) return;
                // Inert outside these fixtures: ordinary command blocks never receive an inventory.
                event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BlockEntityType.COMMAND_BLOCK,
                                (block, side) -> side == null ? INVENTORIES.get(block) : null);
        }

        @GameTest(template = "empty")
        public static void craftingStationUsesUnsidedOnlyModdedInventory(GameTestHelper helper) {
                BlockPos pos = new BlockPos(2, 1, 2);
                helper.setBlock(pos, TLBlocks.block(TLBlocks.Workstations.Type.CRAFTING_STATION));
                StationBlockEntity station = (StationBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
                BlockPos neighbor = station.getBlockPos().east();
                helper.getLevel().setBlock(neighbor, Blocks.COMMAND_BLOCK.defaultBlockState(), 3);
                BlockEntity block = helper.getLevel().getBlockEntity(neighbor);
                ItemStackHandler items = new ItemStackHandler(3);
                items.setStackInSlot(0, new ItemStack(Items.APPLE, 5));
                net.minecraft.server.level.ServerPlayer player = null;
                try {
                        INVENTORIES.put(block, items);
                        helper.getLevel().invalidateCapabilities(neighbor);
                        player = helper.makeMockServerPlayerInLevel();
                        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(player.connection.getConnection());
                        player.setPos(station.getBlockPos().getX() + 0.5, station.getBlockPos().getY() + 1, station.getBlockPos().getZ() + 0.5);
                        CraftingStationMenu menu = new CraftingStationMenu(1, player.getInventory(), station);
                        player.containerMenu = menu;
                        helper.assertValueEqual(menu.sideSlotCount(), 3, "An unsided-only handler is discovered");
                        menu.clicked(CraftingStationMenu.SIDE_START, 0, ClickType.QUICK_MOVE, player);
                        helper.assertTrue(items.getStackInSlot(0).isEmpty(), "Shift-click writes to the actual unsided inventory");
                        helper.assertTrue(player.getInventory().contains(new ItemStack(Items.APPLE)), "Extracted items reach the player");
                        INVENTORIES.remove(block);
                        helper.getLevel().invalidateCapabilities(neighbor);
                        helper.assertTrue(!menu.stillValid(player), "Losing the unsided capability invalidates the menu");
                } finally {
                        INVENTORIES.remove(block);
                        helper.getLevel().invalidateCapabilities(neighbor);
                        if (player != null) {
                                try {
                                        player.closeContainer();
                                } finally {
                                        player.containerMenu = player.inventoryMenu;
                                        helper.getLevel().getServer().getPlayerList().remove(player);
                                        player.discard();
                                        if (player.connection.getConnection().channel() instanceof io.netty.channel.embedded.EmbeddedChannel channel) {
                                                channel.finishAndReleaseAll();
                                        }
                                }
                        }
                }
                helper.succeed();
        }
}
