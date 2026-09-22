package com.nurby.tinkerslegacy.tools;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.library.material.layer.MaterialLayer;
import com.nurby.tinkerslegacy.library.material.stat.MaterialStatMap;
import com.nurby.tinkerslegacy.library.tool.ToolDefinition;
import com.nurby.tinkerslegacy.library.tool.ToolPart;
import com.nurby.tinkerslegacy.registry.ToolStatTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Tool;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PickaxeDefinition extends ToolDefinition {

    public PickaxeDefinition() {
        super(
                id("pickaxe"),
                List.of(
                        new ToolPart(
                                id("pick_head"),
                                ToolStatTypes.HEAD.getId()
                        ),
                        new ToolPart(
                                id("tool_rod"),
                                ToolStatTypes.HANDLE.getId()
                        ),
                        new ToolPart(
                                id("binding"),
                                ToolStatTypes.EXTRA.getId()
                        )
                )
        );
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(
                TinkersLegacy.MODID,
                path
        );
    }

    @Override
    public float attackSpeed() {
        return 1.2f;
    }

    @Override
    public void addExtraToolRules(ItemStack stack, Map<String, Number> stats, List<Tool.Rule> rules, float miningSpeed) {
        rules.add(
                Tool.Rule.minesAndDrops(
                        BlockTags.MINEABLE_WITH_PICKAXE,
                        miningSpeed
                )
        );
    }

    @Override
    public boolean canPerformAction(
            ItemStack stack,
            ItemAbility itemAbility
    ) {
        return itemAbility == ItemAbilities.PICKAXE_DIG;
    }
}