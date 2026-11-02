package com.nurby.tinkerslegacy.test;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.registry.TLBlocks;
import com.nurby.tinkerslegacy.registry.TLItems;

import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.stream.IntStream;

@GameTestHolder(TinkersLegacy.MODID)
@PrefixGameTestTemplate(false)
public final class ParityRecipeGameTests {

        // Slime Recipes
        @GameTest(template = "empty")
        public static void slimeCompressionRoundTripsEveryRegisteredColor(GameTestHelper helper) {
                for (var color : TLBlocks.SlimyBlocks.SlimeType.values()) {
                        ItemLike ball = color == TLBlocks.SlimyBlocks.SlimeType.GREEN ? Items.SLIME_BALL
                                        : TLItems.item(TLItems.Items.Slimeballs.Type.valueOf(color.name()));
                        packing(helper, ball, TLBlocks.SlimyBlocks.congealed(color).get(), 2);
                        ItemLike full = color == TLBlocks.SlimyBlocks.SlimeType.GREEN ? Blocks.SLIME_BLOCK
                                        : TLBlocks.SlimyBlocks.slime(color).get();
                        packing(helper, ball, full, 3);
                }
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void slimeCompressionRejectsMixedColorsAndWrongLayout(GameTestHelper helper) {
                var blue = TLItems.item(TLItems.Items.Slimeballs.Type.BLUE);
                var purple = TLItems.item(TLItems.Items.Slimeballs.Type.PURPLE);
                var manager = helper.getLevel().getRecipeManager();
                var mixed = CraftingInput.of(2, 2, List.of(new ItemStack(blue), new ItemStack(blue),
                                new ItemStack(purple), new ItemStack(blue)));
                helper.assertTrue(manager.getRecipeFor(RecipeType.CRAFTING, mixed, helper.getLevel()).isEmpty(),
                                "Different slime colors cannot silently compress into another color");
                var wrong = CraftingInput.of(3, 2, List.of(new ItemStack(blue), new ItemStack(blue), new ItemStack(blue),
                                new ItemStack(blue), ItemStack.EMPTY, ItemStack.EMPTY));
                helper.assertTrue(manager.getRecipeFor(RecipeType.CRAFTING, wrong, helper.getLevel()).isEmpty(),
                                "Four balls still require the original square layout");
                var stacked = CraftingInput.of(1, 1, List.of(new ItemStack(blue, 4)));
                helper.assertTrue(manager.getRecipeFor(RecipeType.CRAFTING, stacked, helper.getLevel()).isEmpty(),
                                "Four balls in one slot cannot bypass the recipe's four ingredients");
                helper.succeed();
        }

        // Seared Recipes
        @GameTest(template = "empty")
        public static void searedBricksReachTheOriginalDecorativeCycleAndCrackedVariant(GameTestHelper helper) {
                ItemStack brick = new ItemStack(TLItems.item(TLItems.Items.Bricks.Type.SEARED));
                ItemStack current = craft(helper, CraftingInput.of(2, 2, List.of(brick, brick, brick, brick)));
                helper.assertTrue(current.is(TLBlocks.block(TLBlocks.SearedBlocks.Type.BRICKS).asItem()),
                                "The existing four-brick starter recipe remains usable");
                for (var type : List.of(TLBlocks.SearedBlocks.Type.BRICK_FANCY, TLBlocks.SearedBlocks.Type.BRICK_SQUARE,
                                TLBlocks.SearedBlocks.Type.BRICK_TRIANGLE, TLBlocks.SearedBlocks.Type.CREEPER,
                                TLBlocks.SearedBlocks.Type.BRICK_SMALL, TLBlocks.SearedBlocks.Type.TILE,
                                TLBlocks.SearedBlocks.Type.ROAD, TLBlocks.SearedBlocks.Type.PAVER, TLBlocks.SearedBlocks.Type.BRICKS)) {
                        current = craft(helper, CraftingInput.of(1, 1, List.of(current)));
                        helper.assertTrue(current.is(TLBlocks.block(type).asItem()), "Original decorative conversion produces " + type);
                        helper.assertValueEqual(current.getCount(), 1, "Decorative conversions conserve block count");
                }
                ItemStack paver = craft(helper, CraftingInput.of(1, 1,
                                List.of(new ItemStack(TLBlocks.block(TLBlocks.SearedBlocks.Type.STONE)))));
                helper.assertTrue(paver.is(TLBlocks.block(TLBlocks.SearedBlocks.Type.PAVER).asItem()),
                                "Seared Stone enters the original decorative cycle at Paver");
                var furnace = new SingleRecipeInput(current);
                var cracked = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.SMELTING, furnace, helper.getLevel()).orElseThrow();
                helper.assertTrue(cracked.value().assemble(furnace, helper.getLevel().registryAccess())
                                .is(TLBlocks.block(TLBlocks.SearedBlocks.Type.BRICK_CRACKED).asItem()), "A furnace cracks Seared Bricks");
                helper.assertValueEqual(cracked.value().getCookingTime(), 200, "Cracking takes the original ten seconds");
                helper.assertValueEqual(cracked.value().getExperience(), 0.1F, "Original cracked-brick experience");
                var grout = new SingleRecipeInput(new ItemStack(TLBlocks.block(TLBlocks.IntermediaryBlocks.Type.GROUT)));
                var smelted = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.SMELTING, grout, helper.getLevel()).orElseThrow();
                helper.assertTrue(smelted.value().assemble(grout, helper.getLevel().registryAccess())
                                .is(TLItems.item(TLItems.Items.Bricks.Type.SEARED)), "Grout still smelts into one Seared Brick");
                helper.assertValueEqual(smelted.value().getExperience(), 0.4F, "Grout restores the original smelting experience");
                helper.succeed();
        }

        // Food
        @GameTest(template = "empty")
        public static void coloredSlimeballsAreAlwaysEdibleAndApplyOriginalEffects(GameTestHelper helper) {
                for (var type : TLItems.Items.Slimeballs.Type.values()) {
                        var player = helper.makeMockPlayer(GameType.SURVIVAL);
                        ItemStack stack = new ItemStack(TLItems.item(type), 2);
                        helper.assertTrue(stack.is(Tags.Items.SLIME_BALLS), "Colored slimeballs retain the common ingredient tag");
                        var food = stack.get(DataComponents.FOOD);
                        helper.assertTrue(food != null && food.canAlwaysEat() && player.canEat(food.canAlwaysEat()),
                                        "Original slimeball can be eaten even at full hunger: " + type);
                        helper.assertValueEqual(food.eatDurationTicks(), 32, "Slimeballs keep normal eating duration");
                        int nutrition = type == TLItems.Items.Slimeballs.Type.MAGMA ? 2 : 1;
                        float saturation = switch (type) {
                                case BLUE -> 2.0F;
                                case PURPLE, MAGMA -> 4.0F;
                                case BLOOD -> 3.0F;
                        };
                        player.getFoodData().setFoodLevel(10);
                        player.getFoodData().setSaturation(0);
                        ItemStack remaining = stack.finishUsingItem(helper.getLevel(), player);
                        helper.assertValueEqual(remaining.getCount(), 1, "Eating consumes exactly one slimeball");
                        helper.assertValueEqual(player.getFoodData().getFoodLevel(), 10 + nutrition, "Original nutrition");
                        helper.assertValueEqual(player.getFoodData().getSaturationLevel(), saturation, "Original saturation gain");
                        List<MobEffectInstance> effects = switch (type) {
                                case BLUE -> List.of(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 900, 2),
                                                new MobEffectInstance(MobEffects.JUMP, 1200, 2));
                                case PURPLE -> List.of(new MobEffectInstance(MobEffects.UNLUCK, 900),
                                                new MobEffectInstance(MobEffects.LUCK, 1200));
                                case MAGMA -> List.of(new MobEffectInstance(MobEffects.WEAKNESS, 900),
                                                new MobEffectInstance(MobEffects.WITHER, 300), new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1200));
                                case BLOOD -> List.of(new MobEffectInstance(MobEffects.POISON, 900, 2),
                                                new MobEffectInstance(MobEffects.HEALTH_BOOST, 1200));
                        };
                        helper.assertValueEqual(player.getActiveEffects().size(), effects.size(), "Eating applies every listed effect");
                        for (var expected : effects) {
                                var actual = player.getEffect(expected.getEffect());
                                helper.assertTrue(actual != null, "Every original effect is applied on eating: " + type);
                                helper.assertValueEqual(actual.getDuration(), expected.getDuration(), "Original effect duration");
                                helper.assertValueEqual(actual.getAmplifier(), expected.getAmplifier(), "Original effect level");
                        }
                }
                helper.succeed();
        }

        // Helpers
        private static void packing(GameTestHelper helper, ItemLike ball, ItemLike block, int width) {
                var input = CraftingInput.of(width, width, IntStream.range(0, width * width)
                                .mapToObj(index -> new ItemStack(ball)).toList());
                ItemStack packed = craft(helper, input);
                helper.assertTrue(packed.is(block.asItem()), "Matching slimeballs compress into their own block");
                helper.assertValueEqual(packed.getCount(), 1, "Compression makes one block");
                ItemStack unpacked = craft(helper, CraftingInput.of(1, 1, List.of(packed)));
                helper.assertTrue(unpacked.is(ball.asItem()), "Decompression preserves slime color");
                helper.assertValueEqual(unpacked.getCount(), width * width, "Decompression returns the exact original material count");
        }

        private static ItemStack craft(GameTestHelper helper, CraftingInput input) {
                var recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel()).orElseThrow();
                return recipe.value().assemble(input, helper.getLevel().registryAccess());
        }
}
