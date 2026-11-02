package com.nurby.tinkerslegacy.tools;

import com.nurby.tinkerslegacy.library.tool.ToolPartRequirement;
import com.nurby.tinkerslegacy.registry.ToolParts;
import com.nurby.tinkerslegacy.registry.ToolStatTypes;
import java.util.List;

public final class CrossbowDefinition extends RangedToolDefinition {
        public CrossbowDefinition() {
                super("crossbow", Kind.CROSSBOW,
                                new ToolPartRequirement(ToolParts.get(ToolParts.Type.TOUGH_TOOL_ROD).getId(),
                                                List.of(ToolStatTypes.HANDLE.getId(), ToolStatTypes.EXTRA.getId())),
                                limb(),
                                new ToolPartRequirement(ToolParts.get(ToolParts.Type.TOUGH_BINDING).getId(),
                                                List.of(ToolStatTypes.EXTRA.getId())), string());
        }
}
