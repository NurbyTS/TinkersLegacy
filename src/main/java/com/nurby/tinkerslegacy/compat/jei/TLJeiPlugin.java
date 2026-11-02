package com.nurby.tinkerslegacy.compat.jei;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.client.screen.CraftingStationScreen;
import com.nurby.tinkerslegacy.client.screen.StationScreen;
import com.nurby.tinkerslegacy.item.MaterialShardItem;
import com.nurby.tinkerslegacy.item.PatternItem;
import com.nurby.tinkerslegacy.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.library.material.layer.MaterialLayer;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLItems;
import com.nurby.tinkerslegacy.registry.TLBlocks;
import com.nurby.tinkerslegacy.registry.TLRegistries;
import com.nurby.tinkerslegacy.registry.TLStationMenus;
import com.nurby.tinkerslegacy.registry.ToolParts;
import com.nurby.tinkerslegacy.menu.CraftingStationMenu;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;
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

                registration.addIngredientInfo(
                                TLBlocks.block(TLBlocks.Workstations.Type.CRAFTING_STATION),
                                Component.translatable("jei.tinkerslegacy.crafting_station"));

                var kit = ToolParts.part(ToolParts.Type.SHARPENING_KIT);
                for (var material : TLRegistries.MATERIALS) {
                        if (!material.available() || !material.parts().contains(kit.id())) {
                                continue;
                        }

                        ItemStack stack = new ItemStack(TLItems.Parts.item(ToolParts.Type.SHARPENING_KIT));
                        stack.set(TLDataComponents.MATERIALS, kit.statTypes().stream()
                                        .map(type -> new MaterialLayer(type, material.id())).toList());
                        registration.addItemStackInfo(
                                        stack,
                                        Component.translatable("jei.tinkerslegacy.sharpening_kit"));
                }
        }

        // Crafting
        @Override
        public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
                registration.addRecipeCatalysts(
                                RecipeTypes.CRAFTING,
                                TLBlocks.block(TLBlocks.Workstations.Type.CRAFTING_STATION));
        }

        @Override
        public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
                registration.addRecipeTransferHandler(new CraftingTransferInfo());
        }

        @Override
        public void registerGuiHandlers(IGuiHandlerRegistration registration) {
                registration.addRecipeClickArea(CraftingStationScreen.class, 88, 32, 28, 23, RecipeTypes.CRAFTING);
                registration.addGuiContainerHandler(CraftingStationScreen.class, new IGuiContainerHandler<>() {
                        @Override
                        public List<Rect2i> getGuiExtraAreas(CraftingStationScreen screen) {
                                return screen.getGuiExtraAreas();
                        }
                });
                registration.addGuiContainerHandler(StationScreen.class, new IGuiContainerHandler<>() {
                        @Override
                        public List<Rect2i> getGuiExtraAreas(StationScreen screen) {
                                return screen.getGuiExtraAreas();
                        }
                });
        }

        private static final class CraftingTransferInfo
                        implements IRecipeTransferInfo<CraftingStationMenu, RecipeHolder<CraftingRecipe>> {
                @Override
                public Class<CraftingStationMenu> getContainerClass() {
                        return CraftingStationMenu.class;
                }

                @Override
                public Optional<MenuType<CraftingStationMenu>> getMenuType() {
                        return Optional.of(TLStationMenus.CRAFTING_STATION.get());
                }

                @Override
                public RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
                        return RecipeTypes.CRAFTING;
                }

                @Override
                public boolean canHandle(CraftingStationMenu menu, RecipeHolder<CraftingRecipe> recipe) {
                        return true;
                }

                @Override
                public List<Slot> getRecipeSlots(CraftingStationMenu menu, RecipeHolder<CraftingRecipe> recipe) {
                        return List.copyOf(menu.slots.subList(1, 10));
                }

                @Override
                public List<Slot> getInventorySlots(CraftingStationMenu menu, RecipeHolder<CraftingRecipe> recipe) {
                        return List.copyOf(menu.slots.subList(10, menu.slots.size()));
                }
        }

        @Override
        public ResourceLocation getPluginUid() {
                return ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, "jei");
        }

        @Override
        public void registerItemSubtypes(ISubtypeRegistration registration) {
                MaterialSubtypeInterpreter interpreter = new MaterialSubtypeInterpreter();
                PatternSubtypeInterpreter patterns = new PatternSubtypeInterpreter();

                for (Item item : BuiltInRegistries.ITEM) {
                        if (item instanceof DynamicTool || TLItems.Parts.isPart(item)) {
                                registration.registerSubtypeInterpreter(item, interpreter);
                        } else if (item instanceof PatternItem) {
                                registration.registerSubtypeInterpreter(item, patterns);
                        }
                }
        }

        private static final class PatternSubtypeInterpreter implements ISubtypeInterpreter<ItemStack> {
                @Override
                public Object getSubtypeData(ItemStack stack, UidContext context) {
                        return stack.get(TLDataComponents.PART);
                }

                @Override
                public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
                        ResourceLocation part = stack.get(TLDataComponents.PART);
                        return part == null ? "" : part.toString();
                }
        }

        private static final class MaterialSubtypeInterpreter implements ISubtypeInterpreter<ItemStack> {
                @Override
                public Object getSubtypeData(ItemStack stack, UidContext context) {
                        if (stack.getItem() instanceof MaterialShardItem) {
                                return getLegacyStringSubtypeInfo(stack, context);
                        }
                        List<MaterialLayer> layers = stack.get(TLDataComponents.MATERIALS);

                        if (layers == null || layers.isEmpty()) {
                                return null;
                        }

                        return List.copyOf(layers);
                }

                @Override
                public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
                        if (stack.getItem() instanceof MaterialShardItem) {
                                ResourceLocation material = stack.get(TLDataComponents.MATERIAL_ID);
                                return material == null ? "" : material + ":"
                                                + stack.getOrDefault(TLDataComponents.MATERIAL_UNITS, 72);
                        }
                        List<MaterialLayer> layers = stack.get(TLDataComponents.MATERIALS);

                        if (layers == null || layers.isEmpty()) {
                                return "";
                        }

                        return layers.stream().map(layer -> layer.statType() + "=" + layer.material())
                                        .collect(Collectors.joining(";"));
                }
        }
}
