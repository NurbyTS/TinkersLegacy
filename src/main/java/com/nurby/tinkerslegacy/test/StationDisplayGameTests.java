package com.nurby.tinkerslegacy.test;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.block.StationBlock;
import com.nurby.tinkerslegacy.library.station.StationDefinition;
import com.nurby.tinkerslegacy.library.station.StationLayout;
import com.nurby.tinkerslegacy.library.station.StationOperations;
import com.nurby.tinkerslegacy.registry.TLBlocks;
import com.nurby.tinkerslegacy.registry.TLStations;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(TinkersLegacy.MODID)
@PrefixGameTestTemplate(false)
public final class StationDisplayGameTests {

        // Original Layouts
        @GameTest(template = "empty")
        public static void tabletopSlotsFollowOriginalRolesAndSizes(GameTestHelper helper) {
                ItemStack flat = new ItemStack(Items.PAPER);
                check(helper, StationLayout.tabletop(TLStations.STENCIL_TABLE, 0, flat), 0, 1.03125F, 0, 0.8F, false);
                for (int slot = 0; slot < 9; slot++) {
                        check(helper, StationLayout.tabletop(TLStations.CRAFTING_STATION, slot, flat),
                                        new float[] { 0.1875F, 0, -0.1875F }[slot % 3], 1.00390625F,
                                        new float[] { 0.1875F, 0, -0.1875F }[slot / 3], 0.125F, false);
                }
                // Pattern first here, unlike the original input/input/pattern/remainder inventory.
                float[][] corners = { { 0.2125F, 0.2125F }, { 0.2125F, -0.2125F },
                                { -0.2125F, -0.2125F }, { -0.2125F, 0.2125F } };
                for (int slot = 0; slot < corners.length; slot++) {
                        check(helper, StationLayout.tabletop(TLStations.PART_BUILDER, slot, flat),
                                        corners[slot][0], 1.03125F, corners[slot][1], 0.375F, false);
                }
                float[][] pentagon = { { 0, 0 }, { 18F / 61, -20F / 61 }, { 22F / 61, 5F / 61 },
                                { 0, 23F / 61 }, { -22F / 61, 5F / 61 }, { -18F / 61, -20F / 61 } };
                for (var station : List.of(TLStations.TOOL_STATION, TLStations.TOOL_FORGE)) {
                        for (int slot = 0; slot < pentagon.length; slot++) {
                                check(helper, StationLayout.tabletop(station, slot, flat),
                                                pentagon[slot][0], 1.03125F, pentagon[slot][1],
                                                slot == 0 ? 0.4875F : 0.375F, false);
                        }
                }
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void blocksRestOnSurfacePanesStayFlatAndExtraFamiliesFit(GameTestHelper helper) {
                ItemStack cube = new ItemStack(Items.STONE);
                for (var station : List.of(TLStations.STENCIL_TABLE, TLStations.PART_BUILDER,
                                TLStations.CRAFTING_STATION, TLStations.TOOL_STATION, TLStations.TOOL_FORGE)) {
                        for (int slot = 0; slot < station.inputSlots(); slot++) {
                                var block = StationLayout.tabletop(station, slot, cube);
                                near(helper, block.y() - block.scale() / 2, 1, "An upright cube's lower face rests on the tabletop");
                                helper.assertTrue(block.upright(), "Ordinary block items remain upright");
                                var pane = StationLayout.tabletop(station, slot, new ItemStack(Items.GLASS_PANE));
                                helper.assertTrue(!pane.upright(), "Panes lie flat like original panes");
                                near(helper, pane.scale(), block.scale(), "Panes keep block-item size without the cube lift");
                        }
                }
                near(helper, StationLayout.tabletop(TLStations.PART_BUILDER, 1, cube).scale(), 0.17578125F,
                                "Part Builder cube scale is raw, without an additional FIXED half-scale");
                var extension = new StationDefinition(ResourceLocation.fromNamespaceAndPath("test", "workbench"),
                                ResourceLocation.fromNamespaceAndPath("test", "other"), 3, 18, List.of(StationOperations.CRAFT));
                for (int slot = 0; slot < extension.inputSlots(); slot++) {
                        var display = StationLayout.tabletop(extension, slot, cube);
                        helper.assertTrue(Math.abs(display.x()) + display.scale() / 2 <= 0.5F
                                        && Math.abs(display.z()) + display.scale() / 2 <= 0.5F,
                                        "An extension family's fallback fits its inventory on the table");
                }
                helper.succeed();
        }

        // Block State
        @GameTest(template = "empty")
        public static void stationFacingFollowsPlacementAndLoadsOldStates(GameTestHelper helper) {
                var player = helper.makeMockPlayer(GameType.SURVIVAL);
                for (var type : TLBlocks.Workstations.Type.values()) {
                        var block = TLBlocks.block(type);
                        if (!(block instanceof StationBlock)) continue;
                        CompoundTag old = new CompoundTag();
                        old.putString("Name", BuiltInRegistries.BLOCK.getKey(block).toString());
                        BlockState restored = BlockState.CODEC.parse(NbtOps.INSTANCE, old).getOrThrow();
                        helper.assertValueEqual(restored.getValue(StationBlock.FACING), Direction.SOUTH,
                                        "Old station states without facing retain a deterministic default");
                        for (Direction facing : Direction.Plane.HORIZONTAL) {
                                player.setYRot(facing.toYRot());
                                BlockPos pos = helper.absolutePos(BlockPos.ZERO);
                                var context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND,
                                                new ItemStack(block), new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
                                var placed = block.getStateForPlacement(context);
                                helper.assertValueEqual(placed.getValue(StationBlock.FACING), facing.getOpposite(),
                                                "Station faces back toward the player who placed it");
                                helper.assertValueEqual(placed.rotate(Rotation.CLOCKWISE_90).getValue(StationBlock.FACING),
                                                facing.getOpposite().getClockWise(), "Structure rotation updates the table direction");
                                helper.assertValueEqual(placed.mirror(Mirror.LEFT_RIGHT).getValue(StationBlock.FACING),
                                                Mirror.LEFT_RIGHT.mirror(facing.getOpposite()), "Mirroring updates the display direction");
                        }
                }
                helper.succeed();
        }

        // Assertions
        private static void check(GameTestHelper helper, StationLayout.Display actual,
                        float x, float y, float z, float scale, boolean upright) {
                near(helper, actual.x(), x, "Original X offset");
                near(helper, actual.y(), y, "Original model center height");
                near(helper, actual.z(), z, "Original Z offset");
                near(helper, actual.scale(), scale, "Original raw-model scale");
                helper.assertValueEqual(actual.upright(), upright, "Original block/pane orientation");
        }

        private static void near(GameTestHelper helper, float actual, float expected, String message) {
                helper.assertTrue(Math.abs(actual - expected) < 0.00001F, message + ": " + actual + " != " + expected);
        }
}
