package com.nurby.tinkerslegacy.content.item.dynamic;

import com.nurby.tinkerslegacy.api.ClientHandler;
import com.nurby.tinkerslegacy.api.tool.ToolDefinition;
import com.nurby.tinkerslegacy.api.tool.ToolLeveling;
import com.nurby.tinkerslegacy.api.tool.ToolModifiers;
import com.nurby.tinkerslegacy.api.tool.ToolState;
import com.nurby.tinkerslegacy.api.tool.modifier.ModifierEffects;
import com.nurby.tinkerslegacy.api.tool.traits.ToolTraits;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.util.TinkersUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

public class DynamicTool extends Item {
        private static final Map<String, TagKey<Block>> HARVEST_ROLES = Map.of(
                        "harvest_level_axe", BlockTags.MINEABLE_WITH_AXE,
                        "harvest_level_shovel", BlockTags.MINEABLE_WITH_SHOVEL,
                        "harvest_level_pickaxe", BlockTags.MINEABLE_WITH_PICKAXE);
        private final DeferredHolder<ToolDefinition, ToolDefinition> definition;

        public DynamicTool(DeferredHolder<ToolDefinition, ToolDefinition> definition) {
                super(new Item.Properties().stacksTo(1).setNoRepair());
                this.definition = definition;
        }

        public ToolDefinition getDefinition() {
                return definition.value();
        }

        /**
          * Calculates and initializes all dynamic data stored on the tool stack.
          *
          * <p>This includes the tool's stats, durability, vanilla tool component,
          * and attribute modifiers.</p>
          */
        public void calculateStats(ItemStack stack) {
                var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
                // A singleplayer server also exists on the client; only its own thread may use this fallback.
                calculateStats(stack, server != null && server.isSameThread() ? server.registryAccess() : null);
        }

        public void calculateStats(ItemStack stack, @Nullable HolderLookup.Provider registries) {
            List<ResourceLocation> layers = stack.get(TLDataComponents.MATERIALS);

                if (layers == null) {
                        return;
                }

                ToolState.initialize(stack, false);
                Map<String, Number> base = TLDataComponents.normalizeStats(getDefinition().calculateStats(layers));
                stack.set(TLDataComponents.BASE_STATS, base);
                Map<String, Number> calculated = new LinkedHashMap<>(ToolTraits.finishStats(stack,
                                ToolModifiers.apply(getDefinition(), stack, base, ToolTraits.applyStats(stack, base))));
                calculated.computeIfPresent("mining_speed", (name, speed) ->
                                speed.doubleValue() * getDefinition().miningSpeedModifier());
                Map<String, Number> stats = TLDataComponents.normalizeStats(calculated);
                stack.set(TLDataComponents.MODIFIED_STATS, stats);

                initializeDurability(stack, stats);
                initializeToolComponent(stack, stats);
                initializeAttributeModifiers(stack, stats);
                ModifierEffects.applyProperties(stack);
                if (registries != null) {
                        ModifierEffects.applyEnchantments(stack, registries);
                        ToolTraits.applyEnchantments(stack, registries);
                }
                stack.set(TLDataComponents.TOOL_DATA_VERSION, ToolState.DATA_VERSION);
        }

        private void initializeDurability(
                        ItemStack stack,
                        Map<String, Number> stats
        ) {
                int durability = Math.max(1, stats.get("durability").intValue());
                int damage = ToolState.damage(stack);

                stack.set(
                                DataComponents.MAX_DAMAGE,
                                durability
                );

                // A reduced capacity cannot erase wear; an increased capacity cannot repair a broken tool.
                if (damage >= durability || ToolState.broken(stack)) {
                        stack.set(TLDataComponents.BROKEN, true);
                        stack.set(TLDataComponents.BROKEN_DAMAGE, damage);
                }

                stack.set(
                                DataComponents.DAMAGE,
                                Math.min(damage, durability - 1)
                );
        }

        private void initializeToolComponent(
                        ItemStack stack,
                        Map<String, Number> stats
        ) {
                List<Tool.Rule> rules = new ArrayList<>();
                float miningSpeed = stats.getOrDefault("mining_speed", 1.0f).floatValue();

                addDefaultToolRules(stack, stats, rules);
                getDefinition().addExtraToolRules(stack, stats, rules, miningSpeed);
                stack.set(
                                DataComponents.TOOL,
                                new Tool(
                                                rules,
                                                1.0f,
                                                1
                                )
                );
        }

        private void addDefaultToolRules(ItemStack stack, Map<String, Number> stats, List<Tool.Rule> rules) {
                if (stats.containsKey("harvest_level")) {
                        int level = stats.get("harvest_level").intValue();
                        if (level < 4) rules.add(Tool.Rule.deniesDrops(com.nurby.tinkerslegacy.registry.TLTags.Blocks.NEEDS_COBALT_TOOL));
                        if (level < 3) rules.add(Tool.Rule.deniesDrops(BlockTags.NEEDS_DIAMOND_TOOL));
                        if (level < 2) rules.add(Tool.Rule.deniesDrops(BlockTags.NEEDS_IRON_TOOL));
                        if (level < 1) rules.add(Tool.Rule.deniesDrops(BlockTags.NEEDS_STONE_TOOL));
                        return;
                }
                var denied = HARVEST_ROLES.entrySet().stream().filter(role -> stats.containsKey(role.getKey()))
                                .flatMap(role -> BuiltInRegistries.BLOCK.getOrCreateTag(role.getValue()).stream())
                                .distinct().filter(holder -> {
                                        BlockState state = holder.value().defaultBlockState();
                                        return harvestLevel(stats, state) < requiredLevel(state);
                                })
                                .sorted(java.util.Comparator.comparingInt(holder -> BuiltInRegistries.BLOCK.getId(holder.value())))
                                .toList();
                if (!denied.isEmpty()) {
                        // Direct sets compare structurally across previews and packets; compound NeoForge sets do not.
                        rules.add(new Tool.Rule(HolderSet.direct(denied), Optional.empty(), Optional.of(false)));
                }
        }

        private static boolean hasHarvestRoles(Map<String, Number> stats) {
                return HARVEST_ROLES.keySet().stream().anyMatch(stats::containsKey);
        }

        private static int harvestLevel(Map<String, Number> stats, BlockState state) {
                return HARVEST_ROLES.entrySet().stream().filter(role -> state.is(role.getValue()))
                                .mapToInt(role -> stats.getOrDefault(role.getKey(), -1).intValue()).max().orElse(-1);
        }

        private static int requiredLevel(BlockState state) {
                if (state.is(com.nurby.tinkerslegacy.registry.TLTags.Blocks.NEEDS_COBALT_TOOL)) return 4;
                if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) return 3;
                if (state.is(BlockTags.NEEDS_IRON_TOOL)) return 2;
                return state.is(BlockTags.NEEDS_STONE_TOOL) ? 1 : 0;
        }

        private void initializeAttributeModifiers(
                        ItemStack stack,
                        Map<String, Number> stats
        ) {
            if (getDefinition() instanceof com.nurby.tinkerslegacy.content.tools.ProjectileToolDefinition) {
                        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
                        return;
                }
                ItemAttributeModifiers attributes =
                                ItemAttributeModifiers.builder()
                                                .add(
                                                                Attributes.ATTACK_DAMAGE,
                                                                new AttributeModifier(
                                                                                Item.BASE_ATTACK_DAMAGE_ID,
                                                                                TinkersUtils.getActualDamage(stack) - 1, // To account for the player's base 1 attack damage.
                                                                                AttributeModifier.Operation.ADD_VALUE
                                                                ),
                                                                EquipmentSlotGroup.MAINHAND
                                                )
                                                .add(
                                                                Attributes.ATTACK_SPEED,
                                                                new AttributeModifier(
                                                                                Item.BASE_ATTACK_SPEED_ID,
                                                                                stats.get("attack_speed").doubleValue() - 4.0D,
                                                                                AttributeModifier.Operation.ADD_VALUE
                                                                ),
                                                                EquipmentSlotGroup.MAINHAND
                                                )
                                                .build();

                stack.set(
                                DataComponents.ATTRIBUTE_MODIFIERS,
                                attributes
                );
        }

        // Appearance
        @Override
        public boolean isFoil(ItemStack stack) {
                // Original tools only glow for an active effect, not their backing enchantments.
                return ToolTraits.has(stack, "shocking") && ToolTraits.data(stack).get("charge") >= 100;
        }

        // Name and Description
        @Override
        public Component getName(ItemStack stack) {
            List<ResourceLocation> layers =
                                stack.get(TLDataComponents.MATERIALS);

                return TinkersUtils.getToolName(
                                getDefinition(),
                                layers
                );
        }

        @Override
        public void appendHoverText(
                        ItemStack stack,
                        TooltipContext context,
                        List<Component> tooltipComponents,
                        TooltipFlag tooltipFlag
        ) {
                super.appendHoverText(
                                stack,
                                context,
                                tooltipComponents,
                                tooltipFlag
                );

                if (FMLEnvironment.dist.isClient()) {
                        ClientHandler.addToolTooltip(
                                        stack,
                                        context,
                                        tooltipComponents,
                                        tooltipFlag
                        );
                }
        }

        /*
          * Vanilla Item hooks.
          *
          * Most tool-specific behavior belongs to ToolDefinition rather than
          * DynamicTool. These overrides exist primarily to bridge Minecraft's
          * Item API to the tool definition.
          */

        @Override
        public InteractionResult useOn(UseOnContext context) {
                ItemStack stack = context.getItemInHand();
                if (ToolState.broken(stack)) return InteractionResult.FAIL;
                InteractionResult result = getDefinition().useOn(context);
                if (result == null) result = super.useOn(context);
                return result;
        }

        @Override
        public InteractionResultHolder<ItemStack> use(
                        Level level,
                        Player player,
                        InteractionHand hand
        ) {
                if (ToolState.broken(player.getItemInHand(hand))) return InteractionResultHolder.fail(player.getItemInHand(hand));
                return getDefinition().use(
                                level,
                                player,
                                hand
                );
        }

        @Override
        public void releaseUsing(
                        ItemStack stack,
                        Level level,
                        LivingEntity entity,
                        int timeCharged
        ) {
                if (ToolState.broken(stack)) return;
                getDefinition().releaseUsing(
                                stack,
                                level,
                                entity,
                                timeCharged
                );
        }

        @Override
        public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int timeLeft) {
            if (getDefinition() instanceof com.nurby.tinkerslegacy.content.tools.RangedToolDefinition ranged) {
                com.nurby.tinkerslegacy.api.tool.ToolRangedActions.tick(ranged, stack, level, entity, timeLeft);
                }
        }

        @Override
        public boolean useOnRelease(ItemStack stack) {
            return getDefinition() instanceof com.nurby.tinkerslegacy.content.tools.RangedToolDefinition ranged && ranged.kind() == com.nurby.tinkerslegacy.content.tools.RangedToolDefinition.Kind.CROSSBOW;
        }

        @Override
        public int getUseDuration(
                        ItemStack stack,
                        LivingEntity entity
        ) {
                return getDefinition().getUseDuration(
                                stack,
                                entity
                );
        }

        @Override
        public UseAnim getUseAnimation(ItemStack stack) {
                return getDefinition().getUseAnimation(stack);
        }

        @Override
        public boolean canPerformAction(
                        ItemStack stack,
                        ItemAbility itemAbility
        ) {
                if (ToolState.broken(stack)) return false;
                return getDefinition().canPerformAction(
                                stack,
                                itemAbility
                );
        }

        @Override
        public boolean hurtEnemy(
                        ItemStack stack,
                        LivingEntity target,
                        LivingEntity attacker
        ) {
            if (getDefinition() instanceof com.nurby.tinkerslegacy.content.tools.ProjectileToolDefinition)
                return false;
                if (ToolState.broken(stack)) return false;
                return getDefinition().hurtEnemy(
                                stack,
                                target,
                                attacker
                );
        }

        @Override
        public boolean canDisableShield(
                        ItemStack stack,
                        ItemStack shield,
                        LivingEntity entity,
                        LivingEntity attacker
        ) {
                if (ToolState.broken(stack)) return false;
                return getDefinition().canDisableShield(
                                stack,
                                shield,
                                entity,
                                attacker
                );
        }

        @Override
        public InteractionResult interactLivingEntity(
                        ItemStack stack,
                        Player player,
                        LivingEntity target,
                        InteractionHand hand
        ) {
                if (ToolState.broken(stack)) return InteractionResult.FAIL;
                return getDefinition().interactLivingEntity(
                                stack,
                                player,
                                target,
                                hand
                );
        }

        /*
          * Hooks containing DynamicTool-specific logic.
          */

        @Override
        public boolean mineBlock(
                        ItemStack stack,
                        Level level,
                        BlockState state,
                        BlockPos pos,
                        LivingEntity miningEntity
        ) {
            if (getDefinition() instanceof com.nurby.tinkerslegacy.content.tools.ProjectileToolDefinition)
                return false;
                if (ToolState.broken(stack)) return false;
                if (!getDefinition().damagesOnBlockBreak(
                                stack,
                                level,
                                state,
                                pos,
                                miningEntity
                )) {
                        return true;
                }

                if (!level.isClientSide) {
                        Tool tool = stack.get(DataComponents.TOOL);
                        boolean effective = tool != null && tool.rules().stream().anyMatch(rule ->
                                        rule.speed().isPresent() && rule.blocks().contains(state.getBlockHolder()));
                        stack.hurtAndBreak(effective ? 1 : 2, miningEntity, EquipmentSlot.MAINHAND);
                }

                return getDefinition().isHarvestTool();
        }

        @Override
        public <T extends LivingEntity> int damageItem(
                        ItemStack stack,
                        int amount,
                        @Nullable T entity,
                        Consumer<Item> onBroken
        ) {
                if (entity != null && (entity.level().isClientSide || entity.hasInfiniteMaterials())) return 0;
                ToolState.initialize(stack, entity != null);
                if (entity != null && entity.level() instanceof ServerLevel level && amount > 0) {
                        amount = EnchantmentHelper.processDurabilityChange(level, stack, amount);
                }
                ToolState.hurt(stack, amount, entity);
                return 0;
        }

        @Override
        public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
                // Non-player callbacks have no outgoing damage; use the category's minimum wear.
                hurtOnHit(stack, attacker, 0.0F);
        }

        public void hurtOnHit(ItemStack stack, LivingEntity attacker, float damage) {
                if (attacker.level().isClientSide || attacker.hasInfiniteMaterials() || ToolState.broken(stack)) return;
            if (getDefinition() instanceof com.nurby.tinkerslegacy.content.tools.ProjectileToolDefinition)
                return;
                float wear = Math.max(1.0F, damage / 10.0F);
                if (!getDefinition().isWeapon()) wear *= 2.0F;
                stack.hurtAndBreak((int) wear, attacker, EquipmentSlot.MAINHAND);
        }

        @Override
        public float getDestroySpeed(ItemStack stack, BlockState state) {
            if (getDefinition() instanceof com.nurby.tinkerslegacy.content.tools.ProjectileToolDefinition)
                return 1.0F;
                return ToolState.broken(stack) ? 0.0F : super.getDestroySpeed(stack, state);
        }

        @Override
        public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
                if (ToolState.broken(stack)) return false;
                Map<String, Number> stats = stack.getOrDefault(TLDataComponents.MODIFIED_STATS, Map.of());
                // Runtime tag checks immediately honor datapack reloads, before the next component refresh.
                if (hasHarvestRoles(stats)) return harvestLevel(stats, state) >= requiredLevel(state);
                return super.isCorrectToolForDrops(stack, state);
        }

        @Override
        public boolean isBarVisible(ItemStack stack) {
                return ToolState.broken(stack) || super.isBarVisible(stack);
        }

        @Override
        public int getBarWidth(ItemStack stack) {
                return ToolState.broken(stack) ? 0 : super.getBarWidth(stack);
        }

        @Override
        public int getBarColor(ItemStack stack) {
                return ToolState.broken(stack) ? 0xAA0000 : super.getBarColor(stack);
        }

        @Override
        public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean selected) {
                if (!level.isClientSide) {
                        boolean legacy = !stack.has(TLDataComponents.PROGRESS)
                                || stack.getOrDefault(TLDataComponents.TOOL_DATA_VERSION, 0) != ToolState.DATA_VERSION;
                        ToolState.initialize(stack, true);
                        if (legacy) calculateStats(stack, level.registryAccess());
                        else if (entity.tickCount % 20 == 0) {
                                Map<String, Number> stats = stack.getOrDefault(TLDataComponents.MODIFIED_STATS, Map.of());
                                if (hasHarvestRoles(stats)) initializeToolComponent(stack, stats);
                        }
                        if (legacy || entity.tickCount % 20 == 0) ToolLeveling.synchronize(stack);
                        ToolTraits.inventoryTick(stack, level, entity, selected);
                        ModifierEffects.inventoryTick(stack, level, entity, selected);
                }
                super.inventoryTick(stack, level, entity, slotId, selected);
        }
}
