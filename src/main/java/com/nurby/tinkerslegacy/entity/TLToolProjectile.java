package com.nurby.tinkerslegacy.entity;

import com.nurby.tinkerslegacy.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.library.tool.ToolRangedActions;
import com.nurby.tinkerslegacy.library.tool.ToolModifiers;
import com.nurby.tinkerslegacy.library.tool.ToolVanillaProjectiles;
import com.nurby.tinkerslegacy.library.tool.ToolVisualEffects;
import com.nurby.tinkerslegacy.library.tool.traits.ToolTraits;
import com.nurby.tinkerslegacy.library.tool.traits.ToolTraitEffects;
import com.nurby.tinkerslegacy.library.tool.modifier.ModifierEffects;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLDamageTypes;
import com.nurby.tinkerslegacy.registry.TLToolEntities;
import com.nurby.tinkerslegacy.tools.ProjectileToolDefinition;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class TLToolProjectile extends AbstractArrow implements ItemSupplier, IEntityWithComplexSpawn {
        private static final EntityDataAccessor<ItemStack> DISPLAY = SynchedEntityData.defineId(
                        TLToolProjectile.class, EntityDataSerializers.ITEM_STACK);
        private static final EntityDataAccessor<Boolean> EMBEDDED = SynchedEntityData.defineId(
                        TLToolProjectile.class, EntityDataSerializers.BOOLEAN);
        private static final EntityDataAccessor<Integer> FLIGHT_TICKS = SynchedEntityData.defineId(
                        TLToolProjectile.class, EntityDataSerializers.INT);
        private final java.util.Set<Integer> piercedEntities = new java.util.HashSet<>();
        private ItemStack launcherStack = ItemStack.EMPTY;
        private UUID ammunitionId;
        private UUID launcherId;
        private float impactDamage;
        private float vanillaLaunchVelocity;
        private boolean recoverable;
        private boolean levelingEligible = true;

        public TLToolProjectile(EntityType<? extends TLToolProjectile> type, Level level) {
                super(type, level);
        }

        public TLToolProjectile(Level level, LivingEntity player, ItemStack ammunition, ItemStack launcher,
                        float damage, boolean recoverable) {
                super(TLToolEntities.PROJECTILE.get(), player, level, ammunition.copyWithCount(1), launcher.isEmpty() ? null : launcher);
                entityData.set(DISPLAY, ammunition.copyWithCount(1));
                ammunitionId = ammunition.get(TLDataComponents.TOOL_ID);
                launcherId = launcher.get(TLDataComponents.TOOL_ID);
                launcherStack = launcher.copyWithCount(1);
                impactDamage = damage;
                levelingEligible = !player.hasInfiniteMaterials();
                setRecoverable(recoverable);
        }

        // Getters
        @Override
        public ItemStack getItem() {
                return entityData.get(DISPLAY);
        }

        public ItemStack effectsStack() {
                ItemStack actual = ammunitionStack();
                ItemStack result = (actual.isEmpty() ? getItem() : actual).copy();
                // The final shot can land after its source ammunition has become empty.
                result.set(TLDataComponents.BROKEN, false);
                result.remove(TLDataComponents.BROKEN_DAMAGE);
                // Launcher attributes are included at launch; ordinary impact traits belong to the ammunition.
                return result;
        }

        private ItemStack ammunitionStack() {
                if (ammunitionId != null && getOwner() instanceof Player player) {
                        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                                ItemStack candidate = player.getInventory().getItem(slot);
                                if (ammunitionId.equals(candidate.get(TLDataComponents.TOOL_ID))) return candidate;
                        }
                }
                return ItemStack.EMPTY;
        }

        private void saveEffects(ItemStack effects) {
                ItemStack actual = ammunitionStack();
                if (actual.isEmpty()) return;
                actual.set(TLDataComponents.MODIFIERS, effects.getOrDefault(TLDataComponents.MODIFIERS, List.of()));
                actual.set(TLDataComponents.TRAIT_DATA, ToolTraits.data(effects));
                if (actual.getItem() instanceof DynamicTool item) item.calculateStats(actual);
                ModifierEffects.applyEnchantments(actual, registryAccess());
                ToolTraits.applyEnchantments(actual, registryAccess());
        }

        public boolean canEarnXp() {
                return levelingEligible;
        }

        public UUID ammunitionIdentity() {
                return ammunitionId;
        }

        public List<UUID> contributingTools() {
                LinkedHashSet<UUID> tools = new LinkedHashSet<>();
                if (ammunitionId != null) tools.add(ammunitionId);
                if (launcherId != null) tools.add(launcherId);
                return List.copyOf(tools);
        }

        public boolean isShuriken() {
                return getItem().getItem() instanceof DynamicTool item
                                && item.getDefinition() instanceof ProjectileToolDefinition projectile
                                && projectile.kind() == ProjectileToolDefinition.Kind.SHURIKEN;
        }

        public boolean isEmbedded() {
                return entityData.get(EMBEDDED);
        }

        public int flightTicks() {
                return entityData.get(FLIGHT_TICKS);
        }

        public float impactDamage() {
                return impactDamage;
        }

        @Override
        protected ItemStack getDefaultPickupItem() {
                return new ItemStack(Items.ARROW);
        }

        @Override
        protected void defineSynchedData(SynchedEntityData.Builder builder) {
                super.defineSynchedData(builder);
                builder.define(DISPLAY, ItemStack.EMPTY);
                builder.define(EMBEDDED, false);
                builder.define(FLIGHT_TICKS, 0);
        }

        @Override
        protected double getDefaultGravity() {
                double gravity = isShuriken() ? (tickCount / 10) * 0.04 : super.getDefaultGravity();
                if (ToolTraits.has(getItem(), "endspeed")) return gravity / 250;
                return ToolTraits.has(getItem(), "hovering") ? gravity * 0.05 : gravity;
        }

        @Override
        public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
                boolean vanilla = ToolVanillaProjectiles.weapon(launcherStack);
                if (vanilla) {
                        vanillaLaunchVelocity = velocity;
                        float accuracy = getItem().getOrDefault(TLDataComponents.MODIFIED_STATS, java.util.Map.<String, Number>of())
                                        .getOrDefault("accuracy", 1).floatValue();
                        inaccuracy = ToolTraits.projectileInaccuracy(getItem(), inaccuracy + (1 / Math.max(accuracy, 0.01F) - 1) * velocity / 2);
                }
                super.shoot(x, y, z, velocity, inaccuracy);
                if (vanilla) ToolTraits.projectileLaunch(this);
        }

        @Override
        public void tick() {
                if (!level().isClientSide && !inGround) entityData.set(FLIGHT_TICKS, flightTicks() + 1);
                flightStep();
                if (ToolTraits.has(getItem(), "endspeed") && tickCount > 1) {
                        int ticks = tickCount;
                        double traveled = 0;
                        Vec3 start = position();
                        // Keep vanilla collision checks for every accelerated segment, including protected entities.
                        for (int steps = 0; steps < 20000 && !inGround && !isRemoved() && traveled < 40; steps++) {
                                Vec3 previous = position();
                                flightStep();
                                tickCount = ticks;
                                double distance = position().distanceToSqr(previous);
                                traveled += distance;
                                if (distance < 0.001) break;
                        }
                        ToolVisualEffects.endspeed(this, start, traveled);
                }
                if (!inGround && !isRemoved() && ToolTraits.has(getItem(), "hovering")) ToolVisualEffects.hovering(this);
        }

        private void flightStep() {
                super.tick();
                if (!level().isClientSide) entityData.set(EMBEDDED, inGround);
                if (!inGround) {
                        double vanillaDrag = isInWater() ? getWaterInertia() : 0.99;
                        if (ToolTraits.has(getItem(), "endspeed")) setDeltaMovement(getDeltaMovement().scale(1 / vanillaDrag));
                        else if (ToolTraits.has(getItem(), "hovering")) setDeltaMovement(getDeltaMovement().scale(0.99 / vanillaDrag));
                        else if (isShuriken()) setDeltaMovement(getDeltaMovement().scale(0.95 / 0.99));
                        ModifierEffects.projectileMovement(getItem(), this);
                }
        }

        // Impacts
        @Override
        protected void onHitBlock(BlockHitResult hit) {
                super.onHitBlock(hit);
                if (!level().isClientSide) entityData.set(EMBEDDED, inGround);
                ToolTraits.projectileHitBlock(this);
        }

        @Override
        protected boolean canHitEntity(Entity entity) {
                return !piercedEntities.contains(entity.getId()) && super.canHitEntity(entity);
        }

        @Override
        protected void onHitEntity(EntityHitResult hit) {
                if (level().isClientSide) return;
                Entity target = hit.getEntity();
                Entity owner = getOwner();
                DamageSource ordinary = damageSources().arrow(this, owner == null ? this : owner);
                ItemStack effects = effectsStack();
                float damage = impactDamage;
                boolean vanilla = ToolVanillaProjectiles.weapon(launcherStack);
                if (vanilla && level() instanceof ServerLevel serverLevel) {
                        float fullSpeed = launcherStack.getItem() instanceof BowItem ? 3 : 3.15F;
                        float base = EnchantmentHelper.modifyDamage(serverLevel, launcherStack, target, ordinary, impactDamage / fullSpeed);
                        damage = (float) Math.ceil(Math.max(0, (vanillaLaunchVelocity > 0 ? vanillaLaunchVelocity : getDeltaMovement().length()) * base));
                        if (isCritArrow()) damage += random.nextInt(Math.max(1, (int) damage / 2 + 2));
                }
                if (owner instanceof LivingEntity attacker && target instanceof LivingEntity living) {
                        damage = ToolTraits.attackDamage(effects, attacker, living, damage);
                        damage = ModifierEffects.modifyDamage(effects, attacker, living, damage);
                        if (living instanceof EnderMan && ToolTraits.has(effects, "enderference")) {
                                ToolTraitEffects.apply(living, ToolTraitEffects.ENDERFERENCE, 100, 1);
                                // Endermen reject the projectile damage tag even when teleportation is suppressed.
                                ordinary = new DamageSource(registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                                                .getHolderOrThrow(DamageTypes.PLAYER_ATTACK), this, attacker);
                        }
                }
                if (effects.getItem() instanceof DynamicTool tool) {
                        damage = (float) com.nurby.tinkerslegacy.util.TinkersUtils.calcCutoffDamage(damage, tool.getDefinition().damageCutoff());
                }
                float health = target instanceof LivingEntity living ? living.getHealth() : 0;
                if (owner instanceof LivingEntity attacker && target instanceof LivingEntity living) {
                        ToolTraits.beforeHit(effects, attacker, living, this);
                        ModifierEffects.beforeHit(effects, attacker, living, this);
                }
                int previousFire = target.getRemainingFireTicks();
                if (isOnFire() && target.getType() != EntityType.ENDERMAN) target.igniteForSeconds(5);
                boolean bolt = getItem().getItem() instanceof DynamicTool item
                                && item.getDefinition() instanceof ProjectileToolDefinition projectile
                                && projectile.kind() == ProjectileToolDefinition.Kind.BOLT;
                boolean damaged = damage > 0 && target.hurt(ordinary, bolt ? damage / 2 : damage);
                if (target instanceof LivingEntity living && living.getHealth() < health) damaged = true;
                if (damaged && bolt && target instanceof LivingEntity living && target.isAlive()) {
                        int immunity = living.invulnerableTime;
                        living.invulnerableTime = 0;
                        DamageSource piercing = new DamageSource(level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                                        .getHolderOrThrow(TLDamageTypes.ARMOR_BYPASSING), this, owner);
                        target.hurt(piercing, damage / 2);
                        living.invulnerableTime = immunity;
                }
                if (!damaged) target.setRemainingFireTicks(previousFire);
                if (damaged) {
                        if (levelingEligible && owner instanceof Player player) {
                                com.nurby.tinkerslegacy.library.tool.ToolLeveling.launcherHit(player, launcherId,
                                                com.nurby.tinkerslegacy.library.tool.ToolLeveling.launcherXp(launcherStack), getDeltaMovement().length());
                        }
                        if (owner instanceof LivingEntity attacker && target instanceof LivingEntity living) {
                                float actual = Math.max(0, health - living.getHealth());
                                ToolTraits.afterHit(effects, attacker, living, actual, this);
                                ModifierEffects.afterHit(effects, attacker, living, actual);
                                saveEffects(effects);
                        }
                        if (owner instanceof LivingEntity living) living.setLastHurtMob(target);
                        if (vanilla && target instanceof LivingEntity living && level() instanceof ServerLevel serverLevel) {
                                doKnockback(living, ordinary);
                                EnchantmentHelper.doPostAttackEffectsWithItemSource(serverLevel, living, ordinary, getWeaponItem());
                        }
                        playSound(SoundEvents.ARROW_HIT, 1, 1.2F);
                        if (getPierceLevel() > 0) piercedEntities.add(target.getId());
                        if (getPierceLevel() <= 0 || piercedEntities.size() > getPierceLevel()) discard();
                } else if (isShuriken()) {
                        discard();
                } else {
                        setDeltaMovement(getDeltaMovement().scale(-0.1));
                        setYRot(getYRot() + 180);
                }
        }

        // Recovery
        public void setRecoverable(boolean recoverable) {
                this.recoverable = recoverable;
                pickup = recoverable ? Pickup.ALLOWED : Pickup.CREATIVE_ONLY;
        }

        @Override
        protected boolean tryPickup(Player player) {
                if (pickup == Pickup.CREATIVE_ONLY) return player.hasInfiniteMaterials();
                if (!recoverable || pickup != Pickup.ALLOWED || ammunitionId == null) return false;
                for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                        ItemStack stack = player.getInventory().getItem(slot);
                        if (matchesAmmunition(stack) && ToolRangedActions.restoreAmmo(stack)) {
                                recoverable = false;
                                pickup = Pickup.DISALLOWED;
                                return true;
                        }
                }
                return false;
        }

        private boolean matchesAmmunition(ItemStack stack) {
                ItemStack reference = getItem();
                return stack.is(reference.getItem())
                                && Objects.equals(stack.get(TLDataComponents.MATERIALS), reference.get(TLDataComponents.MATERIALS))
                                && ToolModifiers.sameModifiers(stack, reference)
                                && Objects.equals(stack.get(TLDataComponents.BASE_STATS), reference.get(TLDataComponents.BASE_STATS));
        }

        // Synchronization
        @Override
        public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
                buffer.writeDouble(getDeltaMovement().x);
                buffer.writeDouble(getDeltaMovement().y);
                buffer.writeDouble(getDeltaMovement().z);
        }

        @Override
        public void readSpawnData(RegistryFriendlyByteBuf buffer) {
                setDeltaMovement(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        }

        // Persistence
        @Override
        public void addAdditionalSaveData(CompoundTag tag) {
                super.addAdditionalSaveData(tag);
                if (!getItem().isEmpty()) tag.put("ToolDisplay", getItem().save(registryAccess()));
                if (!launcherStack.isEmpty()) tag.put("ToolLauncher", launcherStack.save(registryAccess()));
                if (ammunitionId != null) tag.putUUID("AmmunitionId", ammunitionId);
                if (launcherId != null) tag.putUUID("LauncherId", launcherId);
                tag.putFloat("ImpactDamage", impactDamage);
                tag.putFloat("VanillaLaunchVelocity", vanillaLaunchVelocity);
                tag.putBoolean("Recoverable", recoverable);
                tag.putBoolean("LevelingEligible", levelingEligible);
                tag.putInt("FlightTicks", flightTicks());
        }

        @Override
        public void readAdditionalSaveData(CompoundTag tag) {
                super.readAdditionalSaveData(tag);
                entityData.set(DISPLAY, ItemStack.parseOptional(registryAccess(), tag.getCompound("ToolDisplay")));
                launcherStack = ItemStack.parseOptional(registryAccess(), tag.getCompound("ToolLauncher"));
                ammunitionId = tag.hasUUID("AmmunitionId") ? tag.getUUID("AmmunitionId") : null;
                launcherId = tag.hasUUID("LauncherId") ? tag.getUUID("LauncherId") : null;
                impactDamage = Math.max(0, tag.getFloat("ImpactDamage"));
                vanillaLaunchVelocity = Math.max(0, tag.getFloat("VanillaLaunchVelocity"));
                recoverable = tag.getBoolean("Recoverable");
                // Older saved projectiles predate this launch-time guard.
                levelingEligible = !tag.contains("LevelingEligible") || tag.getBoolean("LevelingEligible");
                entityData.set(EMBEDDED, inGround);
                entityData.set(FLIGHT_TICKS, Math.max(0, tag.getInt("FlightTicks")));
        }
}
