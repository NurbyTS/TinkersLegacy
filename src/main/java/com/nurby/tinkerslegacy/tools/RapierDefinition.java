package com.nurby.tinkerslegacy.tools;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.library.tool.ToolDefinition;
import com.nurby.tinkerslegacy.library.tool.ToolPart;
import com.nurby.tinkerslegacy.registry.TLDamageTypes;
import com.nurby.tinkerslegacy.registry.ToolStatTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class RapierDefinition extends ToolDefinition {
    public RapierDefinition() {
        super(
                id("rapier"),
                List.of(
                        new ToolPart(id("sword_blade"), ToolStatTypes.HEAD.getId()),
                        new ToolPart(id("tool_rod"), ToolStatTypes.HANDLE.getId()),
                        new ToolPart(id("cross_guard"), ToolStatTypes.EXTRA.getId())
                )
        );
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(
                TinkersLegacy.MODID,
                path
        );
    }

    @Override
    public boolean isTool() {
        return false;
    }

    @Override
    public float damagePotential() {
        return 0.55f;
    }

    @Override
    public float damageCutoff() {
        return 13f;
    }

    @Override
    public float attackSpeed() {
        return 3.0f;
    }

    @Override
    public float knockback() {
        return 0.6f;
    }

    @Override
    public float durabilityModifier() {
        return 0.8F;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack stack = player.getItemInHand(hand);

        if (!player.onGround()) {
            return InteractionResultHolder.pass(stack);
        }

        player.causeFoodExhaustion(0.1F);

        Vec3 look = player.getLookAngle();

        player.setDeltaMovement(
                -look.x * 0.5D,
                player.getDeltaMovement().y + 0.32D,
                -look.z * 0.5D
        );

        player.getCooldowns().addCooldown(
                stack.getItem(),
                4
        );

        if (hand == InteractionHand.MAIN_HAND) {
            ItemStack offhand = player.getOffhandItem();

            if (!offhand.isEmpty() || offhand.getItem() instanceof ShieldItem) {
                return InteractionResultHolder.pass(stack);
            }
        }

        return InteractionResultHolder.success(stack);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public boolean dealDamage(
            Player player,
            Entity target,
            DamageSource source,
            float damage
    ) {
        if (!(target instanceof LivingEntity)) {
            return target.hurt(source, damage);
        }

        float halfDamage = damage / 2.0F;

        target.invulnerableTime = 0;

        boolean hit = target.hurt(source, halfDamage);

        if (hit) {
            target.invulnerableTime = 0;

            target.hurt(
                    TLDamageTypes.rapierDamageSource(player),
                    halfDamage
            );
        }

        return hit;
    }
}
