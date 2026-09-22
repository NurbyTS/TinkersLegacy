package com.nurby.tinkerslegacy.tools;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.library.material.layer.MaterialLayer;
import com.nurby.tinkerslegacy.library.material.stat.MaterialStatMap;
import com.nurby.tinkerslegacy.library.tool.ToolDefinition;
import com.nurby.tinkerslegacy.library.tool.ToolPart;
import com.nurby.tinkerslegacy.registry.ToolStatTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MattockDefinition extends ToolDefinition {

    public MattockDefinition() {
        super(
                id("mattock"),
                List.of(
                        new ToolPart(
                                id("axe_head"),
                                ToolStatTypes.HEAD.getId()
                        ),
                        new ToolPart(
                                id("shovel_head"),
                                ToolStatTypes.HEAD.getId()
                        ),
                        new ToolPart(
                                id("tool_rod"),
                                ToolStatTypes.HANDLE.getId()
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
    public float baseDamage() {
        return 3.7f;
    }

    @Override
    public float damagePotential() {
        return 0.9F;
    }

    @Override
    public float attackSpeed() {
        return 0.9f;
    }

    @Override
    public float knockback() {
        return 1.1f;
    }

    @Override
    protected int calculateHarvestLevel(
            List<MaterialLayer> layers
    ) {
        return -1;
    }

    @Override
    protected void calculateSpecialStats(
            List<MaterialLayer> layers,
            Map<String, Number> stats
    ) {
        MaterialStatMap axeHead =
                getPartStats(
                        layers,
                        id("axe_head")
                ).statMap();

        MaterialStatMap shovelHead =
                getPartStats(
                        layers,
                        id("shovel_head")
                ).statMap();

        stats.put(
                "harvest_level_axe",
                axeHead.getInt("harvest_level")
        );

        stats.put(
                "harvest_level_shovel",
                shovelHead.getInt("harvest_level")
        );
    }

    @Override
    public void addExtraToolRules(ItemStack stack, Map<String, Number> stats, List<Tool.Rule> rules, float miningSpeed) {
        rules.add(
                Tool.Rule.minesAndDrops(
                        BlockTags.MINEABLE_WITH_AXE,
                        miningSpeed
                )
        );

        rules.add(
                Tool.Rule.minesAndDrops(
                        BlockTags.MINEABLE_WITH_SHOVEL,
                        miningSpeed
                )
        );
    }

    @Override
    public InteractionResult useOn(
            UseOnContext context
    ) {
        return Items.DIAMOND_HOE.useOn(context);
    }

    @Override
    public boolean canPerformAction(
            ItemStack stack,
            ItemAbility itemAbility
    ) {
        return itemAbility == ItemAbilities.AXE_DIG
                || itemAbility == ItemAbilities.SHOVEL_DIG
                || itemAbility == ItemAbilities.HOE_TILL;
    }

    private static float round(float value) {
        return Math.round(value * 100F) / 100F;
    }
}