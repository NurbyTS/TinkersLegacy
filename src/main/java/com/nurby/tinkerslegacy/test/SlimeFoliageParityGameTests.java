package com.nurby.tinkerslegacy.test;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.registry.TLBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(TinkersLegacy.MODID)
@PrefixGameTestTemplate(false)
public final class SlimeFoliageParityGameTests {

        @GameTest(template = "empty", timeoutTicks = 100)
        public static void congealedTrunksSupportLeavesAndRemovalRestoresDecay(GameTestHelper helper) {
                var level = helper.getLevel();
                BlockPos trunk = helper.absolutePos(new BlockPos(1, 2, 1));
                BlockPos first = trunk.above();
                BlockPos second = first.above();
                var leaves = TLBlocks.block(TLBlocks.SlimyPlants.Type.BLUE_LEAVES).defaultBlockState()
                                .setValue(LeavesBlock.PERSISTENT, false);
                level.setBlockAndUpdate(first, leaves);
                level.setBlockAndUpdate(second, leaves);
                level.setBlockAndUpdate(trunk, TLBlocks.SlimyBlocks.congealed(
                                TLBlocks.SlimyBlocks.SlimeType.BLUE).get().defaultBlockState());
                helper.runAfterDelay(5, () -> {
                        helper.assertValueEqual(level.getBlockState(first).getValue(LeavesBlock.DISTANCE), 1,
                                        "Congealed slime supports nonpersistent slime leaves");
                        helper.assertValueEqual(level.getBlockState(second).getValue(LeavesBlock.DISTANCE), 2,
                                        "Leaf support propagates through neighboring leaves");
                        level.getBlockState(first).randomTick(level, first, level.random);
                        helper.assertTrue(level.getBlockState(first).is(leaves.getBlock()), "Supported leaves survive random decay ticks");
                        level.removeBlock(trunk, false);
                });
                helper.runAfterDelay(20, () -> {
                        if (!level.getBlockState(second).isAir()) {
                                helper.assertValueEqual(level.getBlockState(second).getValue(LeavesBlock.DISTANCE), 7,
                                                "Removing the trunk propagates the unsupported distance");
                                level.getBlockState(second).randomTick(level, second, level.random);
                        }
                        helper.assertTrue(level.getBlockState(second).isAir(), "Unsupported nonpersistent leaves decay");
                        level.setBlockAndUpdate(first, leaves);
                        level.setBlockAndUpdate(trunk, Blocks.OAK_LOG.defaultBlockState());
                });
                helper.runAfterDelay(25, () -> {
                        helper.assertValueEqual(level.getBlockState(first).getValue(LeavesBlock.DISTANCE), 1,
                                        "Ordinary logs retain modern vanilla support behavior");
                        helper.succeed();
                });
        }
}
