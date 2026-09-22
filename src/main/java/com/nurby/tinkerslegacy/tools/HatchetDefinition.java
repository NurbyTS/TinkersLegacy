package com.nurby.tinkerslegacy.tools;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.library.material.layer.MaterialLayer;
import com.nurby.tinkerslegacy.library.material.stat.MaterialStat;
import com.nurby.tinkerslegacy.library.material.stat.MaterialStatMap;
import com.nurby.tinkerslegacy.library.tool.ToolDefinition;
import com.nurby.tinkerslegacy.library.tool.ToolPart;
import com.nurby.tinkerslegacy.registry.ToolStatTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.common.ItemAbilities;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HatchetDefinition extends ToolDefinition {

    public HatchetDefinition() {
        super(
                id("hatchet"),
                List.of(
                        new ToolPart(id("axe_head"), ToolStatTypes.HEAD.getId()),
                        new ToolPart(id("tool_rod"), ToolStatTypes.HANDLE.getId()),
                        new ToolPart(id("binding"), ToolStatTypes.EXTRA.getId())
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
        return 1.55f;
    }

    @Override
    public float damagePotential() {
        return 1.1F;
    }

    @Override
    public float knockback() {
        return 1.3f;
    }

    @Override
    public float attackSpeed() {
        return 1.1f;
    }

    @Override
    public void addExtraToolRules(ItemStack stack, Map<String, Number> stats, List<Tool.Rule> rules, float miningSpeed) {
        rules.add(
                Tool.Rule.minesAndDrops(
                        BlockTags.MINEABLE_WITH_AXE,
                        miningSpeed
                )
        );
    }

    @Override
    public boolean canPerformAction(
            ItemStack stack,
            ItemAbility itemAbility
    ) {
        return itemAbility == ItemAbilities.AXE_STRIP
                || itemAbility == ItemAbilities.AXE_SCRAPE
                || itemAbility == ItemAbilities.AXE_WAX_OFF;
    }

    @Override
    public boolean canDisableShield(
            ItemStack stack,
            ItemStack shield,
            LivingEntity entity,
            LivingEntity attacker
    ) {
        return true;
    }

    @Override
    public boolean damagesOnBlockBreak(
            ItemStack stack,
            Level level,
            BlockState state,
            BlockPos pos,
            LivingEntity entity
    ) {
        return !state.is(BlockTags.LEAVES);
    }
}