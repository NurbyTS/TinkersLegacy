package com.nurby.tinkerslegacy.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.library.material.MaterialDefinition;
import com.nurby.tinkerslegacy.library.material.layer.MaterialLayer;
import com.nurby.tinkerslegacy.library.tool.ToolDefinition;
import com.nurby.tinkerslegacy.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLItems;
import com.nurby.tinkerslegacy.registry.TLRegistries;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ToolCommand {

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher =
                event.getDispatcher();

        dispatcher.register(
                Commands.literal("tinkerslegacy")
                        .then(
                                Commands.literal("tool")
                                        .then(
                                                Commands.argument(
                                                                "tool",
                                                                StringArgumentType.word()
                                                        )
                                                        .suggests((context, builder) -> {
                                                            for (Map.Entry<ResourceKey<ToolDefinition>, ToolDefinition> toolEntry : TLRegistries.TOOLS.entrySet()) {
                                                                ToolDefinition tool = toolEntry.getValue();

                                                                builder.suggest(
                                                                        tool.id().getPath()
                                                                );
                                                            }

                                                            return builder.buildFuture();
                                                        })
                                                        .executes(context ->
                                                                showUsage(
                                                                        context.getSource()
                                                                )
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "material",
                                                                                StringArgumentType.greedyString()
                                                                        )
                                                                        .suggests((context, builder) -> {
                                                                            for (Map.Entry<ResourceKey<MaterialDefinition>, MaterialDefinition> materialEntry : TLRegistries.MATERIALS.entrySet()) {
                                                                                MaterialDefinition material = materialEntry.getValue();

                                                                                builder.suggest(
                                                                                        material.id().getPath()
                                                                                );
                                                                            }

                                                                            return builder.buildFuture();
                                                                        })
                                                                        .executes(context ->
                                                                                createTool(
                                                                                        context.getSource(),
                                                                                        StringArgumentType.getString(
                                                                                                context,
                                                                                                "tool"
                                                                                        ),
                                                                                        StringArgumentType.getString(
                                                                                                context,
                                                                                                "material"
                                                                                        )
                                                                                )
                                                                        )
                                                        )
                                        )
                        )
        );
    }

    private static int createTool(
            CommandSourceStack source,
            String toolName,
            String materialsInput
    ) {
        ResourceLocation toolId =
                ResourceLocation.fromNamespaceAndPath(
                        TinkersLegacy.MODID,
                        toolName
                );

        ToolDefinition definition = TLRegistries.TOOLS.get(toolId);

        if (definition == null) {
            source.sendFailure(
                    Component.literal(
                            "Unknown tool: " + toolName
                    )
            );

            return 0;
        }

        String[] materialNames =
                materialsInput.trim().split("\\s+");

        int expected =
                definition.parts().size();

        if (materialNames.length != expected) {
            source.sendFailure(
                    Component.literal(
                            "Tool "
                                    + toolName
                                    + " requires "
                                    + expected
                                    + " materials, but "
                                    + materialNames.length
                                    + " were provided."
                    )
            );

            return 0;
        }

        List<MaterialLayer> layers =
                new ArrayList<>();

        for (int i = 0; i < expected; i++) {
            ResourceLocation materialId =
                    ResourceLocation.fromNamespaceAndPath(
                            TinkersLegacy.MODID,
                            materialNames[i]
                    );

            MaterialDefinition material = TLRegistries.MATERIALS.get(materialId);

            if (material == null) {
                source.sendFailure(
                        Component.literal(
                                "Unknown material: "
                                        + materialNames[i]
                        )
                );

                return 0;
            }

            var part = definition.parts().get(i);

            if (!material.stats().containsKey(part.statType())) {
                source.sendFailure(
                        Component.literal(
                                "Material "
                                        + materialNames[i]
                                        + " does not support stat type "
                                        + part.statType().getPath()
                                        + " required by part "
                                        + part.part().getPath()
                        )
                );

                return 0;
            }

            layers.add(
                    new MaterialLayer(
                            part.statType(),
                            material.id()
                    )
            );
        }

        DynamicTool item = TLItems.Tools.get(definition);

        if (item == null) {
            source.sendFailure(
                    Component.literal(
                            "No item registered for tool: "
                                    + toolName
                    )
            );

            return 0;
        }

        ItemStack stack =
                new ItemStack(item);

        stack.set(
                TLDataComponents.MATERIALS,
                List.copyOf(layers)
        );

        item.calculateStats(stack);

        if (source.getPlayer() != null) {
            if (!source.getPlayer().getInventory().add(stack)) {
                source.getPlayer().drop(
                        stack,
                        false
                );
            }
        } else {
            source.sendFailure(
                    Component.literal(
                            "This command must be run by a player."
                    )
            );

            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "Created "
                                + materialsInput
                                + " "
                                + toolName
                ),
                true
        );

        return 1;
    }

    private static int showUsage(
            CommandSourceStack source
    ) {
        source.sendFailure(
                Component.literal(
                        "Usage: /tinkerslegacy tool <tool> <material> [material] ..."
                )
        );

        return 0;
    }

    private ToolCommand() {
    }
}