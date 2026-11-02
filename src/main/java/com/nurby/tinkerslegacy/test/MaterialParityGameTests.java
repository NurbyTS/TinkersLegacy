package com.nurby.tinkerslegacy.test;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.library.material.layer.MaterialLayer;
import com.nurby.tinkerslegacy.library.station.MaterialInputs;
import com.nurby.tinkerslegacy.library.station.StationOperations;
import com.nurby.tinkerslegacy.library.station.StationRecipes;
import com.nurby.tinkerslegacy.library.tool.ToolAssembly;
import com.nurby.tinkerslegacy.library.tool.ToolModifiers;
import com.nurby.tinkerslegacy.library.tool.ToolProgress;
import com.nurby.tinkerslegacy.library.tool.ToolState;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLItems;
import com.nurby.tinkerslegacy.registry.TLMaterials;
import com.nurby.tinkerslegacy.registry.TLRegistries;
import com.nurby.tinkerslegacy.registry.TLStations;
import com.nurby.tinkerslegacy.registry.TLTools;
import com.nurby.tinkerslegacy.registry.ToolParts;
import com.nurby.tinkerslegacy.registry.ToolStatTypes;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.nurby.tinkerslegacy.test.ToolAssemblyGameTests.*;

@GameTestHolder(TinkersLegacy.MODID)
@PrefixGameTestTemplate(false)
public final class MaterialParityGameTests {

        // Crafting
        @GameTest(template = "empty")
        public static void netherrackPartsAssembleAndRepairInTheClassicWorkflow(GameTestHelper helper) {
                helper.assertTrue(MaterialInputs.match(new ItemStack(Items.NETHERRACK)).craftable(),
                                "Original netherrack is a Part Builder material");
                var definition = TLTools.tool(TLTools.Type.PICKAXE);
                List<ItemStack> parts = new ArrayList<>();
                for (var requirement : definition.partRequirements()) {
                        ItemStack pattern = new ItemStack(TLItems.item(TLItems.Items.IntermediaryItems.Type.PATTERN));
                        pattern.set(TLDataComponents.PART, requirement.part());
                        int amount = StationRecipes.partCost(requirement.part()) / MaterialInputs.UNIT;
                        List<ItemStack> inputs = List.of(pattern, new ItemStack(Items.NETHERRACK, amount),
                                        ItemStack.EMPTY, ItemStack.EMPTY);
                        var built = StationRecipes.evaluate(TLStations.PART_BUILDER, inputs,
                                        StationOperations.BUILD_PART, null, "", 0);
                        helper.assertTrue(built.valid(), "Netherrack builds " + requirement.part());
                        helper.assertTrue(built.inputsAfter().get(1).isEmpty(), "The whole part cost is consumed");
                        helper.assertTrue(ItemStack.matches(pattern, built.inputsAfter().getFirst()), "Patterns remain reusable");
                        parts.add(built.output());
                }
                while (parts.size() < TLStations.TOOL_STATION.inputSlots()) {
                        parts.add(ItemStack.EMPTY);
                }
                var assembled = StationRecipes.evaluate(TLStations.TOOL_STATION, parts,
                                StationOperations.ASSEMBLE, definition.id(), "", 0);
                helper.assertTrue(assembled.valid(), "The Tool Station assembles netherrack parts");
                ItemStack tool = assembled.output();
                helper.assertTrue(tool.get(TLDataComponents.MATERIALS).stream().allMatch(layer -> layer.material().equals(id("netherrack"))),
                                "All physical parts retain the netherrack material");
                helper.assertTrue(tool.getMaxDamage() < 200, "Netherrack no longer inherits the diamond placeholder durability");
                ToolState.hurt(tool, 10);
                var repaired = StationRecipes.evaluate(TLStations.TOOL_STATION,
                                maintenance(tool, new ItemStack(Items.NETHERRACK)), StationOperations.REPAIR, null, "", 0);
                helper.assertTrue(repaired.valid(), "Raw netherrack repairs a matching head in the Tool Station");
                helper.assertValueEqual(ToolState.damage(repaired.output()), 0, "The netherrack head is repaired");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void diamondIsAModifierInsteadOfANewToolMaterial(GameTestHelper helper) {
                helper.assertTrue(!TLMaterials.DIAMOND.get().available(), "The legacy material is not a new crafting choice");
                helper.assertTrue(tool(TLTools.Type.PICKAXE, id("diamond")).isEmpty(), "Diamond parts cannot assemble a new tool");
                helper.assertTrue(!ToolAssembly.matches(part(ToolParts.Type.PICKAXE_HEAD, id("diamond")),
                                TLTools.tool(TLTools.Type.PICKAXE).partRequirements().getFirst()), "Diamond replacement heads are rejected");
                ItemStack ordinary = tool(TLTools.Type.PICKAXE, id("wood"));
                ItemStack modified = ToolModifiers.applyIngredient(ordinary, new ItemStack(Items.DIAMOND));
                helper.assertTrue(!modified.isEmpty(), "Diamond remains a working modifier");
                helper.assertValueEqual(modified.get(TLDataComponents.MODIFIERS).getFirst().id(), ToolModifiers.DIAMOND,
                                "Diamond installs its original modifier");
                var parameters = new CreativeModeTab.ItemDisplayParameters(FeatureFlags.DEFAULT_FLAGS, false,
                                helper.getLevel().registryAccess());
                for (String name : List.of("parts", "tools")) {
                        CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(id(name));
                        tab.buildContents(parameters);
                        for (ItemStack stack : tab.getDisplayItems()) {
                                helper.assertTrue(stack.getOrDefault(TLDataComponents.MATERIALS, List.<MaterialLayer>of())
                                                .stream().noneMatch(layer -> layer.material().equals(id("diamond"))),
                                                "Diamond material variants stay out of the " + name + " tab");
                                helper.assertTrue(!id("diamond").equals(stack.get(TLDataComponents.MATERIAL_ID)),
                                                "Diamond shards stay out of creative tabs");
                        }
                }
                helper.succeed();
        }

        // Saved Tools
        @GameTest(template = "empty")
        public static void existingDiamondToolsKeepTheirSavedStateAndAcceptRealParts(GameTestHelper helper) {
                ItemStack existing = legacyTool(TLTools.Type.PICKAXE, id("diamond"));
                ToolProgress progress = new ToolProgress(2, 123, 2);
                existing.set(TLDataComponents.PROGRESS, progress);
                ToolState.hurt(existing, 10);
                ItemStack loaded = ItemStack.parse(helper.getLevel().registryAccess(),
                                existing.save(helper.getLevel().registryAccess())).orElseThrow();
                ((DynamicTool) loaded.getItem()).calculateStats(loaded);
                helper.assertValueEqual(loaded.get(TLDataComponents.MATERIALS), existing.get(TLDataComponents.MATERIALS),
                                "Legacy diamond material layers remain readable");
                helper.assertValueEqual(loaded.get(TLDataComponents.PROGRESS), progress, "Legacy progress survives loading");
                helper.assertValueEqual(ToolState.damage(loaded), 10, "Legacy damage survives recalculation");
                var replaced = StationRecipes.evaluate(TLStations.TOOL_STATION,
                                maintenance(loaded, part(ToolParts.Type.PICKAXE_HEAD, id("iron"))),
                                StationOperations.REPLACE, null, "", 0);
                helper.assertTrue(replaced.valid(), "An existing diamond tool can adopt a supported replacement part");
                helper.assertValueEqual(replaced.output().get(TLDataComponents.MATERIALS).getFirst().material(), id("iron"),
                                "The new head uses iron");
                helper.assertValueEqual(replaced.output().get(TLDataComponents.MATERIALS).get(1).material(), id("diamond"),
                                "Unchanged legacy layers are retained");
                helper.assertValueEqual(replaced.output().get(TLDataComponents.PROGRESS), progress, "Replacement preserves earned progress");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void correctedMaterialDurabilityNeverErasesExistingWear(GameTestHelper helper) {
                ItemStack existing = tool(TLTools.Type.PICKAXE, id("netherrack"));
                existing.set(DataComponents.MAX_DAMAGE, 1000);
                existing.setDamageValue(500);
                ((DynamicTool) existing.getItem()).calculateStats(existing);
                helper.assertTrue(ToolState.broken(existing), "A tool worn beyond its corrected capacity becomes repairable and broken");
                helper.assertValueEqual(ToolState.damage(existing), 500, "Recalculation preserves all existing wear");
                helper.assertTrue(existing.getDamageValue() < existing.getMaxDamage(), "Vanilla cannot destroy the stored tool");
                ToolState.repair(existing, 400);
                helper.assertTrue(!ToolState.broken(existing), "Repair restores use once wear fits the corrected durability");
                helper.assertValueEqual(ToolState.damage(existing), 100, "Only the actual repair amount removes wear");
                helper.succeed();
        }

        // Material Catalog
        @GameTest(template = "empty")
        public static void theOriginalCatalogKeepsPartRolesAndCraftingMethods(GameTestHelper helper) {
                List<String> heads = List.of("wood", "stone", "flint", "cactus", "bone", "obsidian",
                                "prismarine", "endstone", "paper", "sponge", "firewood", "knightslime", "slime",
                                "blueslime", "magmaslime", "iron", "pigiron", "netherrack", "ardite", "cobalt",
                                "manyullyn", "copper", "bronze", "lead", "silver", "electrum", "steel");
                Set<String> metals = Set.of("knightslime", "iron", "pigiron", "ardite", "cobalt", "manyullyn",
                                "copper", "bronze", "lead", "silver", "electrum", "steel");
                for (String name : heads) {
                        var material = TLRegistries.MATERIALS.get(id(name));
                        helper.assertTrue(material != null, "The original material is registered: " + name);
                        Set<ResourceLocation> expected = new HashSet<>(Set.of(
                                        ToolStatTypes.HEAD.getId(), ToolStatTypes.HANDLE.getId(),
                                        ToolStatTypes.EXTRA.getId(), ToolStatTypes.BOW.getId()));
                        if (name.equals("wood") || name.equals("bone")) {
                                expected.add(ToolStatTypes.ARROW_SHAFT.getId());
                        }
                        helper.assertValueEqual(material.stats().keySet(), expected, "Original supported roles for " + name);
                        helper.assertTrue(material.available() && material.hasShard(), "Head materials keep their shards: " + name);
                        helper.assertValueEqual(material.recipeInfo().craftable(), !metals.contains(name) || name.equals("knightslime"),
                                        "Original Part Builder availability for " + name);
                        helper.assertValueEqual(material.recipeInfo().castable(), metals.contains(name) || name.equals("obsidian"),
                                        "Original casting availability for " + name);
                }
                helper.assertTrue(TLMaterials.OBSIDIAN.get().recipeInfo().craftable() && TLMaterials.OBSIDIAN.get().recipeInfo().castable(),
                                "Obsidian supports both original crafting methods");
                helper.assertTrue(TLMaterials.KNIGHTSLIME.get().recipeInfo().craftable() && TLMaterials.KNIGHTSLIME.get().recipeInfo().castable(),
                                "Knightslime supports both original crafting methods");
                helper.assertTrue(!TLRegistries.MATERIALS.containsKey(id("unstable")),
                                "The original statless, unregistered unstable stub is not invented as a usable material");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void originalMaterialStatsReplaceVanillaPlaceholders(GameTestHelper helper) {
                var stone = TLMaterials.STONE.get();
                helper.assertValueEqual(stone.color(), 0x999999, "Original stone color");
                helper.assertValueEqual(stone.stats().get(ToolStatTypes.HEAD.getId()).statMap().getInt("durability"), 120,
                                "Original stone head durability");
                helper.assertValueEqual(stone.stats().get(ToolStatTypes.HEAD.getId()).statMap().getFloat("mining_speed"), 4.0F,
                                "Original stone mining speed");
                helper.assertValueEqual(stone.stats().get(ToolStatTypes.HANDLE.getId()).statMap().getFloat("modifier"), 0.5F,
                                "Original stone handle multiplier");
                helper.assertValueEqual(stone.stats().get(ToolStatTypes.HANDLE.getId()).statMap().getInt("durability"), -50,
                                "Negative handle durability is preserved");
                var iron = TLMaterials.IRON.get();
                helper.assertValueEqual(iron.color(), 0xCACACA, "Original iron color");
                helper.assertValueEqual(iron.stats().get(ToolStatTypes.HEAD.getId()).statMap().getInt("durability"), 204,
                                "Iron uses Tinkers durability instead of vanilla iron durability");
                helper.assertValueEqual(iron.stats().get(ToolStatTypes.HEAD.getId()).statMap().getFloat("attack_damage"), 4.0F,
                                "Original iron head damage");
                helper.assertValueEqual(iron.stats().get(ToolStatTypes.HANDLE.getId()).statMap().getFloat("modifier"), 0.85F,
                                "Original iron handle multiplier");
                helper.assertValueEqual(iron.stats().get(ToolStatTypes.HANDLE.getId()).statMap().getInt("durability"), 60,
                                "Original iron handle durability");
                helper.assertValueEqual(TLMaterials.OBSIDIAN.get().stats().get(ToolStatTypes.HEAD.getId()).statMap().getInt("harvest_level"), 4,
                                "Obsidian heads retain the original cobalt mining level");
                helper.assertValueEqual(TLMaterials.COBALT.get().stats().get(ToolStatTypes.HEAD.getId()).statMap().getFloat("mining_speed"), 12.0F,
                                "Original cobalt mining speed");
                helper.assertValueEqual(TLMaterials.MANYULLYN.get().stats().get(ToolStatTypes.HEAD.getId()).statMap().getFloat("attack_damage"), 8.72F,
                                "Original manyullyn head damage");
                helper.assertValueEqual(TLMaterials.BRONZE.get().stats().get(ToolStatTypes.EXTRA.getId()).statMap().getInt("durability"), 80,
                                "Compatibility materials also retain original stats");
                helper.succeed();
        }

        // Traits
        @GameTest(template = "empty")
        public static void roleTraitsReplaceDefaultsInsteadOfAddingBoth(GameTestHelper helper) {
                helper.assertValueEqual(TLMaterials.STONE.get().traits(ToolStatTypes.HEAD.getId()), List.of(id("cheapskate")),
                                "Stone heads do not also gain the fallback Cheap trait");
                helper.assertValueEqual(TLMaterials.STONE.get().traits(ToolStatTypes.HANDLE.get()), List.of(id("cheap")),
                                "Handles receive the material's default traits");
                helper.assertValueEqual(TLMaterials.FLINT.get().traits(ToolStatTypes.HEAD.getId()), List.of(id("crude2")),
                                "Flint heads retain the original second-level trait identifier");
                helper.assertValueEqual(TLMaterials.FLINT.get().traits(ToolStatTypes.EXTRA.getId()), List.of(id("crude1")),
                                "Flint bindings use the first-level trait");
                helper.assertValueEqual(TLMaterials.IRON.get().traits(ToolStatTypes.HEAD.getId()), List.of(id("magnetic2")),
                                "Iron heads use Magnetic II without inheriting Magnetic I");
                helper.assertValueEqual(TLMaterials.IRON.get().traits(ToolStatTypes.HANDLE.getId()), List.of(id("magnetic1")),
                                "Iron handles use Magnetic I");
                helper.assertValueEqual(TLMaterials.PAPER.get().traits(ToolStatTypes.HEAD.getId()), List.of(id("writable2")),
                                "Paper keeps the distinct Writable head identity");
                helper.assertValueEqual(TLMaterials.PAPER.get().traits(ToolStatTypes.EXTRA.getId()), List.of(id("writable1")),
                                "Paper extras use the other Writable identity");
                helper.assertValueEqual(TLMaterials.BONE.get().traits(ToolStatTypes.ARROW_SHAFT.getId()), List.of(id("splitting")),
                                "Bone shafts use Splitting rather than the default Fractured trait");
                helper.assertValueEqual(TLMaterials.ENDSTONE.get().traits(ToolStatTypes.PROJECTILE.getId()), List.of(id("enderference")),
                                "Projectile overrides remain available independently of the physical head's stats");
                helper.assertValueEqual(TLMaterials.PRISMARINE.get().traits(ToolStatTypes.HEAD.getId()),
                                List.of(id("jagged"), id("aquadynamic")), "Multiple head traits preserve acquisition order");
                helper.assertValueEqual(TLMaterials.WOOD.get().traits(ToolStatTypes.BOW.getId()), List.of(id("ecological")),
                                "A role without its own override receives the default trait");
                helper.assertTrue(TLMaterials.STRING.get().traits(ToolStatTypes.BOWSTRING.getId()).isEmpty(),
                                "Bowstring materials do not gain invented traits");
                helper.succeed();
        }

        // Inputs
        private static List<ItemStack> maintenance(ItemStack tool, ItemStack ingredient) {
                List<ItemStack> inputs = new ArrayList<>(List.of(tool, ingredient));
                while (inputs.size() < TLStations.TOOL_STATION.inputSlots()) {
                        inputs.add(ItemStack.EMPTY);
                }
                return inputs;
        }
}
