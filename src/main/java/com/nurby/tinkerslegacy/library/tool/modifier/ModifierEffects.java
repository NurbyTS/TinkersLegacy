package com.nurby.tinkerslegacy.library.tool.modifier;

import com.nurby.tinkerslegacy.block.GlowBlock;
import com.nurby.tinkerslegacy.entity.TLToolProjectile;
import com.nurby.tinkerslegacy.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.library.tool.ToolModifiers;
import com.nurby.tinkerslegacy.library.tool.ToolModifierEntry;
import java.util.List;
import com.nurby.tinkerslegacy.library.tool.ToolState;
import com.nurby.tinkerslegacy.library.tool.traits.ToolTraits;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.tools.ProjectileToolDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;

public final class ModifierEffects {
        private static final ThreadLocal<RandomSource> RANDOM = ThreadLocal.withInitial(RandomSource::create);

        private ModifierEffects() {
        }

        private static List<ToolModifierEntry> entries(ItemStack stack) {
                return stack.getOrDefault(TLDataComponents.MODIFIERS, List.of());
        }

        // Durability
        public static int adjustDamage(ItemStack stack, int amount, LivingEntity entity) {
                return adjustDamage(stack, amount, amount, entity);
        }

        public static int adjustDamage(ItemStack stack, int original, int amount, LivingEntity entity) {
                int level = ToolModifiers.level(stack, ToolModifiers.REINFORCED);
                if (level >= 5) return 0;
                RandomSource random = entity == null ? RANDOM.get() : entity.getRandom();
                // Reinforced cancels the original wear, retaining extra wear added by material traits.
                int result = level > 0 && random.nextFloat() <= level * 0.2F ? Math.max(0, amount - original) : amount;
                for (ToolModifierEntry entry : entries(stack)) {
                        var definition = ToolModifiers.get(entry.id());
                        if (definition != null) result = definition.runtime().durability(stack, entry.points(), result, entity);
                }
                return Math.max(0, result);
        }

        // Combat
        public static float modifyDamage(ItemStack stack, LivingEntity attacker, LivingEntity target, float damage) {
                if (ToolState.broken(stack)) return damage;
                if (target.getType().is(EntityTypeTags.UNDEAD)) damage += ToolModifiers.points(stack, ToolModifiers.SMITE) * (7F / 24F);
                if (target.getType().is(EntityTypeTags.ARTHROPOD)) damage += ToolModifiers.points(stack, ToolModifiers.BANE) * (7F / 24F);
                for (ToolModifierEntry entry : entries(stack)) {
                        var definition = ToolModifiers.get(entry.id());
                        if (definition != null) damage = definition.runtime().damage(stack, entry.points(), attacker, target, damage);
                }
                return ToolTraits.has(stack, "squeaky") ? 0 : damage;
        }

        public static void beforeHit(ItemStack stack, LivingEntity attacker, LivingEntity target) {
                beforeHit(stack, attacker, target, attacker);
        }

        public static void beforeHit(ItemStack stack, LivingEntity attacker, LivingEntity target, Entity directSource) {
                if (attacker.level().isClientSide || ToolState.broken(stack) || !target.isAlive() || ToolTraits.has(stack, "squeaky")) return;
                int shulking = ToolModifiers.points(stack, ToolModifiers.SHULKING);
                if (shulking > 0) target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, shulking / 2 + 10));
                int webbed = ToolModifiers.level(stack, ToolModifiers.WEBBED);
                if (webbed > 0) target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, webbed * 20, 1));
                int fiery = ToolModifiers.points(stack, ToolModifiers.FIERY);
                if (fiery > 0 && !target.fireImmune()) {
                        target.igniteForSeconds(1 + fiery / 8);
                        int immunity = target.invulnerableTime;
                        var movement = target.getDeltaMovement();
                        try {
                                target.hurt(new DamageSource(target.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                                                .getHolderOrThrow(DamageTypes.IN_FIRE), directSource, attacker), fiery / 15F);
                        } finally {
                                target.invulnerableTime = immunity;
                                target.setDeltaMovement(movement);
                        }
                }
        }

        public static void afterHit(ItemStack stack, LivingEntity attacker, LivingEntity target, float actualDamage) {
                if (attacker.level().isClientSide || ToolState.broken(stack)) return;
                int necrotic = ToolModifiers.level(stack, ToolModifiers.NECROTIC);
                if (necrotic > 0) attacker.heal(actualDamage * 0.1F * necrotic);
                int knockback = ToolModifiers.points(stack, ToolModifiers.KNOCKBACK);
                if (knockback > 0) {
                        double yaw = Math.toRadians(attacker.getYRot());
                        target.knockback(knockback * 0.05, Math.sin(yaw), -Math.cos(yaw));
                }
                for (ToolModifierEntry entry : entries(stack)) {
                        var definition = ToolModifiers.get(entry.id());
                        if (definition != null) definition.runtime().afterHit(stack, entry.points(), attacker, target, actualDamage);
                }
                for (int heart = 0; heart < (int) (actualDamage / 2); heart++) {
                        advanceLuck(stack, attacker.getRandom(), attacker.registryAccess());
                }
        }

        // Properties
        public static void applyProperties(ItemStack stack) {
                if (ToolModifiers.level(stack, ToolModifiers.REINFORCED) >= 5) {
                        stack.set(DataComponents.UNBREAKABLE, new Unbreakable(false));
                }
                // Preserve explicit Unbreakable components supplied by commands or extensions.
        }

        // Enchantments
        public static void applyEnchantments(ItemStack stack, HolderLookup.Provider registries) {
                if (!(stack.getItem() instanceof DynamicTool tool)) return;
                ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(
                                stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY));
                enchantments.removeIf(holder -> holder.is(Enchantments.FORTUNE) || holder.is(Enchantments.LOOTING)
                                || holder.is(Enchantments.SILK_TOUCH));
                var lookup = registries.lookupOrThrow(Registries.ENCHANTMENT);
                int luck = ToolModifiers.level(stack, ToolModifiers.LUCK);
                if (luck > 0) {
                        if (tool.getDefinition().isHarvestTool()) enchantments.set(lookup.getOrThrow(Enchantments.FORTUNE), luck);
                        if (tool.getDefinition().isWeapon() || tool.getDefinition() instanceof ProjectileToolDefinition) {
                                enchantments.set(lookup.getOrThrow(Enchantments.LOOTING), luck);
                        }
                }
                if (ToolModifiers.has(stack, ToolModifiers.SILKTOUCH)) enchantments.set(lookup.getOrThrow(Enchantments.SILK_TOUCH), 1);
                stack.set(DataComponents.ENCHANTMENTS, enchantments.toImmutable());
                for (ToolModifierEntry entry : entries(stack)) {
                        var definition = ToolModifiers.get(entry.id());
                        if (definition != null) definition.runtime().enchantments(stack, entry.points(), registries);
                }
        }

        // Mining
        public static void afterBlockBreak(ItemStack stack, ServerLevel level, BlockPos pos) {
                advanceLuck(stack, level.random, level.registryAccess());
                for (ToolModifierEntry entry : entries(stack)) {
                        var definition = ToolModifiers.get(entry.id());
                        if (definition != null) definition.runtime().afterBlockBreak(stack, entry.points(), level, pos);
                }
        }

        private static void advanceLuck(ItemStack stack, RandomSource random, HolderLookup.Provider registries) {
                int points = ToolModifiers.points(stack, ToolModifiers.LUCK);
                if (points > 0 && points < 360 && random.nextFloat() < 0.03F) {
                        ToolModifiers.setPoints(stack, ToolModifiers.LUCK, points + 1);
                        if (stack.getItem() instanceof DynamicTool item) item.calculateStats(stack);
                        applyEnchantments(stack, registries);
                        ToolTraits.applyEnchantments(stack, registries);
                }
        }

        // Inventory
        public static void inventoryTick(ItemStack stack, Level level, Entity entity, boolean selected) {
                if (level.isClientSide || ToolState.broken(stack)) return;
                for (ToolModifierEntry entry : entries(stack)) {
                        var definition = ToolModifiers.get(entry.id());
                        if (definition != null) definition.runtime().inventoryTick(stack, entry.points(), level, entity, selected);
                }
                if (ToolModifiers.has(stack, ToolModifiers.GLOWING) && selected && level.getMaxLocalRawBrightness(entity.blockPosition()) < 8
                                && BuiltInRegistries.BLOCK.get(ToolModifiers.id("glow")) instanceof GlowBlock glow) {
                        BlockPos pos = entity.blockPosition();
                        for (BlockPos candidate : new BlockPos[] {pos, pos.above(), pos.north(), pos.east(), pos.south(), pos.west(), pos.below()}) {
                                if (glow.addGlow(level, candidate)) {
                                        if (!(entity instanceof Player player) || !player.isCreative()) {
                                                ToolState.hurt(stack, 1, entity instanceof LivingEntity living ? living : null);
                                        }
                                        break;
                                }
                        }
                }
                int moss = ToolModifiers.level(stack, ToolModifiers.MENDING_MOSS);
                if (moss == 0 || ToolState.damage(stack) <= 0 || stack.getOrDefault(TLDataComponents.MOSS_XP, 0) <= 0) return;
                if (entity instanceof Player player && player.getOffhandItem() != stack
                                && player.getInventory().items.subList(0, 9).stream().noneMatch(item -> item == stack)) return;
                long time = level.getGameTime();
                if (time - stack.getOrDefault(TLDataComponents.MOSS_LAST_HEAL, 0L) <= 150) return;
                stack.set(TLDataComponents.MOSS_XP, stack.get(TLDataComponents.MOSS_XP) - 1);
                stack.set(TLDataComponents.MOSS_LAST_HEAL, time);
                int repairs = stack.getOrDefault(TLDataComponents.REPAIR_COUNT, 0);
                ToolState.repair(stack, ToolTraits.heal(stack, 2 + moss));
                stack.set(TLDataComponents.REPAIR_COUNT, repairs);
        }

        public static int storeMossXp(ItemStack stack, int amount) {
                int level = ToolModifiers.level(stack, ToolModifiers.MENDING_MOSS);
                if (level <= 0 || amount <= 0) return 0;
                int maximum = level == 1 ? 100 : level == 2 ? 300 : 900;
                int stored = stack.getOrDefault(TLDataComponents.MOSS_XP, 0);
                int accepted = Math.min(amount, Math.max(0, maximum - stored));
                if (accepted > 0) stack.set(TLDataComponents.MOSS_XP, stored + accepted);
                return accepted;
        }

        public static int looting(LootContext context, Holder<Enchantment> enchantment, int vanilla) {
                if (!enchantment.is(Enchantments.LOOTING)) return vanilla;
                DamageSource source = context.getParamOrNull(LootContextParams.DAMAGE_SOURCE);
                if (source == null) return vanilla;
                ItemStack stack = source.getDirectEntity() instanceof TLToolProjectile projectile ? projectile.getItem()
                                : source.getEntity() instanceof LivingEntity living ? living.getMainHandItem() : ItemStack.EMPTY;
                return Math.max(vanilla, ToolModifiers.level(stack, ToolModifiers.LUCK));
        }

        // Projectiles
        public static void projectileMovement(ItemStack stack, TLToolProjectile projectile) {
                if (!projectile.isInWater() || !ToolModifiers.has(stack, ToolModifiers.FINS)) return;
                boolean shuriken = stack.getItem() instanceof DynamicTool item
                                && item.getDefinition() instanceof ProjectileToolDefinition definition
                                && definition.kind() == ProjectileToolDefinition.Kind.SHURIKEN;
                double retention = shuriken ? 0.96 : 0.992;
                projectile.setDeltaMovement(projectile.getDeltaMovement().scale(retention / 0.6));
        }
}
