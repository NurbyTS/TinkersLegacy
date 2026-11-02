package com.nurby.tinkerslegacy.content.tools;

import com.nurby.tinkerslegacy.api.tool.ToolDefinition;
import com.nurby.tinkerslegacy.api.tool.ToolPart;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public abstract class ClassicToolDefinition extends ToolDefinition {
        protected ClassicToolDefinition(ResourceLocation id, List<ToolPart> parts) {
                super(id, parts);
        }
}
