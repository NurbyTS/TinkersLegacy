package com.nurby.tinkerslegacy.test;

import com.nurby.tinkerslegacy.TLConfig;
import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.block.entity.StationBlockEntity;
import com.nurby.tinkerslegacy.library.station.MaterialInputs;
import com.nurby.tinkerslegacy.menu.StationMenu;
import com.nurby.tinkerslegacy.registry.TLBlocks;
import com.nurby.tinkerslegacy.registry.ToolParts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.nurby.tinkerslegacy.test.ToolAssemblyGameTests.id;
import static com.nurby.tinkerslegacy.test.ToolAssemblyGameTests.part;

@GameTestHolder(TinkersLegacy.MODID)
@PrefixGameTestTemplate(false)
public final class StationPolicyGameTests {
        // Material Policy
        @GameTest(template = "empty")
        public static void explicitMaterialPolicyOverridesLocalConfiguration(GameTestHelper helper) {
                boolean fallback = TLConfig.PART_BUILDER_METAL_FALLBACK.get();
                boolean override = TLConfig.CRAFT_CASTABLE_MATERIALS.get();
                try {
                        TLConfig.PART_BUILDER_METAL_FALLBACK.set(true);
                        TLConfig.CRAFT_CASTABLE_MATERIALS.set(true);
                        helper.assertTrue(!MaterialInputs.match(new ItemStack(Items.IRON_INGOT), false).craftable(),
                                        "A synchronized false policy rejects metal despite permissive local configuration");
                        helper.assertTrue(!MaterialInputs.match(MaterialInputs.shard(id("iron"), 1), false).craftable(),
                                        "The same policy applies to material shards");
                        TLConfig.PART_BUILDER_METAL_FALLBACK.set(false);
                        TLConfig.CRAFT_CASTABLE_MATERIALS.set(false);
                        helper.assertTrue(MaterialInputs.match(new ItemStack(Items.IRON_INGOT), true).craftable(),
                                        "A synchronized true policy permits metal despite restrictive local configuration");
                        helper.assertTrue(MaterialInputs.match(new ItemStack(Items.OAK_PLANKS), false).craftable(),
                                        "Ordinary craftable materials never depend on the metal fallback");
                        helper.assertTrue(!MaterialInputs.match(new ItemStack(Items.DIAMOND), true).craftable(),
                                        "Legacy non-castable materials remain unavailable");
                        helper.assertTrue(!MaterialInputs.match(part(ToolParts.Type.SHARPENING_KIT, id("iron")), true).craftable(),
                                        "Repair kits never become Part Builder ingredients");
                } finally {
                        TLConfig.PART_BUILDER_METAL_FALLBACK.set(fallback);
                        TLConfig.CRAFT_CASTABLE_MATERIALS.set(override);
                }
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void menuSynchronizesEffectivePolicyAndRoutesOnlyEligibleMaterials(GameTestHelper helper) {
                boolean fallback = TLConfig.PART_BUILDER_METAL_FALLBACK.get();
                boolean override = TLConfig.CRAFT_CASTABLE_MATERIALS.get();
                try {
                        TLConfig.PART_BUILDER_METAL_FALLBACK.set(false);
                        TLConfig.CRAFT_CASTABLE_MATERIALS.set(false);
                        BlockPos pos = new BlockPos(2, 1, 2);
                        helper.setBlock(pos, TLBlocks.block(TLBlocks.Workstations.Type.PART_BUILDER));
                        var station = (StationBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
                        var player = helper.makeMockPlayer(GameType.SURVIVAL);
                        player.setPos(station.getBlockPos().getCenter());
                        var menu = new StationMenu(1, player.getInventory(), station);
                        int[] received = { -1 };
                        menu.setSynchronizer(new ContainerSynchronizer() {
                                @Override
                                public void sendInitialData(AbstractContainerMenu container, NonNullList<ItemStack> items,
                                                ItemStack carried, int[] data) {
                                        received[0] = data[0];
                                }

                                @Override
                                public void sendDataChange(AbstractContainerMenu container, int id, int value) {
                                        if (id == 0) received[0] = value;
                                }

                                @Override
                                public void sendSlotChange(AbstractContainerMenu container, int index, ItemStack stack) {
                                }

                                @Override
                                public void sendCarriedChange(AbstractContainerMenu container, ItemStack stack) {
                                }
                        });
                        helper.assertValueEqual(received[0], 0, "Opening the menu synchronizes the disabled policy");
                        helper.assertTrue(!menu.getSlot(1).mayPlace(new ItemStack(Items.IRON_INGOT)),
                                        "The authoritative menu rejects metal while both settings are disabled");
                        TLConfig.CRAFT_CASTABLE_MATERIALS.set(true);
                        menu.broadcastChanges();
                        helper.assertValueEqual(received[0], 1, "The existing override synchronizes its effective enabled policy");
                        player.getInventory().setItem(0, new ItemStack(Items.IRON_INGOT, 3));
                        menu.quickMoveStack(player, menu.resultSlot() + 28);
                        helper.assertValueEqual(station.getItem(1).getCount(), 3, "Shift-clicking routes permitted metal to a material input");
                        helper.assertTrue(station.getItem(1).is(Items.IRON_INGOT), "The transferred material retains its identity");
                        helper.assertTrue(!menu.getSlot(2).mayPlace(new ItemStack(Items.OAK_PLANKS)),
                                        "Different materials cannot mix even when metal fallback is enabled");
                        helper.assertTrue(!menu.getSlot(2).mayPlace(part(ToolParts.Type.SHARPENING_KIT, id("iron"))),
                                        "Matching-material repair kits remain excluded");
                        TLConfig.CRAFT_CASTABLE_MATERIALS.set(false);
                        menu.broadcastChanges();
                        helper.assertValueEqual(received[0], 0, "Configuration changes update an already-open menu");
                        helper.assertValueEqual(station.getItem(1).getCount(), 3, "Changing policy never deletes existing inputs");
                } finally {
                        TLConfig.PART_BUILDER_METAL_FALLBACK.set(fallback);
                        TLConfig.CRAFT_CASTABLE_MATERIALS.set(override);
                }
                helper.succeed();
        }
}
