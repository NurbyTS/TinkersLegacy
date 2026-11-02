package com.nurby.tinkerslegacy.test;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.TLConfig;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLMaterials;
import com.nurby.tinkerslegacy.registry.TLStations;
import com.nurby.tinkerslegacy.registry.ToolParts;
import com.nurby.tinkerslegacy.library.station.StationRecipes;
import com.nurby.tinkerslegacy.library.station.StationOperations;
import com.nurby.tinkerslegacy.block.SlimyVineBlock;
import com.nurby.tinkerslegacy.library.station.MaterialInputs;
import com.nurby.tinkerslegacy.registry.TLBlocks;
import com.nurby.tinkerslegacy.registry.TLItems;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VineBlock;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;

import static com.nurby.tinkerslegacy.test.ToolAssemblyGameTests.id;

@GameTestHolder(TinkersLegacy.MODID)
@PrefixGameTestTemplate(false)
public final class NativeMaterialGameTests {

        // Ingredients
        @GameTest(template = "empty")
        public static void nativeFoliageRetainsItsOwnMaterialAndOriginalUnitValue(GameTestHelper helper) {
                Map<TLBlocks.SlimyPlants.Type, String> names = Map.of(
                                TLBlocks.SlimyPlants.Type.BLUE_LEAVES, "slimeleaf_blue",
                                TLBlocks.SlimyPlants.Type.ORANGE_LEAVES, "slimeleaf_orange",
                                TLBlocks.SlimyPlants.Type.PURPLE_LEAVES, "slimeleaf_purple",
                                TLBlocks.SlimyPlants.Type.BLUE_VINE, "slimevine_blue",
                                TLBlocks.SlimyPlants.Type.PURPLE_VINE, "slimevine_purple");
                names.forEach((type, name) -> {
                        var block = TLBlocks.block(type);
                        var match = MaterialInputs.match(new ItemStack(block));
                        helper.assertTrue(match != null, "Native foliage is recognized by its material tag: " + type);
                        helper.assertValueEqual(match.material(), id(name), "Generic leaves/vines do not replace the slime material");
                        helper.assertValueEqual(match.value(), block instanceof SlimyVineBlock ? MaterialInputs.UNIT : MaterialInputs.SHARD,
                                        "Original foliage material value");
                        helper.assertTrue(match.craftable(), "Native foliage supports the Part Builder");
                });
                helper.assertTrue(TLBlocks.block(TLBlocks.IntermediaryBlocks.Type.GLOW).asItem() == Items.AIR,
                                "Glow is an effect block without a obtainable block item");
                helper.succeed();
        }

        // Recipes
        @GameTest(template = "empty")
        public static void originalFallbackRecipesCompleteTheModifierIngredientWorkflow(GameTestHelper helper) {
                var manager = helper.getLevel().getRecipeManager();
                ItemStack plank = new ItemStack(Items.OAK_PLANKS);
                var input = CraftingInput.of(3, 3, List.of(ItemStack.EMPTY, plank, ItemStack.EMPTY,
                                plank, new ItemStack(Items.LAVA_BUCKET), plank, ItemStack.EMPTY, plank, ItemStack.EMPTY));
                var recipe = manager.getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel()).orElseThrow();
                ItemStack lavawood = recipe.value().assemble(input, helper.getLevel().registryAccess());
                helper.assertTrue(lavawood.is(TLBlocks.block(TLBlocks.IntermediaryBlocks.Type.LAVAWOOD).asItem()),
                                "Four planks around a lava bucket make original fallback Lavawood");
                helper.assertTrue(recipe.value().getRemainingItems(input).get(4).is(Items.BUCKET),
                                "Lavawood crafting returns the empty bucket");
                var fireInput = CraftingInput.of(3, 1, List.of(lavawood,
                                new ItemStack(Items.BLAZE_POWDER), new ItemStack(Items.BLAZE_POWDER)));
                var fireRecipe = manager.getRecipeFor(RecipeType.CRAFTING, fireInput, helper.getLevel()).orElseThrow();
                helper.assertTrue(fireRecipe.value().assemble(fireInput, helper.getLevel().registryAccess())
                                .is(TLBlocks.block(TLBlocks.IntermediaryBlocks.Type.FIREWOOD).asItem()),
                                "Lavawood and two blaze powder make original Firewood");

                var soilInput = CraftingInput.of(3, 1, List.of(new ItemStack(Items.DIRT),
                                new ItemStack(Items.ROTTEN_FLESH), new ItemStack(Items.BONE_MEAL)));
                var soilRecipe = manager.getRecipeFor(RecipeType.CRAFTING, soilInput, helper.getLevel()).orElseThrow();
                ItemStack graveyard = soilRecipe.value().assemble(soilInput, helper.getLevel().registryAccess());
                helper.assertTrue(graveyard.is(TLBlocks.block(TLBlocks.IntermediaryBlocks.Type.GRAVEYARD_SOIL).asItem()),
                                "Dirt, rotten flesh and bone meal make Graveyard Soil");
                var furnace = new SingleRecipeInput(graveyard);
                var smelting = manager.getRecipeFor(RecipeType.SMELTING, furnace, helper.getLevel()).orElseThrow();
                helper.assertTrue(smelting.value().assemble(furnace, helper.getLevel().registryAccess())
                                .is(TLBlocks.block(TLBlocks.IntermediaryBlocks.Type.CONSECRATED_SOIL).asItem()),
                                "Smelting Graveyard Soil makes Consecrated Soil");

                ItemStack obsidian = new ItemStack(Items.OBSIDIAN);
                var plateInput = CraftingInput.of(3, 3, List.of(obsidian, obsidian, obsidian,
                                obsidian, new ItemStack(Items.GOLD_INGOT), obsidian, obsidian, obsidian, obsidian));
                var plateRecipe = manager.getRecipeFor(RecipeType.CRAFTING, plateInput, helper.getLevel()).orElseThrow();
                helper.assertTrue(plateRecipe.value().assemble(plateInput, helper.getLevel().registryAccess())
                                .is(TLItems.item(TLItems.Items.Miscellaneous.Type.REINFORCED_PLATE)),
                                "Reinforcement retains the original no-Smeltery gold-ingot recipe");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void nativeSlimeCrystalsAndAlloysHaveOriginalSurvivalRecipes(GameTestHelper helper) {
                var manager = helper.getLevel().getRecipeManager();
                var purple = TLItems.item(TLItems.Items.Slimeballs.Type.PURPLE);
                var crystals = Map.of(
                                TLBlocks.IntermediaryBlocks.Type.GREEN_SLIMY_MUD, TLItems.Items.Gems.Type.GREEN_SLIME_CRYSTAL,
                                TLBlocks.IntermediaryBlocks.Type.BLUE_SLIMY_MUD, TLItems.Items.Gems.Type.BLUE_SLIME_CRYSTAL,
                                TLBlocks.IntermediaryBlocks.Type.MAGMA_SLIMY_MUD, TLItems.Items.Gems.Type.MAGMA_SLIME_CRYSTAL);
                crystals.forEach((mud, crystal) -> {
                        var input = new SingleRecipeInput(new ItemStack(TLBlocks.block(mud)));
                        var recipe = manager.getRecipeFor(RecipeType.SMELTING, input, helper.getLevel()).orElseThrow();
                        helper.assertTrue(recipe.value().assemble(input, helper.getLevel().registryAccess()).is(TLItems.item(crystal)),
                                        "Every slimy mud smelts into its corresponding crystal");
                        helper.assertValueEqual(recipe.value().getExperience(), 0.75F, "Original slime crystal experience");
                });
                var mudInput = CraftingInput.of(3, 2, List.of(new ItemStack(Items.SLIME_BALL), new ItemStack(Items.SLIME_BALL),
                                new ItemStack(Items.SLIME_BALL), new ItemStack(Items.SLIME_BALL), new ItemStack(Items.SAND), new ItemStack(Items.DIRT)));
                var mudRecipe = manager.getRecipeFor(RecipeType.CRAFTING, mudInput, helper.getLevel()).orElseThrow();
                helper.assertTrue(mudRecipe.value().assemble(mudInput, helper.getLevel().registryAccess())
                                .is(TLBlocks.block(TLBlocks.IntermediaryBlocks.Type.GREEN_SLIMY_MUD).asItem()),
                                "Four slimeballs plus sand and dirt make one slimy mud");
                var dyedInput = CraftingInput.of(2, 1, List.of(new ItemStack(Items.SLIME_BALL), new ItemStack(Items.PURPLE_DYE)));
                var dyedRecipe = manager.getRecipeFor(RecipeType.CRAFTING, dyedInput, helper.getLevel()).orElseThrow();
                helper.assertTrue(dyedRecipe.value().assemble(dyedInput, helper.getLevel().registryAccess()).is(purple),
                                "Purple slimeballs use the original no-world dye fallback");
                var manyullynInput = CraftingInput.of(3, 1, List.of(new ItemStack(TLItems.item(TLItems.Items.Ingots.Type.COBALT)),
                                new ItemStack(TLItems.item(TLItems.Items.Ingots.Type.ARDITE)), new ItemStack(Items.COAL_BLOCK)));
                var alloyRecipe = manager.getRecipeFor(RecipeType.CRAFTING, manyullynInput, helper.getLevel()).orElseThrow();
                helper.assertTrue(alloyRecipe.value().assemble(manyullynInput, helper.getLevel().registryAccess())
                                .is(TLItems.item(TLItems.Items.Ingots.Type.MANYULLYN)), "The original no-Smeltery alloy recipe is present");
                var expanderInput = CraftingInput.of(3, 3, List.of(ItemStack.EMPTY, new ItemStack(Items.PISTON), ItemStack.EMPTY,
                                new ItemStack(Items.LAPIS_LAZULI), new ItemStack(purple), new ItemStack(Items.LAPIS_LAZULI),
                                ItemStack.EMPTY, new ItemStack(Items.PISTON), ItemStack.EMPTY));
                var expanderRecipe = manager.getRecipeFor(RecipeType.CRAFTING, expanderInput, helper.getLevel()).orElseThrow();
                helper.assertTrue(expanderRecipe.value().assemble(expanderInput, helper.getLevel().registryAccess())
                                .is(TLItems.item(TLItems.Items.Miscellaneous.Type.EXPANDER_VERTICAL)), "Vertical expander retains its original orientation");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void metalPartFallbackCanBeDisabledWithoutChangingMaterialMetadata(GameTestHelper helper) {
                boolean fallback = TLConfig.PART_BUILDER_METAL_FALLBACK.get();
                boolean override = TLConfig.CRAFT_CASTABLE_MATERIALS.get();
                try {
                        TLConfig.CRAFT_CASTABLE_MATERIALS.set(false);
                        TLConfig.PART_BUILDER_METAL_FALLBACK.set(true);
                        ItemStack pattern = new ItemStack(TLItems.item(TLItems.Items.IntermediaryItems.Type.PATTERN));
                        pattern.set(TLDataComponents.PART, ToolParts.get(ToolParts.Type.PICKAXE_HEAD).getId());
                        var inputs = List.of(pattern, new ItemStack(Items.IRON_INGOT, 2), ItemStack.EMPTY, ItemStack.EMPTY);
                        helper.assertTrue(!TLMaterials.IRON.get().recipeInfo().craftable() && TLMaterials.IRON.get().recipeInfo().castable(),
                                        "The fallback does not rewrite original material metadata");
                        helper.assertTrue(StationRecipes.evaluate(TLStations.PART_BUILDER, inputs, StationOperations.BUILD_PART,
                                        null, "", 0).valid(), "Metal heads can be shaped while the Smeltery is unavailable");
                        TLConfig.PART_BUILDER_METAL_FALLBACK.set(false);
                        helper.assertTrue(!StationRecipes.evaluate(TLStations.PART_BUILDER, inputs, StationOperations.BUILD_PART,
                                        null, "", 0).valid(), "Disabling the fallback restores casting-only metal parts");
                        helper.assertTrue(!MaterialInputs.match(MaterialInputs.shard(id("iron"), 1)).craftable(),
                                        "Disabling the fallback applies to metal shards too");
                        helper.assertTrue(MaterialInputs.match(new ItemStack(Items.OBSIDIAN)).craftable(),
                                        "Original craftable-and-castable materials remain craftable");
                        TLConfig.CRAFT_CASTABLE_MATERIALS.set(true);
                        helper.assertTrue(MaterialInputs.match(new ItemStack(Items.IRON_NUGGET)).craftable(),
                                        "The existing castable-material override remains available");
                        helper.assertValueEqual(MaterialInputs.match(new ItemStack(Items.IRON_NUGGET)).value(), 16,
                                        "Nuggets preserve their original integer material units");
                } finally {
                        TLConfig.PART_BUILDER_METAL_FALLBACK.set(fallback);
                        TLConfig.CRAFT_CASTABLE_MATERIALS.set(override);
                }
                helper.succeed();
        }

        // Vines
        @GameTest(template = "empty")
        public static void slimeVinesGrowDownwardAndStopAtTheirOriginalEndStage(GameTestHelper helper) {
                var level = helper.getLevel();
                var pos = helper.absolutePos(new BlockPos(1, 3, 1));
                var vine = TLBlocks.block(TLBlocks.SlimyPlants.Type.BLUE_VINE);
                level.setBlockAndUpdate(pos.north(), Blocks.STONE.defaultBlockState());
                var state = vine.defaultBlockState().setValue(VineBlock.NORTH, true).setValue(SlimyVineBlock.STAGE, 2);
                level.setBlockAndUpdate(pos, state);
                level.setBlockAndUpdate(pos.below(), Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
                var random = RandomSource.create(1234);
                for (int index = 0; index < 64; index++) {
                        state.randomTick(level, pos, random);
                }
                helper.assertTrue(level.isEmptyBlock(pos.below()), "The terminal vine stage never extends");
                state = state.setValue(SlimyVineBlock.STAGE, 0);
                level.setBlockAndUpdate(pos, state);
                for (int index = 0; index < 64; index++) {
                        state.randomTick(level, pos, random);
                }
                helper.assertTrue(level.getBlockState(pos.below()).is(vine), "A supported vine extends downward");
                helper.assertTrue(level.isEmptyBlock(pos.above()), "Slime vines do not adopt vanilla upward growth");
                helper.succeed();
        }
}
