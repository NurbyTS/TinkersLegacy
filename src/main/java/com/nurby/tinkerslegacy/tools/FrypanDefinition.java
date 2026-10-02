package com.nurby.tinkerslegacy.tools;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.library.tool.ToolDefinition;
import com.nurby.tinkerslegacy.library.tool.ToolPart;
import com.nurby.tinkerslegacy.registry.ToolStatTypes;
import com.nurby.tinkerslegacy.util.TinkersUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class FrypanDefinition extends ToolDefinition {
    public FrypanDefinition() {
        super(
                id("frypan"),
                List.of(
                        new ToolPart(id("pan_head"), ToolStatTypes.HEAD.getId()),
                        new ToolPart(id("tool_rod"), ToolStatTypes.HANDLE.getId())
                )
        );
    }

    public static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, name);
    }

    @Override
    public float attackSpeed() {
        return 1.4F;
    }

    @Override
    public float knockback() {
        return 2.0F;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 5 * 20;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        player.startUsingItem(hand);
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    @Override
    public void releaseUsing(
            ItemStack stack,
            Level level,
            LivingEntity entity,
            int timeLeft
    ) {
        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }

        float progress = Math.min(
                1.0F,
                (getUseDuration(stack, entity) - timeLeft) / 30.0F
        );

        float strength = 0.1F + 2.5F * progress * progress;

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(3.2D));

        AABB searchBox = player
                .getBoundingBox()
                .expandTowards(look.scale(3.2D))
                .inflate(1.0D);

        EntityHitResult hit =
                ProjectileUtil.getEntityHitResult(
                        player,
                        eye,
                        end,
                        searchBox,
                        target -> target != player && target.isPickable(),
                        3.2D * 3.2D
                );

        if (hit == null) {
            return;
        }

        Entity target = hit.getEntity();

        float damage = (float) (
                TinkersUtils.getActualDamage(stack)
                        * (1.0F + 0.3F * progress)
        );

        DamageSource source = level.damageSources().playerAttack(player);

        if (!target.hurt(source, damage)) {
            return;
        }

        Vec3 velocity = new Vec3(
                look.x * strength,
                look.y / 3.0D * strength
                        + 0.1D
                        + 0.4D * progress,
                look.z * strength
        );

        target.setDeltaMovement(
                target.getDeltaMovement().add(velocity)
        );
    }
}