package com.nurby.tinkerslegacy.item.dynamic;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.library.ClientHandler;
import com.nurby.tinkerslegacy.library.material.layer.MaterialLayer;
import com.nurby.tinkerslegacy.library.tool.ToolDefinition;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.util.TinkersUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class DynamicTool extends Item {
    private final DeferredHolder<ToolDefinition, ToolDefinition> definition;

    public DynamicTool(DeferredHolder<ToolDefinition, ToolDefinition> definition) {
        super(new Item.Properties().stacksTo(1));
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
        List<MaterialLayer> layers = stack.get(TLDataComponents.MATERIALS);

        if (layers == null) {
            return;
        }

        Map<String, Number> stats = getDefinition().calculateStats(layers);

        stack.set(TLDataComponents.BASE_STATS, stats);

        // TODO: Apply modifier and trait changes to produce the modified stats.
        stack.set(TLDataComponents.MODIFIED_STATS, stats);

        initializeDurability(stack, stats);
        initializeToolComponent(stack, stats);
        initializeAttributeModifiers(stack, stats);
    }

    private void initializeDurability(
            ItemStack stack,
            Map<String, Number> stats
    ) {
        int durability = stats.get("durability").intValue();

        stack.set(
                DataComponents.MAX_DAMAGE,
                durability
        );

        Integer currentDamage = stack.get(DataComponents.DAMAGE);

        stack.set(
                DataComponents.DAMAGE,
                currentDamage == null
                        ? 0
                        : Math.min(currentDamage, durability)
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
        int harvestLevel = (int)stats.getOrDefault("harvest_level", -1);

        if (harvestLevel == -1) {
            // TODO: Make tool rules able to "merge" tags, so for example deny drops for NEEDS_IRON_TOOL and AXE_EFFECTIVE at the same time, for now, just average
            int h1 = (int)stats.getOrDefault("harvest_level_axe", -1);
            int h2 = (int)stats.getOrDefault("harvest_level_shovel", -1);
            int h3 = (int)stats.getOrDefault("harvest_level_pickaxe", -1);
            harvestLevel = Math.max(Math.max(h1, h2), h3);
        }

        if (harvestLevel < 3) {
            rules.add(
                    Tool.Rule.deniesDrops(
                            BlockTags.NEEDS_DIAMOND_TOOL
                    )
            );
        }

        if (harvestLevel < 2) {
            rules.add(
                    Tool.Rule.deniesDrops(
                            BlockTags.NEEDS_IRON_TOOL
                    )
            );
        }

        if (harvestLevel < 1) {
            rules.add(
                    Tool.Rule.deniesDrops(
                            BlockTags.NEEDS_STONE_TOOL
                    )
            );
        }
    }

    private void initializeAttributeModifiers(
            ItemStack stack,
            Map<String, Number> stats
    ) {
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

    // Name and Description
    @Override
    public Component getName(ItemStack stack) {
        List<MaterialLayer> layers =
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
        InteractionResult result =
                getDefinition().useOn(context);

        return result != null
                ? result
                : super.useOn(context);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
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
        getDefinition().releaseUsing(
                stack,
                level,
                entity,
                timeCharged
        );
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
        if (!getDefinition().damagesOnBlockBreak(
                stack,
                level,
                state,
                pos,
                miningEntity
        )) {
            return true;
        }

        return super.mineBlock(
                stack,
                level,
                state,
                pos,
                miningEntity
        );
    }

    @Override
    public <T extends LivingEntity> int damageItem(
            ItemStack stack,
            int amount,
            @Nullable T entity,
            Consumer<Item> onBroken
    ) {
        // TODO: Implement dynamic tool durability logic.
        return super.damageItem(
                stack,
                amount,
                entity,
                onBroken
        );
    }
}