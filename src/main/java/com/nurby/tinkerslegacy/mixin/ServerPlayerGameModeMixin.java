package com.nurby.tinkerslegacy.mixin;

import com.nurby.tinkerslegacy.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.library.tool.ToolLeveling;
import com.nurby.tinkerslegacy.library.tool.ToolAreaActions;
import com.nurby.tinkerslegacy.library.tool.ToolState;
import com.nurby.tinkerslegacy.tools.ScytheDefinition;
import com.nurby.tinkerslegacy.tools.ExcavatorDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {

        @Shadow @Final protected ServerPlayer player;
        @Unique private ItemStack tinkerslegacy$miningTool = ItemStack.EMPTY;
        @Unique private boolean tinkerslegacy$eligibleBreak;
        @Unique private boolean tinkerslegacy$validToolBreak;
        @Unique private boolean tinkerslegacy$removedBlock;
        @Unique private ToolAreaActions.Plan tinkerslegacy$area;

        @Unique private ItemStack tinkerslegacy$actionTool = ItemStack.EMPTY;
        @Unique private BlockState tinkerslegacy$actionState;
        @Unique private BlockPos tinkerslegacy$caneTop;
        @Unique private boolean tinkerslegacy$eligibleAction;

        // Successful Actions

        @Inject(method = "useItemOn", at = @At("HEAD"))
        private void tinkerslegacy$beforeUse(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand,
                        BlockHitResult hit, CallbackInfoReturnable<InteractionResult> callback) {
                tinkerslegacy$actionTool = stack;
                tinkerslegacy$actionState = level.getBlockState(hit.getBlockPos());
                tinkerslegacy$eligibleAction = stack.getItem() instanceof DynamicTool tool
                                && !ToolAreaActions.handlesUse(tool.getDefinition())
                                && !player.hasInfiniteMaterials() && !ToolState.broken(stack);
                tinkerslegacy$caneTop = null;
                if (tinkerslegacy$eligibleAction && tinkerslegacy$actionState.is(Blocks.SUGAR_CANE)) {
                        BlockPos top = hit.getBlockPos();
                        while (level.getBlockState(top.above()).is(Blocks.SUGAR_CANE)) top = top.above();
                        tinkerslegacy$caneTop = top;
                }
        }

        @Inject(method = "useItemOn", at = @At("RETURN"))
        private void tinkerslegacy$afterUse(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand,
                        BlockHitResult hit, CallbackInfoReturnable<InteractionResult> callback) {
                if (tinkerslegacy$eligibleAction && callback.getReturnValue().consumesAction()) {
                        BlockState before = tinkerslegacy$actionState;
                        BlockState after = level.getBlockState(hit.getBlockPos());
                        boolean transformed = !before.equals(after)
                                        && (after.is(Blocks.FARMLAND) || after.is(Blocks.DIRT_PATH)
                                        || before.getBlock() instanceof CropBlock crop && crop.isMaxAge(before)
                                        && before.getBlock() == after.getBlock() && crop.getAge(after) == 0);
                        boolean harvestedCane = tinkerslegacy$caneTop != null
                                        && !level.getBlockState(tinkerslegacy$caneTop).is(Blocks.SUGAR_CANE);
                        if (transformed || harvestedCane) ToolLeveling.award(tinkerslegacy$actionTool, player, 1);
                }
                tinkerslegacy$actionTool = ItemStack.EMPTY;
                tinkerslegacy$actionState = null;
                tinkerslegacy$caneTop = null;
                tinkerslegacy$eligibleAction = false;
        }

        // Successful Mining

        @Inject(method = "destroyBlock", at = @At("HEAD"))
        private void tinkerslegacy$beforeBreak(BlockPos pos, CallbackInfoReturnable<Boolean> callback) {
                tinkerslegacy$miningTool = player.getMainHandItem();
                BlockState state = player.serverLevel().getBlockState(pos);
                tinkerslegacy$removedBlock = false;
                tinkerslegacy$area = ToolAreaActions.prepare(player, pos, state);
                tinkerslegacy$validToolBreak = tinkerslegacy$miningTool.getItem() instanceof DynamicTool
                                && !player.hasInfiniteMaterials() && !ToolState.broken(tinkerslegacy$miningTool);
                tinkerslegacy$eligibleBreak = tinkerslegacy$validToolBreak
                                && state.getDestroySpeed(player.level(), pos) > 0
                                && ToolAreaActions.effective(tinkerslegacy$miningTool, state);
        }

        @Inject(method = "removeBlock", at = @At("RETURN"))
        private void tinkerslegacy$afterRemoval(BlockPos pos, BlockState state, boolean canHarvest,
                        CallbackInfoReturnable<Boolean> callback) {
                tinkerslegacy$removedBlock |= callback.getReturnValueZ();
                if (callback.getReturnValueZ() && tinkerslegacy$validToolBreak) {
                        if (tinkerslegacy$eligibleBreak) ToolLeveling.award(tinkerslegacy$miningTool, player, 1);
                        com.nurby.tinkerslegacy.library.tool.traits.ToolTraits.afterBlockBreak(
                                        tinkerslegacy$miningTool, player.serverLevel(), state, pos, player);
                        com.nurby.tinkerslegacy.library.tool.modifier.ModifierEffects.afterBlockBreak(
                                        tinkerslegacy$miningTool, player.serverLevel(), pos);
                        tinkerslegacy$eligibleBreak = false;
                        tinkerslegacy$validToolBreak = false;
                }
        }

        @Inject(method = "destroyBlock", at = @At("RETURN"))
        private void tinkerslegacy$afterBreak(BlockPos pos, CallbackInfoReturnable<Boolean> callback) {
                ToolAreaActions.Plan area = tinkerslegacy$area;
                boolean removed = tinkerslegacy$removedBlock;
                tinkerslegacy$miningTool = ItemStack.EMPTY;
                tinkerslegacy$eligibleBreak = false;
                tinkerslegacy$removedBlock = false;
                tinkerslegacy$validToolBreak = false;
                tinkerslegacy$area = null;
                ToolAreaActions.finish(player, area, removed);
        }
}
