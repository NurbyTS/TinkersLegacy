package com.nurby.tinkerslegacy.content.tools;

import com.nurby.tinkerslegacy.api.tool.ToolPartRequirement;
import com.nurby.tinkerslegacy.registry.tools.ToolParts;
import com.nurby.tinkerslegacy.registry.tools.ToolStatTypes;
import java.util.List;

public final class ArrowDefinition extends ProjectileToolDefinition {
        public ArrowDefinition() {
                super("arrow", Kind.ARROW,
                                new ToolPartRequirement(ToolParts.get(ToolParts.Type.ARROW_SHAFT).getId(),
                                                List.of(ToolStatTypes.ARROW_SHAFT.getId())),
                                new ToolPartRequirement(ToolParts.get(ToolParts.Type.ARROW_HEAD).getId(),
                                                List.of(ToolStatTypes.HEAD.getId())), fletching());
        }
}
