package com.nurby.tinkerslegacy.test;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.library.station.MaterialInputs;
import com.nurby.tinkerslegacy.library.station.StationRecipes;
import com.nurby.tinkerslegacy.library.station.StationOperations;
import com.nurby.tinkerslegacy.library.material.MaterialBuilder;
import com.nurby.tinkerslegacy.library.material.MaterialDefinition;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLMaterials;
import com.nurby.tinkerslegacy.registry.ToolStatTypes;
import com.nurby.tinkerslegacy.registry.TLItems;
import com.nurby.tinkerslegacy.registry.TLRegistries;
import com.nurby.tinkerslegacy.registry.TLStations;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.stream.Collectors;

import static com.nurby.tinkerslegacy.test.ToolAssemblyGameTests.id;

@GameTestHolder(TinkersLegacy.MODID)
@PrefixGameTestTemplate(false)
public final class MaterialShardGameTests {

        // Classification
        @GameTest(template = "empty")
        public static void shardsArePartsWithoutBecomingAssemblyShapes(GameTestHelper helper) {
                var item = TLItems.Parts.MATERIAL_SHARD.get();
                helper.assertTrue(TLItems.Parts.isPart(item), "Shards are classified as tool parts");
                helper.assertValueEqual(BuiltInRegistries.ITEM.getKey(item), id("material_shard"), "Saved item ID remains unchanged");
                helper.assertTrue(!TLRegistries.PARTS.containsKey(id("material_shard")), "A material shard has no assembly definition");
                helper.assertTrue(StationRecipes.shapes().contains(id("material_shard")), "Shards remain cuttable without an assembly definition");
                ItemStack stack = MaterialInputs.shard(id("wood"), 3);
                helper.assertTrue(!stack.has(TLDataComponents.PART) && !stack.has(TLDataComponents.MATERIALS),
                                "Shards do not invent tool-part stat roles");
                helper.assertValueEqual(stack.get(TLDataComponents.MATERIAL_ID), id("wood"), "Saved material component remains unchanged");
                ItemStack restored = ItemStack.parse(helper.getLevel().registryAccess(),
                                stack.save(helper.getLevel().registryAccess())).orElseThrow();
                helper.assertTrue(ItemStack.matches(stack, restored), "Existing shard data survives saving");
                helper.succeed();
        }

        // Crafting
        @GameTest(template = "empty")
        public static void shardStencilBuildsHalfUnitsAndReturnsAllExcess(GameTestHelper helper) {
                ItemStack blank = new ItemStack(TLItems.item(TLItems.Items.IntermediaryItems.Type.BLANK_PATTERN));
                var cut = StationRecipes.evaluate(TLStations.STENCIL_TABLE, List.of(blank),
                                StationOperations.CUT_PATTERN, id("material_shard"), "", 0);
                helper.assertTrue(cut.valid(), "A blank pattern can be cut into the original shard stencil");
                helper.assertValueEqual(cut.output().get(TLDataComponents.PART), id("material_shard"), "Shard stencil shape ID");
                helper.assertValueEqual(StationRecipes.partCost(id("material_shard")), 72, "Shard stencil cost is half a unit");

                var inputs = List.of(cut.output(), new ItemStack(Items.STICK), ItemStack.EMPTY, ItemStack.EMPTY);
                var built = StationRecipes.evaluate(TLStations.PART_BUILDER, inputs, StationOperations.BUILD_PART, null, "", 0);
                helper.assertTrue(built.valid(), "One stick supplies exactly one wood shard");
                helper.assertTrue(ItemStack.matches(built.output(), MaterialInputs.shard(id("wood"), 1)), "Material-backed wood shard output");
                helper.assertTrue(built.inputsAfter().get(1).isEmpty() && built.inputsAfter().get(3).isEmpty(),
                                "Exact half-unit input leaves no excess material");
                helper.assertTrue(ItemStack.matches(inputs.getFirst(), built.inputsAfter().getFirst()), "Shard stencil is reusable");
                helper.assertValueEqual(inputs.get(1).getCount(), 1, "Preview does not consume the player's stick");

                inputs = List.of(cut.output(), new ItemStack(Items.COBBLESTONE), ItemStack.EMPTY, ItemStack.EMPTY);
                built = StationRecipes.evaluate(TLStations.PART_BUILDER, inputs, StationOperations.BUILD_PART, null, "", 0);
                helper.assertTrue(built.valid(), "A stone unit splits into two half-unit shards");
                helper.assertTrue(ItemStack.matches(built.output(), MaterialInputs.shard(id("stone"), 1)), "One stone shard in primary output");
                helper.assertTrue(ItemStack.matches(built.inputsAfter().get(3), MaterialInputs.shard(id("stone"), 1)),
                                "The second stone shard remains in the remainder output");

                inputs = List.of(cut.output(), new ItemStack(Items.OAK_LOG), ItemStack.EMPTY, MaterialInputs.shard(id("wood"), 58));
                helper.assertTrue(!StationRecipes.evaluate(TLStations.PART_BUILDER, inputs,
                                StationOperations.BUILD_PART, null, "", 0).valid(), "Seven excess wood shards require enough remainder space");
                inputs = List.of(cut.output(), new ItemStack(Items.OAK_LOG), ItemStack.EMPTY, MaterialInputs.shard(id("wood"), 57));
                built = StationRecipes.evaluate(TLStations.PART_BUILDER, inputs, StationOperations.BUILD_PART, null, "", 0);
                helper.assertTrue(built.valid(), "All seven excess wood shards fit exactly");
                helper.assertValueEqual(built.inputsAfter().get(3).getCount(), 64, "The complete log value is preserved");
                inputs = List.of(cut.output(), new ItemStack(Items.IRON_INGOT), ItemStack.EMPTY, ItemStack.EMPTY);
                helper.assertTrue(StationRecipes.evaluate(TLStations.PART_BUILDER, inputs,
                                StationOperations.BUILD_PART, null, "", 0).valid(), "The original no-Smeltery fallback permits metal shards");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void onlyCraftableOrCastableHeadMaterialsHaveShards(GameTestHelper helper) {
                var expected = TLRegistries.MATERIALS.stream().filter(MaterialDefinition::hasShard)
                                .map(material -> material.id()).collect(Collectors.toSet());
                var variants = TLItems.Parts.MATERIAL_SHARD.get().variants();
                helper.assertValueEqual(variants.stream().map(stack -> stack.get(TLDataComponents.MATERIAL_ID))
                                .collect(Collectors.toSet()), expected, "Variants follow original head-stat and crafting eligibility");
                for (ItemStack shard : variants) {
                        var material = MaterialInputs.match(shard);
                        helper.assertTrue(material != null, "Registered shards are usable material");
                        helper.assertValueEqual(material.value(), 72, "Each shard remains half a material unit");
                        helper.assertValueEqual(shard.getCount(), 1, "Creative variant stack count");
                }
                helper.assertValueEqual(expected.size(), 27, "All 27 original head materials have shards");
                helper.assertTrue(!expected.contains(id("diamond")), "Legacy diamond is not a shard material");
                var shaftOnly = MaterialBuilder.material(id("shaft_only"))
                                .renderInfo(TLMaterials.WOOD.get().renderInfo())
                                .stats(TLMaterials.WOOD.get().stats().get(ToolStatTypes.ARROW_SHAFT.getId()))
                                .build();
                helper.assertTrue(shaftOnly.available() && !shaftOnly.hasShard(),
                                "A craftable shaft-only material does not become a shard");
                ItemStack legacy = TLItems.Parts.MATERIAL_SHARD.get().withMaterial(id("diamond"), 1);
                helper.assertTrue(MaterialInputs.match(legacy) == null, "Legacy diamond shards cannot become new material inputs");
                helper.assertTrue(MaterialInputs.shard(id("diamond"), 1).isEmpty(), "The shard factory rejects unsupported materials");
                helper.assertTrue(MaterialInputs.match(MaterialInputs.shard(id("netherrack"), 1)).craftable(), "Netherrack shards remain craftable");
                helper.assertTrue(MaterialInputs.match(MaterialInputs.shard(id("wood"), 1)).craftable(), "Wood shards remain craftable");
                helper.assertTrue(MaterialInputs.match(MaterialInputs.shard(id("stone"), 1)).craftable(), "Stone shards remain craftable");
                helper.assertTrue(MaterialInputs.match(MaterialInputs.shard(id("iron"), 1)).craftable(), "Metal shards support the original no-Smeltery fallback");
                helper.assertTrue(MaterialInputs.match(MaterialInputs.shard(id("missing_material"), 1)) == null,
                                "Unknown material IDs cannot masquerade as valid shards");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void shardVariantsAppearInPartsAndNotOrdinaryItems(GameTestHelper helper) {
                CreativeModeTab parts = BuiltInRegistries.CREATIVE_MODE_TAB.get(id("parts"));
                CreativeModeTab items = BuiltInRegistries.CREATIVE_MODE_TAB.get(id("items"));
                var parameters = new CreativeModeTab.ItemDisplayParameters(FeatureFlags.DEFAULT_FLAGS, false,
                                helper.getLevel().registryAccess());
                parts.buildContents(parameters);
                items.buildContents(parameters);
                for (ItemStack shard : TLItems.Parts.MATERIAL_SHARD.get().variants()) {
                        helper.assertTrue(parts.contains(shard), "Every material shard appears in the Parts tab");
                        helper.assertTrue(!items.contains(shard), "The ordinary Items tab excludes material shards");
                }
                helper.succeed();
        }
}
