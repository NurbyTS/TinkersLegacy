package com.nurby.tinkerslegacy.library.tool.traits;

import com.nurby.tinkerslegacy.registry.TLSounds;

import com.nurby.tinkerslegacy.event.ToolCombatAbilities;
import com.nurby.tinkerslegacy.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.library.tool.ToolAreaActions;
import com.nurby.tinkerslegacy.library.tool.ToolState;
import com.nurby.tinkerslegacy.library.tool.ToolVisualEffects;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.mixin.ServerPlayerGameModeAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import javax.annotation.Nullable;

record BuiltinToolTrait(String name) implements TraitDefinition {
        // Display
        @Override
        public int color() {
                return switch (name) {
                        case "alien", "squeaky" -> 0xFFFF55;
                        case "aquadynamic", "jagged" -> 0x55FFFF;
                        case "aridiculous" -> 0xAA0000;
                        case "autosmelt" -> 0xFF5500;
                        case "baconlicious" -> 0xFFAAAA;
                        case "cheap", "stonebound" -> 0x555555;
                        case "cheapskate" -> 0xAAAAAA;
                        case "coldblooded", "crumbling", "hellish" -> 0xFF0000;
                        case "crude1", "crude2" -> 0x424242;
                        case "duritos", "unnatural" -> 0xFF55FF;
                        case "ecological", "slimey_green", "slimey_blue", "splinters" -> 0x55FF55;
                        case "enderference" -> 0x00AAAA;
                        case "insatiable" -> 0xAA00AA;
                        case "lightweight" -> 0x00FF00;
                        case "magnetic1", "magnetic2" -> 0xDDDDDD;
                        case "momentum" -> 0x5555FF;
                        case "petramor", "tasty" -> 0xFF5555;
                        case "prickly", "spiky" -> 0x00AA00;
                        default -> 0xFFFFFF;
                };
        }

        // Stats
        @Override
        public int bonusSlots() {
                return name.equals("writable1") || name.equals("writable2") ? 1 : 0;
        }

        @Override
        public void applyStats(ItemStack stack, Map<String, Number> stats) {
                switch (name) {
                        case "fractured" -> add(stats, "attack_damage", 1.5);
                        case "lightweight" -> {
                                multiply(stats, "attack_speed", 1.1);
                                multiply(stats, "draw_speed", 1.1);
                        }
                        case "alien" -> {
                                ToolTraitData data = ToolTraits.data(stack);
                                add(stats, "durability", data.get("alien_durability"));
                                add(stats, "mining_speed", data.get("alien_speed"));
                                add(stats, "attack_damage", data.get("alien_attack"));
                        }
                        default -> { }
                }
        }

        private static void add(Map<String, Number> stats, String key, double value) {
                if (stats.containsKey(key)) stats.put(key, stats.get(key).doubleValue() + value);
        }

        private static void multiply(Map<String, Number> stats, String key, double value) {
                if (stats.containsKey(key)) stats.put(key, stats.get(key).doubleValue() * value);
        }

        // Durability
        @Override
        public int damage(ItemStack stack, int original, int amount, @Nullable LivingEntity entity) {
                if (original <= 0 || entity == null) return amount;
                switch (name) {
                        case "duritos" -> {
                                float roll = entity.getRandom().nextFloat();
                                if (roll < 0.1F) return amount + original;
                                if (roll < 0.5F) return Math.max(0, amount - original);
                        }
                        case "dense" -> {
                                double chance = Math.pow(0.75 * ToolState.damage(stack) / Math.max(1.0, stack.getMaxDamage()), 3);
                                if (chance > entity.getRandom().nextFloat()) return Math.max(0, amount - Math.max(original / 2, 1));
                        }
                        case "insatiable" -> { return amount + ToolTraitEffects.level(entity, ToolTraitEffects.INSATIABLE) / 3; }
                        default -> { }
                }
                return amount;
        }

        @Override
        public int heal(ItemStack stack, int original, int amount) {
                return name.equals("cheap") ? amount + original * 5 / 100 : amount;
        }

        // Damage
        @Override
        public float attackDamage(ItemStack stack, LivingEntity attacker, LivingEntity target, float original, float amount) {
                return switch (name) {
                        case "aridiculous" -> amount + 2 * ToolTraitEvents.aridiculousness(attacker.level(), attacker.blockPosition());
                        case "coldblooded" -> amount + (target.getHealth() == target.getMaxHealth() ? original / 2F : 0);
                        case "crude1" -> amount + (target.getArmorValue() == 0 ? original * 0.05F : 0);
                        case "crude2" -> amount + (target.getArmorValue() == 0 ? original * 0.1F : 0);
                        case "hellish" -> amount + (target.fireImmune() ? 0 : 4);
                        case "holy" -> amount + (target.getType().is(EntityTypeTags.UNDEAD) ? 5 : 0);
                        case "insatiable" -> amount + ToolTraitEffects.level(attacker, ToolTraitEffects.INSATIABLE) / 3F;
                        case "jagged" -> amount + (float) Math.log(ToolState.damage(stack) / 72.0 + 1) * 2;
                        case "splintering" -> amount + (target.hasEffect(ToolTraitEffects.SPLINTER)
                                        ? 0.3F * (ToolTraitEffects.level(target, ToolTraitEffects.SPLINTER) + 1) : 0);
                        case "superheat" -> amount + (target.isOnFire() ? original * 0.35F : 0);
                        default -> amount;
                };
        }

        @Override
        public void beforeHit(ItemStack stack, LivingEntity attacker, LivingEntity target) {
                switch (name) {
                        case "magnetic1", "magnetic2" -> ToolTraitEffects.apply(attacker, ToolTraitEffects.MAGNETIC, 30, ToolTraits.level(stack, "magnetic"));
                        case "shocking" -> {
                                if (ToolTraits.data(stack).get("charge") >= 100) {
                                        if (ToolTraits.shockDamage(target, attacker, 5)) {
                                                ToolVisualEffects.shocking(target);
                                                charge(stack, 0, attacker);
                                                attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 50, 5));
                                        }
                                } else charge(stack, ToolTraits.data(stack).get("charge") + 15 *
                                                (attacker instanceof Player player ? ToolCombatAbilities.attackStrength(player) : 1), attacker);
                        }
                        default -> { }
                }
        }

        @Override
        public void afterHit(ItemStack stack, LivingEntity attacker, LivingEntity target, float damage) {
                switch (name) {
                        case "holy" -> {
                                if (target.isAlive() && target.getType().is(EntityTypeTags.UNDEAD)) target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 50));
                        }
                        case "insatiable" -> ToolTraitEffects.apply(attacker, ToolTraitEffects.INSATIABLE, 100,
                                        Math.min(10, ToolTraitEffects.level(attacker, ToolTraitEffects.INSATIABLE) + 1));
                        case "poisonous" -> {
                                if (target.isAlive()) target.addEffect(new MobEffectInstance(MobEffects.POISON, 101));
                        }
                        case "prickly" -> ToolTraits.thornDamage(target, attacker,
                                        0.5F + Math.max(-0.5F, (float) attacker.getRandom().nextGaussian() * 0.75F));
                        case "sharp" -> {
                                if (target.isAlive()) {
                                        target.setLastHurtByMob(attacker);
                                        ToolTraitEffects.apply(target, ToolTraitEffects.BLEEDING, 121, 0);
                                }
                        }
                        case "splintering" -> ToolTraitEffects.apply(target, ToolTraitEffects.SPLINTER, 40,
                                        target.hasEffect(ToolTraitEffects.SPLINTER) ? Math.min(5, ToolTraitEffects.level(target, ToolTraitEffects.SPLINTER) + 1) : 0);
                        case "freezing" -> {
                                MobEffectInstance old = target.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
                                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, old == null ? 0 : Math.min(4, old.getAmplifier() + 1)));
                        }
                        case "enderference" -> {
                                if (target instanceof EnderMan) ToolTraitEffects.apply(target, ToolTraitEffects.ENDERFERENCE, 100, 1);
                        }
                        case "baconlicious" -> {
                                if (!target.isAlive()) bacon(target.level(), target.blockPosition(), 0.05F);
                        }
                        case "slimey_green", "slimey_blue" -> {
                                if (!target.isAlive()) slime(attacker, target.blockPosition());
                        }
                        case "splinters" -> splinter(attacker);
                        default -> { }
                }
        }

        // Mining
        @Override
        public void afterBlockBreak(ItemStack stack, ServerLevel level, BlockState state, BlockPos pos, LivingEntity entity) {
                switch (name) {
                        case "autosmelt" -> {
                                if (ToolAreaActions.effective(stack, state)) ToolVisualEffects.autosmelt(level, pos);
                        }
                        case "momentum" -> ToolTraitEffects.apply(entity, ToolTraitEffects.MOMENTUM,
                                        Math.max(1, (int) (300 / stack.getOrDefault(TLDataComponents.MODIFIED_STATS, Map.<String, Number>of())
                                                        .getOrDefault("mining_speed", 1).doubleValue())),
                                        Math.min(32, ToolTraitEffects.level(entity, ToolTraitEffects.MOMENTUM) + 1));
                        case "magnetic1", "magnetic2" -> ToolTraitEffects.apply(entity, ToolTraitEffects.MAGNETIC, 30, ToolTraits.level(stack, "magnetic"));
                        case "petramor" -> {
                                if (state.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK, ToolTraits.id("petramor_blocks"))) && level.random.nextFloat() < 0.1F) ToolTraits.passiveHeal(stack, 5);
                        }
                        case "baconlicious" -> bacon(level, pos, 0.005F);
                        case "slimey_green", "slimey_blue" -> {
                                if (ToolAreaActions.effective(stack, state)) slime(entity, pos);
                        }
                        case "shocking" -> {
                                charge(stack, ToolTraits.data(stack).get("charge") + 15, entity);
                                if (ToolTraits.data(stack).get("charge") >= 100) {
                                        charge(stack, 0, entity);
                                        entity.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 50, 2));
                                }
                        }
                        case "splinters" -> splinter(entity);
                        default -> { }
                }
        }

        // Inventory
        @Override
        public void inventoryTick(ItemStack stack, Level level, Entity entity, boolean selected) {
                if (!(entity instanceof LivingEntity living)) return;
                switch (name) {
                        case "ecological" -> {
                                if (level.random.nextInt(800) == 0 && living.getUseItem() != stack) ToolTraits.passiveHeal(stack, 1);
                        }
                        case "tasty" -> {
                                if (!selected || !(living instanceof Player player) || !player.getFoodData().needsFood()
                                                || ToolState.broken(stack) || stack.getMaxDamage() - ToolState.damage(stack) < 5) return;
                                float chance = player.getHealth() < player.getMaxHealth() ? 0.03F : 0.01F;
                                if (player.getFoodData().getFoodLevel() <= 10) chance += (10 - player.getFoodData().getFoodLevel()) * 0.0025F
                                                - player.getFoodData().getSaturationLevel() * 0.005F;
                                if (level.random.nextFloat() < chance) {
                                        player.getFoodData().eat(1, 0);
                                        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 0.8F, 1);
                                        stack.hurtAndBreak(5, player, EquipmentSlot.MAINHAND);
                                }
                        }
                        case "alien" -> alien(stack, living);
                        case "shocking" -> {
                                if (!selected || level.getGameTime() % 5 != 0 || living.getUseItem() == stack || ToolTraits.data(stack).get("charge") >= 100) return;
                                ToolTraitData data = ToolTraits.data(stack);
                                double distance = data.contains("charge_x") ? Math.sqrt(Math.pow(entity.getX() - data.get("charge_x"), 2)
                                                + Math.pow(entity.getY() - data.get("charge_y"), 2) + Math.pow(entity.getZ() - data.get("charge_z"), 2)) : 0;
                                if (distance >= 0.1 || !data.contains("charge_x")) {
                                        stack.set(TLDataComponents.TRAIT_DATA, data.with("charge_x", entity.getX()).with("charge_y", entity.getY()).with("charge_z", entity.getZ()));
                                        charge(stack, data.get("charge") + Math.min(5, distance) * 2, entity);
                                }
                        }
                        default -> { }
                }
        }

        private static void charge(ItemStack stack, double charge, Entity entity) {
                double previous = ToolTraits.data(stack).get("charge");
                stack.set(TLDataComponents.TRAIT_DATA, ToolTraits.data(stack).with("charge", Math.clamp(charge, 0, 100)));
                if (entity instanceof ServerPlayer player) {
                        if (previous < 100 && charge >= 100) {
                                player.playNotifySound(TLSounds.SHOCKING_CHARGED.get(), SoundSource.PLAYERS,
                                                0.8F, 0.8F + player.getRandom().nextFloat() * 0.2F);
                        } else if (previous > 0 && charge == 0) {
                                player.level().playSound(null, player.blockPosition(), TLSounds.SHOCKING_DISCHARGE.get(),
                                                SoundSource.PLAYERS, 1, 1);
                        }
                }
        }

        private static void alien(ItemStack stack, LivingEntity entity) {
                if (entity instanceof net.neoforged.neoforge.common.util.FakePlayer || entity.tickCount % 72 != 0 || entity.getUseItem() == stack) return;
                if (entity instanceof ServerPlayer player
                                && ((ServerPlayerGameModeAccessor) player.gameMode).tinkerslegacy$isDestroyingBlock()) return;
                ToolTraitData data = ToolTraits.data(stack);
                if (!data.contains("alien_pool_durability")) {
                        int[] pool = new int[3];
                        for (int point = 0; point < 800; point++) pool[entity.getRandom().nextInt(3)]++;
                        data = data.with("alien_pool_durability", pool[0]).with("alien_pool_speed", pool[1] * 0.007F).with("alien_pool_attack", pool[2] * 0.005F);
                }
                String stat = entity.tickCount % 216 == 0 ? "attack" : entity.tickCount % 144 == 0 ? "speed" : "durability";
                double step = stat.equals("attack") ? 0.005F : stat.equals("speed") ? 0.007F : 1;
                if (data.get("alien_" + stat) < data.get("alien_pool_" + stat)) data = data.with("alien_" + stat, data.get("alien_" + stat) + step);
                stack.set(TLDataComponents.TRAIT_DATA, data);
                if (stack.getItem() instanceof DynamicTool item) item.calculateStats(stack);
        }

        private static void bacon(Level level, BlockPos pos, float chance) {
                if (level.random.nextFloat() < chance && BuiltInRegistries.ITEM.containsKey(ToolTraits.id("bacon"))) {
                        Block.popResource(level, pos, new ItemStack(BuiltInRegistries.ITEM.get(ToolTraits.id("bacon"))));
                }
        }

        private void slime(LivingEntity owner, BlockPos pos) {
                if (owner.level().random.nextFloat() >= 0.0033F) return;
                EntityType<?> type = name.equals("slimey_blue") ? BuiltInRegistries.ENTITY_TYPE.get(ToolTraits.id("blue_slime")) : EntityType.SLIME;
                if (type != null && type.create(owner.level()) instanceof Slime slime) {
                        slime.setSize(1, true);
                        slime.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                        slime.setLastHurtByMob(owner);
                        owner.level().addFreshEntity(slime);
                }
        }

        private static void splinter(LivingEntity entity) {
                if (entity.getRandom().nextInt(150) == 0) ToolTraits.secondaryDamage(entity, null, 0.1F);
        }
}
