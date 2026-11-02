package com.nurby.tinkerslegacy.library.tool.traits;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.entity.TLToolProjectile;
import com.nurby.tinkerslegacy.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.library.material.layer.MaterialLayer;
import com.nurby.tinkerslegacy.library.tool.ToolState;
import com.nurby.tinkerslegacy.library.tool.ToolVisualEffects;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLDamageTypes;
import com.nurby.tinkerslegacy.registry.TLRegistries;
import com.nurby.tinkerslegacy.registry.ToolStatTypes;
import com.nurby.tinkerslegacy.tools.ProjectileToolDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

public final class ToolTraits {
        private static final Map<ResourceLocation, TraitDefinition> DEFINITIONS = new LinkedHashMap<>();
        private static final ThreadLocal<Boolean> SECONDARY = ThreadLocal.withInitial(() -> false);
        private static final ThreadLocal<Entity> DIRECT_SOURCE = new ThreadLocal<>();

        static {
                for (String name : List.of("alien", "aquadynamic", "aridiculous", "autosmelt", "baconlicious", "cheap",
                                "cheapskate", "coldblooded", "crude1", "crude2", "crumbling", "dense", "depthdigger", "duritos",
                                "ecological", "enderference", "established", "flammable", "fractured", "heavy", "hellish", "holy",
                                "insatiable", "jagged", "lightweight", "magnetic1", "magnetic2", "momentum", "petramor", "poisonous",
                                "prickly", "sharp", "shocking", "slimey_green", "slimey_blue", "spiky", "splintering", "splinters",
                                "squeaky", "superheat", "stiff", "stonebound", "tasty", "unnatural", "writable1", "writable2",
                                "breakable", "endspeed", "freezing", "hovering", "splitting")) {
                        register(id(name), new BuiltinToolTrait(name));
                }
        }

        private ToolTraits() {
        }

        // Registration
        public static void register(ResourceLocation id, TraitDefinition definition) {
                if (DEFINITIONS.putIfAbsent(id, definition) != null) throw new IllegalArgumentException("Duplicate trait: " + id);
        }

        public static Map<ResourceLocation, TraitDefinition> definitions() {
                return Collections.unmodifiableMap(DEFINITIONS);
        }

        public static ResourceLocation id(String name) {
                return ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, name);
        }

        // Getters
        public static List<ResourceLocation> materialTraits(ResourceLocation materialId, ResourceLocation statRole) {
                var material = TLRegistries.MATERIALS.containsKey(materialId) ? TLRegistries.MATERIALS.get(materialId) : null;
                return material == null ? List.of() : material.traits(statRole);
        }

        public static List<ResourceLocation> traits(ItemStack stack) {
                List<ResourceLocation> snapshot = stack.get(TLDataComponents.EFFECT_TRAITS);
                if (snapshot != null) return snapshot;
                if (!(stack.getItem() instanceof DynamicTool tool)) return List.of();
                boolean projectileHead = tool.getDefinition() instanceof ProjectileToolDefinition projectile
                                && projectile.kind() != ProjectileToolDefinition.Kind.BOLT;
                LinkedHashSet<ResourceLocation> traits = new LinkedHashSet<>();
                for (MaterialLayer layer : stack.getOrDefault(TLDataComponents.MATERIALS, List.<MaterialLayer>of())) {
                        addTraits(traits, layer, projectileHead);
                }
                for (MaterialLayer layer : stack.getOrDefault(TLDataComponents.EMBOSSMENT, List.<MaterialLayer>of())) {
                        addTraits(traits, layer, projectileHead);
                }
                return List.copyOf(traits);
        }

        private static void addTraits(LinkedHashSet<ResourceLocation> traits, MaterialLayer layer, boolean projectileHead) {
                traits.addAll(materialTraits(layer.material(), layer.statType()));
                // Arrow heads and shuriken blades also supply projectile traits, without adding a rendered material layer.
                if (projectileHead && layer.statType().equals(ToolStatTypes.HEAD.getId())) {
                        traits.addAll(materialTraits(layer.material(), ToolStatTypes.PROJECTILE.getId()));
                }
        }

        public static int color(ItemStack stack, ResourceLocation id) {
                boolean projectileHead = stack.getItem() instanceof DynamicTool tool
                                && tool.getDefinition() instanceof ProjectileToolDefinition projectile
                                && projectile.kind() != ProjectileToolDefinition.Kind.BOLT;
                for (var component : List.of(TLDataComponents.MATERIALS, TLDataComponents.EMBOSSMENT)) {
                        for (MaterialLayer layer : stack.getOrDefault(component, List.<MaterialLayer>of())) {
                                if (!TLRegistries.MATERIALS.containsKey(layer.material())) continue;
                                var material = TLRegistries.MATERIALS.get(layer.material());
                                LinkedHashSet<ResourceLocation> traits = new LinkedHashSet<>();
                                addTraits(traits, layer, projectileHead);
                                if (traits.stream().anyMatch(trait -> displayId(trait).equals(displayId(id)))) {
                                        // The first source of a trait keeps its material color, including leveled traits.
                                        return material.color();
                                }
                        }
                }
                TraitDefinition definition = DEFINITIONS.get(id);
                return definition == null ? 0xFFFFFF : definition.color();
        }

        public static ResourceLocation displayId(ResourceLocation id) {
                if (id.getNamespace().equals(TinkersLegacy.MODID)
                                && id.getPath().matches("(?:magnetic|crude|writable)[12]")) {
                        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(),
                                        id.getPath().substring(0, id.getPath().length() - 1));
                }
                return id;
        }

        public static boolean isPresent(ItemStack stack, ResourceLocation id) {
                return traits(stack).contains(id);
        }

        public static boolean has(ItemStack stack, String name) {
                return isPresent(stack, id(name));
        }

        public static int level(ItemStack stack, String name) {
                return (has(stack, name + "1") ? 1 : 0) + (has(stack, name + "2") ? 2 : 0);
        }

        public static int bonusSlots(ItemStack stack) {
                return traits(stack).stream().map(DEFINITIONS::get).filter(java.util.Objects::nonNull)
                                .mapToInt(TraitDefinition::bonusSlots).sum();
        }

        public static ToolTraitData data(ItemStack stack) {
                return stack.getOrDefault(TLDataComponents.TRAIT_DATA, ToolTraitData.EMPTY);
        }

        // Calculated Effects
        public static Map<String, Number> applyStats(ItemStack stack, Map<String, Number> base) {
                Map<String, Number> result = new LinkedHashMap<>(base);
                for (ResourceLocation id : traits(stack)) {
                        TraitDefinition trait = DEFINITIONS.get(id);
                        if (trait != null) trait.applyStats(stack, result);
                }
                return result;
        }

        public static Map<String, Number> finishStats(ItemStack stack, Map<String, Number> stats) {
                Map<String, Number> result = new LinkedHashMap<>(stats);
                if (has(stack, "cheapskate") && result.containsKey("durability")) {
                        result.put("durability", Math.max(1, result.get("durability").intValue() * 80 / 100));
                }
                return result;
        }

        public static void applyEnchantments(ItemStack stack) {
                if (has(stack, "squeaky")) {
                        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
                        if (server != null && server.isSameThread()) applyEnchantments(stack, server.registryAccess());
                }
        }

        public static void applyEnchantments(ItemStack stack, net.minecraft.core.HolderLookup.Provider access) {
                if (has(stack, "squeaky")) {
                        var enchantment = access.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH);
                        EnchantmentHelper.updateEnchantments(stack, mutable -> mutable.set(enchantment, 1));
                }
        }

        public static int damage(ItemStack stack, int amount, @Nullable LivingEntity entity) {
                int result = amount;
                for (ResourceLocation id : traits(stack)) {
                        TraitDefinition trait = DEFINITIONS.get(id);
                        if (trait != null) result = trait.damage(stack, amount, result, entity);
                }
                return Math.max(0, result);
        }

        public static int heal(ItemStack stack, int amount) {
                int result = amount;
                for (ResourceLocation id : traits(stack)) {
                        TraitDefinition trait = DEFINITIONS.get(id);
                        if (trait != null) result = trait.heal(stack, amount, result);
                }
                return Math.max(0, result);
        }

        // Actions
        public static float attackDamage(ItemStack stack, LivingEntity attacker, LivingEntity target, float amount) {
                float result = amount;
                for (ResourceLocation id : traits(stack)) {
                        TraitDefinition trait = DEFINITIONS.get(id);
                        if (trait != null) result = trait.attackDamage(stack, attacker, target, amount, result);
                }
                if (has(stack, "squeaky")) {
                        if (!attacker.level().isClientSide) attacker.level().playSound(null, attacker.blockPosition(),
                                        com.nurby.tinkerslegacy.registry.TLSounds.TOY_SQUEAK.get(), net.minecraft.sounds.SoundSource.PLAYERS,
                                        1, 0.8F + attacker.getRandom().nextFloat() * 0.4F);
                        return 0;
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
                        for (ResourceLocation id : traits(stack)) {
                                TraitDefinition trait = DEFINITIONS.get(id);
                                if (trait != null) trait.beforeHit(stack, attacker, target);
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
                        for (ResourceLocation id : traits(stack)) {
                                TraitDefinition trait = DEFINITIONS.get(id);
                                if (trait != null) trait.afterHit(stack, attacker, target, damage);
                        }
                } finally {
                        if (previous == null) DIRECT_SOURCE.remove();
                        else DIRECT_SOURCE.set(previous);
                }
        }

        public static void afterBlockBreak(ItemStack stack, ServerLevel level, BlockState state, BlockPos pos, LivingEntity entity) {
                for (ResourceLocation id : traits(stack)) {
                        TraitDefinition trait = DEFINITIONS.get(id);
                        if (trait != null) trait.afterBlockBreak(stack, level, state, pos, entity);
                }
        }

        public static void inventoryTick(ItemStack stack, Level level, Entity entity, boolean selected) {
                if (level.isClientSide) return;
                for (ResourceLocation id : traits(stack)) {
                        TraitDefinition trait = DEFINITIONS.get(id);
                        if (trait != null) trait.inventoryTick(stack, level, entity, selected);
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
                var resistance = target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE);
                ResourceLocation resistanceId = id("trait_knockback_guard");
                boolean suppressKnockback = !shock && resistance != null && !resistance.hasModifier(resistanceId);
                if (suppressKnockback) resistance.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                resistanceId, 1, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
                SECONDARY.set(true);
                try {
                        if (!shock) target.invulnerableTime = 0;
                        Entity direct = toolCredit ? DIRECT_SOURCE.get() == null ? attacker : DIRECT_SOURCE.get() : null;
                        DamageSource source = new DamageSource(target.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                                        .getHolderOrThrow(type), direct, attacker);
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

        // Projectiles
        public static int projectileCount(ItemStack stack, RandomSource random) {
                return has(stack, "splitting") && random.nextFloat() < 0.5F ? 2 : 1;
        }

        public static float projectileInaccuracy(ItemStack stack, float inaccuracy) {
                return has(stack, "endspeed") ? inaccuracy * 2F / 3F : inaccuracy;
        }

        public static void projectileLaunch(TLToolProjectile projectile) {
                if (has(projectile.getItem(), "hovering")) projectile.setDeltaMovement(projectile.getDeltaMovement().scale(0.5));
                if (has(projectile.getItem(), "endspeed")) projectile.setDeltaMovement(projectile.getDeltaMovement().scale(0.1));
        }

        public static void projectileHitBlock(TLToolProjectile projectile) {
                if (!projectile.level().isClientSide && has(projectile.getItem(), "breakable") && projectile.level().random.nextFloat() < 0.5F) {
                        projectile.discard();
                }
        }
}
