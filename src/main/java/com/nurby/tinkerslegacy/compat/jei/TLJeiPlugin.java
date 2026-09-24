package com.nurby.tinkerslegacy.compat.jei;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.item.dynamic.DynamicPart;
import com.nurby.tinkerslegacy.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.library.material.layer.MaterialLayer;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.stream.Collectors;

@JeiPlugin
public class TLJeiPlugin implements IModPlugin {
    @Override 
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, "jei");
    }

    @Override 
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        MaterialSubtypeInterpreter interpreter = 
            new MaterialSubtypeInterpreter();

        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof DynamicTool || item instanceof DynamicPart) {
                registration.registerSubtypeInterpreter(item, interpreter);
            }
        }
    }

    private static final class MaterialSubtypeInterpreter implements ISubtypeInterpreter<ItemStack> {
        @Override 
        public Object getSubtypeData(ItemStack stack, UidContext context) {
            List<MaterialLayer> layers = stack.get(TLDataComponents.MATERIALS);

            if (layers == null || layers.isEmpty()) {
                return null;
            }

            return List.copyOf(layers);
        }

        @Override public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
            List<MaterialLayer> layers = stack.get(TLDataComponents.MATERIALS);

            if (layers == null || layers.isEmpty()) {
                return "";
            }

            return layers.stream().map(layer -> layer.statType() + "=" + layer.material()).collect(Collectors.joining(";"));
        }
    }
}
