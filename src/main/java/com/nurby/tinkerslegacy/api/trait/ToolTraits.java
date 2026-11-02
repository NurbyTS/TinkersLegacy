/*package com.nurby.tinkerslegacy.library.tool.traits;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.content.entity.TLToolProjectile;
import com.nurby.tinkerslegacy.content.item.dynamic.DynamicPart;
import com.nurby.tinkerslegacy.content.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.library.tool.ToolPartRequirement;
import com.nurby.tinkerslegacy.library.tool.ToolState;
import com.nurby.tinkerslegacy.library.tool.ToolVisualEffects;
import com.nurby.tinkerslegacy.library.trait.TraitDefinition;
import com.nurby.tinkerslegacy.library.trait.TraitInstance;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLDamageTypes;
import com.nurby.tinkerslegacy.registry.TLRegistries;
import com.nurby.tinkerslegacy.registry.tools.ToolStatTypes;
import com.nurby.tinkerslegacy.content.tools.ProjectileToolDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

public final class ToolTraits {
    private static final ThreadLocal<Boolean> SECONDARY = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Entity> DIRECT_SOURCE = new ThreadLocal<>();

    private ToolTraits() {
    }

    public static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, name);
    }

    public static List<ResourceLocation> materialTraits(ResourceLocation materialId, ResourceLocation statRole) {
        var material = TLRegistries.MATERIALS.containsKey(materialId) ? TLRegistries.MATERIALS.get(materialId) : null;
        return material == null ? List.of() : material.traits(statRole);
    }

    public static List<TraitInstance> traits(ItemStack stack) {
        // TODO: Expand this section when integrating the modifier system to read exact 'amounts' (e.g., 50 redstone)
        // from a dedicated component. For now, material traits default to amount = 0.
        List<ResourceLocation> forcedTraits = stack.get(TLDataComponents.EFFECT_TRAITS);
        if (forcedTraits != null) {
            Map<ResourceLocation, Integer> levels = new LinkedHashMap<>();
            for (ResourceLocation id : forcedTraits) {
                levels.put(id, levels.getOrDefault(id, 0) + 1);
            }
            return levels.entrySet().stream().map(e -> new TraitInstance(e.getKey(), e.getValue(), 0)).toList();
        }

        Map<ResourceLocation, Integer> levels = new LinkedHashMap<>();

        if (stack.getItem() instanceof DynamicTool tool) {
            boolean projectileHead = tool.getDefinition() instanceof ProjectileToolDefinition projectile
                    && projectile.kind() != ProjectileToolDefinition.Kind.BOLT;

            collectComponentTraits(stack, TLDataComponents.MATERIALS.get(), tool.getDefinition().partRequirements().stream().map(ToolPartRequirement::statTypes).toList(), projectileHead, levels);
            collectComponentTraits(stack, TLDataComponents.EMBOSSMENT.get(), tool.getDefinition().partRequirements().stream().map(ToolPartRequirement::statTypes).toList(), projectileHead, levels);
        } else if (stack.getItem() instanceof DynamicPart part) {
            collectComponentTraits(stack, TLDataComponents.MATERIALS.get(), List.of(part.definition().statTypes()), false, levels);
        }

        return levels.entrySet().stream().map(e -> new TraitInstance(e.getKey(), e.getValue(), 0)).toList();
    }

    private static void collectComponentTraits(ItemStack stack, net.minecraft.core.component.DataComponentType<List<ResourceLocation>> componentType,
                                               List<List<ResourceLocation>> statTypesPerPart, boolean projectileHead, Map<ResourceLocation, Integer> levels) {
        List<ResourceLocation> materials = stack.getOrDefault(componentType, List.of());
        if (materials.isEmpty()) return;

        int offset = 0;
        for (List<ResourceLocation> statTypes : statTypesPerPart) {
            int partSize = statTypes.size();
            if (offset + partSize > materials.size()) break;

            for (int i = 0; i < partSize; i++) {
                ResourceLocation materialId = materials.get(offset + i);
                ResourceLocation statType = statTypes.get(i);
                addTraits(levels, materialId, statType, projectileHead);
            }
            offset += partSize;
        }
    }

    private static void addTraits(Map<ResourceLocation, Integer> levels, ResourceLocation materialId, ResourceLocation statType, boolean projectileHead) {
        for (ResourceLocation traitId : materialTraits(materialId, statType)) {
            levels.put(traitId, levels.getOrDefault(traitId, 0) + 1);
        }
        if (projectileHead && statType.equals(ToolStatTypes.HEAD.getId())) {
            for (ResourceLocation traitId : materialTraits(materialId, ToolStatTypes.PROJECTILE.getId())) {
                levels.put(traitId, levels.getOrDefault(traitId, 0) + 1);
            }
        }
    }

    public static int color(ItemStack stack, ResourceLocation id) {
        if (!(stack.getItem() instanceof DynamicTool tool)) {
            TraitDefinition definition = TLRegistries.TRAITS.get(id);
            return definition == null ? 0xFFFFFF : definition.color();
        }

        boolean projectileHead = tool.getDefinition() instanceof ProjectileToolDefinition projectile
                && projectile.kind() != ProjectileToolDefinition.Kind.BOLT;

        for (var componentType : List.of(TLDataComponents.MATERIALS, TLDataComponents.EMBOSSMENT)) {
            List<ResourceLocation> materials = stack.getOrDefault(componentType, List.of());
            if (materials.isEmpty()) continue;

            int offset = 0;
            for (var requirement : tool.getDefinition().partRequirements()) {
                int partSize = requirement.statTypes().size();
                if (offset + partSize > materials.size()) break;

                for (int i = 0; i < partSize; i++) {
                    ResourceLocation materialId = materials.get(offset + i);
                    ResourceLocation statType = requirement.statTypes().get(i);

                    if (!TLRegistries.MATERIALS.containsKey(materialId)) continue;
                    var material = TLRegistries.MATERIALS.get(materialId);
                    Map<ResourceLocation, Integer> tempLevels = new LinkedHashMap<>();
                    addTraits(tempLevels, materialId, statType, projectileHead);

                    if (tempLevels.keySet().stream().anyMatch(trait -> displayId(trait).equals(displayId(id)))) {
                        return material.color();
                    }
                }
                offset += partSize;
            }
        }
        TraitDefinition definition = TLRegistries.TRAITS.get(id);
        return definition == null ? 0xFFFFFF : definition.color();
    }

    public static ResourceLocation displayId(ResourceLocation id) {
        return id;
    }

    public static boolean isPresent(ItemStack stack, ResourceLocation id) {
        return level(stack, id) > 0;
    }

    public static boolean has(ItemStack stack, String name) {
        return level(stack, id(name)) > 0;
    }

    public static int level(ItemStack stack, ResourceLocation traitId) {
        for (TraitInstance instance : traits(stack)) {
            if (instance.id().equals(traitId)) return instance.level();
        }
        return 0;
    }

    public static int level(ItemStack stack, String name) {
        return level(stack, id(name));
    }

    public static int bonusSlots(ItemStack stack) {
        int total = 0;
        for (TraitInstance instance : traits(stack)) {
            TraitDefinition trait = instance.getDefinition();
            if (trait != null) total += trait.bonusSlots(instance.level(), instance.amount());
        }
        return total;
    }

    public static ToolTraitData data(ItemStack stack) {
        return stack.getOrDefault(TLDataComponents.TRAIT_DATA, ToolTraitData.EMPTY);
    }

    public static Map<String, Number> applyStats(ItemStack stack, Map<String, Number> base) {
        Map<String, Number> result = new LinkedHashMap<>(base);
        for (TraitInstance instance : traits(stack)) {
            TraitDefinition trait = instance.getDefinition();
            if (trait != null) trait.applyStats(stack, instance.level(), instance.amount(), result);
        }
        return result;
    }

    public static Map<String, Number> finishStats(ItemStack stack, Map<String, Number> stats) {
        Map<String, Number> result = new LinkedHashMap<>(stats);
        for (TraitInstance instance : traits(stack)) {
            TraitDefinition trait = instance.getDefinition();
            if (trait != null) trait.finishStats(stack, instance.level(), instance.amount(), result);
        }
        return result;
    }

    public static void applyEnchantments(ItemStack stack) {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null && server.isSameThread()) applyEnchantments(stack, server.registryAccess());
    }

    public static void applyEnchantments(ItemStack stack, net.minecraft.core.HolderLookup.Provider access) {
        for (TraitInstance instance : traits(stack)) {
            TraitDefinition trait = instance.getDefinition();
            if (trait != null) trait.applyEnchantments(stack, instance.level(), instance.amount(), access);
        }
    }

    public static int damage(ItemStack stack, int amount, @Nullable LivingEntity entity) {
        int result = amount;
        for (TraitInstance instance : traits(stack)) {
            TraitDefinition trait = instance.getDefinition();
            if (trait != null) result = trait.damage(stack, instance.level(), instance.amount(), amount, result, entity);
        }
        return Math.max(0, result);
    }

    public static int heal(ItemStack stack, int amount) {
        int result = amount;
        for (TraitInstance instance : traits(stack)) {
            TraitDefinition trait = instance.getDefinition();
            if (trait != null) result = trait.heal(stack, instance.level(), instance.amount(), amount, result);
        }
        return Math.max(0, result);
    }

    public static float attackDamage(ItemStack stack, LivingEntity attacker, LivingEntity target, float amount) {
        float result = amount;
        for (TraitInstance instance : traits(stack)) {
            TraitDefinition trait = instance.getDefinition();
            if (trait != null) result = trait.attackDamage(stack, instance.level(), instance.amount(), attacker, target, amount, result);
        }
        return Math.max(0, result);
    }

    public static void beforeHit(ItemStack stack, LivingEntity attacker, LivingEntity target) {
        beforeHit(stack, attacker, target, attacker);
    }

    public static void beforeHit(ItemStack stack, LivingEntity attacker, LivingEntity target, Entity directSource) {
        if (attacker.level().isClientSide || SECONDARY.get()) return;
        Entity previous = DIRECT_SOURCE.get();
        DIRECT_SOURCE.set(directSource);
        try {
            for (TraitInstance instance : traits(stack)) {
                TraitDefinition trait = instance.getDefinition();
                if (trait != null) trait.beforeHit(stack, instance.level(), instance.amount(), attacker, target);
            }
        } finally {
            if (previous == null) DIRECT_SOURCE.remove();
            else DIRECT_SOURCE.set(previous);
        }
    }

    public static void afterHit(ItemStack stack, LivingEntity attacker, LivingEntity target, float damage) {
        afterHit(stack, attacker, target, damage, attacker);
    }

    public static void afterHit(ItemStack stack, LivingEntity attacker, LivingEntity target, float damage, Entity directSource) {
        if (attacker.level().isClientSide || SECONDARY.get()) return;
        Entity previous = DIRECT_SOURCE.get();
        DIRECT_SOURCE.set(directSource);
        try {
            for (TraitInstance instance : traits(stack)) {
                TraitDefinition trait = instance.getDefinition();
                if (trait != null) trait.afterHit(stack, instance.level(), instance.amount(), attacker, target, damage);
            }
        } finally {
            if (previous == null) DIRECT_SOURCE.remove();
            else DIRECT_SOURCE.set(previous);
        }
    }

    public static void afterBlockBreak(ItemStack stack, ServerLevel level, BlockState state, BlockPos pos, LivingEntity entity) {
        for (TraitInstance instance : traits(stack)) {
            TraitDefinition trait = instance.getDefinition();
            if (trait != null) trait.afterBlockBreak(stack, instance.level(), instance.amount(), level, state, pos, entity);
        }
    }

    public static void inventoryTick(ItemStack stack, Level level, Entity entity, boolean selected) {
        if (level.isClientSide) return;
        for (TraitInstance instance : traits(stack)) {
            TraitDefinition trait = instance.getDefinition();
            if (trait != null) trait.inventoryTick(stack, instance.level(), instance.amount(), level, entity, selected);
        }
    }

    public static void passiveHeal(ItemStack stack, int amount) {
        if (ToolState.broken(stack) || amount <= 0) return;
        int repairs = stack.getOrDefault(TLDataComponents.REPAIR_COUNT, 0);
        ToolState.repair(stack, amount);
        stack.set(TLDataComponents.REPAIR_COUNT, repairs);
    }

    public static boolean secondaryDamage(LivingEntity target, @Nullable LivingEntity attacker, float amount) {
        return secondaryDamage(target, attacker, amount, TLDamageTypes.ARMOR_BYPASSING, false, true);
    }

    public static boolean thornDamage(LivingEntity target, LivingEntity attacker, float amount) {
        boolean hit = secondaryDamage(target, attacker, amount, TLDamageTypes.TRAIT_ABSOLUTE, false, true);
        if (hit) ToolVisualEffects.thorns(target);
        return hit;
    }

    public static boolean shockDamage(LivingEntity target, LivingEntity attacker, float amount) {
        return secondaryDamage(target, attacker, amount, net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK, true, true);
    }

    public static boolean bleedDamage(LivingEntity target, @Nullable LivingEntity attacker, float amount) {
        return secondaryDamage(target, attacker, amount, net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK, false, false);
    }

    private static boolean secondaryDamage(LivingEntity target, @Nullable LivingEntity attacker, float amount,
                                           net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> type,
                                           boolean shock, boolean toolCredit) {
        if (amount <= 0 || target.level().isClientSide || !target.isAlive()) return false;
        int immunity = target.invulnerableTime;
        boolean previous = SECONDARY.get();
        boolean hit = false;
        var resistance = target.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        ResourceLocation resistanceId = id("trait_knockback_guard");
        boolean suppressKnockback = !shock && resistance != null && !resistance.hasModifier(resistanceId);

        if (suppressKnockback) {
            resistance.addTransientModifier(new AttributeModifier(resistanceId, 1, AttributeModifier.Operation.ADD_VALUE));
        }

        SECONDARY.set(true);
        try {
            if (!shock) target.invulnerableTime = 0;
            Entity direct = toolCredit ? DIRECT_SOURCE.get() == null ? attacker : DIRECT_SOURCE.get() : null;
            DamageSource source = new DamageSource(target.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type), direct, attacker);
            hit = target.hurt(source, amount);
            return hit;
        } finally {
            target.invulnerableTime = shock && hit ? 0 : immunity;
            if (suppressKnockback) resistance.removeModifier(resistanceId);
            SECONDARY.set(previous);
        }
    }

    public static boolean secondary() {
        return SECONDARY.get();
    }

    public static int projectileCount(ItemStack stack, RandomSource random) {
        int count = 1;
        for (TraitInstance instance : traits(stack)) {
            TraitDefinition trait = instance.getDefinition();
            if (trait != null) count = trait.projectileCount(stack, instance.level(), instance.amount(), random, count);
        }
        return count;
    }

    public static float projectileInaccuracy(ItemStack stack, float inaccuracy) {
        float result = inaccuracy;
        for (TraitInstance instance : traits(stack)) {
            TraitDefinition trait = instance.getDefinition();
            if (trait != null) result = trait.projectileInaccuracy(stack, instance.level(), instance.amount(), result);
        }
        return result;
    }

    public static void projectileLaunch(TLToolProjectile projectile) {
        for (TraitInstance instance : traits(projectile.getItem())) {
            TraitDefinition trait = instance.getDefinition();
            if (trait != null) trait.projectileLaunch(projectile, instance.level(), instance.amount());
        }
    }

    public static void projectileHitBlock(TLToolProjectile projectile) {
        for (TraitInstance instance : traits(projectile.getItem())) {
            TraitDefinition trait = instance.getDefinition();
            if (trait != null) trait.projectileHitBlock(projectile, instance.level(), instance.amount());
        }
    }
}*/