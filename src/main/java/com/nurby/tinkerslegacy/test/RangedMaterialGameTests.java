package com.nurby.tinkerslegacy.test;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.library.station.MaterialInputs;
import com.nurby.tinkerslegacy.library.station.StationOperations;
import com.nurby.tinkerslegacy.library.station.StationRecipes;
import com.nurby.tinkerslegacy.library.station.StationResult;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLItems;
import com.nurby.tinkerslegacy.registry.TLMaterials;
import com.nurby.tinkerslegacy.registry.TLStations;
import com.nurby.tinkerslegacy.registry.ToolParts;
import com.nurby.tinkerslegacy.registry.ToolStatTypes;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Set;

import static com.nurby.tinkerslegacy.test.ToolAssemblyGameTests.id;

@GameTestHolder(TinkersLegacy.MODID)
@PrefixGameTestTemplate(false)
public final class RangedMaterialGameTests {

        // Crafting
        @GameTest(template = "empty")
        public static void bowstringsAndFletchingsUseTheirOriginalMaterialsAndCosts(GameTestHelper helper) {
                for (var item : List.of(Items.STRING, Items.VINE)) {
                        var result = build(ToolParts.Type.BOW_STRING, new ItemStack(item));
                        helper.assertTrue(result.valid(), "One string or vine crafts a reusable-pattern bowstring");
                        helper.assertTrue(result.inputsAfter().get(1).isEmpty(), "A bowstring consumes one whole unit");
                        helper.assertValueEqual(result.output().get(TLDataComponents.MATERIALS).getFirst().statType(),
                                        ToolStatTypes.BOWSTRING.getId(), "Bowstrings carry their own stats, not extra-part stats");
                }
                var feather = build(ToolParts.Type.FLETCHING, new ItemStack(Items.FEATHER, 2));
                helper.assertTrue(feather.valid(), "Two feathers craft a fletching");
                helper.assertTrue(feather.inputsAfter().get(1).isEmpty(), "Fletching costs two whole units");
                var leaves = build(ToolParts.Type.FLETCHING, new ItemStack(Items.OAK_LEAVES, 4));
                helper.assertTrue(leaves.valid(), "Four half-unit leaves craft a fletching");
                helper.assertTrue(leaves.inputsAfter().get(1).isEmpty(), "Leaf fletching consumes four leaves");
                helper.assertValueEqual(leaves.output().get(TLDataComponents.MATERIALS).getFirst().material(),
                                id("leaf"), "Fletching retains the leaf material");
                helper.assertTrue(!build(ToolParts.Type.FLETCHING, new ItemStack(Items.FEATHER)).valid(),
                                "One feather cannot pay for a complete fletching");
                helper.assertTrue(!build(ToolParts.Type.BOW_STRING, new ItemStack(Items.OAK_PLANKS)).valid(),
                                "Wood is not a bowstring material");
                helper.assertTrue(!build(ToolParts.Type.PICKAXE_HEAD, new ItemStack(Items.FEATHER, 2)).valid(),
                                "Fletching materials cannot become tool heads");
                helper.succeed();
        }

        // Stats
        @GameTest(template = "empty")
        public static void originalRangedStatsDoNotInventSpecialMaterialShards(GameTestHelper helper) {
                for (var material : List.of(TLMaterials.STRING, TLMaterials.VINE, TLMaterials.SLIMEVINE_BLUE, TLMaterials.SLIMEVINE_PURPLE,
                                TLMaterials.BLAZE, TLMaterials.REED, TLMaterials.ICE, TLMaterials.ENDROD,
                                TLMaterials.FEATHER, TLMaterials.LEAF, TLMaterials.SLIMELEAF_BLUE,
                                TLMaterials.SLIMELEAF_ORANGE, TLMaterials.SLIMELEAF_PURPLE)) {
                        helper.assertTrue(material.get().available(), "Special ranged materials are craftable");
                        helper.assertTrue(!material.get().hasShard(), "Special ranged-only materials never gain head-material shards");
                        helper.assertTrue(MaterialInputs.shard(material.getId(), 1).isEmpty(), "The shard factory rejects special materials");
                }
                var wood = TLMaterials.WOOD.get().stats().get(ToolStatTypes.BOW.getId()).statMap();
                helper.assertValueEqual(wood.getFloat("draw_speed"), 1.0F, "Original wooden bow draw speed");
                helper.assertValueEqual(wood.getFloat("range"), 1.0F, "Original wooden bow range");
                helper.assertValueEqual(TLMaterials.WOOD.get().stats().get(ToolStatTypes.ARROW_SHAFT.getId()).statMap()
                                .getInt("bonus_ammo"), 0, "Original wooden shafts do not grant bonus ammunition");
                for (var material : List.of(TLMaterials.STONE, TLMaterials.NETHERRACK)) {
                        var bow = material.get().stats().get(ToolStatTypes.BOW.getId()).statMap();
                        helper.assertValueEqual(bow.getFloat("draw_speed"), 0.2F, "Original stone-like draw speed");
                        helper.assertValueEqual(bow.getFloat("range"), 0.4F, "Original stone-like range");
                        helper.assertValueEqual(bow.getFloat("bonus_damage"), -1.0F, "Original stone-like bonus damage");
                }
                var leaf = TLMaterials.LEAF.get().stats().get(ToolStatTypes.FLETCHING.getId()).statMap();
                helper.assertValueEqual(leaf.getFloat("accuracy"), 0.5F, "Original leaf accuracy");
                helper.assertValueEqual(leaf.getFloat("modifier"), 1.5F, "Original leaf ammunition modifier");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void specialRangedMaterialsKeepOriginalStatsAndRestrictedRoles(GameTestHelper helper) {
                for (var material : List.of(TLMaterials.STRING, TLMaterials.VINE,
                                TLMaterials.SLIMEVINE_BLUE, TLMaterials.SLIMEVINE_PURPLE)) {
                        helper.assertValueEqual(material.get().stats().keySet(), Set.of(ToolStatTypes.BOWSTRING.getId()),
                                        "Original bowstrings only provide string stats");
                        helper.assertValueEqual(material.get().stats().get(ToolStatTypes.BOWSTRING.getId()).statMap().getFloat("modifier"),
                                        1.0F, "Original bowstring modifier");
                }
                for (var material : List.of(TLMaterials.BLAZE, TLMaterials.REED, TLMaterials.ICE, TLMaterials.ENDROD)) {
                        helper.assertValueEqual(material.get().stats().keySet(), Set.of(ToolStatTypes.ARROW_SHAFT.getId()),
                                        "Special shaft materials cannot be turned into tool heads");
                }
                var bone = TLMaterials.BONE.get().stats().get(ToolStatTypes.ARROW_SHAFT.getId()).statMap();
                helper.assertValueEqual(bone.getFloat("modifier"), 0.9F, "Original bone shaft ammunition multiplier");
                helper.assertValueEqual(bone.getInt("bonus_ammo"), 5, "Original bone shaft bonus ammunition");
                var reed = TLMaterials.REED.get().stats().get(ToolStatTypes.ARROW_SHAFT.getId()).statMap();
                helper.assertValueEqual(reed.getFloat("modifier"), 1.5F, "Original reed shaft ammunition multiplier");
                helper.assertValueEqual(reed.getInt("bonus_ammo"), 20, "Original reed shaft bonus ammunition");
                for (var material : List.of(TLMaterials.SLIMELEAF_BLUE, TLMaterials.SLIMELEAF_ORANGE, TLMaterials.SLIMELEAF_PURPLE)) {
                        helper.assertValueEqual(material.get().stats().keySet(), Set.of(ToolStatTypes.FLETCHING.getId()),
                                        "Slime leaves only provide fletching stats");
                        var stats = material.get().stats().get(ToolStatTypes.FLETCHING.getId()).statMap();
                        helper.assertValueEqual(stats.getFloat("accuracy"), 0.8F, "Original slime leaf accuracy");
                        helper.assertValueEqual(stats.getFloat("modifier"), 1.25F, "Original slime leaf ammunition multiplier");
                }
                helper.assertValueEqual(TLMaterials.STEEL.get().stats().get(ToolStatTypes.BOW.getId()).statMap().getFloat("bonus_damage"),
                                9.0F, "Compatibility bow materials retain their original ranged damage bonus");
                helper.succeed();
        }

        // Inputs
        private static StationResult build(ToolParts.Type part, ItemStack material) {
                ItemStack pattern = new ItemStack(TLItems.item(TLItems.Items.IntermediaryItems.Type.PATTERN));
                pattern.set(TLDataComponents.PART, ToolParts.get(part).getId());
                return StationRecipes.evaluate(TLStations.PART_BUILDER,
                                List.of(pattern, material, ItemStack.EMPTY, ItemStack.EMPTY),
                                StationOperations.BUILD_PART, null, "", 0);
        }
}
