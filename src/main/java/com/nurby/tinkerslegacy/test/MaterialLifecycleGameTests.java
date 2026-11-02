package com.nurby.tinkerslegacy.test;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.library.material.layer.MaterialLayer;
import com.nurby.tinkerslegacy.library.station.MaterialInputs;
import com.nurby.tinkerslegacy.library.station.StationOperations;
import com.nurby.tinkerslegacy.library.station.StationRecipes;
import com.nurby.tinkerslegacy.library.tool.ToolAssembly;
import com.nurby.tinkerslegacy.library.tool.ToolModifierEntry;
import com.nurby.tinkerslegacy.library.tool.ToolModifiers;
import com.nurby.tinkerslegacy.library.tool.ToolProgress;
import com.nurby.tinkerslegacy.library.tool.ToolState;
import com.nurby.tinkerslegacy.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import static com.nurby.tinkerslegacy.test.ToolAssemblyGameTests.*;

@GameTestHolder(TinkersLegacy.MODID)
@PrefixGameTestTemplate(false)
public final class MaterialLifecycleGameTests {

        // Modifier Allowance
        @GameTest(template = "empty")
        public static void paperRolesSupplyDistinctSlotsWithoutRepeatedPartBonuses(GameTestHelper helper) {
                ItemStack head = pickaxe("paper", "wood", "wood");
                ItemStack binding = pickaxe("wood", "paper", "paper");
                ItemStack both = pickaxe("paper", "paper", "paper");
                helper.assertValueEqual(ToolState.modifierCapacity(head), 4, "A paper head grants one Writable II slot");
                helper.assertValueEqual(ToolState.modifierCapacity(binding), 4, "Repeated Writable I parts grant one slot total");
                helper.assertValueEqual(ToolState.modifierCapacity(both), 5, "Writable I and II combine to two material slots");
                both.set(TLDataComponents.PROGRESS, new ToolProgress(2, 31, 2));
                for (int i = 0; i < 4; i++) ((DynamicTool) both.getItem()).calculateStats(both);
                helper.assertValueEqual(ToolState.freeModifiers(both), 7, "Rebuilds preserve earned slots without accumulating trait slots");
                ItemStack loaded = ItemStack.parse(helper.getLevel().registryAccess(), both.save(helper.getLevel().registryAccess())).orElseThrow();
                helper.assertValueEqual(ToolState.freeModifiers(loaded), 7, "Slot allowance survives persistent component round trips");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void replacementCannotRemoveSpentMaterialSlots(GameTestHelper helper) {
                ItemStack original = pickaxe("paper", "wood", "paper");
                original.set(TLDataComponents.MODIFIERS, List.of(new ToolModifierEntry(ToolModifiers.DIAMOND, 1),
                                new ToolModifierEntry(ToolModifiers.EMERALD, 1), new ToolModifierEntry(ToolModifiers.HASTE, 100),
                                new ToolModifierEntry(ToolModifiers.SHARPNESS, 72)));
                ((DynamicTool) original.getItem()).calculateStats(original);
                helper.assertValueEqual(ToolState.usedModifiers(original), 5, "The five purchased levels consume five slots");
                var replacement = StationRecipes.evaluate(TLStations.TOOL_STATION,
                                List.of(original, part(ToolParts.Type.PICKAXE_HEAD, id("iron")),
                                                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY),
                                StationOperations.REPLACE, null, "", 0);
                helper.assertTrue(!replacement.valid(), "Removing an occupied Writable slot rejects the entire replacement");
                helper.assertValueEqual(original.get(TLDataComponents.MATERIALS).getFirst().material(), id("paper"),
                                "The failed preview preserves the old head");
                helper.succeed();
        }

        // Exact Material Units
        @GameTest(template = "empty")
        public static void paperAndBoneMealUseTheOriginalQuarterUnitValues(GameTestHelper helper) {
                helper.assertValueEqual(MaterialInputs.match(new ItemStack(Items.PAPER)).value(), 36, "Paper is one quarter unit");
                helper.assertValueEqual(MaterialInputs.match(new ItemStack(Items.BONE_MEAL)).value(), 36, "Bone meal is one quarter unit");
                var built = StationRecipes.evaluate(TLStations.PART_BUILDER,
                                List.of(pattern(), new ItemStack(Items.PAPER, 4), ItemStack.EMPTY, ItemStack.EMPTY),
                                StationOperations.BUILD_PART, null, "", 0);
                helper.assertTrue(built.valid(), "Four papers build a one-unit binding");
                helper.assertTrue(built.inputsAfter().get(1).isEmpty() && built.inputsAfter().get(3).isEmpty(), "No paper is lost or overconsumed");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void fractionalRemaindersStayReusableAndNeverDisappear(GameTestHelper helper) {
                var built = StationRecipes.evaluate(TLStations.PART_BUILDER,
                                List.of(pattern(), new ItemStack(Items.PRISMARINE_BRICKS), ItemStack.EMPTY, ItemStack.EMPTY),
                                StationOperations.BUILD_PART, null, "", 0);
                helper.assertTrue(built.valid(), "A prismarine brick supplies nine original fragments");
                ItemStack remainder = built.inputsAfter().get(3);
                helper.assertValueEqual(MaterialInputs.match(remainder).value() * remainder.getCount(), 180,
                                "324 input minus 144 cost returns exactly 180 units");
                var reused = StationRecipes.evaluate(TLStations.PART_BUILDER,
                                List.of(pattern(), remainder, ItemStack.EMPTY, ItemStack.EMPTY),
                                StationOperations.BUILD_PART, null, "", 0);
                helper.assertTrue(reused.valid(), "Fractional remainder stacks can craft another part");
                helper.assertValueEqual(reused.inputsAfter().get(1).getCount(), 1, "One36-unit fragment remains in the input");
                ItemStack full = TLItems.Parts.MATERIAL_SHARD.get().withMaterial(id("prismarine"), 64);
                var blocked = StationRecipes.evaluate(TLStations.PART_BUILDER,
                                List.of(pattern(), new ItemStack(Items.PRISMARINE_BRICKS), ItemStack.EMPTY, full),
                                StationOperations.BUILD_PART, null, "", 0);
                helper.assertTrue(!blocked.valid(), "Insufficient remainder space blocks the transaction");
                helper.assertValueEqual(full.getCount(), 64, "Blocked previews never consume or change existing inventory");
                helper.succeed();
        }

        // Original Nether Ore Tier
        @GameTest(template = "empty")
        public static void cobaltTierUnlocksActualNetherOreDrops(GameTestHelper helper) {
                ItemStack diamond = ToolModifiers.applyIngredient(tool(TLTools.Type.PICKAXE, id("stone")), new ItemStack(Items.DIAMOND));
                ItemStack obsidian = tool(TLTools.Type.PICKAXE, id("obsidian"));
                helper.assertValueEqual(obsidian.get(TLDataComponents.MODIFIED_STATS).get("harvest_level").intValue(), 4,
                                "Original obsidian material reaches the Cobalt harvest tier");
                for (var holder : TLBlocks.OreBlocks.ALL) {
                        var state = holder.get().defaultBlockState();
                        helper.assertTrue(!diamond.isCorrectToolForDrops(state), "Diamond label2 cannot harvest Cobalt ore tier4");
                        helper.assertTrue(!new ItemStack(Items.DIAMOND_PICKAXE).isCorrectToolForDrops(state), "Vanilla diamondtier3 is also insufficient");
                        helper.assertTrue(new ItemStack(Items.NETHERITE_PICKAXE).isCorrectToolForDrops(state),
                                        "Vanilla Netherite is the first vanilla pickaxe tier that Jade should display");
                        helper.assertTrue(obsidian.isCorrectToolForDrops(state), "Cobalt-tier dynamic pickaxes pass the actual drop check");
                }
                var player = helper.makeMockServerPlayerInLevel();
                player.setGameMode(GameType.SURVIVAL);
                BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
                player.moveTo(pos.getX() + 2, pos.getY(), pos.getZ() + 2, 0, 0);
                player.setItemInHand(InteractionHand.MAIN_HAND, obsidian);
                helper.getLevel().setBlockAndUpdate(pos, TLBlocks.block(TLBlocks.OreBlocks.Type.NETHER_COBALT).defaultBlockState());
                helper.assertTrue(player.gameMode.destroyBlock(pos), "Successful survival mining removes cobalt ore");
                helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1))
                                .stream().anyMatch(entity -> entity.getItem().is(TLItems.item(TLItems.Items.RawOres.Type.COBALT))),
                                "The actual mining path produces raw cobalt");
                helper.getLevel().getServer().getPlayerList().remove(player);
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void balanceRefreshPreservesIdentityNameProgressAndWear(GameTestHelper helper) {
                ItemStack stack = tool(TLTools.Type.PICKAXE, id("iron"));
                ToolState.initialize(stack, true);
                var identity = stack.get(TLDataComponents.TOOL_ID);
                var materials = stack.get(TLDataComponents.MATERIALS);
                var progress = new ToolProgress(3, 123, 3);
                stack.set(TLDataComponents.PROGRESS, progress);
                stack.set(DataComponents.CUSTOM_NAME, Component.literal("Old reliable"));
                stack.set(DataComponents.MAX_DAMAGE, 1000);
                stack.setDamageValue(800);
                stack.remove(TLDataComponents.TOOL_DATA_VERSION);
                ((DynamicTool) stack.getItem()).calculateStats(stack);
                helper.assertValueEqual(ToolState.damage(stack), 800, "Reduced capacity never erases old wear");
                helper.assertTrue(ToolState.broken(stack), "An overused tool remains present and repairable");
                helper.assertValueEqual(stack.get(TLDataComponents.TOOL_ID), identity, "Tool identity is retained");
                helper.assertValueEqual(stack.get(TLDataComponents.MATERIALS), materials, "Saved physical materials are retained");
                helper.assertValueEqual(stack.get(TLDataComponents.PROGRESS), progress, "Experience is retained");
                helper.assertValueEqual(stack.getHoverName().getString(), "Old reliable", "Custom names are retained");
                helper.assertValueEqual(stack.get(TLDataComponents.TOOL_DATA_VERSION), ToolState.DATA_VERSION, "The balance migration is recorded");
                helper.succeed();
        }

        private static ItemStack pickaxe(String head, String handle, String binding) {
                return ToolAssembly.create(TLTools.tool(TLTools.Type.PICKAXE), List.of(
                                new MaterialLayer(ToolStatTypes.HEAD.getId(), id(head)),
                                new MaterialLayer(ToolStatTypes.HANDLE.getId(), id(handle)),
                                new MaterialLayer(ToolStatTypes.EXTRA.getId(), id(binding))));
        }

        private static ItemStack pattern() {
                ItemStack pattern = new ItemStack(TLItems.item(TLItems.Items.IntermediaryItems.Type.PATTERN));
                pattern.set(TLDataComponents.PART, ToolParts.get(ToolParts.Type.BINDING).getId());
                return pattern;
        }
}
