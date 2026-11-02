package com.nurby.tinkerslegacy.api.station;

import com.nurby.tinkerslegacy.TinkersLegacy;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.IronBarsBlock;

import java.util.List;

public final class StationLayout {

        // Positions
        public static final List<Position> MAINTENANCE = List.of(
                        new Position(33, 42), new Position(15, 62), new Position(11, 37),
                        new Position(33, 19), new Position(55, 37), new Position(51, 62));

        public static final List<Position> PART_BUILDER = List.of(
                        new Position(26, 35), new Position(48, 26),
                        new Position(48, 44), new Position(132, 35));

        private StationLayout() {
        }

        // Getters
        public static Position part(ResourceLocation tool, int index) {
                if (tool != null && tool.getNamespace().equals(TinkersLegacy.MODID) && index < 4) {
                        // Preserve registry input order while matching the original 1.12 tool silhouettes.
                        List<Position> positions = switch (tool.getPath()) {
                                case "pickaxe" -> List.of(new Position(53, 22), new Position(15, 60), new Position(33, 42));
                                case "shovel" -> List.of(new Position(51, 24), new Position(33, 42), new Position(13, 62));
                                case "hatchet", "kama" -> List.of(new Position(31, 22), new Position(22, 53), new Position(51, 34));
                                case "mattock" -> List.of(new Position(31, 22), new Position(51, 34), new Position(22, 53));
                                case "broadsword", "longsword" -> List.of(new Position(48, 26), new Position(12, 62), new Position(30, 44));
                                case "rapier" -> List.of(new Position(18, 26), new Position(52, 62), new Position(32, 44));
                                case "frypan" -> List.of(new Position(12, 62), new Position(34, 36));
                                case "battlesign" -> List.of(new Position(27, 60), new Position(27, 34));
                                case "hammer" -> List.of(new Position(21, 52), new Position(44, 29),
                                                new Position(57, 48), new Position(25, 16));
                                case "excavator" -> List.of(new Position(25, 46), new Position(45, 26),
                                                new Position(25, 26), new Position(7, 62));
                                case "lumberaxe" -> List.of(new Position(32, 46), new Position(33, 22),
                                                new Position(53, 38), new Position(13, 62));
                                case "scythe" -> List.of(new Position(17, 54), new Position(36, 19),
                                                new Position(56, 29), new Position(37, 47));
                                case "cleaver" -> List.of(new Position(9, 64), new Position(25, 36),
                                                new Position(47, 30), new Position(33, 58));
                                case "shortbow" -> List.of(new Position(36, 23), new Position(14, 45), new Position(38, 47));
                                case "longbow" -> List.of(new Position(44, 19), new Position(10, 53),
                                                new Position(17, 26), new Position(38, 47));
                                case "crossbow" -> List.of(new Position(38, 47), new Position(44, 19),
                                                new Position(14, 23), new Position(18, 51));
                                case "arrow" -> List.of(new Position(32, 41), new Position(50, 23), new Position(14, 59));
                                case "bolt" -> List.of(new Position(40, 37), new Position(20, 53));
                                case "shuriken" -> List.of(new Position(20, 29), new Position(44, 29),
                                                new Position(44, 53), new Position(20, 53));
                                default -> List.of();
                        };
                        if (index < positions.size()) {
                                return positions.get(index);
                        }
                }
                return new Position(11 + index % 3 * 24, 24 + index / 3 * 22);
        }

        public static Position dormant(int index) {
                return index < 4 ? new Position(87 + index * 20, 62)
                                : new Position(index == 4 ? 148 : 88, 38);
        }

        // Tabletop
        public static Display tabletop(StationDefinition station, int slot, ItemStack stack) {
                boolean block = stack.getItem() instanceof BlockItem;
                boolean upright = block && !(((BlockItem) stack.getItem()).getBlock() instanceof IronBarsBlock);
                float scale = block ? 0.375F : 0.8F;
                float x = 0;
                float z = 0;
                float y = 1.03125F;
                if (station.family().equals(StationOperations.CRAFTING_FAMILY) && slot < 9) {
                        scale = 0.125F;
                        x = 3F / 16F - slot % 3 * 3F / 16F;
                        z = 3F / 16F - slot / 3 * 3F / 16F;
                        y = 1 + scale / 32F;
                } else if (station.family().equals(StationOperations.TOOL_FAMILY) && slot < MAINTENANCE.size()) {
                        // The original always displays the repair layout, even during assembly.
                        Position position = MAINTENANCE.get(slot);
                        x = (33 - position.x()) / 61F;
                        z = (42 - position.y()) / 61F;
                        scale *= 0.46875F * (slot == 0 ? 1.3F : 1);
                } else if (station.family().equals(StationOperations.PART_FAMILY) && slot < 4) {
                        // Our pattern slot comes first; original material inputs came first.
                        int original = switch (slot) {
                                case 0 -> 2;
                                case 1 -> 0;
                                case 2 -> 1;
                                default -> 3;
                        };
                        x = original % 2 == 0 ? 0.2125F : -0.2125F;
                        z = original < 2 ? -0.2125F : 0.2125F;
                        scale *= 0.46875F;
                } else if (!station.family().equals(StationOperations.STENCIL_FAMILY) || slot != 0) {
                        // Unknown families and additional slots retain a bounded fallback layout.
                        int columns = (int) Math.ceil(Math.sqrt(station.inputSlots()));
                        int rows = (station.inputSlots() + columns - 1) / columns;
                        float step = 0.8F / columns;
                        x = ((columns - 1) / 2F - slot % columns) * step;
                        z = ((rows - 1) / 2F - slot / columns) * step;
                        scale = step * 0.7F;
                }
                return new Display(x, upright ? 1 + scale / 2 : y, z, scale, upright);
        }

        public record Display(float x, float y, float z, float scale, boolean upright) {
        }

        public record Position(int x, int y) {
        }
}
