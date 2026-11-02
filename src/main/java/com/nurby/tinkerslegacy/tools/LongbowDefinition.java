package com.nurby.tinkerslegacy.tools;

import com.nurby.tinkerslegacy.library.tool.ToolPartRequirement;
import com.nurby.tinkerslegacy.registry.ToolParts;
import com.nurby.tinkerslegacy.registry.ToolStatTypes;
import java.util.List;

public final class LongbowDefinition extends RangedToolDefinition {
        public LongbowDefinition() {
                super("longbow", Kind.LONGBOW, limb(), limb(),
                                new ToolPartRequirement(ToolParts.get(ToolParts.Type.LARGE_PLATE).getId(),
                                                List.of(ToolStatTypes.EXTRA.getId())), string());
        }
}
