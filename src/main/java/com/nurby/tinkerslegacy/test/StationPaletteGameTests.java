package com.nurby.tinkerslegacy.test;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.client.screen.StationPalette;
import com.nurby.tinkerslegacy.library.tool.ToolDefinition;
import com.nurby.tinkerslegacy.library.tool.ToolPart;
import com.nurby.tinkerslegacy.registry.TLTools;
import com.nurby.tinkerslegacy.registry.ToolParts;
import com.nurby.tinkerslegacy.registry.ToolStatTypes;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(TinkersLegacy.MODID)
@PrefixGameTestTemplate(false)
public final class StationPaletteGameTests {

        // Preview Colors
        @GameTest(template = "empty")
        public static void originalPhysicalPartPalettesRespectSavedOrderAndRepeatedRoles(GameTestHelper helper) {
                assertColors(helper, TLTools.Type.PICKAXE, List.of(0xC1C1C1, 0x684E1E, 0x2376DD));
                assertColors(helper, TLTools.Type.MATTOCK, List.of(0xC1C1C1, 0x2376DD, 0x684E1E));
                assertColors(helper, TLTools.Type.SHORTBOW, List.of(0x684E1E, 0x2376DD, 0xFFFFFF));
                assertColors(helper, TLTools.Type.LONGBOW, List.of(0x684E1E, 0x2376DD, 0xC1C1C1, 0xFFFFFF));
                assertColors(helper, TLTools.Type.CROSSBOW, List.of(0x684E1E, 0x2376DD, 0xC1C1C1, 0xFFFFFF));
                assertColors(helper, TLTools.Type.ARROW, List.of(0x684E1E, 0xC1C1C1, 0x2376DD));
                assertColors(helper, TLTools.Type.SHURIKEN, List.of(0x684E1E, 0xC1C1C1, 0x2376DD, 0x7146B0));
                assertColors(helper, TLTools.Type.SCYTHE, List.of(0x684E1E, 0xC1C1C1, 0x2376DD, 0x7146B0));
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void boltLayerPaletteDiffersFromItsTwoPhysicalSlotHints(GameTestHelper helper) {
                var bolt = TLTools.tool(TLTools.Type.BOLT);
                assertColors(helper, TLTools.Type.BOLT, List.of(0xC1C1C1, 0x2376DD));
                helper.assertValueEqual(bolt.renderLayers().stream().map(layer -> StationPalette.layer(bolt, layer)).toList(),
                                List.of(0x684E1E, 0xC1C1C1, 0x2376DD), "The rendered bolt separates brown shaft, gray coating, and blue fletching");
                var crossbow = TLTools.tool(TLTools.Type.CROSSBOW);
                helper.assertValueEqual(crossbow.renderLayers().stream().map(layer -> StationPalette.layer(crossbow, layer)).toList(),
                                List.of(0x684E1E, 0x2376DD, 0xC1C1C1, 0xFFFFFF), "Multiple stat roles do not shift the four visible crossbow colors");
                ToolDefinition extension = new ToolDefinition(ResourceLocation.fromNamespaceAndPath("example", "pickaxe"),
                                List.of(new ToolPart(ToolParts.get(ToolParts.Type.PICKAXE_HEAD).getId(), ToolStatTypes.HEAD.getId()),
                                                new ToolPart(ToolParts.get(ToolParts.Type.AXE_HEAD).getId(), ToolStatTypes.HEAD.getId()))) {
                };
                helper.assertValueEqual(StationPalette.part(extension, 0), 0xC1C1C1, "Unknown tools retain the stat-based palette fallback");
                helper.assertValueEqual(StationPalette.part(extension, 1), 0x2376DD, "Another namespace cannot accidentally inherit a built-in palette");
                helper.succeed();
        }

        private static void assertColors(GameTestHelper helper, TLTools.Type type, List<Integer> expected) {
                var tool = TLTools.tool(type);
                helper.assertValueEqual(java.util.stream.IntStream.range(0, tool.partRequirements().size())
                                .mapToObj(index -> StationPalette.part(tool, index)).toList(), expected, "Original physical-part colors for " + type);
        }
}
