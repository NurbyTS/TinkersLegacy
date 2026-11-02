package com.nurby.tinkerslegacy.content.tools;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.api.station.StationOperations;
import com.nurby.tinkerslegacy.api.station.StationRequirement;
import com.nurby.tinkerslegacy.api.tool.ToolPart;
import com.nurby.tinkerslegacy.registry.tools.ToolStatTypes;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.Map;

public class CleaverDefinition extends ClassicToolDefinition {
        public CleaverDefinition() {
                super(id("cleaver"), List.of(
                                new ToolPart(id("tough_tool_rod"), ToolStatTypes.HANDLE.getId()),
                                new ToolPart(id("large_sword_blade"), ToolStatTypes.HEAD.getId()),
                                new ToolPart(id("large_plate"), ToolStatTypes.HEAD.getId()),
                                new ToolPart(id("tough_tool_rod"), ToolStatTypes.EXTRA.getId())));
        }

        private static ResourceLocation id(String name) {
                return ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, name);
        }

        // Assembly
        @Override
        public StationRequirement stationRequirement(ResourceLocation operation) {
                return new StationRequirement(StationOperations.TOOL_FAMILY, operation.equals(StationOperations.ASSEMBLE) ? 2 : 1);
        }

        // Stats
        @Override
        public boolean isTool() { return false; }

        @Override
        public boolean isWeapon() { return true; }

        @Override
        public float damagePotential() { return 1.2F; }

        @Override
        public float miningSpeedModifier() { return 0.5F; }

        @Override
        public float attackSpeed() { return 0.7F; }

        @Override
        public float damageCutoff() { return 25.0F; }

        @Override
        public float durabilityModifier() { return 2.0F; }

        @Override
        protected double calculateAttackDamage(List<ResourceLocation> layers) {
                return super.calculateAttackDamage(layers) * 1.3F + 3;
        }

        @Override
        public float repairModifierForPart(int index) {
                return index == 1 ? 2.0F : 1.5F;
        }

        // Actions
        @Override
        public void addExtraToolRules(ItemStack stack, Map<String, Number> stats, List<Tool.Rule> rules, float miningSpeed) {
                rules.add(Tool.Rule.minesAndDrops(List.of(Blocks.COBWEB), miningSpeed * 7.5F));
                rules.add(Tool.Rule.minesAndDrops(BlockTags.SWORD_EFFICIENT, miningSpeed));
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
                // The original two-handed weapon reserves right click rather than using the offhand.
                return InteractionResultHolder.consume(player.getItemInHand(hand));
        }
}
