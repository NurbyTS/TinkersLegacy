package com.nurby.tinkerslegacy.content.tools;

import com.nurby.tinkerslegacy.api.tool.ToolPartRequirement;
import com.nurby.tinkerslegacy.registry.TLRegistries;
import com.nurby.tinkerslegacy.registry.tools.ToolParts;
import com.nurby.tinkerslegacy.registry.tools.ToolStatTypes;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class BoltDefinition extends ProjectileToolDefinition {
        public BoltDefinition() {
                super("bolt", Kind.BOLT,
                                new ToolPartRequirement(ToolParts.get(ToolParts.Type.BOLT_CORE).getId(),
                                                List.of(ToolStatTypes.ARROW_SHAFT.getId(), ToolStatTypes.HEAD.getId())), fletching());
        }

        @Override
        public boolean acceptsMaterials(List<ResourceLocation> materials) {
            return TLRegistries.MATERIALS.get(materials.getFirst())
                                .recipeInfo().castable();
        }

        @Override
        public List<Integer> renderLayers() {
                return List.of(0, 1, 2);
        }
}
