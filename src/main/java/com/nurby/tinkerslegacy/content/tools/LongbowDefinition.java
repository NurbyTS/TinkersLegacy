package com.nurby.tinkerslegacy.content.tools;

import com.nurby.tinkerslegacy.api.tool.ToolPartRequirement;
import com.nurby.tinkerslegacy.registry.tools.ToolParts;
import com.nurby.tinkerslegacy.registry.tools.ToolStatTypes;
import java.util.List;

public final class LongbowDefinition extends RangedToolDefinition {
        public LongbowDefinition() {
                super("longbow", Kind.LONGBOW, limb(), limb(),
                                new ToolPartRequirement(ToolParts.get(ToolParts.Type.LARGE_PLATE).getId(),
                                                List.of(ToolStatTypes.EXTRA.getId())), string());
        }
}
