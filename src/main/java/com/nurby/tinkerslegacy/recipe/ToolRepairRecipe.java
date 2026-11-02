package com.nurby.tinkerslegacy.recipe;

import com.nurby.tinkerslegacy.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.library.tool.ToolRepair;
import com.nurby.tinkerslegacy.registry.TLRecipeSerializers;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public final class ToolRepairRecipe extends CustomRecipe {
        public ToolRepairRecipe(CraftingBookCategory category) {
                super(category);
        }

        // Crafting
        @Override
        public boolean matches(CraftingInput input, Level level) {
                return !repair(input).isEmpty();
        }

        @Override
        public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
                return repair(input);
        }

        private static ItemStack repair(CraftingInput input) {
                ItemStack tool = ItemStack.EMPTY;
                List<ItemStack> kits = new ArrayList<>();
                for (ItemStack stack : input.items()) {
                        if (stack.isEmpty()) {
                                continue;
                        }
                        if (stack.getItem() instanceof DynamicTool) {
                                if (!tool.isEmpty() || stack.getCount() != 1) {
                                        return ItemStack.EMPTY;
                                }
                                tool = stack;
                        } else if (ToolRepair.kitMaterial(stack) != null) {
                                kits.add(stack.copyWithCount(1));
                        } else {
                                return ItemStack.EMPTY;
                        }
                }
                return tool.isEmpty() || kits.isEmpty() ? ItemStack.EMPTY : ToolRepair.repair(tool, kits);
        }

        @Override
        public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
                // Like the original recipe, one kit per occupied slot is spent, including excess repair capacity.
                return NonNullList.withSize(input.size(), ItemStack.EMPTY);
        }

        @Override
        public boolean canCraftInDimensions(int width, int height) {
                return width >= 3 && height >= 3;
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
                return TLRecipeSerializers.TOOL_REPAIR.get();
        }
}
