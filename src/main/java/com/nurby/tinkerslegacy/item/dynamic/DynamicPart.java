package com.nurby.tinkerslegacy.item.dynamic;

import com.nurby.tinkerslegacy.library.ClientHandler;
import com.nurby.tinkerslegacy.util.TinkersUtils;
import com.nurby.tinkerslegacy.library.material.layer.MaterialLayer;
import com.nurby.tinkerslegacy.library.part.PartDefinition;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;

public class DynamicPart extends Item {
    private final DeferredHolder<PartDefinition, PartDefinition> definition;

    public DynamicPart(DeferredHolder<PartDefinition, PartDefinition> definition) {
        super(new Item.Properties());
        this.definition = definition;
    }

    public PartDefinition definition() {
        return definition.get();
    }

    @Override
    public Component getName(ItemStack stack) {
        List<MaterialLayer> materials =
                stack.get(TLDataComponents.MATERIALS);

        return TinkersUtils.getMaterialPartName(
                definition().id(),
                materials
        );
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltipComponents,
            TooltipFlag tooltipFlag
    ) {
        if (FMLEnvironment.dist.isClient()) {
            ClientHandler.addPartTooltip(
                    stack,
                    context,
                    tooltipComponents,
                    tooltipFlag
            );
        }
    }
}