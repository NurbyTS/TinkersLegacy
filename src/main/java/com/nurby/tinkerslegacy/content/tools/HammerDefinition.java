package com.nurby.tinkerslegacy.content.tools;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.api.material.stat.MaterialStat;
import com.nurby.tinkerslegacy.api.tool.ToolPart;
import com.nurby.tinkerslegacy.registry.tools.ToolStatTypes;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Tool;

import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HammerDefinition extends AreaToolDefinition {
        public HammerDefinition() {
                super(id("hammer"), List.of(
                                new ToolPart(id("tough_tool_rod"), ToolStatTypes.HANDLE.getId()),
                                new ToolPart(id("hammer_head"), ToolStatTypes.HEAD.getId()),
                                new ToolPart(id("large_plate"), ToolStatTypes.HEAD.getId()),
                                new ToolPart(id("large_plate"), ToolStatTypes.HEAD.getId())));
        }

        private static ResourceLocation id(String name) {
                return ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, name);
        }

        // Stats
        @Override
        public boolean isWeapon() { return true; }

        @Override
        public float miningSpeedModifier() { return 0.4F; }

        @Override
        public float damagePotential() { return 1.2F; }

        @Override
        public float attackSpeed() { return 0.8F; }

        @Override
        public float durabilityModifier() { return 2.5F; }

        @Override
        protected List<MaterialStat> headStats(List<ResourceLocation> layers) {
                List<MaterialStat> heads = new ArrayList<>(super.headStats(layers));
                heads.addFirst(heads.getFirst());
                return heads;
        }

        @Override
        protected int calculateHarvestLevel(List<ResourceLocation> layers) {
                return getPartStats(layers, id("hammer_head")).statMap().getInt("harvest_level");
        }

        @Override
        public float repairModifierForPart(int index) {
                return index == 1 ? 2.5F : 1.5F;
        }

        // Actions
        @Override
        public void addExtraToolRules(ItemStack stack, Map<String, Number> stats, List<Tool.Rule> rules, float miningSpeed) {
                rules.add(Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_PICKAXE, miningSpeed));
        }

        @Override
        public boolean canPerformAction(ItemStack stack, ItemAbility ability) {
                return ability == ItemAbilities.PICKAXE_DIG;
        }

        @Override
        public boolean dealDamage(Player player, Entity target, DamageSource source, float damage) {
                if (target.getType().is(EntityTypeTags.UNDEAD)) damage += 3 + player.getRandom().nextInt(4);
                return target.hurt(source, damage);
        }
}
