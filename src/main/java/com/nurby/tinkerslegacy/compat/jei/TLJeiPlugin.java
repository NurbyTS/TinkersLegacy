package com.nurby.tinkerslegacy.compat.jei;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.item.dynamic.DynamicPart;
import com.nurby.tinkerslegacy.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLBlocks;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.stream.Collectors;

@JeiPlugin
public class TLJeiPlugin implements IModPlugin {
    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        Component description = Component.translatable("jei.tinkerslegacy.nether_ore_generation");

        registration.addIngredientInfo(
                TLBlocks.block(TLBlocks.OreBlocks.Type.NETHER_COBALT),
                description);

        registration.addIngredientInfo(
                TLBlocks.block(TLBlocks.OreBlocks.Type.NETHER_ARDITE),
                description);
    }

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, "jei");
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        MaterialSubtypeInterpreter interpreter = new MaterialSubtypeInterpreter();

        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof DynamicTool || item instanceof DynamicPart) {
                registration.registerSubtypeInterpreter(item, interpreter);
            }
        }
    }

    private static final class MaterialSubtypeInterpreter implements ISubtypeInterpreter<ItemStack> {
        @Override
        public Object getSubtypeData(ItemStack stack, UidContext context) {
            List<ResourceLocation> materials = stack.get(TLDataComponents.MATERIALS);

            if (materials == null || materials.isEmpty()) {
                return null;
            }

            return List.copyOf(materials);
        }

        @Override
        public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
            List<ResourceLocation> materials = stack.get(TLDataComponents.MATERIALS);

            if (materials == null || materials.isEmpty()) {
                return "";
            }

            return materials.stream()
                    .map(ResourceLocation::toString)
                    .collect(Collectors.joining(";"));
        }
    }
}
