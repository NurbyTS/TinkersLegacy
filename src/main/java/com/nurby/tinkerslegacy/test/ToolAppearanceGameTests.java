package com.nurby.tinkerslegacy.test;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.library.tool.ToolModifierEntry;
import com.nurby.tinkerslegacy.library.tool.ToolModifiers;
import com.nurby.tinkerslegacy.library.tool.traits.ToolTraitData;
import com.nurby.tinkerslegacy.library.tool.traits.ToolTraits;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLTools;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.nurby.tinkerslegacy.test.ToolAssemblyGameTests.id;
import static com.nurby.tinkerslegacy.test.ToolAssemblyGameTests.tool;

@GameTestHolder(TinkersLegacy.MODID)
@PrefixGameTestTemplate(false)
public final class ToolAppearanceGameTests {

        // Enchantments
        @GameTest(template = "empty")
        public static void backingEnchantmentsKeepTheirEffectsWithoutAutomaticGlint(GameTestHelper helper) {
                var registries = helper.getLevel().registryAccess();
                var enchantments = registries.lookupOrThrow(Registries.ENCHANTMENT);

                for (TLTools.Type type : List.of(TLTools.Type.PICKAXE, TLTools.Type.SHURIKEN)) {
                        ItemStack sponge = tool(type, id("sponge"));
                        ((DynamicTool) sponge.getItem()).calculateStats(sponge, registries);
                        helper.assertValueEqual(sponge.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)
                                        .getLevel(enchantments.getOrThrow(Enchantments.SILK_TOUCH)), 1,
                                        "Squeaky still supplies actual Silk Touch");
                        helper.assertTrue(!sponge.hasFoil(), "Sponge tools retain their original unenchanted appearance");
                }

                for (TLTools.Type type : List.of(TLTools.Type.PICKAXE, TLTools.Type.BROADSWORD)) {
                        ItemStack lucky = tool(type, id("iron"));
                        lucky.set(TLDataComponents.MODIFIERS, List.of(new ToolModifierEntry(ToolModifiers.LUCK, 60)));
                        ((DynamicTool) lucky.getItem()).calculateStats(lucky, registries);
                        var enchantment = type == TLTools.Type.PICKAXE ? Enchantments.FORTUNE : Enchantments.LOOTING;
                        helper.assertValueEqual(lucky.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)
                                        .getLevel(enchantments.getOrThrow(enchantment)), 1, "Luck keeps its backing enchantment");
                        helper.assertTrue(!lucky.hasFoil(), "Luck does not create a vanilla enchantment glint");
                }

                ItemStack silky = tool(TLTools.Type.PICKAXE, id("iron"));
                silky.set(TLDataComponents.MODIFIERS, List.of(new ToolModifierEntry(ToolModifiers.SILKTOUCH, 1)));
                ((DynamicTool) silky.getItem()).calculateStats(silky, registries);
                helper.assertValueEqual(silky.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)
                                .getLevel(enchantments.getOrThrow(Enchantments.SILK_TOUCH)), 1, "Silky keeps Silk Touch");
                helper.assertTrue(!silky.hasFoil(), "Silky does not create a vanilla enchantment glint");

                ItemStack vanilla = new ItemStack(Items.DIAMOND_PICKAXE);
                vanilla.enchant(enchantments.getOrThrow(Enchantments.SILK_TOUCH), 1);
                helper.assertTrue(vanilla.hasFoil(), "Ordinary enchanted Minecraft items still glow");
                helper.succeed();
        }

        // Trait Effects
        @GameTest(template = "empty")
        public static void shockingGlowsWhenChargedAndStopsAfterDischarge(GameTestHelper helper) {
                ItemStack electrum = tool(TLTools.Type.PICKAXE, id("electrum"));
                helper.assertTrue(!electrum.hasFoil(), "An uncharged Electrum tool does not glow");
                electrum.set(TLDataComponents.TRAIT_DATA, ToolTraitData.EMPTY.with("charge", 99));
                helper.assertTrue(!electrum.hasFoil(), "Partial Shocking charge does not glow");

                var attacker = helper.spawnWithNoFreeWill(EntityType.HUSK, 0, 1, 0);
                var target = helper.spawnWithNoFreeWill(EntityType.HUSK, 2, 1, 2);
                ToolTraits.beforeHit(electrum, attacker, target);
                helper.assertTrue(electrum.hasFoil(), "Reaching full charge enables the original Shocking glint");
                ((DynamicTool) electrum.getItem()).calculateStats(electrum, helper.getLevel().registryAccess());
                helper.assertTrue(electrum.copy().hasFoil(), "Recalculation and copying preserve a charged tool's glint");
                float health = target.getHealth();
                ToolTraits.beforeHit(electrum, attacker, target);
                helper.assertTrue(target.getHealth() < health, "The charged trait still deals its discharge damage");
                helper.assertTrue(!electrum.hasFoil(), "Discharging immediately removes the glint");

                ItemStack iron = tool(TLTools.Type.PICKAXE, id("iron"));
                iron.set(TLDataComponents.TRAIT_DATA, ToolTraitData.EMPTY.with("charge", 100));
                helper.assertTrue(!iron.hasFoil(), "Leftover charge cannot glow after the Shocking material is removed");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void explicitGlintOverridesRemainAvailable(GameTestHelper helper) {
                ItemStack sponge = tool(TLTools.Type.SHURIKEN, id("sponge"));
                sponge.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
                helper.assertTrue(sponge.hasFoil(), "Commands and extensions can explicitly request a glint");
                sponge.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
                helper.assertTrue(!sponge.hasFoil(), "Removing an override restores the original Sponge appearance");

                ItemStack electrum = tool(TLTools.Type.SHURIKEN, id("electrum"));
                electrum.set(TLDataComponents.TRAIT_DATA, ToolTraitData.EMPTY.with("charge", 100));
                electrum.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false);
                helper.assertTrue(!electrum.hasFoil(), "An explicit false override can hide a trait glint");
                electrum.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
                helper.assertTrue(electrum.hasFoil(), "Removing the override restores a charged Shocking glint");
                helper.succeed();
        }
}
