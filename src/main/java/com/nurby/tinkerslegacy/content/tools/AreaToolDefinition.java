package com.nurby.tinkerslegacy.content.tools;

import com.nurby.tinkerslegacy.api.station.StationOperations;
import com.nurby.tinkerslegacy.api.station.StationRequirement;
import com.nurby.tinkerslegacy.api.tool.ToolAreaActions;
import com.nurby.tinkerslegacy.api.tool.ToolPart;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public abstract class AreaToolDefinition extends ClassicToolDefinition {
        protected AreaToolDefinition(ResourceLocation id, List<ToolPart> parts) {
                super(id, parts);
        }

        // Assembly
        @Override
        public StationRequirement stationRequirement(ResourceLocation operation) {
                return new StationRequirement(StationOperations.TOOL_FAMILY, operation.equals(StationOperations.ASSEMBLE) ? 2 : 1);
        }

        // Area Mining
        public int areaDepth() {
                return 1;
        }

        public List<BlockPos> areaBlocks(ServerPlayer player, BlockPos origin, BlockState state) {
                return ToolAreaActions.blocks(player, origin, areaDepth());
        }
}
