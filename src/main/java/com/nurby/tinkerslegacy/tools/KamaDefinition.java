package com.nurby.tinkerslegacy.tools;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.library.material.layer.MaterialLayer;
import com.nurby.tinkerslegacy.library.material.stat.MaterialStat;
import com.nurby.tinkerslegacy.library.material.stat.MaterialStatMap;
import com.nurby.tinkerslegacy.library.tool.ToolDefinition;
import com.nurby.tinkerslegacy.library.tool.ToolAreaActions;
import com.nurby.tinkerslegacy.library.tool.ToolPart;
import com.nurby.tinkerslegacy.registry.ToolStatTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.IShearable;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class KamaDefinition extends ToolDefinition {

        public KamaDefinition() {
                super(
                                id("kama"),
                                List.of(
                                                new ToolPart(
                                                                id("kama_head"),
                                                                ToolStatTypes.HEAD.getId()
                                                ),
                                                new ToolPart(
                                                                id("tool_rod"),
                                                                ToolStatTypes.HANDLE.getId()
                                                ),
                                                new ToolPart(
                                                                id("binding"),
                                                                ToolStatTypes.EXTRA.getId()
                                                )
                                )
                );
        }

        private static ResourceLocation id(String path) {
                return ResourceLocation.fromNamespaceAndPath(
                                TinkersLegacy.MODID,
                                path
                );
        }

        @Override
        public boolean isWeapon() {
                return true;
        }

        @Override
        public float attackSpeed() {
                return 1.3f;
        }

        @Override
        public void addExtraToolRules(ItemStack stack, Map<String, Number> stats, List<Tool.Rule> rules, float miningSpeed) {
                rules.add(
                                Tool.Rule.minesAndDrops(
                                                BlockTags.MINEABLE_WITH_HOE,
                                                miningSpeed
                                )
                );
        }

        @Override
        public InteractionResult interactLivingEntity(
                        ItemStack stack,
                        Player player,
                        LivingEntity target,
                        InteractionHand hand
        ) {
                return ToolAreaActions.shearArea(stack, player, target, hand);
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
                return ToolAreaActions.useArea(context, this::useSingle);
        }

        private InteractionResult useSingle(UseOnContext context) {
                BlockPos pos = context.getClickedPos();
                Level level = context.getLevel();
                BlockState state = level.getBlockState(pos);

                if (state.getBlock() instanceof CropBlock crop
                                && crop.isMaxAge(state)) {
                        return harvestCrop(context, crop);
                }

                if (state.getBlock() instanceof SugarCaneBlock) {
                        return harvestSugarCane(context);
                }

                return Items.DIAMOND_HOE.useOn(context);
        }

        private InteractionResult harvestCrop(
                        UseOnContext context,
                        CropBlock crop
        ) {
                Level level = context.getLevel();
                BlockPos pos = context.getClickedPos();
                Player player = context.getPlayer();
                ItemStack stack = context.getItemInHand();
                BlockState state = level.getBlockState(pos);

                if (level.isClientSide) {
                        return InteractionResult.sidedSuccess(true);
                }

                if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, context.getClickedFace(), stack)
                                || NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, player)).isCanceled()) {
                        return InteractionResult.PASS;
                }

                List<ItemStack> drops =
                                Block.getDrops(
                                                state,
                                                (ServerLevel) level,
                                                pos,
                                                null,
                                                player,
                                                stack
                                );

                if (!level.setBlock(pos, crop.getStateForAge(0), 2)) {
                        return InteractionResult.PASS;
                }

                for (ItemStack drop : drops) {
                        Block.popResource(level, pos, drop);
                }

                level.levelEvent(
                                2001,
                                pos,
                                Block.getId(state)
                );

                playHarvestEffect(
                                (ServerLevel) level,
                                player,
                                pos
                );

                stack.hurtAndBreak(
                                1,
                                player,
                                LivingEntity.getSlotForHand(
                                                context.getHand()
                                )
                );

                return InteractionResult.sidedSuccess(false);
        }

        private InteractionResult harvestSugarCane(
                        UseOnContext context
        ) {
                Level level = context.getLevel();
                BlockPos clickedPos = context.getClickedPos();
                Player player = context.getPlayer();
                ItemStack stack = context.getItemInHand();

                if (level.isClientSide) {
                        return InteractionResult.sidedSuccess(true);
                }

                BlockPos basePos = clickedPos;

                while (
                                level.getBlockState(basePos.below())
                                                .is(Blocks.SUGAR_CANE)
                ) {
                        basePos = basePos.below();
                }

                BlockPos current = basePos;

                while (level.getBlockState(current.above()).is(Blocks.SUGAR_CANE)) {
                        current = current.above();
                }

                boolean harvested = false;

                // Harvest downwards so removing a lower section cannot drop the upper sections twice.
                while (current.getY() > basePos.getY()) {
                        BlockState state = level.getBlockState(current);

                        if (!level.mayInteract(player, current)
                                        || !player.mayUseItemAt(current, context.getClickedFace(), stack)
                                        || NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, current, state, player)).isCanceled()
                                        || !level.destroyBlock(current, true, player)) {
                                break;
                        }

                        harvested = true;
                        current = current.below();
                }

                if (!harvested) {
                        return InteractionResult.PASS;
                }

                playHarvestEffect(
                                (ServerLevel) level,
                                player,
                                basePos
                );

                stack.hurtAndBreak(
                                1,
                                player,
                                LivingEntity.getSlotForHand(
                                                context.getHand()
                                )
                );

                return InteractionResult.sidedSuccess(false);
        }

        private void playHarvestEffect(
                        ServerLevel level,
                        Player player,
                        BlockPos target
        ) {
                double dx = target.getX() + 0.5D - player.getX();
                double dz = target.getZ() + 0.5D - player.getZ();

                double length = Math.sqrt(dx * dx + dz * dz);

                if (length > 0.0D) {
                        dx /= length;
                        dz /= length;
                }

                double particleX = player.getX() + dx * 0.7D;
                double particleZ = player.getZ() + dz * 0.7D;

                level.sendParticles(
                                ParticleTypes.SWEEP_ATTACK,
                                particleX,
                                player.getY() + 1.0D,
                                particleZ,
                                1,
                                0.0D,
                                0.0D,
                                0.0D,
                                0.0D
                );

                level.playSound(
                                null,
                                player.getX(),
                                player.getY(),
                                player.getZ(),
                                SoundEvents.PLAYER_ATTACK_SWEEP,
                                SoundSource.PLAYERS,
                                1.0F,
                                1.0F
                );
        }
}