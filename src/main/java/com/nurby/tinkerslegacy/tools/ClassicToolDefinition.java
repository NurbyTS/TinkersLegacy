package com.nurby.tinkerslegacy.tools;

import com.nurby.tinkerslegacy.library.tool.ToolDefinition;
import com.nurby.tinkerslegacy.library.tool.ToolPart;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public abstract class ClassicToolDefinition extends ToolDefinition {
        protected ClassicToolDefinition(ResourceLocation id, List<ToolPart> parts) {
                super(id, parts);
        }
}
