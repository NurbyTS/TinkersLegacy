package com.nurby.tinkerslegacy.library.material.recipe;

public record StencilRecipeInfo() implements MaterialRecipeInfo {

        @Override
        public boolean craftable() {
                return true;
        }
}
