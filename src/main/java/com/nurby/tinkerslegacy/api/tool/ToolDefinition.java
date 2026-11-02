package com.nurby.tinkerslegacy.api.tool;

import com.nurby.tinkerslegacy.TLConfig;
import com.nurby.tinkerslegacy.api.material.MaterialDefinition;
import com.nurby.tinkerslegacy.api.material.stat.MaterialStat;
import com.nurby.tinkerslegacy.api.station.StationOperations;
import com.nurby.tinkerslegacy.api.station.StationRequirement;
import com.nurby.tinkerslegacy.registry.TLRegistries;
import com.nurby.tinkerslegacy.registry.tools.ToolStatTypes;
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
    private final List<ToolPartRequirement> partRequirements;

    protected ToolDefinition(ResourceLocation id, List<ToolPart> parts) {
        this.id = id;
        this.parts = List.copyOf(parts);
        this.partRequirements = this.parts.stream().map(part -> new ToolPartRequirement(part.part(), List.of(part.statType()))).toList();
    }

    protected ToolDefinition(ResourceLocation id, ToolPartRequirement... requirements) {
        this.id = id;
        this.partRequirements = List.of(requirements);
        this.parts = this.partRequirements.stream().flatMap(requirement -> requirement.statTypes().stream().map(statType -> new ToolPart(requirement.part(), statType))).toList();
    }

    // Getters
    public List<ToolPartRequirement> partRequirements() {
        return partRequirements;
    }

    public List<Integer> repairParts() {
        List<Integer> indices = new ArrayList<>();
        for (int index = 0; index < parts.size(); index++) {
            if (parts.get(index).statType().equals(ToolStatTypes.HEAD.getId())) {
                indices.add(index);
            }
        }
        return List.copyOf(indices);
    }

    public float repairModifierForPart(int index) {
        return 1.0F;
    }

    public List<ResourceLocation> defaultMaterials(MaterialDefinition material) {
        return parts.stream().map(part -> material.id()).toList();
    }

    public List<Integer> renderLayers() {
        List<Integer> result = new ArrayList<>();
        int layer = 0;
        for (ToolPartRequirement part : partRequirements) {
            result.add(layer);
            layer += part.statTypes().size();
        }
        return List.copyOf(result);
    }

    public boolean acceptsMaterials(List<ResourceLocation> materials) {
        return materials != null && materials.size() == parts.size();
    }

    public StationRequirement stationRequirement(ResourceLocation operation) {
        return new StationRequirement(StationOperations.TOOL_FAMILY, 1);
    }

    public boolean isHarvestTool() {
        return isTool();
    }

    public boolean isWeapon() {
        return false;
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

    public final Map<String, Number> calculateStats(List<ResourceLocation> materials) {
        Map<String, Number> stats = new HashMap<>();

        stats.put("attack_damage", calculateAttackDamage(materials));

        stats.put("attack_speed", attackSpeed());

        stats.put("durability", calculateDurability(materials));

        stats.put("mining_speed", calculateMiningSpeed(materials));

        int harvestLevel = calculateHarvestLevel(materials);

        if (harvestLevel != -1) {
            stats.put("harvest_level", harvestLevel);
        }

        stats.put("free_modifiers", TLConfig.NEW_TOOL_MIN_MODIFIERS.getAsInt());

        calculateSpecialStats(materials, stats);

        return Map.copyOf(stats);
    }

    protected List<MaterialStat> headStats(List<ResourceLocation> materials) {
        return getStats(materials, ToolStatTypes.HEAD.getId());
    }

    protected static double average(List<MaterialStat> stats, String name) {
        return stats.stream().mapToDouble(stat -> stat.statMap().getFloat(name)).average().orElse(0);
    }

    protected double calculateAttackDamage(List<ResourceLocation> materials) {
        return average(headStats(materials), "attack_damage");
    }

    protected double calculateDurability(List<ResourceLocation> materials) {
        List<MaterialStat> handles = getStats(materials, ToolStatTypes.HANDLE.getId());
        List<MaterialStat> extras = getStats(materials, ToolStatTypes.EXTRA.getId());
        int durability = Math.max(1, (int) average(headStats(materials), "durability"));
        durability += Math.round((float) average(extras, "durability"));
        if (!handles.isEmpty()) {
            durability = Math.round(durability * (float) average(handles, "modifier"));
            durability += Math.round((float) average(handles, "durability"));
        }
        return (int) (Math.max(1, durability) * durabilityModifier());
    }

    protected double calculateMiningSpeed(List<ResourceLocation> materials) {
        // Modifiers operate on the material stat before the tool's mining multiplier.
        return average(headStats(materials), "mining_speed");
    }

    // This is intentionally overridable for tools such as the mattock.
    protected int calculateHarvestLevel(List<ResourceLocation> materials) {
        return getStatMax(materials, ToolStatTypes.HEAD.getId(), "harvest_level");
    }

    protected void calculateSpecialStats(List<ResourceLocation> materials, Map<String, Number> stats) {
    }

    protected double getStatSum(List<ResourceLocation> materials, ResourceLocation statType, String field) {
        return getStats(materials, statType).stream().mapToDouble(stat -> stat.statMap().getFloat(field)).sum();
    }

    protected double getStatAverage(List<ResourceLocation> materials, ResourceLocation statType, String field) {
        return getStats(materials, statType).stream().mapToDouble(stat -> stat.statMap().getFloat(field)).average().orElse(0.0D);
    }

    protected double getStatMin(List<ResourceLocation> materials, ResourceLocation statType, String field) {
        return getStats(materials, statType).stream().mapToDouble(stat -> stat.statMap().getFloat(field)).min().orElse(0.0D);
    }

    protected double getStatMaxValue(List<ResourceLocation> materials, ResourceLocation statType, String field) {
        return getStats(materials, statType).stream().mapToDouble(stat -> stat.statMap().getFloat(field)).max().orElse(0.0D);
    }

    protected int getStatMax(List<ResourceLocation> materials, ResourceLocation statType, String field) {
        List<MaterialStat> stats = getStats(materials, statType);

        if (stats.isEmpty()) {
            return -1;
        }

        return stats.stream().mapToInt(stat -> stat.statMap().getInt(field)).max().orElse(-1);
    }

    protected MaterialStat getPartStats(List<ResourceLocation> materials, ResourceLocation partId) {
        int partIndex = getPartIndex(partId);

        if (partIndex >= materials.size()) {
            throw new IllegalArgumentException("Missing material entry for part: " + partId + " (index " + partIndex + ")");
        }

        ResourceLocation materialId = materials.get(partIndex);
        ToolPart part = parts.get(partIndex);
        MaterialDefinition material = getMaterial(materialId);

        MaterialStat stat = material.stats().get(part.statType());

        if (stat == null) {
            throw new IllegalArgumentException("Material " + materialId + " has no stat for type: " + part.statType());
        }

        return stat;
    }

    private int getPartIndex(ResourceLocation partId) {
        for (int i = 0; i < parts.size(); i++) {
            if (parts.get(i).part().equals(partId)) {
                return i;
            }
        }

        throw new IllegalArgumentException("Unknown tool part: " + partId);
    }

    public List<MaterialStat> getStats(List<ResourceLocation> materials, ResourceLocation statType) {
        List<MaterialStat> stats = new ArrayList<>();

        for (int i = 0; i < Math.min(materials.size(), parts.size()); i++) {
            ResourceLocation materialId = materials.get(i);
            ResourceLocation partStatType = parts.get(i).statType();

            if (!partStatType.equals(statType)) {
                continue;
            }

            MaterialDefinition material = getMaterial(materialId);
            MaterialStat stat = material.stats().get(statType);

            if (stat == null) {
                throw new IllegalArgumentException("Material " + materialId + " has no stat for type: " + statType);
            }

            stats.add(stat);
        }

        return List.copyOf(stats);
    }

    private MaterialDefinition getMaterial(ResourceLocation materialId) {
        MaterialDefinition material = TLRegistries.MATERIALS.get(materialId);

        if (material == null) {
            throw new IllegalArgumentException("Unknown material: " + materialId);
        }

        return material;
    }

    public boolean supports(MaterialDefinition material) {
        return parts.stream().allMatch(part -> material.stats().containsKey(part.statType()));
    }

    public void addExtraToolRules(ItemStack stack, Map<String, Number> stats, List<Tool.Rule> rules, float miningSpeed) {
    }

    public InteractionResult useOn(UseOnContext context) {
        return null;
    }

    public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
        return false;
    }

    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeCharged) {
    }

    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 0;
    }

    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        return true;
    }

    public boolean canDisableShield(ItemStack stack, ItemStack shield, LivingEntity entity, LivingEntity attacker) {
        return false;
    }

    public float knockback() {
        return 1.0F;
    }

    public boolean damagesOnBlockBreak(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
        return true;
    }

    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    public boolean dealDamage(Player player, Entity target, DamageSource source, float damage) {
        return target.hurt(source, damage);
    }
}