package com.nurby.tinkerslegacy.test;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.block.entity.StationBlockEntity;
import com.nurby.tinkerslegacy.library.station.StationOperations;
import com.nurby.tinkerslegacy.library.station.StationRecipes;
import com.nurby.tinkerslegacy.menu.StationMenu;
import com.nurby.tinkerslegacy.recipe.StationRecipe;
import com.nurby.tinkerslegacy.registry.TLBlocks;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLItems;
import com.nurby.tinkerslegacy.registry.TLStations;
import com.nurby.tinkerslegacy.registry.TLTools;
import com.nurby.tinkerslegacy.registry.ToolParts;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.nurby.tinkerslegacy.test.ToolAssemblyGameTests.*;

@GameTestHolder(TinkersLegacy.MODID)
@PrefixGameTestTemplate(false)
public final class StationParityGameTests {

        // Replacement
        @GameTest(template = "empty")
        public static void repeatedPartsFollowTheirMaintenanceSlot(GameTestHelper helper) {
                for (int target = 0; target < 4; target++) {
                        ItemStack original = tool(TLTools.Type.SHURIKEN, id("wood"));
                        List<ItemStack> inputs = new ArrayList<>(List.of(original, ItemStack.EMPTY,
                                        ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY));
                        inputs.set(target + 1, part(ToolParts.Type.KNIFE_BLADE, id("stone")));
                        var result = StationRecipes.evaluate(TLStations.TOOL_FORGE, inputs,
                                        StationOperations.MAINTAIN, null, "", 0);
                        helper.assertTrue(result.valid(), "Each of the four repeated blade positions accepts replacement");
                        var layers = result.output().get(TLDataComponents.MATERIALS);
                        helper.assertValueEqual(layers.size(), 8, "Four physical blades each supply HEAD and EXTRA roles");
                        for (int index = 0; index < 8; index++) {
                                int blade = index / 2;
                                helper.assertValueEqual(layers.get(index).material(), id(blade == target ? "stone" : "wood"),
                                                "Maintenance position " + target + " selects both roles of physical blade " + blade);
                                helper.assertValueEqual(layers.get(index).statType(), index % 2 == 0
                                                ? com.nurby.tinkerslegacy.registry.ToolStatTypes.HEAD.getId()
                                                : com.nurby.tinkerslegacy.registry.ToolStatTypes.EXTRA.getId(),
                                                "Replacement preserves the existing material role order");
                        }
                        helper.assertTrue(result.inputsAfter().get(target + 1).isEmpty(),
                                        "Two stat roles consume exactly one physical blade");
                        helper.assertValueEqual(inputs.get(target + 1).getCount(), 1, "Preview does not consume the original part");
                }
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void multipleRepeatedReplacementsKeepDistinctTargets(GameTestHelper helper) {
                List<ItemStack> inputs = List.of(tool(TLTools.Type.SHURIKEN, id("wood")),
                                part(ToolParts.Type.KNIFE_BLADE, id("stone")), ItemStack.EMPTY,
                                part(ToolParts.Type.KNIFE_BLADE, id("iron")), ItemStack.EMPTY, ItemStack.EMPTY);
                var result = StationRecipes.evaluate(TLStations.TOOL_FORGE, inputs,
                                StationOperations.MAINTAIN, null, "", 0);
                helper.assertTrue(result.valid(), "Different repeated blades can be replaced in one transaction");
                var layers = result.output().get(TLDataComponents.MATERIALS);
                helper.assertValueEqual(layers.stream().map(layer -> layer.material()).toList(),
                                List.of(id("stone"), id("stone"), id("wood"), id("wood"),
                                                id("iron"), id("iron"), id("wood"), id("wood")),
                                "First and third physical blades replace their HEAD and EXTRA roles; other blades are retained");
                helper.assertTrue(result.inputsAfter().get(1).isEmpty() && result.inputsAfter().get(3).isEmpty(),
                                "Four changed stat roles consume two physical parts");
                helper.succeed();
        }

        // Shared Menus
        @GameTest(template = "empty")
        public static void simultaneousAndJoiningViewersShareSelectionAndNames(GameTestHelper helper) {
                StationBlockEntity station = station(helper, TLBlocks.Workstations.Type.TOOL_STATION);
                List<ServerPlayer> players = new ArrayList<>();
                try {
                        ServerPlayer first = player(helper, station, players);
                        ServerPlayer second = player(helper, station, players);
                        StationMenu firstMenu = menu(first, station);
                        StationMenu secondMenu = menu(second, station);
                        firstMenu.select(StationOperations.ASSEMBLE, id("shovel"), "Shared shovel", 0);
                        helper.assertValueEqual(secondMenu.selectionState(), firstMenu.selectionState(),
                                        "Connected viewers immediately receive the same layout, operation and name");
                        ServerPlayer third = player(helper, station, players);
                        StationMenu joining = menu(third, station);
                        helper.assertValueEqual(joining.selectionState(), firstMenu.selectionState(), "New viewers inherit shared selection");
                        secondMenu.select(StationOperations.MAINTAIN, null, "Shared maintenance", 2);
                        helper.assertValueEqual(joining.selectionState(), secondMenu.selectionState(), "Changes flow from either viewer");
                        helper.assertValueEqual(firstMenu.selectionState(), secondMenu.selectionState(), "First viewer follows the same authoritative state");

                } finally {
                        cleanup(helper, players);
                }
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void stencilSelectionIsSharedBetweenViewers(GameTestHelper helper) {
                StationBlockEntity station = station(helper, TLBlocks.Workstations.Type.STENCIL_TABLE);
                List<ServerPlayer> players = new ArrayList<>();
                try {
                        ServerPlayer first = player(helper, station, players);
                        ServerPlayer second = player(helper, station, players);
                        StationMenu firstMenu = menu(first, station);
                        StationMenu secondMenu = menu(second, station);
                        firstMenu.select(StationOperations.CUT_PATTERN, ToolParts.get(ToolParts.Type.AXE_HEAD).getId(), "", 0);
                        helper.assertValueEqual(secondMenu.selection(), firstMenu.selection(), "Both stencil menus use the same physical pattern");

                } finally {
                        cleanup(helper, players);
                }
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void staleSelectionEchoCannotEraseNewerTyping(GameTestHelper helper) {
                StationBlockEntity station = station(helper, TLBlocks.Workstations.Type.TOOL_STATION);
                List<ServerPlayer> players = new ArrayList<>();
                try {
                        ServerPlayer player = player(helper, station, players);
                        StationMenu menu = menu(player, station);
                        menu.select(StationOperations.ASSEMBLE, id("pickaxe"), "Newer name", 0);
                        long older = menu.nextSelectionRequest();
                        long current = menu.nextSelectionRequest();
                        menu.acceptStatus(net.minecraft.network.chat.Component.empty(),
                                        new com.nurby.tinkerslegacy.library.station.StationSelection(
                                                        StationOperations.ASSEMBLE, id("pickaxe"), "Older", 0), older);
                        helper.assertValueEqual(menu.name(), "Newer name", "Earlier server echoes do not erase pending text");
                        menu.acceptStatus(net.minecraft.network.chat.Component.empty(),
                                        new com.nurby.tinkerslegacy.library.station.StationSelection(
                                                        StationOperations.ASSEMBLE, id("pickaxe"), "Shared latest name", 0), current);
                        helper.assertValueEqual(menu.name(), "Shared latest name", "Acknowledged shared changes still reach the screen");

                } finally {
                        cleanup(helper, players);
                }
                helper.succeed();
        }

        // Appearance
        @GameTest(template = "empty")
        public static void recipesRetainPlanksLogsAndForgeMetal(GameTestHelper helper) {
                for (var entry : Map.of(TLBlocks.Workstations.Type.STENCIL_TABLE, Blocks.BIRCH_PLANKS,
                                TLBlocks.Workstations.Type.PART_BUILDER, Blocks.ACACIA_LOG,
                                TLBlocks.Workstations.Type.TOOL_FORGE, Blocks.GOLD_BLOCK).entrySet()) {
                        var pattern = TLItems.item(TLItems.Items.IntermediaryItems.Type.BLANK_PATTERN);
                        Ingredient base = Ingredient.of(entry.getValue());
                        ShapedRecipe shaped = new ShapedRecipe("", CraftingBookCategory.MISC,
                                        ShapedRecipePattern.of(Map.of('P', Ingredient.of(pattern), 'B', base), List.of("P", "B")),
                                        new ItemStack(TLBlocks.block(entry.getKey())));
                        StationRecipe recipe = new StationRecipe(shaped, base);
                        CraftingInput input = CraftingInput.of(1, 2, List.of(new ItemStack(pattern), new ItemStack(entry.getValue())));
                        helper.assertTrue(recipe.matches(input, helper.getLevel()), "Material recipe preserves ordinary shaped matching");
                        ItemStack output = recipe.assemble(input, helper.getLevel().registryAccess());
                        helper.assertValueEqual(output.get(TLDataComponents.STATION_BASE), BuiltInRegistries.BLOCK.getKey(entry.getValue()),
                                        "Output records the exact crafting block");
                        helper.assertValueEqual(recipe.getResultItem(helper.getLevel().registryAccess()).get(TLDataComponents.STATION_BASE),
                                        BuiltInRegistries.BLOCK.getKey(entry.getValue()), "A fixed-material recipe preview shows the crafted appearance");
                        helper.assertTrue(!shaped.getResultItem(helper.getLevel().registryAccess()).has(TLDataComponents.STATION_BASE),
                                        "Crafting and preview queries do not mutate the shared recipe output");
                }
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void appearanceSurvivesPlacementSavePickAndLoot(GameTestHelper helper) {
                StationBlockEntity station = station(helper, TLBlocks.Workstations.Type.PART_BUILDER);
                var registry = helper.getLevel().registryAccess();
                ResourceLocation base = BuiltInRegistries.BLOCK.getKey(Blocks.BIRCH_LOG);
                ItemStack stack = new ItemStack(station.getBlockState().getBlock());
                stack.set(TLDataComponents.STATION_BASE, base);
                station.applyComponents(stack.getComponents(), stack.getComponentsPatch());
                helper.assertValueEqual(station.base(), base, "BlockItem component application sets the placed material");
                StationBlockEntity loaded = new StationBlockEntity(station.getBlockPos(), station.getBlockState());
                loaded.loadWithComponents(station.saveWithFullMetadata(registry), registry);
                helper.assertValueEqual(loaded.base(), base, "Save/reload retains station material");
                helper.assertValueEqual(loaded.collectComponents().get(TLDataComponents.STATION_BASE.get()), base,
                                "Block entity exposes its material to copy-components loot");
                ItemStack picked = station.getBlockState().getBlock().getCloneItemStack(helper.getLevel(), station.getBlockPos(), station.getBlockState());
                helper.assertValueEqual(picked.get(TLDataComponents.STATION_BASE), base, "Pick block retains appearance");
                var drops = Block.getDrops(station.getBlockState(), helper.getLevel(), station.getBlockPos(), station);
                helper.assertValueEqual(drops.size(), 1, "Breaking produces one station block");
                helper.assertValueEqual(drops.getFirst().get(TLDataComponents.STATION_BASE), base, "Loot retains appearance");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void oldAndInvalidAppearanceUseSafeDefaults(GameTestHelper helper) {
                StationBlockEntity station = station(helper, TLBlocks.Workstations.Type.PART_BUILDER);
                helper.assertValueEqual(station.base(), BuiltInRegistries.BLOCK.getKey(Blocks.OAK_LOG), "Old part builders use oak logs");
                station.setBase(id("missing_block"));
                helper.assertValueEqual(station.base(), BuiltInRegistries.BLOCK.getKey(Blocks.OAK_LOG), "Missing blocks fall back safely");
                station.setBase(BuiltInRegistries.BLOCK.getKey(station.getBlockState().getBlock()));
                helper.assertValueEqual(station.base(), BuiltInRegistries.BLOCK.getKey(Blocks.OAK_LOG), "Station bases cannot recursively render another station");
                helper.succeed();
        }

        // Setup
        private static StationBlockEntity station(GameTestHelper helper, TLBlocks.Workstations.Type type) {
                BlockPos pos = new BlockPos(2, 1, 2);
                helper.setBlock(pos, TLBlocks.block(type));
                return (StationBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        }

        private static ServerPlayer player(GameTestHelper helper, StationBlockEntity station, List<ServerPlayer> players) {
                ServerPlayer player = helper.makeMockServerPlayerInLevel();
                players.add(player);
                net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(player.connection.getConnection());
                player.setPos(station.getBlockPos().getX() + 0.5, station.getBlockPos().getY() + 1,
                                station.getBlockPos().getZ() + 0.5);
                return player;
        }

        private static StationMenu menu(ServerPlayer player, StationBlockEntity station) {
                StationMenu menu = new StationMenu(1, player.getInventory(), station);
                player.containerMenu = menu;
                return menu;
        }

        private static void cleanup(GameTestHelper helper, List<ServerPlayer> players) {
                for (ServerPlayer player : players) {
                        try {
                                player.closeContainer();
                        } finally {
                                player.containerMenu = player.inventoryMenu;
                                helper.getLevel().getServer().getPlayerList().remove(player);
                                player.discard();
                                if (player.connection.getConnection().channel() instanceof io.netty.channel.embedded.EmbeddedChannel channel) {
                                        channel.finishAndReleaseAll();
                                }
                        }
                }
        }
}
