package com.nurby.tinkerslegacy.tools;

import com.nurby.tinkerslegacy.library.tool.ToolPartRequirement;
import com.nurby.tinkerslegacy.library.material.layer.MaterialLayer;
import com.nurby.tinkerslegacy.registry.TLRegistries;
import com.nurby.tinkerslegacy.registry.ToolParts;
import com.nurby.tinkerslegacy.registry.ToolStatTypes;
import java.util.List;

public final class BoltDefinition extends ProjectileToolDefinition {
        public BoltDefinition() {
                super("bolt", Kind.BOLT,
                                new ToolPartRequirement(ToolParts.get(ToolParts.Type.BOLT_CORE).getId(),
                                                List.of(ToolStatTypes.ARROW_SHAFT.getId(), ToolStatTypes.HEAD.getId())), fletching());
        }

        @Override
        public boolean acceptsMaterials(List<MaterialLayer> materials) {
                return TLRegistries.MATERIALS.get(materials.get(1).material())
                                .recipeInfo().castable();
        }

        @Override
        public List<Integer> renderLayers() {
                return List.of(0, 1, 2);
        }
}
