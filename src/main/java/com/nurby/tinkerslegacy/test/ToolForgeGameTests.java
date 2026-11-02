package com.nurby.tinkerslegacy.test;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.block.entity.StationBlockEntity;
import com.nurby.tinkerslegacy.library.station.StationOperations;
import com.nurby.tinkerslegacy.library.station.StationRecipes;
import com.nurby.tinkerslegacy.library.station.StationRequirement;
import com.nurby.tinkerslegacy.library.tool.ToolDefinition;
import com.nurby.tinkerslegacy.registry.TLBlocks;
import com.nurby.tinkerslegacy.registry.TLStations;
import com.nurby.tinkerslegacy.registry.TLTools;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

import static com.nurby.tinkerslegacy.test.ToolAssemblyGameTests.*;

@GameTestHolder(TinkersLegacy.MODID)
@PrefixGameTestTemplate(false)
public final class ToolForgeGameTests {

        // Tiers
        @GameTest(template = "empty")
        public static void registeredForgeRetainsStationOperationsAndUnlocksHigherTiers(GameTestHelper helper) {
                var pickaxe = TLTools.tool(TLTools.Type.PICKAXE);
                ToolDefinition advanced = new ToolDefinition(id("test_advanced"), pickaxe.parts()) {
                        @Override
                        public StationRequirement stationRequirement(ResourceLocation operation) {
                                return new StationRequirement(StationOperations.TOOL_FAMILY, 2);
                        }
                };
                helper.assertTrue(StationRecipes.supports(TLStations.TOOL_FORGE, advanced, StationOperations.ASSEMBLE),
                                "The registered Forge accepts tools requiring tier two");
                helper.assertTrue(!StationRecipes.supports(TLStations.TOOL_STATION, advanced, StationOperations.ASSEMBLE),
                                "The wooden station must not assemble advanced tools");
                helper.assertValueEqual(TLStations.TOOL_FORGE.operations(), TLStations.TOOL_STATION.operations(),
                                "The Forge retains every ordinary station maintenance operation");

                List<ItemStack> inputs = new ArrayList<>(parts(pickaxe, id("wood")));
                while (inputs.size() < TLStations.TOOL_FORGE.inputSlots()) {
                        inputs.add(ItemStack.EMPTY);
                }
                var result = StationRecipes.evaluate(TLStations.TOOL_FORGE, inputs,
                                StationOperations.ASSEMBLE, pickaxe.id(), "", 0);
                helper.assertTrue(result.valid(), "The Forge still assembles basic tools");
                var pos = new BlockPos(1, 1, 1);
                helper.setBlock(pos, TLBlocks.block(TLBlocks.Workstations.Type.TOOL_FORGE));
                var station = (StationBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
                helper.assertValueEqual(station.definition(), TLStations.TOOL_FORGE, "The placed Forge resolves tier two");
                helper.assertValueEqual(station.getContainerSize(), 6, "The Forge retains the original six input slots");
                helper.assertTrue(station.getBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE), "The metal Forge uses a pickaxe");
                helper.assertTrue(!station.getBlockState().is(BlockTags.MINEABLE_WITH_AXE), "The metal Forge is not a wooden table");
                helper.succeed();
        }

        // Recipes
        @GameTest(template = "empty")
        public static void forgeRecipeUsesThreeBricksAndFourMatchingMetalBlocks(GameTestHelper helper) {
                ItemStack brick = new ItemStack(TLBlocks.block(TLBlocks.SearedBlocks.Type.BRICKS));
                ItemStack iron = new ItemStack(Items.IRON_BLOCK);
                ItemStack station = new ItemStack(TLBlocks.block(TLBlocks.Workstations.Type.TOOL_STATION));
                List<ItemStack> grid = new ArrayList<>(List.of(brick, brick, brick, iron, station, iron,
                                iron, ItemStack.EMPTY, iron));
                var manager = helper.getLevel().getRecipeManager();
                var input = CraftingInput.of(3, 3, grid);
                var recipe = manager.getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
                helper.assertTrue(recipe.isPresent(), "The original Forge layout has a recipe");
                helper.assertTrue(recipe.get().value().assemble(input, helper.getLevel().registryAccess())
                                .is(TLBlocks.block(TLBlocks.Workstations.Type.TOOL_FORGE).asItem()), "The recipe produces a Forge");
                grid.set(8, new ItemStack(Items.GOLD_BLOCK));
                helper.assertTrue(manager.getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(3, 3, grid), helper.getLevel()).isEmpty(),
                                "Each Forge recipe requires four blocks of the same metal");
                helper.succeed();
        }
}
