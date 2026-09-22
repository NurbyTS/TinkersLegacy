package com.nurby.tinkerslegacy.tools;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.library.material.MaterialDefinition;
import com.nurby.tinkerslegacy.library.material.layer.MaterialLayer;
import com.nurby.tinkerslegacy.library.material.stat.MaterialStatMap;
import com.nurby.tinkerslegacy.library.tool.ToolDefinition;
import com.nurby.tinkerslegacy.library.tool.ToolPart;
import com.nurby.tinkerslegacy.registry.ToolStatTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnegative;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LongswordDefinition extends ToolDefinition {

    public LongswordDefinition() {
        super(
                id("longsword"),
                List.of(
                        new ToolPart(
                                id("sword_blade"),
                                ToolStatTypes.HEAD.getId()
                        ),
                        new ToolPart(
                                id("tool_rod"),
                                ToolStatTypes.HANDLE.getId()
                        ),
                        new ToolPart(
                                id("wide_guard"),
                                ToolStatTypes.EXTRA.getId()
                        )
                )
        );
    }

    @Override
    public boolean isTool() {
        return false;
    }

    @Override
    public float damagePotential() {
        return 1.1f;
    }

    @Override
    public float damageCutoff() {
        return 18f;
    }

    @Override
    public float attackSpeed() {
        return 1.4f;
    }

    @Override
    public float durabilityModifier() {
        return 1.05f;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(
                TinkersLegacy.MODID,
                path
        );
    }

    @Override
    public void addExtraToolRules(ItemStack stack, Map<String, Number> stats, List<Tool.Rule> rules, float miningSpeed) {
        rules.add(
                Tool.Rule.minesAndDrops(
                        BlockTags.SWORD_EFFICIENT,
                        miningSpeed
                )
        );
    }

    @Override
    public InteractionResult useOn(
            UseOnContext context
    ) {
        return Items.DIAMOND_SWORD.useOn(context);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isFallFlying()) {
            return InteractionResultHolder.pass(stack);
        }

        if (player.getCooldowns().isOnCooldown(stack.getItem())) {
            return InteractionResultHolder.pass(stack);
        }

        player.startUsingItem(hand);

        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(
            ItemStack stack,
            LivingEntity entity
    ) {
        return 200;
    }

    @Override
    public UseAnim getUseAnimation(
            ItemStack stack
    ) {
        return UseAnim.BOW;
    }

    @Override
    public void releaseUsing(
            ItemStack stack,
            Level level,
            LivingEntity entity,
            int timeCharged
    ) {
        int time =
                getUseDuration(stack, entity) - timeCharged;

        if (time <= 5 || !(entity instanceof Player player)) {
            return;
        }

        player.getCooldowns().addCooldown(
                stack.getItem(),
                3
        );

        player.causeFoodExhaustion(0.2F);
        player.setSprinting(true);

        float increase = Math.min(
                0.02F * time + 0.2F,
                0.56F
        );

        Vec3 look = player.getLookAngle();

        double speed = Math.min(
                0.05F * time,
                0.925F
        );

        player.setDeltaMovement(
                player.getDeltaMovement().x
                        + look.x * speed,
                player.getDeltaMovement().y
                        + increase,
                player.getDeltaMovement().z
                        + look.z * speed
        );
    }

    @Override
    public boolean hurtEnemy(
            ItemStack stack,
            LivingEntity target,
            LivingEntity attacker
    ) {
        if (attacker instanceof Player player
                && player.level() instanceof ServerLevel level) {

            Vec3 direction = player.getLookAngle();

            double length = Math.sqrt(
                    direction.x * direction.x
                            + direction.z * direction.z
            );

            if (length > 0.0D) {
                double offsetX =
                        direction.x / length * 0.7D;

                double offsetZ =
                        direction.z / length * 0.7D;

                level.sendParticles(
                        ParticleTypes.SWEEP_ATTACK,
                        player.getX() + offsetX,
                        player.getY() + 1.0D,
                        player.getZ() + offsetZ,
                        1,
                        0.0D,
                        0.0D,
                        0.0D,
                        0.0D
                );
            }
        }

        return true;
    }
}