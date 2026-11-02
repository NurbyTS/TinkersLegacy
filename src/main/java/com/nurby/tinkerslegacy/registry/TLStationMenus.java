package com.nurby.tinkerslegacy.registry;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.menu.StationMenu;
import com.nurby.tinkerslegacy.menu.CraftingStationMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class TLStationMenus {
        public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(
                        Registries.MENU, TinkersLegacy.MODID);

        public static final DeferredHolder<MenuType<?>, MenuType<StationMenu>> STATION = MENUS.register(
                        "station", () -> IMenuTypeExtension.create(StationMenu::new));

        public static final DeferredHolder<MenuType<?>, MenuType<CraftingStationMenu>> CRAFTING_STATION = MENUS.register(
                        "crafting_station", () -> IMenuTypeExtension.create(CraftingStationMenu::new));

        private TLStationMenus() {
        }

        // Registration
        public static void register(IEventBus modBus) {
                MENUS.register(modBus);
        }
}
