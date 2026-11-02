package com.nurby.tinkerslegacy.content.tools;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.api.tool.ToolPart;
import com.nurby.tinkerslegacy.api.tool.ToolState;
import com.nurby.tinkerslegacy.api.tool.ToolVisualEffects;
import com.nurby.tinkerslegacy.event.ToolCombatEvents;
import com.nurby.tinkerslegacy.registry.tools.ToolStatTypes;
import com.nurby.tinkerslegacy.util.TinkersUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class FrypanDefinition extends ClassicToolDefinition {
        private static final ResourceLocation CHARGE_BONUS = id("frypan_charge");
        private static final ThreadLocal<Charge> CHARGE = new ThreadLocal<>();

        public FrypanDefinition() {
                super(id("frypan"), List.of(
                                new ToolPart(id("tool_rod"), ToolStatTypes.HANDLE.getId()),
                                new ToolPart(id("pan_head"), ToolStatTypes.HEAD.getId())));
        }

        private static ResourceLocation id(String name) {
                return ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, name);
        }

        // Stats
        @Override
        public boolean isTool() { return false; }

        @Override
        public boolean isWeapon() { return true; }

        @Override
        public float attackSpeed() { return 1.4F; }

        @Override
        public float knockback() { return 2.0F; }

        // Charging
        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
                player.startUsingItem(hand);
                return InteractionResultHolder.consume(player.getItemInHand(hand));
        }

        @Override
        public int getUseDuration(ItemStack stack, LivingEntity entity) { return 100; }

        @Override
        public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BOW; }

        @Override
        public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
                if (!(entity instanceof ServerPlayer player) || ToolState.broken(stack)) return;
                float progress = Math.clamp((getUseDuration(stack, player) - timeLeft) / 30.0F, 0, 1);
                Vec3 eye = player.getEyePosition();
                Vec3 look = player.getLookAngle();
                Vec3 end = eye.add(look.scale(3.2));
                Vec3 clipped = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER,
                                ClipContext.Fluid.NONE, player)).getLocation();
                EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, clipped,
                                player.getBoundingBox().expandTowards(look.scale(3.2)).inflate(1),
                                target -> target.isPickable() && !target.isSpectator() && !player.isAlliedTo(target),
                                eye.distanceToSqr(clipped));
                if (hit == null) return;
                Entity target = hit.getEntity();
                var attack = player.getAttribute(Attributes.ATTACK_DAMAGE);
                if (attack == null) return;
                boolean offhand = player.getOffhandItem() == stack && player.getMainHandItem() != stack;
                if (!offhand && player.getMainHandItem() != stack) return;
                ItemStack previousMain = player.getMainHandItem();
                AttributeModifier previousDamage = attack.getModifier(Item.BASE_ATTACK_DAMAGE_ID);
                double change = offhand ? TinkersUtils.getActualDamage(stack) - 1
                                - (previousDamage == null ? 0 : previousDamage.amount()) : 0;
                boolean flaming = progress >= 1 && !target.isOnFire();
                Charge charge = new Charge(player, target);
                CHARGE.set(charge);
                if (offhand) {
                        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                        player.setItemInHand(InteractionHand.OFF_HAND, previousMain);
                }
                if (flaming) target.igniteForSeconds(1);
                attack.addTransientModifier(new AttributeModifier(CHARGE_BONUS, progress * 5 + change,
                                AttributeModifier.Operation.ADD_VALUE));
                try {
                        player.attack(target);
                } finally {
                        attack.removeModifier(CHARGE_BONUS);
                        CHARGE.remove();
                        if (flaming) target.clearFire();
                        if (offhand) {
                                player.setItemInHand(InteractionHand.MAIN_HAND, previousMain);
                                player.setItemInHand(InteractionHand.OFF_HAND, stack);
                        }
                }
                if (!charge.hit) return;
                float strength = 0.1F + 2.5F * progress * progress;
                target.push(look.x * strength, look.y / 3 * strength + 0.1 + 0.4 * progress, look.z * strength);
                target.hurtMarked = true;
                ToolVisualEffects.sweep(player, 0.6);
                level.playSound(null, player.blockPosition(), com.nurby.tinkerslegacy.registry.TLSounds.FRYPAN_BOING.get(),
                                SoundSource.PLAYERS, 2.0F, 1.0F);
        }

        @Override
        public boolean dealDamage(Player player, Entity target, DamageSource source, float damage) {
                boolean hit = target.hurt(source, damage);
                Charge charge = CHARGE.get();
                if (charge != null && charge.player == player && charge.target == target) charge.hit = hit;
                if (hit) player.level().playSound(null, target.blockPosition(), com.nurby.tinkerslegacy.registry.TLSounds.FRYPAN_BOING.get(),
                                SoundSource.PLAYERS, 1.5F, 0.6F + player.getRandom().nextFloat() * 0.2F);
            if (hit && ToolCombatEvents.attackStrength(player) > 0.9F)
                ToolVisualEffects.sweep(player, 0.8);
                return hit;
        }

        private static final class Charge {
                private final Player player;
                private final Entity target;
                private boolean hit;

                private Charge(Player player, Entity target) {
                        this.player = player;
                        this.target = target;
                }
        }
}
