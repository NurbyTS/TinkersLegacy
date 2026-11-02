package com.nurby.tinkerslegacy.content.tools;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.api.material.MaterialDefinition;
import com.nurby.tinkerslegacy.api.station.StationOperations;
import com.nurby.tinkerslegacy.api.station.StationRequirement;
import com.nurby.tinkerslegacy.api.tool.ToolDefinition;
import com.nurby.tinkerslegacy.api.tool.ToolPartRequirement;
import com.nurby.tinkerslegacy.api.tool.ToolRangedActions;
import com.nurby.tinkerslegacy.registry.TLMaterials;
import com.nurby.tinkerslegacy.registry.tools.ToolParts;
import com.nurby.tinkerslegacy.registry.tools.ToolStatTypes;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;

public abstract class ProjectileToolDefinition extends ToolDefinition {
        public enum Kind {
                ARROW, BOLT, SHURIKEN
        }

        private final Kind kind;

        protected ProjectileToolDefinition(String name, Kind kind, ToolPartRequirement... requirements) {
                super(ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, name), requirements);
                this.kind = kind;
        }

        // Getters
        public Kind kind() {
                return kind;
        }

        @Override
        public boolean isTool() {
                return false;
        }

        @Override
        public StationRequirement stationRequirement(ResourceLocation operation) {
                return new StationRequirement(StationOperations.TOOL_FAMILY,
                                operation.equals(StationOperations.ASSEMBLE) && kind != Kind.ARROW ? 2 : 1);
        }

        @Override
        public float damagePotential() {
                return kind == Kind.SHURIKEN ? 0.7F : 1;
        }

        @Override
        protected int calculateHarvestLevel(List<ResourceLocation> layers) {
                return -1;
        }

        @Override
        protected double calculateAttackDamage(List<ResourceLocation> layers) {
                return super.calculateAttackDamage(layers) + (kind == Kind.ARROW ? 2 : kind == Kind.SHURIKEN ? 1 : 0);
        }

        @Override
        protected double calculateDurability(List<ResourceLocation> layers) {
                int durability = Math.max(1, (int) getStatAverage(layers, ToolStatTypes.HEAD.getId(), "durability"));
                if (kind == Kind.SHURIKEN) {
                        return durability + Math.round((float) getStatAverage(layers, ToolStatTypes.EXTRA.getId(), "durability"));
                }
                durability = Math.max(1, Math.round(durability
                                * (float) getStatAverage(layers, ToolStatTypes.FLETCHING.getId(), "modifier")));
                durability = Math.round(durability * (float) getStatAverage(layers, ToolStatTypes.ARROW_SHAFT.getId(), "modifier"));
                durability += Math.round(10 * (float) getStatAverage(layers, ToolStatTypes.ARROW_SHAFT.getId(), "bonus_ammo"));
                return Math.max(1, kind == Kind.BOLT ? (int) (durability * 0.8F) : durability);
        }

        @Override
        protected void calculateSpecialStats(List<ResourceLocation> layers, Map<String, Number> stats) {
                stats.put("accuracy", kind == Kind.SHURIKEN ? 1
                                : Math.clamp(getStatAverage(layers, ToolStatTypes.FLETCHING.getId(), "accuracy"), 0, 1));
        }

        @Override
        public List<ResourceLocation> defaultMaterials(MaterialDefinition material) {
            return parts().stream().map(part ->
                                part.statType().equals(ToolStatTypes.ARROW_SHAFT.getId()) ? TLMaterials.WOOD.getId()
                                                : part.statType().equals(ToolStatTypes.FLETCHING.getId()) ? TLMaterials.FEATHER.getId() : material.id()).toList();
        }

        // Actions
        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
                return kind == Kind.SHURIKEN ? ToolRangedActions.throwShuriken(player.getItemInHand(hand), level, player, hand)
                                : InteractionResultHolder.pass(player.getItemInHand(hand));
        }

        // Parts
        protected static ToolPartRequirement blade() {
                return new ToolPartRequirement(ToolParts.get(ToolParts.Type.KNIFE_BLADE).getId(),
                                List.of(ToolStatTypes.HEAD.getId(), ToolStatTypes.EXTRA.getId()));
        }

        protected static ToolPartRequirement fletching() {
                return new ToolPartRequirement(ToolParts.get(ToolParts.Type.FLETCHING).getId(),
                                List.of(ToolStatTypes.FLETCHING.getId()));
        }
}
