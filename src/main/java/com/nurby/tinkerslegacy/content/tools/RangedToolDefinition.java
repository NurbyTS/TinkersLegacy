package com.nurby.tinkerslegacy.content.tools;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.api.material.MaterialDefinition;
import com.nurby.tinkerslegacy.api.station.StationOperations;
import com.nurby.tinkerslegacy.api.station.StationRequirement;
import com.nurby.tinkerslegacy.api.tool.ToolDefinition;
import com.nurby.tinkerslegacy.api.tool.ToolPartRequirement;
import com.nurby.tinkerslegacy.api.tool.ToolRangedActions;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLMaterials;
import com.nurby.tinkerslegacy.registry.tools.ToolParts;
import com.nurby.tinkerslegacy.registry.tools.ToolStatTypes;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;

public abstract class RangedToolDefinition extends ToolDefinition {
        public enum Kind {
                SHORTBOW, LONGBOW, CROSSBOW
        }

        private final Kind kind;

        protected RangedToolDefinition(String name, Kind kind, ToolPartRequirement... requirements) {
                super(ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, name), requirements);
                this.kind = kind;
        }

        // Getters
        public Kind kind() {
                return kind;
        }

        public int drawTime() {
                return kind == Kind.SHORTBOW ? 12 : kind == Kind.LONGBOW ? 30 : 45;
        }

        public float projectileSpeed() {
                return kind == Kind.SHORTBOW ? 3 : kind == Kind.LONGBOW ? 5.5F : 7;
        }

        public float baseProjectileDamage() {
                return kind == Kind.SHORTBOW ? 0 : kind == Kind.LONGBOW ? 2.5F : 3;
        }

        public float projectileDamageModifier() {
                return kind == Kind.SHORTBOW ? 0.8F : kind == Kind.LONGBOW ? 1.25F : 1.3F;
        }

        public float inaccuracy() {
                return kind == Kind.SHORTBOW ? 1 : kind == Kind.LONGBOW ? 1.2F : 0;
        }

        public float drawProgress(ItemStack stack, int ticks) {
                float speed = stack.getOrDefault(TLDataComponents.MODIFIED_STATS, Map.<String, Number>of())
                                .getOrDefault("draw_speed", 1).floatValue();
                return Math.clamp(speed * ticks / drawTime(), 0, 1);
        }

        @Override
        public StationRequirement stationRequirement(ResourceLocation operation) {
                return new StationRequirement(StationOperations.TOOL_FAMILY,
                                operation.equals(StationOperations.ASSEMBLE) && kind != Kind.SHORTBOW ? 2 : 1);
        }

        @Override
        public boolean isTool() {
                return false;
        }

        @Override
        public float attackSpeed() {
                return kind == Kind.SHORTBOW ? 1.5F : kind == Kind.LONGBOW ? 1.3F : 2;
        }

        @Override
        public float damagePotential() {
                return kind == Kind.CROSSBOW ? 0.8F : 0.7F;
        }

        @Override
        public int calculateHarvestLevel(List<ResourceLocation> layers) {
                return -1;
        }

        @Override
        protected double calculateDurability(List<ResourceLocation> layers) {
                int durability = Math.max(1, (int) getStatAverage(layers, ToolStatTypes.HEAD.getId(), "durability"));
                durability += Math.round((float) getStatAverage(layers, ToolStatTypes.EXTRA.getId(), "durability"));
                if (kind == Kind.CROSSBOW) {
                        durability = Math.round(durability * (float) getStatAverage(layers, ToolStatTypes.HANDLE.getId(), "modifier"));
                        durability += Math.round((float) getStatAverage(layers, ToolStatTypes.HANDLE.getId(), "durability"));
                }
                durability = Math.max(1, Math.round(durability
                                * (float) getStatAverage(layers, ToolStatTypes.BOWSTRING.getId(), "modifier")));
                return kind == Kind.LONGBOW ? (int) (durability * 1.4F) : durability;
        }

        @Override
        protected void calculateSpecialStats(List<ResourceLocation> layers, Map<String, Number> stats) {
                stats.put("draw_speed", Math.max(0.001, getStatAverage(layers, ToolStatTypes.BOW.getId(), "draw_speed")));
                stats.put("range", Math.max(0.001, getStatAverage(layers, ToolStatTypes.BOW.getId(), "range")));
                stats.put("bonus_damage", getStatAverage(layers, ToolStatTypes.BOW.getId(), "bonus_damage")
                                * (kind == Kind.CROSSBOW ? 1.5 : 1));
        }

        @Override
        public List<ResourceLocation> defaultMaterials(MaterialDefinition material) {
            return parts().stream().map(part ->
                                part.statType().equals(ToolStatTypes.BOWSTRING.getId()) ? TLMaterials.STRING.getId() : material.id()).toList();
        }

        // Actions
        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
                return ToolRangedActions.use(this, level, player, hand);
        }

        @Override
        public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
                ToolRangedActions.release(this, stack, level, entity, timeLeft);
        }

        @Override
        public int getUseDuration(ItemStack stack, LivingEntity entity) {
                return 72000;
        }

        @Override
        public UseAnim getUseAnimation(ItemStack stack) {
                return kind == Kind.CROSSBOW ? UseAnim.NONE : UseAnim.BOW;
        }

        // Parts
        protected static ToolPartRequirement limb() {
                return new ToolPartRequirement(ToolParts.get(ToolParts.Type.BOW_LIMB).getId(),
                                List.of(ToolStatTypes.HEAD.getId(), ToolStatTypes.BOW.getId()));
        }

        protected static ToolPartRequirement string() {
                return new ToolPartRequirement(ToolParts.get(ToolParts.Type.BOW_STRING).getId(),
                                List.of(ToolStatTypes.BOWSTRING.getId()));
        }
}
