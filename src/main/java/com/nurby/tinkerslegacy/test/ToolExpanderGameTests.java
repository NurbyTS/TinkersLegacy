package com.nurby.tinkerslegacy.test;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.library.tool.ToolModifiers;
import com.nurby.tinkerslegacy.library.tool.ToolState;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.function.Consumer;

import static com.nurby.tinkerslegacy.test.ToolAssemblyGameTests.id;
import static com.nurby.tinkerslegacy.test.ToolAssemblyGameTests.tool;

@GameTestHolder(TinkersLegacy.MODID)
@PrefixGameTestTemplate(false)
public final class ToolExpanderGameTests {
        private static ItemStack expanded(TLTools.Type type) {
                ItemStack stack = tool(type, id("iron"));
                ToolModifiers.setPoints(stack, ToolModifiers.HARVEST_WIDTH, 1);
                ToolModifiers.setPoints(stack, ToolModifiers.HARVEST_HEIGHT, 1);
                return stack;
        }

        private static BlockHitResult hit(BlockPos pos) {
                return new BlockHitResult(Vec3.atLowerCornerOf(pos).add(0.25, 1, 0.25), Direction.UP, pos, false);
        }

        // Paths
        @GameTest(template = "empty")
        public static void expandedShovelPathsHonorCanceledCellsAndAwardOnlySuccessfulXp(GameTestHelper helper) {
                var player = helper.makeMockServerPlayerInLevel();
                player.setGameMode(GameType.SURVIVAL);
                player.setYRot(180);
                ItemStack stack = expanded(TLTools.Type.SHOVEL);
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
                for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                        helper.getLevel().setBlockAndUpdate(origin.offset(x, 0, z), Blocks.DIRT.defaultBlockState());
                        helper.getLevel().setBlockAndUpdate(origin.offset(x, 1, z), Blocks.AIR.defaultBlockState());
                }
                BlockPos canceled = origin.west();
                Consumer<PlayerInteractEvent.RightClickBlock> listener = event -> {
                        if (event.getEntity() == player && event.getPos().equals(canceled)) event.setCanceled(true);
                };
                NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, PlayerInteractEvent.RightClickBlock.class, listener);
                try {
                        player.gameMode.useItemOn(player, helper.getLevel(), stack, InteractionHand.MAIN_HAND, hit(origin));
                        int paths = 0;
                        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                                if (helper.getLevel().getBlockState(origin.offset(x, 0, z)).is(Blocks.DIRT_PATH)) paths++;
                        }
                        helper.assertValueEqual(paths, 3, "Two expanders target four cells, with one protected cell skipped");
                        helper.assertTrue(helper.getLevel().getBlockState(canceled).is(Blocks.DIRT), "Canceled extra path remains dirt");
                        helper.assertValueEqual(ToolState.damage(stack), 3, "Only successful paths cost durability");
                        helper.assertValueEqual(stack.get(TLDataComponents.PROGRESS).xp(), 3L, "Each successful path earns exactly one XP");
                } finally {
                        NeoForge.EVENT_BUS.unregister(listener);
                        helper.getLevel().getServer().getPlayerList().remove(player);
                }
                helper.succeed();
        }

        // Tilling
        @GameTest(template = "empty")
        public static void expandedMattockTillsNineCellsWithoutRepeatRewards(GameTestHelper helper) {
                var player = helper.makeMockServerPlayerInLevel();
                player.setGameMode(GameType.SURVIVAL);
                ItemStack stack = expanded(TLTools.Type.MATTOCK);
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
                for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                        helper.getLevel().setBlockAndUpdate(origin.offset(x, 0, z), Blocks.DIRT.defaultBlockState());
                        helper.getLevel().setBlockAndUpdate(origin.offset(x, 1, z), Blocks.AIR.defaultBlockState());
                }
                player.gameMode.useItemOn(player, helper.getLevel(), stack, InteractionHand.MAIN_HAND, hit(origin));
                for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                        helper.assertTrue(helper.getLevel().getBlockState(origin.offset(x, 0, z)).is(Blocks.FARMLAND), "The entire three-by-three is tilled");
                }
                helper.assertValueEqual(ToolState.damage(stack), 9, "Nine successful cells cost nine durability");
                helper.assertValueEqual(stack.get(TLDataComponents.PROGRESS).xp(), 9L, "Nested uses do not duplicate XP");
                player.gameMode.useItemOn(player, helper.getLevel(), stack, InteractionHand.MAIN_HAND, hit(origin));
                helper.assertValueEqual(ToolState.damage(stack), 9, "Already-tilled soil costs nothing");
                helper.assertValueEqual(stack.get(TLDataComponents.PROGRESS).xp(), 9L, "Failed repetition earns no XP");
                helper.getLevel().getServer().getPlayerList().remove(player);
                helper.succeed();
        }

        // Crops
        @GameTest(template = "empty")
        public static void expandedKamaHarvestHonorsBreakCancellationAndLeavesSoilAlone(GameTestHelper helper) {
                var player = helper.makeMockServerPlayerInLevel();
                player.setGameMode(GameType.SURVIVAL);
                player.setYRot(180);
                ItemStack stack = expanded(TLTools.Type.KAMA);
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                BlockPos origin = helper.absolutePos(new BlockPos(2, 2, 2));
                CropBlock crop = (CropBlock) Blocks.CARROTS;
                for (int x = -1; x <= 0; x++) for (int z = -1; z <= 0; z++) {
                        helper.getLevel().setBlockAndUpdate(origin.offset(x, -1, z), Blocks.FARMLAND.defaultBlockState());
                        helper.getLevel().setBlockAndUpdate(origin.offset(x, 0, z), crop.getStateForAge(7));
                }
                BlockPos canceled = origin.west();
                Consumer<BlockEvent.BreakEvent> listener = event -> {
                        if (event.getPlayer() == player && event.getPos().equals(canceled)) event.setCanceled(true);
                };
                NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, BlockEvent.BreakEvent.class, listener);
                try {
                        player.gameMode.useItemOn(player, helper.getLevel(), stack, InteractionHand.MAIN_HAND, hit(origin));
                        helper.assertValueEqual(crop.getAge(helper.getLevel().getBlockState(canceled)), 7, "Protected crop remains mature");
                        helper.assertValueEqual(crop.getAge(helper.getLevel().getBlockState(origin.north())), 0, "Neighbor crop is replanted");
                        helper.assertValueEqual(ToolState.damage(stack), 3, "Only three successful harvests cost durability");
                        helper.assertValueEqual(stack.get(TLDataComponents.PROGRESS).xp(), 3L, "Each actual crop harvest earns one XP");
                } finally {
                        NeoForge.EVENT_BUS.unregister(listener);
                        helper.getLevel().getServer().getPlayerList().remove(player);
                }
                helper.succeed();
        }

        // Shearing
        @GameTest(template = "empty")
        public static void expandedKamaShearingSkipsCanceledAnimals(GameTestHelper helper) {
                var player = helper.makeMockServerPlayerInLevel();
                player.setGameMode(GameType.SURVIVAL);
                ItemStack stack = expanded(TLTools.Type.KAMA);
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                var first = helper.spawn(EntityType.SHEEP, new BlockPos(1, 1, 1));
                var second = helper.spawn(EntityType.SHEEP, new BlockPos(2, 1, 1));
                var canceled = helper.spawn(EntityType.SHEEP, new BlockPos(1, 1, 2));
                Consumer<PlayerInteractEvent.EntityInteract> listener = event -> {
                        if (event.getEntity() == player && event.getTarget() == canceled) event.setCanceled(true);
                };
                NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, PlayerInteractEvent.EntityInteract.class, listener);
                try {
                        stack.interactLivingEntity(player, first, InteractionHand.MAIN_HAND);
                        helper.assertTrue(first.isSheared() && second.isSheared(), "The selected and neighboring sheep are sheared");
                        helper.assertTrue(!canceled.isSheared(), "Canceled neighboring sheep keeps its wool");
                        helper.assertValueEqual(ToolState.damage(stack), 2, "Only the two sheared animals cost durability");
                } finally {
                        NeoForge.EVENT_BUS.unregister(listener);
                        helper.getLevel().getServer().getPlayerList().remove(player);
                }
                helper.succeed();
        }
}
