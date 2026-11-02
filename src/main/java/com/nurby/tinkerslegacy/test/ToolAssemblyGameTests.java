package com.nurby.tinkerslegacy.test;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.library.material.layer.MaterialLayer;
import com.nurby.tinkerslegacy.library.tool.ToolAssembly;
import com.nurby.tinkerslegacy.library.tool.ToolDefinition;
import com.nurby.tinkerslegacy.library.tool.ToolPartRequirement;
import com.nurby.tinkerslegacy.library.tool.ToolState;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLItems;
import com.nurby.tinkerslegacy.registry.TLRegistries;
import com.nurby.tinkerslegacy.registry.TLTools;
import com.nurby.tinkerslegacy.registry.ToolParts;
import com.nurby.tinkerslegacy.registry.ToolStatTypes;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder(TinkersLegacy.MODID)
@PrefixGameTestTemplate(false)
public final class ToolAssemblyGameTests {

        // Registration
        @GameTest(template = "empty")
        public static void toolIdsAndFactoriesRemainStable(GameTestHelper helper) {
                List<String> names = List.of("pickaxe", "shovel", "hatchet", "mattock",
                                "kama", "broadsword", "longsword", "rapier", "frypan", "battlesign", "hammer", "excavator",
                                "lumberaxe", "scythe", "cleaver", "shortbow", "longbow", "crossbow", "arrow", "bolt", "shuriken");
                List<String> factories = List.of("PickaxeDefinition", "ShovelDefinition", "HatchetDefinition",
                                "MattockDefinition", "KamaDefinition", "BroadswordDefinition", "LongswordDefinition", "RapierDefinition",
                                "FrypanDefinition", "BattlesignDefinition", "HammerDefinition", "ExcavatorDefinition",
                                "LumberaxeDefinition", "ScytheDefinition", "CleaverDefinition", "ShortbowDefinition",
                                "LongbowDefinition", "CrossbowDefinition", "ArrowDefinition", "BoltDefinition", "ShurikenDefinition");

                helper.assertValueEqual(TLTools.ALL.stream().map(holder -> holder.getId().getPath()).toList(),
                                names, "Saved tool IDs and registration order");
                helper.assertValueEqual(TLTools.Type.values().length, 21, "Built-in tool count");

                for (int index = 0; index < names.size(); index++) {
                        TLTools.Type type = TLTools.Type.values()[index];
                        var holder = TLTools.get(type);
                        helper.assertValueEqual(holder.getId(), id(names.get(index)), "Definition holder ID");
                        helper.assertValueEqual(TLItems.Tools.get(type).getId(), holder.getId(), "Tool item ID");
                        helper.assertValueEqual(holder.get().getClass().getSimpleName(), factories.get(index), "Behavior factory");
                        helper.assertTrue(TLItems.Tools.item(type).getDefinition() == holder.get(),
                                        "Each dynamic item must resolve its own registered definition");
                }
                helper.succeed();
        }

        public static List<ItemStack> exampleParts(ToolDefinition definition, List<MaterialLayer> materials) {
                List<ItemStack> result = new ArrayList<>();
                int layerIndex = 0;
                for (var requirement : definition.partRequirements()) {
                        List<MaterialLayer> roles = materials.subList(layerIndex, layerIndex + requirement.statTypes().size());
                        var part = TLRegistries.PARTS.get(requirement.part());
                        ItemStack stack = new ItemStack(TLItems.Parts.get(requirement.part()).get());
                        stack.set(TLDataComponents.MATERIALS, part.statTypes().stream()
                                        .map(role -> roles.stream().filter(layer -> layer.statType().equals(role)).findFirst()
                                                        .orElse(new MaterialLayer(role, roles.getFirst().material()))).toList());
                        result.add(stack);
                        layerIndex += requirement.statTypes().size();
                }
                return result;
        }

        // Assembly
        @GameTest(template = "empty")
        public static void everyExistingToolAssemblesWithoutConsumingPreviewInputs(GameTestHelper helper) {
                for (TLTools.Type type : TLTools.Type.values()) {
                        ToolDefinition definition = TLTools.tool(type);
                        var material = TLRegistries.MATERIALS.get(id(type == TLTools.Type.BOLT ? "iron" : "wood"));
                        List<MaterialLayer> expected = definition.defaultMaterials(material);
                        List<ItemStack> inputs = exampleParts(definition, expected);
                        inputs.forEach(stack -> stack.setCount(2));
                        List<ItemStack> before = copies(inputs);
                        ItemStack result = ToolAssembly.assemble(definition, inputs);

                        helper.assertTrue(!result.isEmpty(), "Wooden " + definition.id() + " must assemble");
                        helper.assertTrue(result.is(TLItems.Tools.item(type)), "Assembly must produce the selected tool");
                        helper.assertValueEqual(result.getCount(), 1, "Assembly output count");
                        helper.assertValueEqual(result.get(TLDataComponents.MATERIALS),
                                        expected,
                                        "Saved material-layer order");
                        helper.assertTrue(!result.has(TLDataComponents.TOOL_ID), "A preview must not acquire a combat identity");
                        assertInputs(helper, inputs, before);
                }
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void swordsRequireTheirOriginalDistinctGuards(GameTestHelper helper) {
                List<TLTools.Type> swords = List.of(TLTools.Type.BROADSWORD, TLTools.Type.LONGSWORD, TLTools.Type.RAPIER);
                List<ToolParts.Type> guards = List.of(ToolParts.Type.WIDE_GUARD, ToolParts.Type.HAND_GUARD, ToolParts.Type.CROSS_GUARD);
                for (int index = 0; index < swords.size(); index++) {
                        ToolDefinition definition = TLTools.tool(swords.get(index));
                        var guard = ToolParts.get(guards.get(index));
                        helper.assertValueEqual(definition.partRequirements().get(2).part(), guard.getId(),
                                        "Each sword uses its original guard shape");
                        helper.assertValueEqual(guard.get().castingCost(), 1, "Each guard costs one material unit");
                        List<ItemStack> inputs = parts(definition, id("wood"));
                        ItemStack output = ToolAssembly.assemble(definition, inputs);
                        helper.assertTrue(!output.isEmpty(), "The original guard assembles the corresponding sword");
                        helper.assertValueEqual(output.get(TLDataComponents.MATERIALS).stream().map(MaterialLayer::statType).toList(),
                                        List.of(ToolStatTypes.HEAD.getId(), ToolStatTypes.HANDLE.getId(), ToolStatTypes.EXTRA.getId()),
                                        "Saved sword material layers retain their existing order");
                        inputs.set(2, part(guards.get((index + 1) % guards.size()), id("wood")));
                        helper.assertTrue(ToolAssembly.assemble(definition, inputs).isEmpty(),
                                        "A different sword's guard must not satisfy this assembly shape");
                }
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void invalidPartsAndMaterialsAreRejected(GameTestHelper helper) {
                ToolDefinition definition = TLTools.tool(TLTools.Type.PICKAXE);
                List<ItemStack> inputs = parts(definition, id("wood"));
                helper.assertTrue(ToolAssembly.assemble(definition, inputs.subList(0, 2)).isEmpty(),
                                "Missing physical parts must fail");

                inputs.set(0, part(ToolParts.Type.AXE_HEAD, id("wood")));
                helper.assertTrue(ToolAssembly.assemble(definition, inputs).isEmpty(), "The wrong head shape must fail");
                inputs.set(0, part(ToolParts.Type.PICKAXE_HEAD, id("unknown_material")));
                helper.assertTrue(ToolAssembly.assemble(definition, inputs).isEmpty(), "Unknown materials must fail");
                inputs.set(0, part(ToolParts.Type.PICKAXE_HEAD, id("wood")));
                inputs.getFirst().set(TLDataComponents.MATERIALS, List.of(
                                new MaterialLayer(ToolStatTypes.HEAD.getId(), id("wood")),
                                new MaterialLayer(ToolStatTypes.HEAD.getId(), id("stone"))));
                helper.assertTrue(ToolAssembly.assemble(definition, inputs).isEmpty(), "Duplicate stat roles must fail");
                inputs.getFirst().set(TLDataComponents.MATERIALS,
                                List.of(new MaterialLayer(ToolStatTypes.HANDLE.getId(), id("wood"))));
                helper.assertTrue(ToolAssembly.assemble(definition, inputs).isEmpty(), "A head cannot use handle stats");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void onePhysicalPartCanSupplyMultipleMaterialRoles(GameTestHelper helper) {
                ToolDefinition probe = new ToolDefinition(id("pickaxe"),
                                new ToolPartRequirement(ToolParts.get(ToolParts.Type.LARGE_PLATE).getId(),
                                                List.of(ToolStatTypes.HEAD.getId(), ToolStatTypes.EXTRA.getId())),
                                new ToolPartRequirement(ToolParts.get(ToolParts.Type.TOOL_ROD).getId(),
                                                List.of(ToolStatTypes.HANDLE.getId()))) {
                };
                ItemStack plate = part(ToolParts.Type.LARGE_PLATE, id("wood"));
                plate.set(TLDataComponents.MATERIALS, List.of(
                                new MaterialLayer(ToolStatTypes.EXTRA.getId(), id("stone")),
                                new MaterialLayer(ToolStatTypes.HEAD.getId(), id("wood"))));
                ItemStack rod = part(ToolParts.Type.TOOL_ROD, id("wood"));
                ItemStack output = ToolAssembly.assemble(probe, List.of(plate, rod));

                helper.assertTrue(!output.isEmpty(), "Two physical inputs must provide all three stat layers");
                helper.assertValueEqual(output.get(TLDataComponents.MATERIALS), List.of(
                                new MaterialLayer(ToolStatTypes.HEAD.getId(), id("wood")),
                                new MaterialLayer(ToolStatTypes.EXTRA.getId(), id("stone")),
                                new MaterialLayer(ToolStatTypes.HANDLE.getId(), id("wood"))), "Requirement order overrides stored part-layer order");
                helper.assertValueEqual(plate.getCount(), 1, "A preview must retain the physical plate");
                plate.set(TLDataComponents.MATERIALS,
                                List.of(new MaterialLayer(ToolStatTypes.HEAD.getId(), id("wood"))));
                helper.assertTrue(ToolAssembly.assemble(probe, List.of(plate, rod)).isEmpty(),
                                "A multi-role part must carry every one of its defined roles");
                helper.succeed();
        }

        // Inputs
        static ResourceLocation id(String path) {
                return ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, path);
        }

        static ItemStack part(ToolParts.Type type, ResourceLocation material) {
                var definition = ToolParts.part(type);
                ItemStack stack = new ItemStack(TLItems.Parts.item(type));
                stack.set(TLDataComponents.MATERIALS, definition.statTypes().stream()
                                .map(role -> new MaterialLayer(role, material)).toList());
                return stack;
        }

        static List<ItemStack> parts(ToolDefinition definition, ResourceLocation material) {
                List<ItemStack> result = new ArrayList<>();
                for (var requirement : definition.partRequirements()) {
                        var part = TLRegistries.PARTS.get(requirement.part());
                        ItemStack stack = new ItemStack(TLItems.Parts.get(requirement.part()).get());
                        stack.set(TLDataComponents.MATERIALS, part.statTypes().stream()
                                        .map(role -> new MaterialLayer(role, material)).toList());
                        result.add(stack);
                }
                return result;
        }

        static ItemStack tool(TLTools.Type type, ResourceLocation material) {
                ToolDefinition definition = TLTools.tool(type);
                return ToolAssembly.assemble(definition, exampleParts(definition,
                                definition.defaultMaterials(TLRegistries.MATERIALS.get(material))));
        }

        static ItemStack legacyTool(TLTools.Type type, ResourceLocation material) {
                var item = TLItems.Tools.item(type);
                ItemStack stack = new ItemStack(item);
                stack.set(TLDataComponents.MATERIALS, item.getDefinition().parts().stream()
                                .map(part -> new MaterialLayer(part.statType(), material)).toList());
                ToolState.initialize(stack, false);
                item.calculateStats(stack);
                return stack;
        }

        static List<ItemStack> copies(List<ItemStack> inputs) {
                return inputs.stream().map(ItemStack::copy).toList();
        }

        static void assertInputs(GameTestHelper helper, List<ItemStack> actual, List<ItemStack> expected) {
                helper.assertValueEqual(actual.size(), expected.size(), "Input slot count");
                for (int index = 0; index < actual.size(); index++) {
                        helper.assertTrue(ItemStack.matches(actual.get(index), expected.get(index)),
                                        "Preview mutated input slot " + index);
                }
        }
}
