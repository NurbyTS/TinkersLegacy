package com.nurby.tinkerslegacy.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.library.material.MaterialDefinition;
import com.nurby.tinkerslegacy.library.material.layer.MaterialLayer;
import com.nurby.tinkerslegacy.library.tool.ToolDefinition;
import com.nurby.tinkerslegacy.library.tool.ToolAssembly;
import com.nurby.tinkerslegacy.library.tool.ToolState;
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
                                                                                                                                                                if (!material.available()) {
                                                                                                                                                                        continue;
                                                                                                                                                                }

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

                List<MaterialLayer> layers = new ArrayList<>();
                if (materialNames.length == 1) {
                        ResourceLocation materialId = ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, materialNames[0]);
                        MaterialDefinition material = TLRegistries.MATERIALS.get(materialId);
                        if (material == null || !material.available()) {
                                source.sendFailure(Component.literal("Unknown material: " + materialNames[0]));
                                return 0;
                        }
                        layers.addAll(definition.defaultMaterials(material));
                } else {
                        boolean physicalParts = materialNames.length == definition.partRequirements().size();
                        if (!physicalParts && materialNames.length != definition.parts().size()) {
                                source.sendFailure(Component.literal("Provide one default material, one material per physical part,"
                                                + " or " + definition.parts().size() + " explicit material layers."));
                                return 0;
                        }
                        int index = 0;
                        for (var requirement : definition.partRequirements()) {
                                for (var role : requirement.statTypes()) {
                                        ResourceLocation materialId = ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, materialNames[index]);
                                        MaterialDefinition material = TLRegistries.MATERIALS.get(materialId);
                                        if (material == null || !material.available() || !material.stats().containsKey(role)) {
                                                source.sendFailure(Component.literal("Material " + materialNames[index]
                                                                + " cannot supply " + role.getPath() + " for " + requirement.part().getPath()));
                                                return 0;
                                        }
                                        layers.add(new MaterialLayer(role, materialId));
                                        if (!physicalParts) index++;
                                }
                                if (physicalParts) index++;
                        }
                }

                ItemStack stack = ToolAssembly.create(definition, layers);

                if (stack.isEmpty()) {
                        source.sendFailure(Component.literal("Invalid assembly or no item registered for tool: " + toolName));
                        return 0;
                }

                ToolState.initialize(stack, true);

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