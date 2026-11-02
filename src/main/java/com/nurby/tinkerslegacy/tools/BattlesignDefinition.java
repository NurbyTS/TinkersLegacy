package com.nurby.tinkerslegacy.tools;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.library.tool.ToolPart;
import com.nurby.tinkerslegacy.registry.ToolStatTypes;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

import java.util.List;

public class BattlesignDefinition extends ClassicToolDefinition {
        public BattlesignDefinition() {
                super(id("battlesign"), List.of(
                                new ToolPart(id("tool_rod"), ToolStatTypes.HANDLE.getId()),
                                new ToolPart(id("sign_head"), ToolStatTypes.HEAD.getId())));
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
        public float damagePotential() { return 0.86F; }

        @Override
        public float attackSpeed() { return 1.2F; }

        // Blocking
        @Override
        public boolean canPerformAction(ItemStack stack, ItemAbility ability) {
                return ability == ItemAbilities.SHIELD_BLOCK;
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
                player.startUsingItem(hand);
                return InteractionResultHolder.consume(player.getItemInHand(hand));
        }

        @Override
        public int getUseDuration(ItemStack stack, LivingEntity entity) { return 72000; }

        @Override
        public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BLOCK; }
}
