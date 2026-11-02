package com.nurby.tinkerslegacy.api.material.recipe;

public interface MaterialRecipeInfo {

        // Properties
        default boolean craftable() {
                return false;
        }

        default boolean castable() {
                return false;
        }
}
