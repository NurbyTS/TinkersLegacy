package com.nurby.tinkerslegacy.datagen;

import com.nurby.tinkerslegacy.registry.TLBlocks;
import com.nurby.tinkerslegacy.registry.TLItems;
import com.nurby.tinkerslegacy.registry.TLOreFamilies;

import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public final class TLCookingRecipes {
        private TLCookingRecipes() {
        }

        // Cooking Types
        public enum Type {
                SMELTING("smelting", SimpleCookingRecipeBuilder::smelting),
                BLASTING("blasting", SimpleCookingRecipeBuilder::blasting),
                SMOKING("smoking", SimpleCookingRecipeBuilder::smoking),
                CAMPFIRE("campfire_cooking", SimpleCookingRecipeBuilder::campfireCooking);

                private final String suffix;
                private final CookingFactory factory;

                Type(String suffix, CookingFactory factory) {
                        this.suffix = suffix;
                        this.factory = factory;
                }

                public String suffix() {
                        return suffix;
                }

                public SimpleCookingRecipeBuilder builder(
                                Ingredient input,
                                RecipeCategory category,
                                ItemLike result,
                                float experience,
                                int cookingTime) {
                        return factory.create(input, category, result, experience, cookingTime);
                }
        }

        @FunctionalInterface
        private interface CookingFactory {
                SimpleCookingRecipeBuilder create(
                                Ingredient input,
                                RecipeCategory category,
                                ItemLike result,
                                float experience,
                                int cookingTime);
        }

        // Definitions
        public record CookingMethod(
                        Type type,
                        int cookingTime,
                        @Nullable String recipeName) {
                public CookingMethod(Type type, int cookingTime) {
                        this(type, cookingTime, null);
                }

                public CookingMethod {
                        Objects.requireNonNull(type, "Cooking type");

                        if (cookingTime <= 0) {
                                throw new IllegalArgumentException("Cooking time must be positive");
                        }
                }
        }

        public record CookingRecipe(
                        Supplier<? extends ItemLike> input,
                        Supplier<? extends ItemLike> result,
                        RecipeCategory category,
                        float experience,
                        List<CookingMethod> methods) {
                public CookingRecipe {
                        Objects.requireNonNull(input, "Cooking input");
                        Objects.requireNonNull(result, "Cooking result");
                        Objects.requireNonNull(category, "Recipe category");
                        methods = List.copyOf(methods);

                        if (!Float.isFinite(experience) || experience < 0.0F) {
                                throw new IllegalArgumentException("Cooking experience must be finite and nonnegative");
                        }

                        if (methods.isEmpty()) {
                                throw new IllegalArgumentException("A cooking recipe needs at least one method");
                        }

                        var types = EnumSet.noneOf(Type.class);

                        for (var method : methods) {
                                if (!types.add(method.type())) {
                                        throw new IllegalArgumentException("Duplicate cooking type: " + method.type());
                                }
                        }
                }
        }

        public static final List<CookingRecipe> ALL = createRecipes();

        // Recipes
        private static List<CookingRecipe> createRecipes() {
                List<CookingRecipe> recipes = new ArrayList<>();

                // Metals
                addOreRecipes(
                                recipes,
                                TLOreFamilies.COBALT,
                                0.7F,
                                new CookingMethod(Type.SMELTING, 200),
                                new CookingMethod(Type.BLASTING, 100));

                addOreRecipes(
                                recipes,
                                TLOreFamilies.ARDITE,
                                0.7F,
                                new CookingMethod(Type.SMELTING, 200),
                                new CookingMethod(Type.BLASTING, 100));

                // Intermediary Items
                recipes.add(new CookingRecipe(
                                TLBlocks.get(TLBlocks.IntermediaryBlocks.Type.GROUT),
                                TLItems.get(TLItems.Items.Bricks.Type.SEARED),
                                RecipeCategory.MISC,
                                0.1F,
                                List.of(new CookingMethod(Type.SMELTING, 200, "seared_brick"))));

                return List.copyOf(recipes);
        }

        // Helpers
        private static void addOreRecipes(
                        List<CookingRecipe> recipes,
                        TLOreFamilies.OreFamily family,
                        float experience,
                        CookingMethod... methods) {
                List<CookingMethod> cookingMethods = List.of(methods);

                recipes.add(new CookingRecipe(
                                family.rawItem(),
                                family.metal().ingot(),
                                RecipeCategory.MISC,
                                experience,
                                cookingMethods));

                for (var variant : family.variants()) {
                        recipes.add(new CookingRecipe(
                                        variant,
                                        family.metal().ingot(),
                                        RecipeCategory.MISC,
                                        experience,
                                        cookingMethods));
                }
        }
}
