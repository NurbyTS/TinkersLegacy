package com.nurby.tinkerslegacy.library.station;

import com.nurby.tinkerslegacy.block.StationBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class StationConnections {
        private static final int MAX_STATIONS = 64;
        private static final List<Direction> DIRECTIONS = List.of(
                        Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);

        private StationConnections() {
        }

        // Getters
        public static List<BlockPos> find(Level level, BlockPos start) {
                List<BlockPos> stations = new ArrayList<>();
                Set<BlockPos> visited = new HashSet<>();
                ArrayDeque<BlockPos> pending = new ArrayDeque<>();
                pending.add(start.immutable());

                while (!pending.isEmpty() && stations.size() < MAX_STATIONS) {
                        BlockPos position = pending.removeFirst();
                        if (!visited.add(position) || !level.hasChunkAt(position)
                                        || !(level.getBlockState(position).getBlock() instanceof StationBlock)) {
                                continue;
                        }
                        stations.add(position);
                        for (Direction direction : DIRECTIONS) {
                                pending.addLast(position.relative(direction));
                        }
                }
                return List.copyOf(stations);
        }
}
