package com.nurby.tinkerslegacy.test;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.library.material.layer.MaterialLayer;
import com.nurby.tinkerslegacy.library.tool.ToolModifiers;
import com.nurby.tinkerslegacy.library.tool.ToolProgress;
import com.nurby.tinkerslegacy.library.tool.ToolStatText;
import com.nurby.tinkerslegacy.library.tool.ToolTooltip;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLItems;
import com.nurby.tinkerslegacy.registry.TLTools;
import com.nurby.tinkerslegacy.registry.ToolParts;
import com.nurby.tinkerslegacy.registry.ToolStatTypes;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

import static com.nurby.tinkerslegacy.test.ToolAssemblyGameTests.id;
import static com.nurby.tinkerslegacy.test.ToolAssemblyGameTests.part;
import static com.nurby.tinkerslegacy.test.ToolAssemblyGameTests.tool;

@GameTestHolder(TinkersLegacy.MODID)
@PrefixGameTestTemplate(false)
public final class TooltipParityGameTests {

        // Tool Views

        @GameTest(template = "empty")
        public static void toolViewsAreExclusiveAndPreserveGameplayState(GameTestHelper helper) {
                ItemStack stack = tool(TLTools.Type.PICKAXE, id("wood"));
                ToolModifiers.setPoints(stack, ToolModifiers.HASTE, 51);
                ItemStack before = stack.copy();
                var attributes = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
                helper.assertTrue(attributes != null && !attributes.modifiers().isEmpty(),
                                "The tool starts with functional attack attributes");

                List<Component> normal = toolLines(stack, false, false);
                helper.assertTrue(has(normal, "trait.tinkerslegacy.ecological"), "Normal view lists material traits");
                helper.assertTrue(has(normal, "modifier.tinkerslegacy.haste.2"), "Normal view lists the named modifier level");
                helper.assertTrue(has(normal, "tinkerslegacy.tooltip.hold_stats")
                                && has(normal, "tinkerslegacy.tooltip.hold_more"), "Normal view offers both detail shortcuts");
                helper.assertTrue(!has(normal, "tinkerslegacy.stat.durability")
                                && !has(normal, "tinkerslegacy.stat.free_modifiers")
                                && !has(normal, "tinkerslegacy.tool.free_modifiers"), "Normal view omits stat and slot summaries");
                Component attack = find(normal, "attribute.modifier.plus.0");
                assertColor(helper, attack, ChatFormatting.BLUE.getColor(), "Original normal-view attack line");

                List<Component> shift = toolLines(stack, true, false);
                helper.assertTrue(has(shift, "tinkerslegacy.stat.durability")
                                && has(shift, "tinkerslegacy.stat.mining_speed"), "Shift displays tool statistics");
                helper.assertTrue(!has(shift, "modifier.tinkerslegacy.haste.2")
                                && !has(shift, "trait.tinkerslegacy.ecological")
                                && !has(shift, "tinkerslegacy.tooltip.hold_stats")
                                && !has(shift, "attribute.modifier.plus.0"), "Shift replaces the normal view");
                List<Component> control = toolLines(stack, false, true);
                helper.assertValueEqual(headings(control).size(), 3, "Ctrl lists the pickaxe's three physical parts");
                helper.assertTrue(!has(control, "modifier.tinkerslegacy.haste.2")
                                && !has(control, "tinkerslegacy.tooltip.hold_more"), "Ctrl replaces normal modifier and hint lines");
                helper.assertValueEqual(toolLines(stack, true, true), shift, "Shift takes precedence when both keys are held");
                helper.assertTrue(ItemStack.matches(stack, before), "Reading every view must preserve the complete item state");
                helper.assertValueEqual(stack.get(DataComponents.ATTRIBUTE_MODIFIERS), attributes,
                                "Tooltip parity must not remove vanilla gameplay attributes");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void levelingUsesOriginalOrderColorsAndCapVisibility(GameTestHelper helper) {
                ItemStack stack = tool(TLTools.Type.PICKAXE, id("wood"));
                stack.set(TLDataComponents.PROGRESS, new ToolProgress(0, 1234, 0));
                stack.set(TLDataComponents.NEXT_LEVEL_XP, 2000L);
                stack.set(TLDataComponents.LEVELING_ENABLED, true);
                stack.set(TLDataComponents.LEVEL_CAPPED, false);
                for (boolean shift : List.of(false, true)) {
                        List<Component> lines = toolLines(stack, shift, false);
                        helper.assertValueEqual(key(lines.getFirst()), "tinkerslegacy.tool.level", "Level appears immediately after the name");
                        helper.assertValueEqual(key(lines.get(1)), "tinkerslegacy.tool.xp", "XP follows the level in every view");
                        assertColor(helper, lines.getFirst(), ChatFormatting.GRAY.getColor(), "Level label");
                        Component name = argument(lines.getFirst(), 0);
                        helper.assertValueEqual(key(name), "tinkerslegacy.tool.level.1", "Zero promotions display Clumsy");
                        assertColor(helper, name, 0x66CC33, "Original Clumsy hue");
                        assertColor(helper, lines.get(1), ChatFormatting.GRAY.getColor(), "XP label");
                        assertColor(helper, argument(lines.get(1), 0), 0xFFFFFF, "Current XP");
                        assertColor(helper, argument(lines.get(1), 1), 0xFFFFFF, "Required XP");
                        helper.assertValueEqual(argument(lines.get(1), 0).getString(), "1234", "XP has no thousands grouping");
                }
                stack.set(TLDataComponents.PROGRESS, new ToolProgress(1, 0, 1));
                assertColor(helper, argument(toolLines(stack, false, true).getFirst(), 0), 0x3399CC,
                                "The next promotion uses the original Comfortable hue");
                stack.set(TLDataComponents.LEVEL_CAPPED, true);
                List<Component> capped = toolLines(stack, false, false);
                helper.assertTrue(has(capped, "tinkerslegacy.tool.level") && !has(capped, "tinkerslegacy.tool.xp")
                                && !has(capped, "tinkerslegacy.tool.level_capped"), "Capped tools retain the level without an XP replacement row");
                stack.set(TLDataComponents.LEVEL_CAPPED, false);
                stack.set(TLDataComponents.LEVELING_ENABLED, false);
                List<Component> disabled = toolLines(stack, false, false);
                helper.assertTrue(has(disabled, "tinkerslegacy.tool.level") && !has(disabled, "tinkerslegacy.tool.xp"),
                                "Disabling leveling preserves its earned level while hiding inactive XP");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void rangedStatisticsRetainAttackAndOnlyPositiveFreeSlots(GameTestHelper helper) {
                for (TLTools.Type type : List.of(TLTools.Type.SHORTBOW, TLTools.Type.CROSSBOW,
                                TLTools.Type.ARROW, TLTools.Type.BOLT, TLTools.Type.SHURIKEN)) {
                        ItemStack stack = tool(type, id(type == TLTools.Type.BOLT ? "iron" : "wood"));
                        stack.set(TLDataComponents.PROGRESS, ToolProgress.EMPTY);
                        stack.set(TLDataComponents.STARTING_MODIFIERS, 3);
                        List<Component> lines = toolLines(stack, true, false);
                        boolean launcher = type == TLTools.Type.SHORTBOW || type == TLTools.Type.CROSSBOW;
                        helper.assertTrue(has(lines, "tinkerslegacy.stat.attack_damage"), type + " retains its attack statistic");
                        helper.assertTrue(has(lines, "tinkerslegacy.stat.free_modifiers"), type + " displays its available modifiers");
                        if (launcher) {
                                helper.assertTrue(has(lines, "tinkerslegacy.stat.durability")
                                                && has(lines, "tinkerslegacy.stat.draw_speed")
                                                && has(lines, "tinkerslegacy.stat.range")
                                                && has(lines, "tinkerslegacy.stat.bonus_damage"), "Launchers display their full stat set");
                        } else {
                                helper.assertTrue(has(lines, "tinkerslegacy.stat.ammo")
                                                && has(lines, "tinkerslegacy.stat.accuracy"), "Ammunition displays count and accuracy");
                                helper.assertTrue(!has(lines, "tinkerslegacy.stat.durability"), "Ammunition replaces durability with ammo");
                        }
                        stack.set(TLDataComponents.STARTING_MODIFIERS, 0);
                        helper.assertTrue(!has(toolLines(stack, true, false), "tinkerslegacy.stat.free_modifiers"),
                                        "Zero free slots do not add a Modifiers: 0 row");
                }
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void controlGroupsMultipleRolesUnderTheirPhysicalPart(GameTestHelper helper) {
                ItemStack bow = tool(TLTools.Type.SHORTBOW, id("wood"));
                List<Component> bowHeadings = headings(toolLines(bow, false, true));
                helper.assertValueEqual(bowHeadings.stream().map(TooltipParityGameTests::key).toList(),
                                List.of("item.tinkerslegacy.parts.bow_limb", "item.tinkerslegacy.parts.bow_limb",
                                                "item.tinkerslegacy.parts.bow_string"), "Each physical limb has one heading despite head and bow roles");
                ItemStack crossbow = tool(TLTools.Type.CROSSBOW, id("wood"));
                List<Component> crossbowHeadings = headings(toolLines(crossbow, false, true));
                helper.assertValueEqual(crossbowHeadings.stream().map(TooltipParityGameTests::key).toList(),
                                List.of("item.tinkerslegacy.parts.tough_tool_rod", "item.tinkerslegacy.parts.bow_limb",
                                                "item.tinkerslegacy.parts.tough_binding", "item.tinkerslegacy.parts.bow_string"),
                                "Crossbow handle and extra roles share one tough rod heading");
                assertColor(helper, crossbowHeadings.getFirst(), 0x8E661B, "The physical wood part uses its material color");

                List<Component> iron = toolLines(tool(TLTools.Type.SHORTBOW, id("iron")), false, true);
                Component firstLimb = headings(iron).getFirst();
                int start = iron.indexOf(firstLimb);
                int end = start + 1;
                while (end < iron.size() && !iron.get(end).getStyle().isUnderlined()) end++;
                List<Component> limb = iron.subList(start + 1, end);
                List<Component> magnetic = limb.stream().filter(line -> key(line).equals("trait.tinkerslegacy.magnetic")).toList();
                helper.assertValueEqual(magnetic.size(), 2, "One iron limb separately explains its head and bow source traits");
                helper.assertTrue(has(List.of(magnetic.getFirst()), "enchantment.level.2"), "Head Magnetic II appears first");
                helper.assertTrue(magnetic.get(1).getSiblings().isEmpty(), "Bow Magnetic stays level one instead of merging to Magnetic III");
                int attack = limb.indexOf(find(limb, "tinkerslegacy.stat.attack_damage"));
                int firstTrait = limb.indexOf(magnetic.getFirst());
                int secondTrait = limb.indexOf(magnetic.get(1));
                int drawSpeed = limb.indexOf(find(limb, "tinkerslegacy.stat.draw_speed"));
                helper.assertTrue(attack < firstTrait && firstTrait < secondTrait && secondTrait < drawSpeed,
                                "All physical-part traits follow its first role's stats and precede its bow statistics");
                helper.assertValueEqual(headings(iron).size(), 3, "Source trait detail does not duplicate physical limb headings");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void brokenAndEmptyLabelsUseOriginalEmphasis(GameTestHelper helper) {
                ItemStack pickaxe = tool(TLTools.Type.PICKAXE, id("wood"));
                pickaxe.set(TLDataComponents.BROKEN, true);
                Component broken = find(toolLines(pickaxe, false, false), "tinkerslegacy.tool.broken");
                assertWarning(helper, broken, "Broken tool warning");
                assertWarning(helper, find(toolLines(pickaxe, true, false), "tinkerslegacy.tool.broken"), "Broken durability value");
                helper.assertTrue(!has(List.of(ToolStatText.durability(pickaxe, false)), "tinkerslegacy.tool.broken"),
                                "Detailed station durability can still display the numeric zero");
                ItemStack arrow = tool(TLTools.Type.ARROW, id("wood"));
                arrow.set(TLDataComponents.BROKEN, true);
                assertWarning(helper, find(toolLines(arrow, false, false), "tinkerslegacy.tool.empty"), "Empty ammunition warning");
                assertWarning(helper, find(toolLines(arrow, true, false), "tinkerslegacy.tool.empty"), "Empty ammunition count");
                helper.assertTrue(!has(toolLines(arrow, true, false), "tinkerslegacy.tool.broken"), "Empty ammunition is not labeled Broken");
                helper.assertValueEqual(ToolStatText.number(1234.5), "1,234.5", "Original stat number grouping");
                helper.succeed();
        }

        // Parts

        @GameTest(template = "empty")
        public static void partTraitsGroupRolesAndShiftUsesWhiteHeadings(GameTestHelper helper) {
                ItemStack wood = part(ToolParts.Type.BOW_LIMB, id("wood"));
                List<Component> simple = partLines(wood, false);
                helper.assertValueEqual(components(simple).stream().filter(line -> key(line).equals("trait.tinkerslegacy.ecological")).count(),
                                1L, "Matching trait sets across roles display once");
                helper.assertTrue(!has(simple, "tinkerslegacy.material_stat_type.head")
                                && !has(simple, "tinkerslegacy.material_stat_type.bow"), "Identical role traits need no role prefix");

                ItemStack cactus = part(ToolParts.Type.BOW_LIMB, id("cactus"));
                List<Component> normal = partLines(cactus, false);
                helper.assertTrue(has(normal, "trait.tinkerslegacy.prickly") && has(normal, "trait.tinkerslegacy.spiky"),
                                "Different role traits remain visible");
                helper.assertTrue(has(normal, "tinkerslegacy.material_stat_type.head")
                                && has(normal, "tinkerslegacy.material_stat_type.bow"), "Different trait groups identify their roles");
                helper.assertTrue(normal.getFirst().getSiblings().getFirst().getStyle().isItalic(), "Trait group role labels are italic");
                List<Component> shifted = partLines(cactus, true);
                List<Component> headings = headings(shifted);
                helper.assertValueEqual(headings.stream().map(TooltipParityGameTests::key).toList(),
                                List.of("tinkerslegacy.material_stat_type.head", "tinkerslegacy.material_stat_type.bow"),
                                "Shift groups the part's stats under both role headings");
                for (Component heading : headings) assertColor(helper, heading, 0xFFFFFF, "Part stat heading");
                helper.assertTrue(!has(shifted, "tinkerslegacy.tooltip.hold_stats"), "Expanded parts omit the Shift hint");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void sharpeningKitsAlwaysShowInstructionsAndMiningTier(GameTestHelper helper) {
                ItemStack kit = part(ToolParts.Type.SHARPENING_KIT, id("cobalt"));
                List<Component> normal = partLines(kit, false);
                helper.assertValueEqual(normal.stream().map(TooltipParityGameTests::key).toList(),
                                List.of("tinkerslegacy.tooltip.sharpening_kit.flint", "tinkerslegacy.tooltip.sharpening_kit.upgrade",
                                                "tinkerslegacy.stat.harvest_level"), "Sharpening kits show their fixed original instructions and tier");
                helper.assertValueEqual(partLines(kit, true), normal, "Sharpening-kit instructions do not change with Shift");
                assertColor(helper, find(normal, "tinkerslegacy.mininglevel.4"), 0x2882D4, "Cobalt kit mining tier");
                helper.assertTrue(!has(normal, "tinkerslegacy.tooltip.hold_stats")
                                && !has(normal, "tinkerslegacy.stat.durability"), "Kits do not act like regular stat-bearing part tooltips");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void ordinaryShardsStayQuietAndFractionalFragmentsExplainTheirValue(GameTestHelper helper) {
                var item = TLItems.Parts.MATERIAL_SHARD.get();
                List<Component> ordinary = new ArrayList<>();
                item.appendHoverText(item.withMaterial(id("stone"), 1), Item.TooltipContext.EMPTY, ordinary, TooltipFlag.NORMAL);
                helper.assertTrue(ordinary.isEmpty(), "Standard half-unit shards add no custom tooltip text");
                List<Component> fragment = new ArrayList<>();
                item.appendHoverText(item.withMaterial(id("stone"), 1, 36), Item.TooltipContext.EMPTY, fragment, TooltipFlag.NORMAL);
                helper.assertValueEqual(fragment.size(), 1, "Nonstandard remainder fragments explain their value once");
                helper.assertValueEqual(key(fragment.getFirst()), "tooltip.tinkerslegacy.material_units", "Fractional value label");
                helper.assertValueEqual(((TranslatableContents) fragment.getFirst().getContents()).getArgs()[0], "1/4",
                                "Thirty-six material units are displayed as one quarter");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void missingMaterialDefinitionsKeepTooltipViewsReadable(GameTestHelper helper) {
                ItemStack stack = tool(TLTools.Type.CROSSBOW, id("wood"));
                stack.set(TLDataComponents.MATERIALS, stack.get(TLDataComponents.MATERIALS).stream()
                                .map(layer -> new MaterialLayer(layer.statType(), id("removed_tooltip_material"))).toList());
                stack.remove(TLDataComponents.EFFECT_TRAITS);
                for (boolean shift : List.of(false, true)) {
                        helper.assertTrue(!toolLines(stack, shift, false).isEmpty(), "Missing material data must not crash normal or stat views");
                }
                List<Component> headers = headings(toolLines(stack, false, true));
                helper.assertValueEqual(headers.size(), 4, "Missing materials retain physical part headings");
                for (Component header : headers) assertColor(helper, header, 0xFFFFFF, "Missing material fallback heading");
                ItemStack limb = part(ToolParts.Type.BOW_LIMB, id("removed_tooltip_material"));
                helper.assertTrue(partLines(limb, true).isEmpty(), "Missing part material stats are safely omitted");
                ItemStack kit = part(ToolParts.Type.SHARPENING_KIT, id("removed_tooltip_material"));
                helper.assertValueEqual(partLines(kit, false).size(), 2, "A removed kit material retains instructions without inventing a mining tier");
                helper.assertTrue(ToolStatText.materialStats(new MaterialLayer(ToolStatTypes.HEAD.getId(),
                                id("removed_tooltip_material"))).isEmpty(), "Defaultless material lookup is safe");
                helper.succeed();
        }

        // Assertions

        private static List<Component> toolLines(ItemStack stack, boolean shift, boolean control) {
                List<Component> lines = new ArrayList<>();
                ToolTooltip.addTool(stack, lines, shift, control, true);
                return lines;
        }

        private static List<Component> partLines(ItemStack stack, boolean shift) {
                List<Component> lines = new ArrayList<>();
                ToolTooltip.addPart(stack, lines, shift);
                return lines;
        }

        private static List<Component> headings(List<Component> lines) {
                return lines.stream().filter(line -> line.getStyle().isUnderlined()).toList();
        }

        private static List<Component> components(List<Component> lines) {
                List<Component> result = new ArrayList<>();
                for (Component line : lines) {
                        result.add(line);
                        if (line.getContents() instanceof TranslatableContents text) {
                                for (Object argument : text.getArgs()) {
                                        if (argument instanceof Component component) result.addAll(components(List.of(component)));
                                }
                        }
                        result.addAll(components(line.getSiblings()));
                }
                return result;
        }

        private static String key(Component component) {
                return component.getContents() instanceof TranslatableContents text ? text.getKey() : "";
        }

        private static boolean has(List<Component> lines, String key) {
                return components(lines).stream().anyMatch(line -> key(line).equals(key));
        }

        private static Component find(List<Component> lines, String key) {
                return components(lines).stream().filter(line -> key(line).equals(key)).findFirst()
                                .orElseThrow(() -> new AssertionError("Missing tooltip translation " + key));
        }

        private static Component argument(Component line, int index) {
                return (Component) ((TranslatableContents) line.getContents()).getArgs()[index];
        }

        private static void assertColor(GameTestHelper helper, Component component, int expected, String message) {
                helper.assertTrue(component.getStyle().getColor() != null, message + " has an explicit color");
                helper.assertValueEqual(component.getStyle().getColor().getValue(), expected, message);
        }

        private static void assertWarning(GameTestHelper helper, Component component, String message) {
                assertColor(helper, component, ChatFormatting.DARK_RED.getColor(), message);
                helper.assertTrue(component.getStyle().isBold(), message + " is bold");
        }
}
