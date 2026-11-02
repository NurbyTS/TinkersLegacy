package com.nurby.tinkerslegacy.test;

import com.mojang.serialization.Lifecycle;
import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.library.material.layer.MaterialLayer;
import com.nurby.tinkerslegacy.library.tool.ToolAssembly;
import com.nurby.tinkerslegacy.library.tool.ToolModifierEntry;
import com.nurby.tinkerslegacy.library.tool.ToolModifiers;
import com.nurby.tinkerslegacy.library.tool.ToolProgress;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLItems;
import com.nurby.tinkerslegacy.registry.TLRegistries;
import com.nurby.tinkerslegacy.registry.TLTools;
import io.netty.buffer.Unpooled;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.connection.ConnectionType;

import java.util.List;

import static com.nurby.tinkerslegacy.test.ToolAssemblyGameTests.id;

@GameTestHolder(TinkersLegacy.MODID)
@PrefixGameTestTemplate(false)
public final class ToolRegistryContextGameTests {

        // Creative Packets
        @GameTest(template = "empty")
        public static void spongePreviewsUseTheSuppliedRegistryAndEncodeCreativeSlots(GameTestHelper helper) {
                RegistryAccess registries = copyEnchantments(helper.getLevel().registryAccess());

                CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(id("tools"));

                try {
                        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(helper.getLevel().enabledFeatures(), true, registries));

                        for (TLTools.Type type : List.of(TLTools.Type.SHURIKEN, TLTools.Type.PICKAXE)) {
                                ItemStack stack = tab.getDisplayItems().stream()
                                                .filter(item -> item.is(TLItems.Tools.item(type)))
                                                .filter(item -> item.getOrDefault(TLDataComponents.MATERIALS, List.<MaterialLayer>of()).stream()
                                                                .anyMatch(layer -> layer.material().equals(id("sponge"))))
                                                .findFirst().orElseThrow();
                                assertEnchantment(helper, stack, registries, Enchantments.SILK_TOUCH, 1);
                                ItemStack received = roundTrip(stack, registries);
                                assertEnchantment(helper, received, registries, Enchantments.SILK_TOUCH, 1);
                                helper.assertTrue(!stack.hasFoil() && !received.hasFoil(),
                                                "Squeaky keeps Silk Touch without adding a glint to previews or received tools");
                                helper.assertValueEqual(received.get(TLDataComponents.MATERIALS), stack.get(TLDataComponents.MATERIALS),
                                                "The creative slot packet preserves the sponge material layers");
                                helper.assertTrue(!received.has(TLDataComponents.TOOL_ID), "A creative preview does not claim a real tool identity");
                        }

                        for (ItemStack stack : tab.getDisplayItems()) {
                                helper.assertTrue(!roundTrip(stack, registries).isEmpty(), "Every generated tool preview encodes for its supplied registry");
                        }
                } finally {
                        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(helper.getLevel().enabledFeatures(), true,
                                        helper.getLevel().registryAccess()));
                }

                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void rebuildingForAnotherConnectionReplacesForeignEnchantmentHolders(GameTestHelper helper) {
                RegistryAccess first = copyEnchantments(helper.getLevel().registryAccess());
                RegistryAccess second = copyEnchantments(helper.getLevel().registryAccess());
                ItemStack stack = create(TLTools.Type.SHURIKEN, "sponge", first);
                stack.setDamageValue(3);
                ToolProgress progress = new ToolProgress(2, 37, 2);
                stack.set(TLDataComponents.PROGRESS, progress);

                helper.assertTrue(first.registryOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH)
                                != second.registryOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH),
                                "Independent connections have different enchantment value identities");
                boolean rejected = false;

                try {
                        roundTrip(stack, second);
                } catch (IllegalArgumentException expected) {
                        rejected = expected.getMessage().contains("Can't find id")
                                        && expected.getMessage().contains("minecraft:silk_touch");
                }

                helper.assertTrue(rejected, "The negative control reproduces the foreign-holder creative-slot encoding failure");
                ((DynamicTool) stack.getItem()).calculateStats(stack, second);
                ItemStack received = roundTrip(stack, second);
                assertEnchantment(helper, received, second, Enchantments.SILK_TOUCH, 1);
                helper.assertValueEqual(received.getDamageValue(), 3, "Registry rebinding preserves existing damage");
                helper.assertValueEqual(received.get(TLDataComponents.PROGRESS), progress, "Registry rebinding preserves leveling progress");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void luckAndSilkyModifiersUseThePacketRegistries(GameTestHelper helper) {
                RegistryAccess first = copyEnchantments(helper.getLevel().registryAccess());
                RegistryAccess second = copyEnchantments(helper.getLevel().registryAccess());
                ItemStack pickaxe = create(TLTools.Type.PICKAXE, "iron", first);
                ItemStack sword = create(TLTools.Type.BROADSWORD, "iron", first);
                ItemStack silky = create(TLTools.Type.PICKAXE, "iron", first);
                pickaxe.set(TLDataComponents.MODIFIERS, List.of(new ToolModifierEntry(ToolModifiers.LUCK, 60)));
                sword.set(TLDataComponents.MODIFIERS, List.of(new ToolModifierEntry(ToolModifiers.LUCK, 60)));
                silky.set(TLDataComponents.MODIFIERS, List.of(new ToolModifierEntry(ToolModifiers.SILKTOUCH, 1)));

                for (ItemStack stack : List.of(pickaxe, sword, silky)) {
                        ((DynamicTool) stack.getItem()).calculateStats(stack, first);
                        ((DynamicTool) stack.getItem()).calculateStats(stack, second);
                        helper.assertValueEqual(roundTrip(stack, second).get(TLDataComponents.MODIFIERS),
                                        stack.get(TLDataComponents.MODIFIERS), "Modifier progress survives creative-slot encoding");
                }

                assertEnchantment(helper, pickaxe, second, Enchantments.FORTUNE, 1);
                assertEnchantment(helper, sword, second, Enchantments.LOOTING, 1);
                assertEnchantment(helper, silky, second, Enchantments.SILK_TOUCH, 1);
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void explicitPreviewRegistriesCannotReplaceTheServersToolHolders(GameTestHelper helper) {
                RegistryAccess server = helper.getLevel().registryAccess();
                RegistryAccess preview = copyEnchantments(server);
                ItemStack display = create(TLTools.Type.SHURIKEN, "sponge", preview);
                ItemStack real = create(TLTools.Type.PICKAXE, "sponge", server);
                ((DynamicTool) display.getItem()).calculateStats(display, preview);
                ((DynamicTool) real.getItem()).calculateStats(real);

                assertEnchantment(helper, roundTrip(display, preview), preview, Enchantments.SILK_TOUCH, 1);
                assertEnchantment(helper, roundTrip(real, server), server, Enchantments.SILK_TOUCH, 1);
                helper.succeed();
        }

        // Helpers
        private static ItemStack create(TLTools.Type type, String material, RegistryAccess registries) {
                var definition = TLTools.tool(type);
                return ToolAssembly.create(definition,
                                definition.defaultMaterials(TLRegistries.MATERIALS.get(id(material))), registries);
        }

        private static RegistryAccess copyEnchantments(RegistryAccess source) {
                MappedRegistry<Enchantment> copy = new MappedRegistry<>(Registries.ENCHANTMENT, Lifecycle.stable());
                source.registryOrThrow(Registries.ENCHANTMENT).holders().forEach(holder -> {
                        Enchantment enchantment = holder.value();
                        copy.register(holder.key(), new Enchantment(enchantment.description(), enchantment.definition(),
                                        enchantment.exclusiveSet(), enchantment.effects()), RegistrationInfo.BUILT_IN);
                });
                copy.freeze();
                return new RegistryAccess.ImmutableRegistryAccess(source.registries().map(entry ->
                                entry.key().equals(Registries.ENCHANTMENT)
                                                ? new RegistryAccess.RegistryEntry<>(Registries.ENCHANTMENT, copy)
                                                : entry)).freeze();
        }

        private static ItemStack roundTrip(ItemStack stack, RegistryAccess registries) {
                RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries, ConnectionType.NEOFORGE);

                try {
                        ServerboundSetCreativeModeSlotPacket.STREAM_CODEC.encode(buffer, new ServerboundSetCreativeModeSlotPacket(36, stack));
                        return ServerboundSetCreativeModeSlotPacket.STREAM_CODEC.decode(buffer).itemStack();
                } finally {
                        buffer.release();
                }
        }

        private static void assertEnchantment(
                        GameTestHelper helper,
                        ItemStack stack,
                        RegistryAccess registries,
                        ResourceKey<Enchantment> key,
                        int level) {
                var holder = registries.registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(key);
                var enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
                helper.assertValueEqual(enchantments.getLevel(holder), level, "Enchantment uses the current registry holder for " + key.location());
                helper.assertTrue(enchantments.keySet().stream().allMatch(value -> value.unwrapKey()
                                .map(id -> registries.registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(id) == value).orElse(false)),
                                "No enchantment component keeps another connection's holder");
        }
}
