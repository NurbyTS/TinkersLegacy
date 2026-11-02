package com.nurby.tinkerslegacy.test;

import com.mojang.authlib.GameProfile;
import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.library.material.layer.MaterialLayer;
import com.nurby.tinkerslegacy.library.station.StationOperations;
import com.nurby.tinkerslegacy.library.tool.ToolAreaActions;
import com.nurby.tinkerslegacy.library.tool.ToolState;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLTools;

import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static com.nurby.tinkerslegacy.test.ToolAssemblyGameTests.id;
import static com.nurby.tinkerslegacy.test.ToolAssemblyGameTests.tool;

@GameTestHolder(TinkersLegacy.MODID)
@PrefixGameTestTemplate(false)
public final class AdvancedToolGameTests {
        // Original Stats
        @GameTest(template = "empty")
        public static void advancedDurabilityUsesAveragesAndOriginalMultipliers(GameTestHelper helper) {
                helper.assertValueEqual(tool(TLTools.Type.HAMMER, id("iron")).getMaxDamage(), 582, "Hammer durability");
                helper.assertValueEqual(tool(TLTools.Type.EXCAVATOR, id("iron")).getMaxDamage(), 483, "Excavator truncates final multiplier");
                helper.assertValueEqual(tool(TLTools.Type.LUMBERAXE, id("iron")).getMaxDamage(), 552, "Lumberaxe durability");
                helper.assertValueEqual(tool(TLTools.Type.SCYTHE, id("iron")).getMaxDamage(), 607, "Scythe averages two handles");
                helper.assertValueEqual(tool(TLTools.Type.CLEAVER, id("iron")).getMaxDamage(), 552, "Cleaver durability");
                helper.assertValueEqual(tool(TLTools.Type.FRYPAN, id("iron")).getMaxDamage(), 233, "Frying pan has two physical parts");
                helper.assertValueEqual(tool(TLTools.Type.BATTLESIGN, id("iron")).getMaxDamage(), 233, "Battlesign has two physical parts");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void hammerWeightsHeadTwiceAndUsesItsHarvestLevel(GameTestHelper helper) {
                var definition = TLTools.tool(TLTools.Type.HAMMER);
                List<MaterialLayer> layers = new ArrayList<>();
                String[] materials = { "wood", "wood", "iron", "stone" };
                for (int index = 0; index < materials.length; index++) {
                        layers.add(new MaterialLayer(definition.parts().get(index).statType(), id(materials[index])));
                }
                var stats = definition.calculateStats(layers);
                helper.assertValueEqual(stats.get("durability").intValue(), 307, "Head is weighted twice before averaging");
                helper.assertValueEqual(stats.get("attack_damage").doubleValue(), 2.75, "Weighted head damage");
                helper.assertValueEqual(stats.get("harvest_level").intValue(), 0, "Plates cannot raise hammer harvest level");
                helper.assertTrue(Math.abs(stats.get("mining_speed").doubleValue() - 3.5) < 0.0001, "Weighted raw head speed before tool multipliers");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void advancedToolsRequireForgeOnlyForAssembly(GameTestHelper helper) {
                for (var type : List.of(TLTools.Type.HAMMER, TLTools.Type.EXCAVATOR, TLTools.Type.LUMBERAXE,
                                TLTools.Type.SCYTHE, TLTools.Type.CLEAVER)) {
                        var definition = TLTools.tool(type);
                        helper.assertValueEqual(definition.stationRequirement(StationOperations.ASSEMBLE).tier(), 2, "Forge assembly");
                        helper.assertValueEqual(definition.stationRequirement(StationOperations.REPAIR).tier(), 1, "Ordinary station repair");
                        helper.assertValueEqual(definition.stationRequirement(StationOperations.MODIFY).tier(), 1, "Ordinary station modifications");
                }
                helper.assertValueEqual(TLTools.tool(TLTools.Type.SCYTHE).repairParts(), List.of(1, 2), "Scythe accepts blade and binding repairs");
                helper.assertValueEqual(TLTools.tool(TLTools.Type.HAMMER).repairModifierForPart(2), 1.5F, "Hammer plate repair scaling");
                helper.succeed();
        }

        // Area Mining
        @GameTest(template = "empty")
        public static void areaGeometryFollowsTheHitFaceWithoutDuplicatingOrigin(GameTestHelper helper) {
                BlockPos origin = new BlockPos(5, 5, 5);
                for (Direction face : Direction.values()) {
                        List<BlockPos> flat = ToolAreaActions.box(origin, face, 1);
                        List<BlockPos> deep = ToolAreaActions.box(origin, face, 3);
                        helper.assertValueEqual(flat.size(), 8, "Flat hammer area");
                        helper.assertValueEqual(deep.size(), 26, "Deep lumberaxe area");
                        helper.assertTrue(!deep.contains(origin), "Origin is consumed by the initial mining action");
                        helper.assertValueEqual(deep.stream().distinct().count(), 26L, "Area coordinates are unique");
                        helper.assertTrue(deep.contains(origin.relative(face.getOpposite(), 2)), "Depth extends into the hit face");
                }
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void hammerAreaRespectsProtectedBlocksAndAwardsOnlySuccessfulXp(GameTestHelper helper) {
                try (TestPlayer test = player(helper)) {
                        ServerPlayer player = test.player();
                        ItemStack stack = tool(TLTools.Type.HAMMER, id("iron"));
                        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                        BlockPos origin = fillFloor(helper);
                        BlockPos protectedBlock = origin.west();
                        Consumer<BlockEvent.BreakEvent> cancel = event -> {
                                if (event.getPlayer() == player && event.getPos().equals(protectedBlock)) event.setCanceled(true);
                        };
                        NeoForge.EVENT_BUS.addListener(cancel);
                        try {
                                player.gameMode.destroyBlock(origin);
                        } finally {
                                NeoForge.EVENT_BUS.unregister(cancel);
                        }
                        helper.assertTrue(helper.getLevel().getBlockState(protectedBlock).is(Blocks.STONE), "Protection survives area mining");
                        long removed = BlockPos.betweenClosedStream(origin.offset(-1, 0, -1), origin.offset(1, 0, 1))
                                        .filter(pos -> helper.getLevel().getBlockState(pos).isAir()).count();
                        helper.assertValueEqual(removed, 8L, "Exactly the permitted 3x3 cells are mined");
                        helper.assertValueEqual(ToolState.damage(stack), 8, "One durability per successful block");
                        helper.assertValueEqual(stack.get(TLDataComponents.PROGRESS).xp(), 8L, "One XP per successful block");
                }
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void canceledOriginCannotStartAreaMining(GameTestHelper helper) {
                try (TestPlayer test = player(helper)) {
                        ServerPlayer player = test.player();
                        ItemStack stack = tool(TLTools.Type.HAMMER, id("iron"));
                        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                        BlockPos origin = fillFloor(helper);
                        Consumer<BlockEvent.BreakEvent> cancel = event -> {
                                if (event.getPlayer() == player && event.getPos().equals(origin)) event.setCanceled(true);
                        };
                        NeoForge.EVENT_BUS.addListener(cancel);
                        try {
                                player.gameMode.destroyBlock(origin);
                        } finally {
                                NeoForge.EVENT_BUS.unregister(cancel);
                        }
                        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-1, 0, -1), origin.offset(1, 0, 1))) {
                                helper.assertTrue(helper.getLevel().getBlockState(pos).is(Blocks.STONE), "Canceled center leaves every neighbor intact");
                        }
                        helper.assertValueEqual(ToolState.damage(stack), 0, "Canceled mining has no wear");
                        helper.assertValueEqual(stack.get(TLDataComponents.PROGRESS).xp(), 0L, "Canceled mining has no XP");
                }
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void finalDurabilityStopsAreaExpansion(GameTestHelper helper) {
                try (TestPlayer test = player(helper)) {
                        ItemStack stack = tool(TLTools.Type.HAMMER, id("iron"));
                        stack.setDamageValue(stack.getMaxDamage() - 1);
                        test.player().setItemInHand(InteractionHand.MAIN_HAND, stack);
                        BlockPos origin = fillFloor(helper);
                        test.player().gameMode.destroyBlock(origin);
                        helper.assertTrue(ToolState.broken(stack), "Center consumes the final durability point");
                        long removed = BlockPos.betweenClosedStream(origin.offset(-1, 0, -1), origin.offset(1, 0, 1))
                                        .filter(pos -> helper.getLevel().getBlockState(pos).isAir()).count();
                        helper.assertValueEqual(removed, 1L, "Broken tools cannot continue mining the area");
                }
                helper.succeed();
        }

        // Paths
        @GameTest(template = "empty")
        public static void excavatorMakesProtectedAwarePathsAndAwardsEachSuccessfulCell(GameTestHelper helper) {
                try (TestPlayer test = player(helper)) {
                        ServerPlayer player = test.player();
                        ItemStack stack = tool(TLTools.Type.EXCAVATOR, id("iron"));
                        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                        BlockPos origin = fillFloor(helper);
                        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-1, 0, -1), origin.offset(1, 0, 1))) {
                                helper.getLevel().setBlockAndUpdate(pos, Blocks.GRASS_BLOCK.defaultBlockState());
                        }
                        BlockPos protectedBlock = origin.west();
                        Consumer<PlayerInteractEvent.RightClickBlock> cancel = event -> {
                                if (event.getEntity() == player && event.getPos().equals(protectedBlock)) event.setCanceled(true);
                        };
                        NeoForge.EVENT_BUS.addListener(cancel);
                        try {
                                player.gameMode.useItemOn(player, player.level(), stack, InteractionHand.MAIN_HAND,
                                                new BlockHitResult(Vec3.atCenterOf(origin), Direction.UP, origin, false));
                        } finally {
                                NeoForge.EVENT_BUS.unregister(cancel);
                        }
                        helper.assertTrue(helper.getLevel().getBlockState(protectedBlock).is(Blocks.GRASS_BLOCK), "Protection prevents path conversion");
                        long paths = BlockPos.betweenClosedStream(origin.offset(-1, 0, -1), origin.offset(1, 0, 1))
                                        .filter(pos -> helper.getLevel().getBlockState(pos).is(Blocks.DIRT_PATH)).count();
                        helper.assertValueEqual(paths, 8L, "Excavator creates the permitted cells of a 3x3 path");
                        helper.assertValueEqual(ToolState.damage(stack), 8, "One durability per created path cell");
                        helper.assertValueEqual(stack.get(TLDataComponents.PROGRESS).xp(), 8L, "One XP per created path cell");
                }
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void cleaverUsesSwordMiningPenaltyAndCobwebBonus(GameTestHelper helper) {
                ItemStack stack = tool(TLTools.Type.CLEAVER, id("iron"));
                helper.assertTrue(Math.abs(stack.getDestroySpeed(Blocks.COBWEB.defaultBlockState()) - 22.5F) < 0.0001F,
                                "Cleaver applies the original half-speed sword penalty and 7.5x cobweb multiplier");
                helper.succeed();
        }

        // Combat
        @GameTest(template = "empty")
        public static void canceledFryingPanChargeCannotKnockBackOrSpendDurability(GameTestHelper helper) {
                try (TestPlayer test = player(helper)) {
                        ServerPlayer player = test.player();
                        player.setXRot(0);
                        ItemStack pan = tool(TLTools.Type.FRYPAN, id("iron"));
                        player.setItemInHand(InteractionHand.MAIN_HAND, pan);
                        LivingEntity target = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 2, 4, 4);
                        target.setPos(player.position().add(0, 0, 2));
                        float health = target.getHealth();
                        Vec3 motion = target.getDeltaMovement();
                        Consumer<AttackEntityEvent> cancel = event -> {
                                if (event.getEntity() == player) event.setCanceled(true);
                        };
                        NeoForge.EVENT_BUS.addListener(cancel);
                        try {
                                TLTools.tool(TLTools.Type.FRYPAN).releaseUsing(pan, player.level(), player, 70);
                        } finally {
                                NeoForge.EVENT_BUS.unregister(cancel);
                        }
                        helper.assertValueEqual(target.getHealth(), health, "Canceled charge does not damage");
                        helper.assertValueEqual(target.getDeltaMovement(), motion, "Canceled charge does not push");
                        helper.assertValueEqual(ToolState.damage(pan), 0, "Canceled charge does not wear the pan");
                        helper.assertTrue(!target.isOnFire(), "Temporary charge fire is restored");
                        target.discard();
                }
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void offhandFryingPanChargeUsesPanAndRestoresBothHands(GameTestHelper helper) {
                try (TestPlayer test = player(helper)) {
                        ServerPlayer player = test.player();
                        player.setXRot(0);
                        ItemStack pan = tool(TLTools.Type.FRYPAN, id("iron"));
                        ItemStack pickaxe = tool(TLTools.Type.PICKAXE, id("wood"));
                        player.setItemInHand(InteractionHand.MAIN_HAND, pickaxe);
                        player.setItemInHand(InteractionHand.OFF_HAND, pan);
                        LivingEntity target = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 2, 4, 4);
                        target.setPos(player.position().add(0, 0, 2));
                        float health = target.getHealth();
                        TLTools.tool(TLTools.Type.FRYPAN).releaseUsing(pan, player.level(), player, 70);
                        helper.assertTrue(target.getHealth() < health, "Charged pan hits the target");
                        helper.assertTrue(target.getDeltaMovement().lengthSqr() > 0, "Successful charge launches the target");
                        helper.assertTrue(player.getMainHandItem() == pickaxe, "Main-hand item is restored");
                        helper.assertTrue(player.getOffhandItem() == pan, "Charged pan remains in the offhand");
                        helper.assertValueEqual(ToolState.damage(pickaxe), 0, "Unrelated main-hand item is not worn");
                        helper.assertTrue(ToolState.damage(pan) > 0, "Charged pan pays melee wear");
                        target.discard();
                }
                helper.succeed();
        }

        private static BlockPos fillFloor(GameTestHelper helper) {
                BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
                for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-1, 0, -1), origin.offset(1, 0, 1))) {
                        helper.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
                }
                return origin;
        }

        private static TestPlayer player(GameTestHelper helper) {
                CommonListenerCookie cookie = CommonListenerCookie.createInitial(
                                new GameProfile(UUID.randomUUID(), "advanced-tool-test"), false);
                ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                                cookie.gameProfile(), cookie.clientInformation());
                Connection connection = new Connection(PacketFlow.SERVERBOUND);
                EmbeddedChannel channel = new EmbeddedChannel(connection);
                helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
                player.setGameMode(GameType.SURVIVAL);
                BlockPos pos = helper.absolutePos(new BlockPos(2, 4, 2));
                player.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 90);
                return new TestPlayer(player, channel);
        }

        private record TestPlayer(ServerPlayer player, EmbeddedChannel channel) implements AutoCloseable {
                @Override
                public void close() {
                        player.server.getPlayerList().remove(player);
                        player.discard();
                        channel.finishAndReleaseAll();
                }
        }
}
