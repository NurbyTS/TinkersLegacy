package com.nurby.tinkerslegacy.event;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.library.tool.ToolState;
import com.nurby.tinkerslegacy.library.tool.ToolAreaActions;
import com.nurby.tinkerslegacy.library.tool.ToolModifiers;
import com.nurby.tinkerslegacy.library.tool.traits.ToolTraits;
import com.nurby.tinkerslegacy.library.tool.modifier.ModifierEffects;
import com.nurby.tinkerslegacy.util.TinkersUtils;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.common.CommonHooks;
import com.nurby.tinkerslegacy.entity.TLToolProjectile;
import net.minecraft.world.level.GameRules;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import com.nurby.tinkerslegacy.tools.BattlesignDefinition;
import com.nurby.tinkerslegacy.tools.CleaverDefinition;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.Map;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = TinkersLegacy.MODID)
public final class ToolCombatAbilities {
        private static final Map<Player, AttackCharge> CHARGES = new WeakHashMap<>();
        private static final ThreadLocal<Boolean> SWEEPING = ThreadLocal.withInitial(() -> false);

        private ToolCombatAbilities() {
        }

        // Attack Charge
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void beforeAttack(AttackEntityEvent event) {
                if (SWEEPING.get()) return;
                Player player = event.getEntity();
                CHARGES.put(player, new AttackCharge(player.level().getGameTime(), player.getAttackStrengthScale(0.5F)));
        }

        public static float attackStrength(Player player) {
                AttackCharge charge = CHARGES.get(player);
                return charge != null && charge.tick() == player.level().getGameTime()
                                ? charge.strength() : player.getAttackStrengthScale(0.5F);
        }

        // Damage
        public static float finishDamage(ItemStack stack, float damage, float strength) {
                if (!(stack.getItem() instanceof DynamicTool tool)) return damage;
                float cooldown = 0.2F + strength * strength * 0.8F;
                // Vanilla supplies cooldown-scaled critical damage; original cutoff precedes that cooldown.
                return (float) TinkersUtils.calcCutoffDamage(damage / cooldown, tool.getDefinition().damageCutoff()) * cooldown;
        }

        public static boolean hit(Player player, ItemStack stack, Entity target, DamageSource source, float damage) {
                if (!(stack.getItem() instanceof DynamicTool tool) || ToolState.broken(stack)) return false;
                float health = target instanceof LivingEntity living ? living.getHealth() : 0;
                if (target instanceof LivingEntity living) {
                        ToolTraits.beforeHit(stack, player, living);
                        ModifierEffects.beforeHit(stack, player, living);
                }
                boolean successful = tool.getDefinition().dealDamage(player, target, source, damage);
                successful |= target instanceof LivingEntity living && living.getHealth() < health;
                if (successful && target instanceof LivingEntity living) {
                        float actual = Math.max(0, health - living.getHealth());
                        ToolTraits.afterHit(stack, player, living, actual);
                        ModifierEffects.afterHit(stack, player, living, actual);
                }
                return successful;
        }

        private static float secondaryDamage(Player player, ItemStack stack, LivingEntity target, float strength) {
                float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
                damage = ModifierEffects.modifyDamage(stack, player, target, ToolTraits.attackDamage(stack, player, target, damage));
                boolean critical = strength > 0.9F && player.fallDistance > 0 && !player.onGround()
                                && !player.onClimbable() && !player.isInWater() && !player.hasEffect(MobEffects.BLINDNESS)
                                && !player.isPassenger() && !player.isSprinting();
                var event = CommonHooks.fireCriticalHit(player, target, critical, critical ? 1.5F : 1);
                if (event.isCriticalHit()) damage *= event.getDamageMultiplier();
                float cooldown = 0.2F + strength * strength * 0.8F;
                return finishDamage(stack, damage * cooldown, strength);
        }

        public static boolean sweep(Player player, Entity target, DamageSource source) {
                if (player.level().isClientSide || SWEEPING.get()
                                || !(player.getMainHandItem().getItem() instanceof DynamicTool tool)) return false;
                ItemStack stack = player.getMainHandItem();
                double width = (ToolAreaActions.width(stack) - 1) / 2.0;
                double height = (ToolAreaActions.height(stack) - 1) / 2.0;
                AABB bounds = new AABB(target.position(), target.position().add(1, 1, 1)).inflate(width, height, width);
                float strength = attackStrength(player);
                boolean hit = false;
                SWEEPING.set(true);
                try {
                        for (LivingEntity other : player.level().getEntitiesOfClass(LivingEntity.class, bounds)) {
                                if (ToolState.broken(stack)) break;
                                if (other == player || other == target || !other.isAlive() || player.isAlliedTo(other)
                                                || !other.isAttackable() || other.skipAttackInteraction(player)
                                                || other instanceof Player defender && !player.canHarmPlayer(defender)
                                                || (width > 1 || height > 1) && other.distanceTo(target) > 3
                                                || NeoForge.EVENT_BUS.post(new AttackEntityEvent(player, other)).isCanceled()) continue;
                                float damage = secondaryDamage(player, stack, other, strength);
                                if (hit(player, stack, other, source, damage)) {
                                        tool.hurtOnHit(stack, player, damage);
                                        player.setLastHurtMob(other);
                                        player.causeFoodExhaustion(0.1F);
                                        hit = true;
                                }
                        }
                } finally {
                        SWEEPING.remove();
                }
                player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                                SoundSource.PLAYERS, 1, 1);
                player.sweepAttack();
                return hit;
        }

        // Battlesign
        public static boolean blocking(Player player) {
                ItemStack stack = player.getUseItem();
                return player.isBlocking() && stack.getItem() instanceof DynamicTool tool
                                && tool.getDefinition() instanceof BattlesignDefinition && !ToolState.broken(stack);
        }

        @SubscribeEvent(priority = EventPriority.LOW)
        public static void block(LivingShieldBlockEvent event) {
                if (!(event.getEntity() instanceof Player player) || player.level().isClientSide || !blocking(player)) return;
                DamageSource source = event.getDamageSource();
                if (source.is(DamageTypeTags.BYPASSES_SHIELD) || source.is(DamageTypeTags.IS_EXPLOSION)
                                || source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC)) {
                        event.setBlocked(false);
                        return;
                }
                if (!event.getOriginalBlock()) return;
                if (source.is(DamageTypeTags.IS_PROJECTILE)) return;
                float damage = event.getOriginalBlockedDamage();
                event.setBlockedDamage(damage * 0.65F);
                event.setShieldDamage(0);
                int wear = damage < 2 ? 1 : Math.round(damage / 2);
                Entity attacker = source.getEntity();
                if (attacker != null && attacker != player && !source.is(DamageTypes.THORNS)) {
                        attacker.hurt(player.damageSources().thorns(player), damage * 0.35F);
                        wear = wear * 3 / 2;
                }
                player.getUseItem().hurtAndBreak(wear, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
        }

        @SubscribeEvent(priority = EventPriority.LOW)
        public static void reflect(ProjectileImpactEvent event) {
                if (!(event.getRayTraceResult() instanceof EntityHitResult hit)
                                || !(hit.getEntity() instanceof ServerPlayer player) || !blocking(player)) return;
                Projectile projectile = event.getProjectile();
                Vec3 motion = projectile.getDeltaMovement();
                Vec3 look = player.getLookAngle();
                if (motion.lengthSqr() < 1.0E-7 || -look.dot(motion.normalize()) < 0.1) return;
                if (projectile instanceof AbstractArrow arrow && arrow.getPierceLevel() > 0) return;
                int wear = 1;
                if (projectile instanceof TLToolProjectile toolProjectile) {
                        wear = Mth.ceil(Math.clamp(toolProjectile.impactDamage(), 0, Integer.MAX_VALUE));
                } else if (projectile instanceof AbstractArrow arrow) {
                        wear = Mth.ceil(Math.clamp(motion.length() * arrow.getBaseDamage(), 0, Integer.MAX_VALUE));
                        if (arrow.isCritArrow()) wear += player.getRandom().nextInt(wear / 2 + 2);
                }
                event.setCanceled(true);
                projectile.setOwner(player);
                projectile.setDeltaMovement(look.scale(motion.length() + 0.2));
                projectile.setPos(player.getEyePosition().add(look.scale(0.8)));
                projectile.hurtMarked = true;
                ((ServerLevel) player.level()).getChunkSource().broadcastAndSend(projectile,
                                new ClientboundSetEntityMotionPacket(projectile));
                ToolLevelingEvents.reflected(player, player.getUseItem(), event, wear);
                player.getUseItem().hurtAndBreak(wear, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
        }

        // Beheading
        public static ItemStack head(LivingEntity entity) {
                if (entity instanceof Player player) {
                        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
                        head.set(DataComponents.PROFILE, new ResolvableProfile(player.getGameProfile()));
                        return head;
                }
                if (entity.getType() == EntityType.WITHER_SKELETON) return new ItemStack(Items.WITHER_SKELETON_SKULL);
                if (entity.getType() == EntityType.SKELETON || entity.getType() == EntityType.STRAY) return new ItemStack(Items.SKELETON_SKULL);
                if (entity.getType() == EntityType.ZOMBIE || entity.getType() == EntityType.HUSK) return new ItemStack(Items.ZOMBIE_HEAD);
                if (entity.getType() == EntityType.CREEPER) return new ItemStack(Items.CREEPER_HEAD);
                if (entity.getType() == EntityType.ENDER_DRAGON) return new ItemStack(Items.DRAGON_HEAD);
                if (entity.getType() == EntityType.PIGLIN || entity.getType() == EntityType.PIGLIN_BRUTE) return new ItemStack(Items.PIGLIN_HEAD);
                return ItemStack.EMPTY;
        }

        private static int beheading(DamageSource source) {
                ItemStack stack;
                if (source.getDirectEntity() instanceof TLToolProjectile projectile) {
                        stack = projectile.getItem();
                } else if (source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker) {
                        stack = attacker.getMainHandItem();
                } else return 0;
                if (!(stack.getItem() instanceof DynamicTool tool) || ToolState.broken(stack)) return 0;
                return Math.min(10, ToolModifiers.level(stack, ToolModifiers.BEHEADING)
                                + (tool.getDefinition() instanceof CleaverDefinition ? 2 : 0));
        }

        @SubscribeEvent
        public static void behead(LivingDropsEvent event) {
                if (event.getEntity() instanceof Player player
                                && player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;
                int level = beheading(event.getSource());
                if (level <= 0 || event.getEntity().getRandom().nextInt(10) >= level) return;
                ItemStack head = head(event.getEntity());
                if (head.isEmpty() || !(event.getEntity() instanceof Player)
                                && event.getDrops().stream().anyMatch(drop -> ItemStack.isSameItemSameComponents(drop.getItem(), head))) return;
                event.getDrops().add(new ItemEntity(event.getEntity().level(), event.getEntity().getX(),
                                event.getEntity().getY(), event.getEntity().getZ(), head));
        }

        @SubscribeEvent
        public static void playerHead(LivingDeathEvent event) {
                if (!(event.getEntity() instanceof Player player)
                                || !player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;
                int level = beheading(event.getSource());
                if (level > 0 && player.getRandom().nextInt(10) < level) player.spawnAtLocation(head(player));
        }

        @SubscribeEvent
        public static void stop(ServerStoppedEvent event) {
                CHARGES.clear();
        }

        private record AttackCharge(long tick, float strength) {
        }
}
