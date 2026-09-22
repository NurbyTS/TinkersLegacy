package com.nurby.tinkerslegacy.tools;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.library.material.layer.MaterialLayer;
import com.nurby.tinkerslegacy.library.material.stat.MaterialStat;
import com.nurby.tinkerslegacy.library.material.stat.MaterialStatMap;
import com.nurby.tinkerslegacy.library.tool.ToolDefinition;
import com.nurby.tinkerslegacy.library.tool.ToolPart;
import com.nurby.tinkerslegacy.registry.ToolStatTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BroadswordDefinition extends ToolDefinition {

    public BroadswordDefinition() {
        super(
                id("broadsword"),
                List.of(
                        new ToolPart(id("sword_blade"), ToolStatTypes.HEAD.getId()),
                        new ToolPart(id("tool_rod"), ToolStatTypes.HANDLE.getId()),
                        new ToolPart(id("wide_guard"), ToolStatTypes.EXTRA.getId())
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
    public boolean isTool() {
        return false;
    }

    @Override
    public float baseDamage() {
        return 2.0f;
    }

    @Override
    public float attackSpeed() {
        return 1.6f;
    }

    @Override
    public float durabilityModifier() {
        return 1.1f;
    }

    @Override
    public void addExtraToolRules(ItemStack stack, Map<String, Number> stats, List<Tool.Rule> rules, float miningSpeed) {
        rules.add(
                Tool.Rule.minesAndDrops(
                        BlockTags.SWORD_EFFICIENT,
                        miningSpeed
                )
        );
    }

    @Override
    public InteractionResult useOn(
            UseOnContext context
    ) {
        return Items.DIAMOND_SWORD.useOn(context);
    }

    @Override
    public boolean canPerformAction(
            ItemStack stack,
            ItemAbility itemAbility
    ) {
        return itemAbility == ItemAbilities.SWORD_SWEEP;
    }
}