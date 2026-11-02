package com.nurby.tinkerslegacy.test;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.library.material.layer.MaterialLayer;
import com.nurby.tinkerslegacy.library.tool.ToolEffectText;
import com.nurby.tinkerslegacy.library.tool.ToolModifierEntry;
import com.nurby.tinkerslegacy.library.tool.ToolModifiers;
import com.nurby.tinkerslegacy.library.tool.traits.ToolTraits;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLRegistries;
import com.nurby.tinkerslegacy.registry.TLTools;
import com.nurby.tinkerslegacy.registry.ToolParts;
import com.nurby.tinkerslegacy.registry.ToolStatTypes;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.nurby.tinkerslegacy.test.ToolAssemblyGameTests.id;
import static com.nurby.tinkerslegacy.test.ToolAssemblyGameTests.part;
import static com.nurby.tinkerslegacy.test.ToolAssemblyGameTests.tool;

@GameTestHolder(TinkersLegacy.MODID)
@PrefixGameTestTemplate(false)
public final class ToolEffectColorGameTests {

        // Modifiers

        @GameTest(template = "empty")
        public static void originalModifierPaletteReachesNamesAndProgress(GameTestHelper helper) {
                Map<String, Integer> expected = Map.ofEntries(
                                Map.entry("diamond", 0x8CF4E2), Map.entry("emerald", 0x41F384),
                                Map.entry("haste", 0x910000), Map.entry("sharpness", 0xFFF6F6),
                                Map.entry("bane_of_arthopods", 0x61BA49), Map.entry("smite", 0xE8D500),
                                Map.entry("beheading", 0x10574B), Map.entry("blasting", 0xFFAA23),
                                Map.entry("fiery", 0xEA9E32), Map.entry("fins", 0xABCDEF),
                                Map.entry("glowing", 0xFFFFAA), Map.entry("harvestwidth", 0xCAF6A2),
                                Map.entry("harvestheight", 0xCAF6A2), Map.entry("knockback", 0x9F9F9F),
                                Map.entry("luck", 0x5A82E2), Map.entry("mending_moss", 0x43AB32),
                                Map.entry("necrotic", 0x5E0000), Map.entry("reinforced", 0x502E83),
                                Map.entry("shulking", 0xAACCFF), Map.entry("silktouch", 0xFBE28B),
                                Map.entry("webbed", 0xFFFFFF), Map.entry("soulbound", 0xF5FBAC));
                ItemStack stack = tool(TLTools.Type.PICKAXE, id("iron"));
                for (var entry : expected.entrySet()) {
                        ResourceLocation modifier = id(entry.getKey());
                        ToolModifiers.setPoints(stack, modifier, 1);
                        assertColor(helper, ToolEffectText.modifierName(stack, modifier), entry.getValue(), entry.getKey());
                        assertColor(helper, ToolEffectText.modifierLine(stack, new ToolModifierEntry(modifier, 1)),
                                        entry.getValue(), entry.getKey() + " progress");
                }
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void levelsAndUnbreakableKeepTheirModifierStyle(GameTestHelper helper) {
                ItemStack stack = tool(TLTools.Type.PICKAXE, id("iron"));
                ToolModifiers.setPoints(stack, ToolModifiers.HASTE, 51);
                Component name = ToolEffectText.modifierName(stack, ToolModifiers.HASTE);
                helper.assertValueEqual(((TranslatableContents) name.getContents()).getKey(),
                                "modifier.tinkerslegacy.haste.2", "The second level uses the original Haster name");
                helper.assertTrue(name.getSiblings().isEmpty(), "The named Haste level does not append a numeral");
                assertColor(helper, name, 0x910000, "The level name retains Haste red");
                Component progress = ToolEffectText.modifierLine(stack, new ToolModifierEntry(ToolModifiers.HASTE, 51));
                var contents = (TranslatableContents) progress.getContents();
                helper.assertValueEqual(contents.getArgs()[1], 51, "Current progress is retained");
                helper.assertValueEqual(contents.getArgs()[2], 100, "Next threshold is retained");
                assertColor(helper, progress, 0x910000, "Progress punctuation and amounts inherit Haste red");
                ToolModifiers.setPoints(stack, ToolModifiers.HASTE, 50);
                Component completed = ToolEffectText.modifierLine(stack, new ToolModifierEntry(ToolModifiers.HASTE, 50));
                helper.assertValueEqual(((TranslatableContents) completed.getContents()).getArgs()[2], 50,
                                "A full Haste level shows 50 / 50 until the next level starts");
                ToolModifiers.setPoints(stack, ToolModifiers.LUCK, 60);
                Component luck = ToolEffectText.modifierName(stack, ToolModifiers.LUCK);
                helper.assertValueEqual(((TranslatableContents) luck.getSiblings().get(1).getContents()).getKey(),
                                "enchantment.level.1", "Luck includes the first effective enchantment level");
                ToolModifiers.setPoints(stack, ToolModifiers.SHARPNESS, 73);
                helper.assertValueEqual(((TranslatableContents) ToolEffectText.modifierName(stack, ToolModifiers.SHARPNESS)
                                .getContents()).getKey(), "modifier.tinkerslegacy.sharpness.2", "Quartz uses the original Sharper name");
                ToolModifiers.setPoints(stack, ToolModifiers.REINFORCED, 5);
                Component unbreakable = ToolEffectText.modifierName(stack, ToolModifiers.REINFORCED);
                helper.assertValueEqual(((TranslatableContents) unbreakable.getContents()).getKey(),
                                "modifier.tinkerslegacy.reinforced.unbreakable", "The final level is called Unbreakable");
                helper.assertTrue(unbreakable.getSiblings().isEmpty(), "Unbreakable does not append a numeral");
                assertColor(helper, unbreakable, 0x502E83, "Unbreakable retains original purple");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void effectListsHideCreativeAndKeepInherentCleaverBeheading(GameTestHelper helper) {
                ItemStack stack = tool(TLTools.Type.CLEAVER, id("iron"));
                ToolModifiers.setPoints(stack, ToolModifiers.CREATIVE, 2);
                ToolModifiers.setPoints(stack, ToolModifiers.HASTE, 51);
                List<Component> normal = ToolEffectText.modifierNames(stack, false);
                helper.assertValueEqual(normal.size(), 2, "Creative is hidden and inherent Beheading remains visible");
                helper.assertValueEqual(((TranslatableContents) normal.getFirst().getContents()).getKey(),
                                "modifier.tinkerslegacy.beheading", "The Cleaver's inherent modifier precedes purchased effects");
                helper.assertValueEqual(((TranslatableContents) normal.getFirst().getSiblings().get(1).getContents()).getKey(),
                                "enchantment.level.2", "An unmodified Cleaver displays Beheading II");
                helper.assertValueEqual(((TranslatableContents) normal.get(1).getContents()).getKey(),
                                "modifier.tinkerslegacy.haste.2", "Inventory effects display names without ingredient progress");
                List<Component> detailed = ToolEffectText.modifierNames(stack, true);
                helper.assertValueEqual(((TranslatableContents) detailed.get(1).getContents()).getKey(),
                                "tinkerslegacy.tool.modifier_progress", "The station shows detailed ingredient progress");
                ToolModifiers.setPoints(stack, ToolModifiers.BEHEADING, 1);
                List<Component> upgraded = ToolEffectText.modifierNames(stack, false);
                helper.assertValueEqual(upgraded.size(), 2, "Applied Beheading does not duplicate the inherent modifier");
                helper.assertValueEqual(((TranslatableContents) upgraded.getFirst().getSiblings().get(1).getContents()).getKey(),
                                "enchantment.level.3", "Purchased Beheading includes the Cleaver's two starting levels");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void fortificationAndEmbossmentUseTheirAppliedMaterial(GameTestHelper helper) {
                ItemStack stack = tool(TLTools.Type.PICKAXE, id("wood"));
                for (String material : List.of("cobalt", "ardite", "obsidian")) {
                        int expected = TLRegistries.MATERIALS.get(id(material)).color();
                        ResourceLocation fortify = id("fortify/tinkerslegacy/" + material);
                        Component fortified = ToolEffectText.modifierName(stack, fortify);
                        assertColor(helper, fortified, expected, "Fortification material " + material);
                        helper.assertValueEqual(((TranslatableContents) fortified.getSiblings().get(1).getContents()).getKey(),
                                        "tinkerslegacy.material." + material, "Fortified names the sharpening material");
                        stack.set(TLDataComponents.EMBOSSMENT, List.of(new MaterialLayer(ToolStatTypes.HEAD.getId(), id(material))));
                        Component embossed = ToolEffectText.modifierName(stack, ToolModifiers.EMBOSSMENT);
                        assertColor(helper, embossed, expected, "Embossment material " + material);
                        helper.assertValueEqual(((TranslatableContents) embossed.getSiblings().get(1).getContents()).getKey(),
                                        "tinkerslegacy.material." + material, "Embossment names the added material");
                }
                assertColor(helper, ToolEffectText.modifierName(id("fortify")), 0xDDDDDD, "Material-free fortification display");
                assertColor(helper, ToolEffectText.modifierName(id("fortify/missing/material")), 0xDDDDDD, "Missing material fallback");
                stack.set(TLDataComponents.EMBOSSMENT, List.of(new MaterialLayer(ToolStatTypes.HEAD.getId(), id("missing_material"))));
                assertColor(helper, ToolEffectText.modifierName(stack, ToolModifiers.EMBOSSMENT), 0xDDDDDD,
                                "Removed embossment materials use the material-free fallback");
                assertColor(helper, ToolEffectText.modifierName(ResourceLocation.parse("example:unknown")), 0xFFFFFF,
                                "Unknown extension modifier uses neutral fallback");
                helper.succeed();
        }

        // Traits

        @GameTest(template = "empty")
        public static void materialTraitsKeepFirstSourceColorsAndMergedLevels(GameTestHelper helper) {
                ItemStack stack = tool(TLTools.Type.PICKAXE, id("wood"));
                Component ecological = ToolEffectText.traitNames(stack, ToolTraits.traits(stack)).getFirst();
                assertColor(helper, ecological, TLRegistries.MATERIALS.get(id("wood")).color(),
                                "Wood supplies its material color instead of generic trait green or purple");
                stack.set(TLDataComponents.MATERIALS, List.of(
                                new MaterialLayer(ToolStatTypes.HEAD.getId(), id("flint")),
                                new MaterialLayer(ToolStatTypes.HANDLE.getId(), id("flint"))));
                stack.set(TLDataComponents.EMBOSSMENT, List.of(new MaterialLayer(ToolStatTypes.HEAD.getId(), id("cobalt"))));
                List<Component> names = ToolEffectText.traitNames(stack, List.of(id("crude2"), id("crude1"), id("momentum")));
                helper.assertValueEqual(names.size(), 2, "Leveled Crude stays one display entry");
                assertColor(helper, names.getFirst(), TLRegistries.MATERIALS.get(id("flint")).color(), "Crude source material");
                helper.assertValueEqual(((TranslatableContents) names.getFirst().getSiblings().get(1).getContents()).getKey(),
                                "enchantment.level.3", "Crude levels remain merged");
                assertColor(helper, names.get(1), TLRegistries.MATERIALS.get(id("cobalt")).color(), "Embossed trait source material");
                Component firstLevel = ToolEffectText.traitNames(stack, List.of(id("crude1"))).getFirst();
                helper.assertTrue(firstLevel.getSiblings().isEmpty(), "A first-level trait does not append I");
                Component writable = ToolEffectText.traitNames(stack, List.of(id("writable1"), id("writable2"))).getFirst();
                helper.assertValueEqual(((TranslatableContents) writable.getSiblings().get(1).getContents()).getKey(),
                                "enchantment.level.2", "Two distinct Writable sources contribute one level each");
                helper.assertTrue(ToolEffectText.traitNames(stack, List.of(id("writable2"))).getFirst().getSiblings().isEmpty(),
                                "A head-only Writable source is still level one");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void partTraitsAndSourceFreeTraitsKeepTheirOwnColors(GameTestHelper helper) {
                ItemStack head = part(ToolParts.Type.PICKAXE_HEAD, id("cactus"));
                assertColor(helper, ToolEffectText.traitNames(head, List.of(id("prickly"))).getFirst(),
                                TLRegistries.MATERIALS.get(id("cactus")).color(), "Part tooltip uses its material color");
                head.set(TLDataComponents.MATERIALS, List.of(new MaterialLayer(ToolStatTypes.HEAD.getId(), id("missing_material"))));
                head.set(TLDataComponents.EMBOSSMENT, List.of(new MaterialLayer(ToolStatTypes.HEAD.getId(), id("missing_embossment"))));
                assertColor(helper, ToolEffectText.traitNames(head, List.of(id("prickly"))).getFirst(), 0x00AA00,
                                "Removed trait materials use the trait's own fallback color");
                assertColor(helper, ToolEffectText.traitName(id("ecological")), 0x55FF55, "Source-free Ecological fallback");
                assertColor(helper, ToolEffectText.traitName(id("crude1")), 0x424242, "Source-free Crude fallback");
                assertColor(helper, ToolEffectText.traitName(id("momentum")), 0x5555FF, "Source-free Momentum fallback");
                assertColor(helper, ToolEffectText.traitName(ResourceLocation.parse("example:unknown")), 0xFFFFFF,
                                "Unknown extension trait fallback");
                helper.succeed();
        }

        private static void assertColor(GameTestHelper helper, Component component, int color, String message) {
                helper.assertTrue(component.getStyle().getColor() != null, message + " has an explicit color");
                helper.assertValueEqual(component.getStyle().getColor().getValue(), color, message);
                component.visit((style, text) -> {
                        helper.assertTrue(style.getColor() != null, message + " child style");
                        helper.assertValueEqual(style.getColor().getValue(), color, message + " child text");
                        return Optional.empty();
                }, Style.EMPTY);
        }
}
