package com.nurby.tinkerslegacy.api.material.recipe;

public record StencilRecipeInfo() implements MaterialRecipeInfo {

        @Override
        public boolean craftable() {
                return true;
        }
}
