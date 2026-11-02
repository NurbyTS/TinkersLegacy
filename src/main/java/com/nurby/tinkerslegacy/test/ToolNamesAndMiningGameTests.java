package com.nurby.tinkerslegacy.test;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.library.material.layer.MaterialLayer;
import com.nurby.tinkerslegacy.library.tool.ToolModifiers;
import com.nurby.tinkerslegacy.registry.TLBlocks;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLTools;
import com.nurby.tinkerslegacy.registry.ToolStatTypes;
import com.nurby.tinkerslegacy.util.TinkersUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.nurby.tinkerslegacy.test.ToolAssemblyGameTests.id;
import static com.nurby.tinkerslegacy.test.ToolAssemblyGameTests.tool;

@GameTestHolder(TinkersLegacy.MODID)
@PrefixGameTestTemplate(false)
public final class ToolNamesAndMiningGameTests {

        // Names
        @GameTest(template = "empty")
        public static void namesUseRepairMaterialsInsteadOfHandlesAndBindings(GameTestHelper helper) {
                var head = new MaterialLayer(ToolStatTypes.HEAD.getId(), id("stone"));
                var handle = new MaterialLayer(ToolStatTypes.HANDLE.getId(), id("wood"));
                var binding = new MaterialLayer(ToolStatTypes.EXTRA.getId(), id("iron"));
                Component expected = Component.translatable("tinkerslegacy.tool.material_name",
                                Component.translatable("tinkerslegacy.material.stone"),
                                Component.translatable("item.tinkerslegacy.tools.pickaxe"));
                helper.assertValueEqual(TinkersUtils.getToolName(TLTools.tool(TLTools.Type.PICKAXE),
                                List.of(head, handle, binding)), expected, "Only the pick head names the tool");
                helper.assertValueEqual(TinkersUtils.getToolName(TLTools.tool(TLTools.Type.PICKAXE), List.of()),
                                Component.translatable("item.tinkerslegacy.tools.pickaxe"), "Empty tools have their plain base name");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void multiHeadNamesRetainOrderAndCollapseDuplicateMaterials(GameTestHelper helper) {
                var wood = new MaterialLayer(ToolStatTypes.HEAD.getId(), id("wood"));
                var stone = new MaterialLayer(ToolStatTypes.HEAD.getId(), id("stone"));
                var iron = new MaterialLayer(ToolStatTypes.HANDLE.getId(), id("iron"));
                Component name = Component.translatable("item.tinkerslegacy.tools.mattock");
                Component combined = Component.translatableWithFallback("tinkerslegacy.material.wood.name", "%s",
                                Component.translatable("tinkerslegacy.material.wood"))
                                .append("-").append(Component.translatableWithFallback("tinkerslegacy.material.stone.name", "%s",
                                                Component.translatable("tinkerslegacy.material.stone")));
                helper.assertValueEqual(TinkersUtils.getToolName(TLTools.tool(TLTools.Type.MATTOCK), List.of(wood, stone, iron)),
                                Component.translatable("tinkerslegacy.tool.material_name", combined, name), "Both heads name a mixed mattock");
                helper.assertValueEqual(TinkersUtils.getToolName(TLTools.tool(TLTools.Type.MATTOCK), List.of(wood, wood, iron)),
                                Component.translatable("tinkerslegacy.tool.material_name",
                                                Component.translatable("tinkerslegacy.material.wood"), name), "Repeated wood is named once");
                helper.succeed();
        }

        // Mining
        @GameTest(template = "empty")
        public static void diamondAndEmeraldRespectClassicIncrementalMiningLevels(GameTestHelper helper) {
                ItemStack stone = tool(TLTools.Type.PICKAXE, id("stone"));
                ItemStack diamond = ToolModifiers.applyIngredient(stone, new ItemStack(Items.DIAMOND));
                helper.assertValueEqual(diamond.get(TLDataComponents.MODIFIED_STATS).get("harvest_level").intValue(), 2,
                                "Diamond raises Stone's iron mining level by one, not straight to obsidian");
                helper.assertTrue(diamond.isCorrectToolForDrops(Blocks.DIAMOND_ORE.defaultBlockState()), "Upgraded stone mines diamond ore");
                helper.assertTrue(!diamond.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()), "It cannot yet mine obsidian");
                ItemStack emeraldFirst = ToolModifiers.applyIngredient(stone, new ItemStack(Items.EMERALD));
                ItemStack both = ToolModifiers.applyIngredient(emeraldFirst, new ItemStack(Items.DIAMOND));
                helper.assertValueEqual(both.get(TLDataComponents.MODIFIED_STATS).get("harvest_level").intValue(), 3,
                                "Emerald then diamond reaches obsidian");
                helper.assertTrue(both.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()), "The rebuilt tool component unlocks obsidian");
                for (var ore : TLBlocks.OreBlocks.ALL) {
                        helper.assertTrue(!both.isCorrectToolForDrops(ore.get().defaultBlockState()), "Original Nether ores require Cobalt tier4: " + ore.getId());
                }
                ItemStack reverse = ToolModifiers.applyIngredient(diamond, new ItemStack(Items.EMERALD));
                helper.assertValueEqual(reverse.get(TLDataComponents.MODIFIED_STATS).get("harvest_level").intValue(), 2,
                                "Emerald does not exceed its original diamond-level cap");
                ((DynamicTool) both.getItem()).calculateStats(both);
                helper.assertTrue(both.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()), "Repeated recalculation retains mining eligibility");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void upgradedPickaxesProduceRealOreDrops(GameTestHelper helper) {
                ServerPlayer player = helper.makeMockServerPlayerInLevel();
                player.setGameMode(GameType.SURVIVAL);
                BlockPos position = helper.absolutePos(new BlockPos(1, 1, 1));
                player.moveTo(position.getX() + 2, position.getY(), position.getZ() + 2, 0, 0);
                ItemStack pickaxe = ToolModifiers.applyIngredient(tool(TLTools.Type.PICKAXE, id("iron")), new ItemStack(Items.DIAMOND));
                player.setItemInHand(InteractionHand.MAIN_HAND, pickaxe);
                helper.getLevel().setBlockAndUpdate(position, Blocks.OBSIDIAN.defaultBlockState());
                helper.assertTrue(player.gameMode.destroyBlock(position), "Survival mining removes obsidian");
                helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(position).inflate(1))
                                .stream().anyMatch(entity -> entity.getItem().is(Items.OBSIDIAN)), "The actual mining callback produces the obsidian drop");
                helper.getLevel().getServer().getPlayerList().remove(player);
                helper.succeed();
        }
}
