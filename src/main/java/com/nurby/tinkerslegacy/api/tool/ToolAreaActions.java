package com.nurby.tinkerslegacy.api.tool;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.content.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.content.tools.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.IShearable;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;

@EventBusSubscriber(modid = TinkersLegacy.MODID)
public final class ToolAreaActions {
        private static final ThreadLocal<Boolean> EXTRA_BREAK = ThreadLocal.withInitial(() -> false);
        private static final ThreadLocal<Boolean> EXTRA_PATH = ThreadLocal.withInitial(() -> false);
        private static final List<TreeTask> TREES = new ArrayList<>();
        private static final int TREE_LIMIT = 4096;

        private ToolAreaActions() {
        }

        // Areas
        public static List<BlockPos> blocks(ServerPlayer player, BlockPos origin, int depth) {
                if (!(player.pick(player.blockInteractionRange(), 1, false) instanceof BlockHitResult hit)
                                || !hit.getBlockPos().equals(origin)) return List.of();
                return box(player.getMainHandItem(), hit, player.getDirection(), depth);
        }

        public static List<BlockPos> box(BlockPos origin, Direction face, int depth) {
                List<BlockPos> result = new ArrayList<>();
                for (int first = -1; first <= 1; first++) {
                        for (int second = -1; second <= 1; second++) {
                                for (int forward = 0; forward < depth; forward++) {
                                        BlockPos pos = switch (face.getAxis()) {
                                                case X -> origin.offset(-face.getStepX() * forward, first, second);
                                                case Y -> origin.offset(first, -face.getStepY() * forward, second);
                                                case Z -> origin.offset(first, second, -face.getStepZ() * forward);
                                        };
                                        if (!pos.equals(origin)) result.add(pos);
                                }
                        }
                }
                return List.copyOf(result);
        }

        public static int width(ItemStack stack) {
                return dimension(stack, true);
        }

        public static int height(ItemStack stack) {
                return dimension(stack, false);
        }

        private static int dimension(ItemStack stack, boolean width) {
                if (!(stack.getItem() instanceof DynamicTool item)) return 1;
                boolean horizontal = ToolModifiers.has(stack, ToolModifiers.HARVEST_WIDTH);
                boolean vertical = ToolModifiers.has(stack, ToolModifiers.HARVEST_HEIGHT);
            if (item.getDefinition() instanceof com.nurby.tinkerslegacy.content.tools.MattockDefinition) {
                        return 1 + (horizontal ? 1 : 0) + (vertical ? 1 : 0);
                }
                int base = item.getDefinition() instanceof AreaToolDefinition ? 3 : 1;
                return base + ((width ? horizontal : vertical) ? base == 3 ? 2 : 1 : 0);
        }

        public static List<BlockPos> box(ItemStack stack, BlockHitResult hit, Direction horizontalFacing, int depth) {
                BlockPos origin = hit.getBlockPos();
                Direction face = hit.getDirection();
                int width = width(stack);
                int height = height(stack);
                int x;
                int y;
                int z;
                BlockPos start;
                if (face.getAxis() == Direction.Axis.Y) {
                        x = horizontalFacing.getStepX() * height + horizontalFacing.getStepZ() * width;
                        y = -face.getStepY() * depth;
                        z = horizontalFacing.getStepX() * width + horizontalFacing.getStepZ() * height;
                        start = origin.offset(-x / 2, 0, -z / 2);
                        if (x % 2 == 0 && (x > 0 && hit.getLocation().x - origin.getX() > 0.5
                                        || x < 0 && hit.getLocation().x - origin.getX() < 0.5)) start = start.offset(Integer.signum(x), 0, 0);
                        if (z % 2 == 0 && (z > 0 && hit.getLocation().z - origin.getZ() > 0.5
                                        || z < 0 && hit.getLocation().z - origin.getZ() < 0.5)) start = start.offset(0, 0, Integer.signum(z));
                } else if (face.getAxis() == Direction.Axis.Z) {
                        x = width;
                        y = height;
                        z = -face.getStepZ() * depth;
                        start = origin.offset(-x / 2, -y / 2, 0);
                        if (x % 2 == 0 && hit.getLocation().x - origin.getX() > 0.5) start = start.east();
                        if (y % 2 == 0 && hit.getLocation().y - origin.getY() > 0.5) start = start.above();
                } else {
                        x = -face.getStepX() * depth;
                        y = height;
                        z = width;
                        start = origin.offset(0, -y / 2, -z / 2);
                        if (y % 2 == 0 && hit.getLocation().y - origin.getY() > 0.5) start = start.above();
                        if (z % 2 == 0 && hit.getLocation().z - origin.getZ() > 0.5) start = start.south();
                }
                boolean limited = stack.getItem() instanceof DynamicTool item && item.getDefinition() instanceof AreaToolDefinition
                                && (ToolModifiers.has(stack, ToolModifiers.HARVEST_WIDTH) || ToolModifiers.has(stack, ToolModifiers.HARVEST_HEIGHT));
                List<BlockPos> result = new ArrayList<>();
                for (int first = 0; first < Math.abs(x); first++) {
                        for (int second = 0; second < Math.abs(y); second++) {
                                for (int third = 0; third < Math.abs(z); third++) {
                                        BlockPos pos = start.offset(first * Integer.signum(x), second * Integer.signum(y), third * Integer.signum(z));
                                        if (!pos.equals(origin) && (!limited || pos.distManhattan(origin) <= 3)) result.add(pos);
                                }
                        }
                }
                return List.copyOf(result);
        }

        public static boolean effective(ItemStack stack, BlockState state) {
                Tool tool = stack.get(DataComponents.TOOL);
                return !state.isAir() && tool != null && tool.rules().stream().anyMatch(rule ->
                                rule.speed().isPresent() && rule.blocks().contains(state.getBlockHolder()));
        }

        @Nullable
        public static Plan prepare(ServerPlayer player, BlockPos pos, BlockState state) {
                ItemStack stack = player.getMainHandItem();
                if (EXTRA_BREAK.get() || player.isShiftKeyDown() || ToolState.broken(stack)
                                || !(stack.getItem() instanceof DynamicTool tool)
                                || !tool.getDefinition().isHarvestTool() || !effective(stack, state)) return null;
                boolean tree = tool.getDefinition() instanceof LumberaxeDefinition && detectTree(player.serverLevel(), pos);
                List<BlockPos> blocks = tree ? tree(player.serverLevel(), pos)
                                : tool.getDefinition() instanceof AreaToolDefinition area ? area.areaBlocks(player, pos, state)
                                : blocks(player, pos, 1);
                return blocks.isEmpty() ? null : new Plan(stack, pos.immutable(), state, blocks,
                                state.getDestroyProgress(player, player.level(), pos), tree);
        }

        // Successful Mining
        public static void finish(ServerPlayer player, @Nullable Plan plan, boolean removed) {
                if (!removed || plan == null || !ready(player, plan)) return;
                if (plan.tree()) {
                        TREES.add(new TreeTask(player, player.serverLevel(), plan, new ArrayDeque<>(plan.blocks())));
                        return;
                }
                for (BlockPos pos : plan.blocks()) {
                        if (!ready(player, plan)) break;
                        breakBlock(player, plan, pos);
                }
        }

        private static boolean ready(ServerPlayer player, Plan plan) {
                return player.isAlive() && !player.hasDisconnected() && player.getMainHandItem() == plan.stack()
                                && !ToolState.broken(plan.stack());
        }

        private static boolean breakBlock(ServerPlayer player, Plan plan, BlockPos pos) {
                ServerLevel level = player.serverLevel();
                if (!level.hasChunkAt(pos) || !level.mayInteract(player, pos)) return false;
                BlockState state = level.getBlockState(pos);
                if (!effective(plan.stack(), state) || state.getDestroySpeed(level, pos) < 0
                                || !player.hasCorrectToolForDrops(state)) return false;
                if (plan.tree() && !state.is(BlockTags.LOGS)) return false;
                float strength = state.getDestroyProgress(player, level, pos);
                if (strength <= 0 || !plan.tree() && plan.strength() / strength > 10) return false;
                EXTRA_BREAK.set(true);
                try {
                        // NeoForge cancellation, loot, wear, and the successful-removal XP hook all run normally.
                        return player.gameMode.destroyBlock(pos);
                } finally {
                        EXTRA_BREAK.set(false);
                }
        }

        @SubscribeEvent
        public static void chop(ServerTickEvent.Post event) {
                for (TreeTask task : List.copyOf(TREES)) {
                        if (task.player().serverLevel() != task.level() || !ready(task.player(), task.plan())
                                        || task.remaining().isEmpty()) {
                                TREES.remove(task);
                                continue;
                        }
                        // The original lumberaxe fells one log per tick instead of freezing the server for a large tree.
                        for (int attempts = 0; attempts < 64 && !task.remaining().isEmpty(); attempts++) {
                                if (breakBlock(task.player(), task.plan(), task.remaining().removeFirst())) break;
                        }
                }
        }

        // Trees
        public static boolean detectTree(ServerLevel level, BlockPos origin) {
                if (!loadedLog(level, origin)) return false;
                BlockPos top = origin;
                ArrayDeque<BlockPos> candidates = new ArrayDeque<>();
                Set<BlockPos> visited = new HashSet<>();
                candidates.add(origin);
                while (!candidates.isEmpty() && visited.size() < TREE_LIMIT) {
                        BlockPos candidate = candidates.removeFirst();
                        if (!visited.add(candidate) || !loadedLog(level, candidate)) continue;
                        if (candidate.getY() > top.getY()) top = candidate;
                        for (int x = -1; x <= 1; x++) {
                                for (int z = -1; z <= 1; z++) candidates.add(candidate.offset(x, 1, z));
                        }
                }
                int leaves = 0;
                for (BlockPos pos : BlockPos.betweenClosed(top.offset(-1, 0, -1), top.offset(1, 2, 1))) {
                        if (level.hasChunkAt(pos) && level.getBlockState(pos).is(BlockTags.LEAVES) && ++leaves >= 5) return true;
                }
                return false;
        }

        public static List<BlockPos> tree(ServerLevel level, BlockPos origin) {
                List<BlockPos> result = new ArrayList<>();
                ArrayDeque<BlockPos> candidates = new ArrayDeque<>();
                Set<BlockPos> visited = new HashSet<>();
                candidates.add(origin);
                while (!candidates.isEmpty() && visited.size() < TREE_LIMIT) {
                        BlockPos pos = candidates.removeFirst();
                        if (!visited.add(pos) || !loadedLog(level, pos)) continue;
                        if (!pos.equals(origin)) result.add(pos);
                        for (Direction direction : Direction.Plane.HORIZONTAL) candidates.add(pos.relative(direction));
                        for (int x = -1; x <= 1; x++) {
                                for (int z = -1; z <= 1; z++) candidates.add(pos.offset(x, 1, z));
                        }
                }
                return List.copyOf(result);
        }

        private static boolean loadedLog(ServerLevel level, BlockPos pos) {
                return level.hasChunkAt(pos) && level.getBlockState(pos).is(BlockTags.LOGS);
        }

        // Interactions
        public static boolean handlesUse(ToolDefinition definition) {
                return definition instanceof ShovelDefinition || definition instanceof MattockDefinition
                                || definition instanceof KamaDefinition || definition instanceof ScytheDefinition
                                || definition instanceof ExcavatorDefinition;
        }

        public static InteractionResult flatten(UseOnContext context) {
                return useArea(context, Items.DIAMOND_SHOVEL::useOn);
        }

        public static InteractionResult till(UseOnContext context) {
                return useArea(context, Items.DIAMOND_HOE::useOn);
        }

        public static InteractionResult useArea(UseOnContext context, Function<UseOnContext, InteractionResult> action) {
                ItemStack stack = context.getItemInHand();
                if (ToolState.broken(stack)) return InteractionResult.FAIL;
                if (!(context.getPlayer() instanceof ServerPlayer player)) return action.apply(context);
                ServerLevel level = player.serverLevel();
                BlockPos origin = context.getClickedPos();
                BlockState before = level.getBlockState(origin);
                BlockPos cane = before.is(Blocks.SUGAR_CANE) ? origin : null;
                if (cane != null) while (level.getBlockState(cane.above()).is(Blocks.SUGAR_CANE)) cane = cane.above();
                InteractionResult result = action.apply(context);
                BlockState after = level.getBlockState(origin);
                boolean transformed = !before.equals(after)
                                && (after.is(Blocks.FARMLAND) || after.is(Blocks.DIRT_PATH)
                                || before.getBlock() instanceof CropBlock crop && crop.isMaxAge(before)
                                && before.getBlock() == after.getBlock() && crop.getAge(after) == 0);
                boolean harvestedCane = cane != null && !level.getBlockState(cane).is(Blocks.SUGAR_CANE);
                boolean changed = result.consumesAction() && (transformed || harvestedCane);
                if (changed && !player.hasInfiniteMaterials()) ToolLeveling.award(stack, player, 1);
                if (!changed || EXTRA_PATH.get() || player.isShiftKeyDown()) return result;

                EXTRA_PATH.set(true);
                try {
                        BlockHitResult original = new BlockHitResult(context.getClickLocation(), context.getClickedFace(), origin, context.isInside());
                        for (BlockPos pos : box(stack, original, player.getDirection(), 1)) {
                                if (ToolState.broken(stack) || player.getItemInHand(context.getHand()) != stack) break;
                                if (!level.hasChunkAt(pos) || !level.mayInteract(player, pos)
                                                || !player.mayUseItemAt(pos, context.getClickedFace(), stack)) continue;
                                BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), context.getClickedFace(), pos, false);
                                BlockState candidate = level.getBlockState(pos);
                                UseOnContext extraContext = new UseOnContext(player, context.getHand(), hit);
                                if (after.is(Blocks.DIRT_PATH)) {
                                        if (candidate.getToolModifiedState(extraContext, ItemAbilities.SHOVEL_FLATTEN, true) == null) continue;
                                } else if (after.is(Blocks.FARMLAND)) {
                                        if (candidate.getToolModifiedState(extraContext, ItemAbilities.HOE_TILL, true) == null) continue;
                                } else if (!(candidate.getBlock() instanceof CropBlock crop && crop.isMaxAge(candidate))
                                                && !candidate.is(Blocks.SUGAR_CANE)) continue;
                                // Every additional cell passes normal interaction and block-tool modification events.
                                player.gameMode.useItemOn(player, level, stack, context.getHand(), hit);
                        }
                } finally {
                        EXTRA_PATH.set(false);
                }
                return result;
        }

        public static InteractionResult shearArea(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
                if (!(target instanceof IShearable)) return InteractionResult.PASS;
                Set<LivingEntity> targets = new LinkedHashSet<>();
                targets.add(target);
                double width = (width(stack) - 1) / 2.0;
                double height = (height(stack) - 1) / 2.0;
                if (width > 0 || height > 0) {
                        AABB bounds = new AABB(target.position(), target.position().add(1, 1, 1)).inflate(width, height, width);
                        targets.addAll(player.level().getEntitiesOfClass(LivingEntity.class, bounds));
                }
                boolean sheared = false;
                for (LivingEntity other : targets) {
                        if (ToolState.broken(stack)) break;
                        if (!(other instanceof IShearable shearable)
                                        || !shearable.isShearable(player, stack, player.level(), other.blockPosition())) continue;
                        if (!player.level().isClientSide) {
                                if (!player.level().mayInteract(player, other.blockPosition())) continue;
                                if (other != target && NeoForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(player, hand, other)).isCanceled()) continue;
                                for (ItemStack drop : shearable.onSheared(player, stack, player.level(), other.blockPosition())) {
                                        shearable.spawnShearedDrop(player.level(), other.blockPosition(), drop);
                                }
                                other.gameEvent(GameEvent.SHEAR, player);
                                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
                        }
                        sheared = true;
                }
                return sheared ? InteractionResult.sidedSuccess(player.level().isClientSide) : InteractionResult.PASS;
        }

        // Crops
        public static InteractionResult harvest(UseOnContext context) {
                if (!(context.getPlayer() instanceof ServerPlayer player)) {
                        return context.getLevel().isClientSide ? InteractionResult.SUCCESS : InteractionResult.PASS;
                }
                ItemStack stack = context.getItemInHand();
                List<BlockPos> positions = new ArrayList<>(box(stack, new BlockHitResult(context.getClickLocation(), context.getClickedFace(), context.getClickedPos(), context.isInside()), player.getDirection(), 3));
                positions.add(context.getClickedPos());
                positions.sort(Comparator.<BlockPos>comparingInt(BlockPos::getY).reversed());
                boolean harvested = false;
                for (BlockPos pos : positions) {
                        if (ToolState.broken(stack)) break;
                        ServerLevel level = player.serverLevel();
                        if (!level.hasChunkAt(pos) || !level.mayInteract(player, pos)
                                        || !player.mayUseItemAt(pos, context.getClickedFace(), stack)) continue;
                        BlockState state = level.getBlockState(pos);
                        BlockState replanted = null;
                        if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
                                replanted = crop.getStateForAge(0);
                        } else if (state.is(Blocks.NETHER_WART) && state.getValue(NetherWartBlock.AGE) == 3) {
                                replanted = state.setValue(NetherWartBlock.AGE, 0);
                        }
                        if (replanted != null) {
                                if (NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, player)).isCanceled()) continue;
                                List<ItemStack> drops = Block.getDrops(state, level, pos, level.getBlockEntity(pos), player, stack);
                                ItemStack seed = drops.stream().filter(drop -> drop.getItem() instanceof BlockItem item
                                                && item.getBlock() == state.getBlock()).findFirst().orElse(ItemStack.EMPTY);
                                if (seed.isEmpty() || !replanted.canSurvive(level, pos) || !level.setBlock(pos, replanted, 3)) continue;
                                seed.shrink(1);
                                for (ItemStack drop : drops) Block.popResource(level, pos, drop);
                                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(context.getHand()));
                                if (!player.hasInfiniteMaterials()) ToolLeveling.award(stack, player, 1);
                                harvested = true;
                        } else if (state.is(Blocks.SUGAR_CANE) && level.getBlockState(pos.below()).is(Blocks.SUGAR_CANE)) {
                                if (!NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, player)).isCanceled()
                                                && level.destroyBlock(pos, true, player)) {
                                        stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(context.getHand()));
                                        if (!player.hasInfiniteMaterials()) ToolLeveling.award(stack, player, 1);
                                        harvested = true;
                                }
                        }
                }
                return harvested ? InteractionResult.CONSUME : InteractionResult.PASS;
        }

        @SubscribeEvent
        public static void stop(ServerStoppedEvent event) {
                TREES.clear();
                EXTRA_BREAK.remove();
                EXTRA_PATH.remove();
        }

        public record Plan(ItemStack stack, BlockPos origin, BlockState state, List<BlockPos> blocks, float strength, boolean tree) {
        }

        private record TreeTask(ServerPlayer player, ServerLevel level, Plan plan, ArrayDeque<BlockPos> remaining) {
        }
}
