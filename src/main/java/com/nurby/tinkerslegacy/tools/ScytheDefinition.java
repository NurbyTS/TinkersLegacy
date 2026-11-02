package com.nurby.tinkerslegacy.tools;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.library.tool.ToolAreaActions;
import com.nurby.tinkerslegacy.library.tool.ToolPart;
import com.nurby.tinkerslegacy.library.tool.ToolState;
import com.nurby.tinkerslegacy.registry.ToolStatTypes;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.AABB;

import net.neoforged.neoforge.common.IShearable;

import java.util.List;
import java.util.Map;

public class ScytheDefinition extends AreaToolDefinition {
        public ScytheDefinition() {
                super(id("scythe"), List.of(
                                new ToolPart(id("tough_tool_rod"), ToolStatTypes.HANDLE.getId()),
                                new ToolPart(id("scythe_head"), ToolStatTypes.HEAD.getId()),
                                new ToolPart(id("tough_binding"), ToolStatTypes.EXTRA.getId()),
                                new ToolPart(id("tough_tool_rod"), ToolStatTypes.HANDLE.getId())));
        }

        private static ResourceLocation id(String name) {
                return ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, name);
        }

        // Stats
        @Override
        public boolean isWeapon() { return true; }

        @Override
        public float damagePotential() { return 0.75F; }

        @Override
        public float attackSpeed() { return 0.9F; }

        @Override
        public float durabilityModifier() { return 2.2F; }

        @Override
        public List<Integer> repairParts() { return List.of(1, 2); }

        // Actions
        @Override
        public int areaDepth() { return 3; }

        @Override
        public void addExtraToolRules(ItemStack stack, Map<String, Number> stats, List<Tool.Rule> rules, float miningSpeed) {
                rules.add(Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_HOE, miningSpeed));
                rules.add(Tool.Rule.minesAndDrops(BlockTags.LEAVES, miningSpeed));
                rules.add(Tool.Rule.minesAndDrops(BlockTags.SWORD_EFFICIENT, miningSpeed));
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
                return ToolAreaActions.harvest(context);
        }

        @Override
        public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
                if (!(target instanceof IShearable)) return InteractionResult.PASS;
                double width = (ToolAreaActions.width(stack) - 1) / 2.0;
                double height = (ToolAreaActions.height(stack) - 1) / 2.0;
                AABB bounds = new AABB(target.position(), target.position().add(1, 1, 1)).inflate(width, height, width);
                boolean sheared = false;
                for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, bounds)) {
                        if (ToolState.broken(stack)) break;
                        if (!(entity instanceof IShearable shearable)
                                        || !shearable.isShearable(player, stack, player.level(), entity.blockPosition())) continue;
                        if (!player.level().isClientSide) {
                                for (ItemStack drop : shearable.onSheared(player, stack, player.level(), entity.blockPosition())) {
                                        shearable.spawnShearedDrop(player.level(), entity.blockPosition(), drop);
                                }
                                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
                        }
                        sheared = true;
                }
                return sheared ? InteractionResult.sidedSuccess(player.level().isClientSide) : InteractionResult.PASS;
        }
}
