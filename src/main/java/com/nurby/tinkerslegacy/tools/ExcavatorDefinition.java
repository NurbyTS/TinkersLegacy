package com.nurby.tinkerslegacy.tools;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.library.tool.ToolAreaActions;
import com.nurby.tinkerslegacy.library.tool.ToolPart;
import com.nurby.tinkerslegacy.registry.ToolStatTypes;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.context.UseOnContext;

import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

import java.util.List;
import java.util.Map;

public class ExcavatorDefinition extends AreaToolDefinition {
        public ExcavatorDefinition() {
                super(id("excavator"), List.of(
                                new ToolPart(id("tough_tool_rod"), ToolStatTypes.HANDLE.getId()),
                                new ToolPart(id("excavator_head"), ToolStatTypes.HEAD.getId()),
                                new ToolPart(id("large_plate"), ToolStatTypes.HEAD.getId()),
                                new ToolPart(id("tough_binding"), ToolStatTypes.EXTRA.getId())));
        }

        private static ResourceLocation id(String name) {
                return ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, name);
        }

        // Stats
        @Override
        public float miningSpeedModifier() { return 0.28F; }

        @Override
        public float damagePotential() { return 1.25F; }

        @Override
        public float attackSpeed() { return 0.7F; }

        @Override
        public float durabilityModifier() { return 1.75F; }

        @Override
        public float repairModifierForPart(int index) {
                return index == 1 ? 1.75F : 1.3125F;
        }

        // Actions
        @Override
        public void addExtraToolRules(ItemStack stack, Map<String, Number> stats, List<Tool.Rule> rules, float miningSpeed) {
                rules.add(Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_SHOVEL, miningSpeed));
        }

        @Override
        public boolean canPerformAction(ItemStack stack, ItemAbility ability) {
                return ability == ItemAbilities.SHOVEL_DIG || ability == ItemAbilities.SHOVEL_FLATTEN;
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
                return ToolAreaActions.flatten(context);
        }
}
