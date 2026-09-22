package com.nurby.tinkerslegacy.library.tool;

import com.nurby.tinkerslegacy.TLConfig;
import com.nurby.tinkerslegacy.library.material.MaterialDefinition;
import com.nurby.tinkerslegacy.library.material.layer.MaterialLayer;
import com.nurby.tinkerslegacy.library.material.stat.MaterialStat;
import com.nurby.tinkerslegacy.registry.TLRegistries;
import com.nurby.tinkerslegacy.registry.ToolStatTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ToolDefinition {

    private final ResourceLocation id;
    private final List<ToolPart> parts;

    protected ToolDefinition(
            ResourceLocation id,
            List<ToolPart> parts
    ) {
        this.id = id;
        this.parts = List.copyOf(parts);
    }

    public ResourceLocation id() {
        return id;
    }

    public List<ToolPart> parts() {
        return parts;
    }

    public float damagePotential() {
        return 1.0F;
    }

    public float baseDamage() {
        return 1.0F;
    }

    public float damageCutoff() {
        return 15.0F;
    }

    public float attackSpeed() {
        return 1.0F;
    }

    public float miningSpeedModifier() {
        return 1.0F;
    }

    public float durabilityModifier() {
        return 1.0f;
    }

    // Used primarily for descriptions, since weapons should not display
    // stats such as mining speed and mining level.
    public boolean isTool() {
        return true;
    }

    public final Map<String, Number> calculateStats(
            List<MaterialLayer> layers
    ) {
        Map<String, Number> stats = new HashMap<>();

        stats.put(
                "attack_damage",
                calculateAttackDamage(layers)
        );

        stats.put(
                "attack_speed",
                attackSpeed()
        );

        stats.put(
                "durability",
                calculateDurability(layers)
        );

        stats.put(
                "mining_speed",
                calculateMiningSpeed(layers)
        );

        int harvestLevel = calculateHarvestLevel(layers);

        if (harvestLevel != -1) {
            stats.put(
                    "harvest_level",
                    harvestLevel
            );
        }

        stats.put(
                "free_modifiers",
                TLConfig.NEW_TOOL_MIN_MODIFIERS.getAsInt()
        );

        calculateSpecialStats(layers, stats);

        return Map.copyOf(stats);
    }

    protected double calculateAttackDamage(
            List<MaterialLayer> layers
    ) {
        return getStatAverage(
                layers,
                ToolStatTypes.HEAD.getId(),
                "attack_damage"
        );
    }

    protected double calculateDurability(
            List<MaterialLayer> layers
    ) {
        double durability =
                getStatSum(
                        layers,
                        ToolStatTypes.HEAD.getId(),
                        "durability"
                );

        durability += getStatSum(
                layers,
                ToolStatTypes.EXTRA.getId(),
                "durability"
        );

        List<MaterialStat> handles =
                getStats(
                        layers,
                        ToolStatTypes.HANDLE.getId()
                );

        for (MaterialStat handle : handles) {
            durability *= handle.statMap()
                    .getFloat("modifier");
        }

        durability += getStatSum(
                layers,
                ToolStatTypes.HANDLE.getId(),
                "durability"
        );

        return durability * durabilityModifier();
    }

    protected double calculateMiningSpeed(
            List<MaterialLayer> layers
    ) {
        return getStatAverage(
                layers,
                ToolStatTypes.HEAD.getId(),
                "mining_speed"
        ) * miningSpeedModifier();
    }

    // This is intentionally overridable for tools such as the mattock.
    protected int calculateHarvestLevel(
            List<MaterialLayer> layers
    ) {
        return getStatMax(
                layers,
                ToolStatTypes.HEAD.getId(),
                "harvest_level"
        );
    }

    protected void calculateSpecialStats(
            List<MaterialLayer> layers,
            Map<String, Number> stats
    ) {
    }

    protected double getStatSum(
            List<MaterialLayer> layers,
            ResourceLocation statType,
            String field
    ) {
        return getStats(layers, statType).stream()
                .mapToDouble(stat ->
                        stat.statMap().getFloat(field)
                )
                .sum();
    }

    protected double getStatAverage(
            List<MaterialLayer> layers,
            ResourceLocation statType,
            String field
    ) {
        return getStats(layers, statType).stream()
                .mapToDouble(stat ->
                        stat.statMap().getFloat(field)
                )
                .average()
                .orElse(0.0D);
    }

    protected double getStatMin(
            List<MaterialLayer> layers,
            ResourceLocation statType,
            String field
    ) {
        return getStats(layers, statType).stream()
                .mapToDouble(stat ->
                        stat.statMap().getFloat(field)
                )
                .min()
                .orElse(0.0D);
    }

    protected double getStatMaxValue(
            List<MaterialLayer> layers,
            ResourceLocation statType,
            String field
    ) {
        return getStats(layers, statType).stream()
                .mapToDouble(stat ->
                        stat.statMap().getFloat(field)
                )
                .max()
                .orElse(0.0D);
    }

    protected int getStatMax(
            List<MaterialLayer> layers,
            ResourceLocation statType,
            String field
    ) {
        List<MaterialStat> stats =
                getStats(layers, statType);

        if (stats.isEmpty()) {
            return -1;
        }

        return stats.stream()
                .mapToInt(stat ->
                        stat.statMap().getInt(field)
                )
                .max()
                .orElse(-1);
    }

    protected MaterialStat getPartStats(
            List<MaterialLayer> layers,
            ResourceLocation partId
    ) {
        int partIndex =
                getPartIndex(
                        layers,
                        partId
                );

        MaterialLayer layer =
                layers.get(partIndex);

        ToolPart part =
                parts.get(partIndex);

        if (!layer.statType().equals(part.statType())) {
            throw new IllegalArgumentException(
                    "Material layer at index "
                            + partIndex
                            + " does not match tool part: "
                            + partId
            );
        }

        MaterialDefinition material =
                getMaterial(layer);

        MaterialStat stat =
                material.stats().get(part.statType());

        if (stat == null) {
            throw new IllegalArgumentException(
                    "Material "
                            + layer.material()
                            + " has no stat for type: "
                            + part.statType()
            );
        }

        return stat;
    }

    private int getPartIndex(
            List<MaterialLayer> layers,
            ResourceLocation partId
    ) {
        for (int i = 0; i < parts.size(); i++) {
            if (!parts.get(i).part().equals(partId)) {
                continue;
            }

            if (i >= layers.size()) {
                throw new IllegalArgumentException(
                        "Missing material layer for part: "
                                + partId
                );
            }

            return i;
        }

        throw new IllegalArgumentException(
                "Unknown tool part: " + partId
        );
    }

    public List<MaterialStat> getStats(
            List<MaterialLayer> layers,
            ResourceLocation statType
    ) {
        List<MaterialStat> stats =
                new ArrayList<>();

        for (MaterialLayer layer : layers) {
            if (!layer.statType().equals(statType)) {
                continue;
            }

            MaterialDefinition material =
                    getMaterial(layer);

            MaterialStat stat =
                    material.stats().get(statType);

            if (stat == null) {
                throw new IllegalArgumentException(
                        "Material "
                                + layer.material()
                                + " has no stat for type: "
                                + statType
                );
            }

            stats.add(stat);
        }

        return List.copyOf(stats);
    }

    private MaterialDefinition getMaterial(
            MaterialLayer layer
    ) {
        MaterialDefinition material =
                TLRegistries.MATERIALS.get(layer.material());

        if (material == null) {
            throw new IllegalArgumentException(
                    "Unknown material: "
                            + layer.material()
            );
        }

        return material;
    }

    public boolean supports(
            MaterialDefinition material
    ) {
        return parts.stream().allMatch(part ->
                material.stats().containsKey(part.statType())
        );
    }

    public void addExtraToolRules(ItemStack stack, Map<String, Number> stats, List<Tool.Rule> rules, float miningSpeed) { }

    public InteractionResult useOn(
            UseOnContext context
    ) {
        return null;
    }

    public boolean canPerformAction(
            ItemStack stack,
            ItemAbility itemAbility
    ) {
        return false;
    }

    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        return InteractionResultHolder.pass(
                player.getItemInHand(hand)
        );
    }

    public void releaseUsing(
            ItemStack stack,
            Level level,
            LivingEntity entity,
            int timeCharged
    ) {
    }

    public int getUseDuration(
            ItemStack stack,
            LivingEntity entity
    ) {
        return 0;
    }

    public UseAnim getUseAnimation(
            ItemStack stack
    ) {
        return UseAnim.NONE;
    }

    public boolean hurtEnemy(
            ItemStack stack,
            LivingEntity target,
            LivingEntity attacker
    ) {
        return true;
    }

    public boolean canDisableShield(
            ItemStack stack,
            ItemStack shield,
            LivingEntity entity,
            LivingEntity attacker
    ) {
        return false;
    }

    public float knockback() {
        return 1.0F;
    }

    public boolean damagesOnBlockBreak(
            ItemStack stack,
            Level level,
            BlockState state,
            BlockPos pos,
            LivingEntity entity
    ) {
        return true;
    }

    public InteractionResult interactLivingEntity(
            ItemStack stack,
            Player player,
            LivingEntity target,
            InteractionHand hand
    ) {
        return InteractionResult.PASS;
    }

    public boolean dealDamage(
            Player player,
            Entity target,
            DamageSource source,
            float damage
    ) {
        return target.hurt(source, damage);
    }
}