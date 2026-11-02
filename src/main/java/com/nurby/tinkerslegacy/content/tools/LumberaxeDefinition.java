package com.nurby.tinkerslegacy.content.tools;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.api.tool.ToolAreaActions;
import com.nurby.tinkerslegacy.api.tool.ToolPart;
import com.nurby.tinkerslegacy.registry.tools.ToolStatTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

import java.util.List;
import java.util.Map;

public class LumberaxeDefinition extends AreaToolDefinition {
        public LumberaxeDefinition() {
                super(id("lumberaxe"), List.of(
                                new ToolPart(id("tough_tool_rod"), ToolStatTypes.HANDLE.getId()),
                                new ToolPart(id("broad_axe_head"), ToolStatTypes.HEAD.getId()),
                                new ToolPart(id("large_plate"), ToolStatTypes.HEAD.getId()),
                                new ToolPart(id("tough_binding"), ToolStatTypes.EXTRA.getId())));
        }

        private static ResourceLocation id(String name) {
                return ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, name);
        }

        // Stats
        @Override
        public float miningSpeedModifier() { return 0.35F; }

        @Override
        public float damagePotential() { return 1.2F; }

        @Override
        public float attackSpeed() { return 0.8F; }

        @Override
        public float durabilityModifier() { return 2.0F; }

        @Override
        public float knockback() { return 1.5F; }

        @Override
        protected double calculateAttackDamage(List<ResourceLocation> layers) {
                return super.calculateAttackDamage(layers) + 2;
        }

        @Override
        public float repairModifierForPart(int index) {
                return index == 1 ? 2.0F : 1.25F;
        }

        // Actions
        @Override
        public int areaDepth() { return 3; }

        @Override
        public List<BlockPos> areaBlocks(ServerPlayer player, BlockPos origin, BlockState state) {
                return ToolAreaActions.detectTree(player.serverLevel(), origin)
                                ? ToolAreaActions.tree(player.serverLevel(), origin) : super.areaBlocks(player, origin, state);
        }

        @Override
        public void addExtraToolRules(ItemStack stack, Map<String, Number> stats, List<Tool.Rule> rules, float miningSpeed) {
                rules.add(Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_AXE, miningSpeed));
        }

        @Override
        public boolean canPerformAction(ItemStack stack, ItemAbility ability) {
                return ability == ItemAbilities.AXE_DIG || ability == ItemAbilities.AXE_STRIP
                                || ability == ItemAbilities.AXE_SCRAPE || ability == ItemAbilities.AXE_WAX_OFF;
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
                return Items.DIAMOND_AXE.useOn(context);
        }
}
