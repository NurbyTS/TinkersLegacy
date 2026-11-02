package com.nurby.tinkerslegacy.content.traits;

import com.nurby.tinkerslegacy.api.tool.traits.ToolTraitData;
import com.nurby.tinkerslegacy.api.tool.traits.ToolTraits;
import com.nurby.tinkerslegacy.api.trait.TraitDefinition;
import com.nurby.tinkerslegacy.content.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.mixin.ServerPlayerGameModeAccessor;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.Map;

public class AlienTrait implements TraitDefinition {

    private static final int TICK_PER_STAT = 72;
    private static final double DURABILITY_STEP = 1.0;
    private static final double SPEED_STEP = 0.007;
    private static final double ATTACK_STEP = 0.005;
    private static final int BASE_STAT_POINTS = 800;

    @Override
    public int color() {
        return 0xFFFF55; // Yellow
    }

    @Override
    public void applyStats(ItemStack stack, int level, int amount, Map<String, Number> stats) {
        ToolTraitData data = ToolTraits.data(stack);

        // Dynamically add the distributed stats on top of the base stats
        add(stats, "durability", data.get("alien_durability"));
        add(stats, "mining_speed", data.get("alien_speed"));
        add(stats, "attack_damage", data.get("alien_attack"));
    }

    @Override
    public void inventoryTick(ItemStack stack, int level, int amount, Level worldLevel, Entity entity, boolean selected) {
        if (worldLevel.isClientSide || entity instanceof FakePlayer || entity.tickCount % TICK_PER_STAT != 0) {
            return;
        }

        if (!(entity instanceof LivingEntity living) || living.getUseItem() == stack) {
            return;
        }

        // Prevent updating if the player is currently breaking a block
        if (entity instanceof ServerPlayer player && ((ServerPlayerGameModeAccessor) player.gameMode).tinkerslegacy$isDestroyingBlock()) {
            return;
        }

        ToolTraitData data = ToolTraits.data(stack);

        // 1. Lazily generate the potential stat pool (scales with trait level!)
        if (!data.contains("alien_pool_durability")) {
            int[] pool = new int[3];
            int totalPoints = BASE_STAT_POINTS * level;

            for (int point = 0; point < totalPoints; point++) {
                pool[entity.getRandom().nextInt(3)]++;
            }

            data = data.with("alien_pool_durability", pool[0] * DURABILITY_STEP).with("alien_pool_speed", pool[1] * SPEED_STEP).with("alien_pool_attack", pool[2] * ATTACK_STEP);
        }

        boolean updated = false;

        // 2. Distribute stats progressively based on modulo ticks
        if (entity.tickCount % (TICK_PER_STAT * 3) == 0) {
            if (data.get("alien_attack") < data.get("alien_pool_attack")) {
                data = data.with("alien_attack", data.get("alien_attack") + ATTACK_STEP);
                updated = true;
            }
        } else if (entity.tickCount % (TICK_PER_STAT * 2) == 0) {
            if (data.get("alien_speed") < data.get("alien_pool_speed")) {
                data = data.with("alien_speed", data.get("alien_speed") + SPEED_STEP);
                updated = true;
            }
        } else {
            if (data.get("alien_durability") < data.get("alien_pool_durability")) {
                data = data.with("alien_durability", data.get("alien_durability") + DURABILITY_STEP);
                updated = true;
            }
        }

        // 3. Save the new data component and force a stat recalculation so it applies immediately
        if (updated) {
            stack.set(TLDataComponents.TRAIT_DATA, data);

            if (stack.getItem() instanceof DynamicTool tool) {
                tool.calculateStats(stack);
            }
        }
    }

    private void add(Map<String, Number> stats, String key, double value) {
        if (value > 0 && stats.containsKey(key)) {
            stats.put(key, stats.get(key).doubleValue() + value);
        }
    }
}