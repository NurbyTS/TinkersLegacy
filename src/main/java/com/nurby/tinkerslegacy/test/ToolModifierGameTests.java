package com.nurby.tinkerslegacy.test;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.block.GlowBlock;
import com.nurby.tinkerslegacy.entity.TLToolProjectile;
import com.nurby.tinkerslegacy.registry.TLToolEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.functions.EnchantedCountIncreaseFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceWithEnchantedBonusCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import java.util.Optional;
import com.nurby.tinkerslegacy.event.ToolCombatAbilities;
import com.nurby.tinkerslegacy.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.library.tool.ToolAreaActions;
import com.nurby.tinkerslegacy.library.tool.ToolModifiers;
import com.nurby.tinkerslegacy.library.tool.ToolState;
import com.nurby.tinkerslegacy.library.tool.modifier.ModifierEffects;
import com.nurby.tinkerslegacy.library.tool.modifier.ToolModifierEvents;
import com.nurby.tinkerslegacy.library.tool.traits.ToolTraits;
import com.nurby.tinkerslegacy.registry.TLBlocks;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLTools;
import com.nurby.tinkerslegacy.registry.ToolParts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

import static com.nurby.tinkerslegacy.test.ToolAssemblyGameTests.id;
import static com.nurby.tinkerslegacy.test.ToolAssemblyGameTests.part;
import static com.nurby.tinkerslegacy.test.ToolAssemblyGameTests.tool;

@GameTestHolder(TinkersLegacy.MODID)
@PrefixGameTestTemplate(false)
public final class ToolModifierGameTests {
        private static ItemStack item(String name) {
                return new ItemStack(BuiltInRegistries.ITEM.get(id(name)));
        }

        private static ItemStack modified(TLTools.Type type, ResourceLocation modifier, int points) {
                ItemStack stack = tool(type, id("iron"));
                ToolModifiers.setPoints(stack, modifier, points);
                ((DynamicTool) stack.getItem()).calculateStats(stack);
                return stack;
        }

        // Recipes
        @GameTest(template = "empty")
        public static void combinationsRequireAllIngredientsAndCommitAtomically(GameTestHelper helper) {
                ItemStack tool = tool(TLTools.Type.PICKAXE, id("iron"));
                List<ItemStack> missing = List.of(new ItemStack(Items.TNT, 2));
                helper.assertTrue(ToolModifiers.modify(tool, missing).isEmpty(), "Blasting requires three TNT");
                helper.assertValueEqual(missing.getFirst().getCount(), 2, "Failed recipes consume nothing");
                List<ItemStack> inputs = List.of(new ItemStack(Items.TNT, 2), new ItemStack(Items.TNT, 1));
                ItemStack result = ToolModifiers.modify(tool, inputs);
                helper.assertValueEqual(ToolModifiers.level(result, ToolModifiers.BLASTING), 1, "TNT can span input slots");
                helper.assertTrue(inputs.stream().allMatch(ItemStack::isEmpty), "The complete bundle is consumed once");
                helper.assertTrue(tool.get(TLDataComponents.MODIFIERS).isEmpty(), "Input tool remains unchanged");
                List<ItemStack> unrelated = List.of(new ItemStack(Items.TNT, 3), new ItemStack(Items.DIRT));
                helper.assertTrue(ToolModifiers.modify(tool, unrelated).isEmpty(), "Unrelated inputs reject the entire transaction");
                helper.assertValueEqual(unrelated.getFirst().getCount(), 3, "Rejected transaction restores all ingredients");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void freeAndFirstLevelSlotsMatchOriginalRules(GameTestHelper helper) {
                ItemStack stack = tool(TLTools.Type.PICKAXE, id("iron"));
                stack.set(TLDataComponents.STARTING_MODIFIERS, 0);
                ItemStack soulbound = ToolModifiers.applyIngredient(stack, new ItemStack(Items.NETHER_STAR));
                helper.assertTrue(!soulbound.isEmpty(), "Soulbound is usable without a modifier slot");
                helper.assertValueEqual(ToolState.freeModifiers(soulbound), 0, "Soulbound does not create or consume slots");
                ItemStack creative = ToolModifiers.applyIngredient(soulbound, item("creative_modifier"));
                helper.assertValueEqual(ToolState.freeModifiers(creative), 1, "Creative modifier adds a permanent slot");
                for (int level = 1; level <= 3; level++) {
                        creative = ToolModifiers.modify(creative, List.of(new ItemStack(Items.TNT, 3)));
                        helper.assertTrue(!creative.isEmpty(), "Later blasting levels do not require additional slots");
                        helper.assertValueEqual(ToolModifiers.level(creative, ToolModifiers.BLASTING), level, "One new blasting level per craft");
                }
                helper.assertValueEqual(ToolState.freeModifiers(creative), 0, "All blasting levels together cost one slot");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void incrementalBlocksNeverOverfillAndLaterDustCanFit(GameTestHelper helper) {
                ItemStack stack = modified(TLTools.Type.PICKAXE, ToolModifiers.HASTE, 45);
                stack.set(TLDataComponents.STARTING_MODIFIERS, 1);
                List<ItemStack> ingredients = List.of(new ItemStack(Items.REDSTONE_BLOCK), new ItemStack(Items.REDSTONE, 8));
                ItemStack result = ToolModifiers.modify(stack, ingredients);
                helper.assertValueEqual(ToolModifiers.points(result, ToolModifiers.HASTE), 50, "Only the remaining five points fit");
                helper.assertValueEqual(ingredients.getFirst().getCount(), 1, "A nine-point block is never partly consumed");
                helper.assertValueEqual(ingredients.get(1).getCount(), 3, "Unused dust stays in the input");
                helper.assertTrue(ToolModifiers.applyIngredient(result, new ItemStack(Items.REDSTONE)).isEmpty(), "Starting another level needs another slot");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void luckUsesTriangularThresholdsAndRealEnchantments(GameTestHelper helper) {
                ItemStack stack = tool(TLTools.Type.HATCHET, id("iron"));
                stack.set(TLDataComponents.STARTING_MODIFIERS, 1);
                for (int boundary : new int[] {60, 180, 360}) {
                        int difference = boundary - ToolModifiers.points(stack, ToolModifiers.LUCK);
                        List<ItemStack> ingredients = List.of(new ItemStack(Items.LAPIS_LAZULI, difference));
                        stack = ToolModifiers.modify(stack, ingredients);
                        helper.assertTrue(!stack.isEmpty(), "Luck accepts the next original threshold with no further slots");
                        ModifierEffects.applyEnchantments(stack, helper.getLevel().registryAccess());
                        int level = boundary == 60 ? 1 : boundary == 180 ? 2 : 3;
                        var registry = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
                        helper.assertValueEqual(EnchantmentHelper.getItemEnchantmentLevel(registry.getHolderOrThrow(Enchantments.FORTUNE), stack), level, "Harvest Fortune level");
                        helper.assertValueEqual(EnchantmentHelper.getItemEnchantmentLevel(registry.getHolderOrThrow(Enchantments.LOOTING), stack), level, "Weapon Looting level");
                        helper.assertValueEqual(ToolState.freeModifiers(stack), 0, "Luck always costs one slot");
                }
                helper.assertTrue(ToolModifiers.applyIngredient(stack, new ItemStack(Items.LAPIS_LAZULI)).isEmpty(), "Luck stops at 360 points");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void fortuneSilkBlastingAndSqueakyAreMutuallyChecked(GameTestHelper helper) {
                ItemStack silk = ToolModifiers.applyIngredient(tool(TLTools.Type.PICKAXE, id("iron")), item("silky_jewel"));
                helper.assertTrue(ToolModifiers.applyIngredient(silk, new ItemStack(Items.LAPIS_LAZULI)).isEmpty(), "Silk rejects Luck");
                helper.assertTrue(ToolModifiers.modify(silk, List.of(new ItemStack(Items.TNT, 3))).isEmpty(), "Silk rejects Blasting");
                ItemStack luck = ToolModifiers.applyIngredient(tool(TLTools.Type.PICKAXE, id("iron")), new ItemStack(Items.LAPIS_LAZULI));
                helper.assertTrue(ToolModifiers.applyIngredient(luck, item("silky_jewel")).isEmpty(), "Partial Luck also rejects Silk");
                ItemStack sponge = tool(TLTools.Type.PICKAXE, id("sponge"));
                helper.assertTrue(ToolModifiers.applyIngredient(sponge, new ItemStack(Items.LAPIS_LAZULI)).isEmpty(), "Squeaky is a trait exclusion, not a purchased modifier");
                ItemStack firewood = tool(TLTools.Type.PICKAXE, id("firewood"));
                helper.assertTrue(ToolModifiers.modify(firewood, List.of(new ItemStack(Items.TNT, 3))).isEmpty(), "Autosmelt excludes Blasting");
                helper.assertTrue(ToolModifiers.applyIngredient(firewood, item("silky_jewel")).isEmpty(), "Autosmelt excludes Silk Touch");
                ToolModifiers.setPoints(firewood, ToolModifiers.SILKTOUCH, 1);
                helper.assertTrue(!ToolModifiers.compatible(firewood), "Replacement cannot add Autosmelt to a Silk Touch tool");
                firewood.set(TLDataComponents.MODIFIERS, List.of());
                firewood.set(TLDataComponents.EMBOSSMENT, sponge.get(TLDataComponents.MATERIALS));
                helper.assertTrue(!ToolModifiers.compatible(firewood), "Squeaky and Autosmelt cannot be combined by embossment");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void fortificationUnlocksActualCobaltTierWithoutSlots(GameTestHelper helper) {
                ItemStack stack = tool(TLTools.Type.PICKAXE, id("iron"));
                stack.set(TLDataComponents.STARTING_MODIFIERS, 0);
                List<ItemStack> ingredients = List.of(part(ToolParts.Type.SHARPENING_KIT, id("obsidian")), new ItemStack(Items.FLINT));
                ItemStack result = ToolModifiers.modify(stack, ingredients);
                helper.assertTrue(!result.isEmpty(), "Kit and flint fortify without slots");
                helper.assertValueEqual(result.get(TLDataComponents.MODIFIED_STATS).get("harvest_level").intValue(), 4, "Obsidian fortification reaches Cobalt level");
                helper.assertTrue(result.isCorrectToolForDrops(TLBlocks.block(TLBlocks.OreBlocks.Type.NETHER_COBALT).defaultBlockState()), "Modern Tool component allows Cobalt ore drops");
                helper.assertValueEqual(ToolState.freeModifiers(result), 0, "Fortify has no purchased slot cost");
                helper.assertTrue(ingredients.stream().allMatch(ItemStack::isEmpty), "One kit and one flint consumed");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void embossmentAddsOnlyRoleTraitsOnceWithoutReplacingMaterialStats(GameTestHelper helper) {
                ItemStack stack = tool(TLTools.Type.PICKAXE, id("iron"));
                var materials = stack.get(TLDataComponents.MATERIALS);
                var base = stack.get(TLDataComponents.BASE_STATS);
                List<ItemStack> ingredients = List.of(part(ToolParts.Type.BINDING, id("paper")), item("green_slime_crystal"),
                                item("blue_slime_crystal"), item("magma_slime_crystal"), new ItemStack(Items.GOLD_BLOCK));
                ItemStack result = ToolModifiers.modify(stack, ingredients);
                helper.assertTrue(!result.isEmpty(), "Matching part and the four support ingredients emboss the tool");
                helper.assertValueEqual(result.get(TLDataComponents.MATERIALS), materials, "Embossment does not replace physical materials");
                helper.assertValueEqual(result.get(TLDataComponents.BASE_STATS), base, "Paper does not replace Iron stats");
                helper.assertTrue(ToolTraits.has(result, "writable1"), "Paper binding adds its EXTRA role trait");
                helper.assertValueEqual(ToolState.freeModifiers(result), 4, "Embossment costs no slot and Writable contributes one");
                helper.assertTrue(ToolModifiers.modify(result, List.of(part(ToolParts.Type.BINDING, id("paper")), item("green_slime_crystal"),
                                item("blue_slime_crystal"), item("magma_slime_crystal"), new ItemStack(Items.GOLD_BLOCK))).isEmpty(), "Only one embossment is allowed");
                helper.succeed();
        }

        // Gameplay
        @GameTest(template = "empty")
        public static void silkTouchProducesTheOreBlockInSurvival(GameTestHelper helper) {
                var player = helper.makeMockServerPlayerInLevel();
                player.setGameMode(GameType.SURVIVAL);
                ItemStack stack = ToolModifiers.applyIngredient(tool(TLTools.Type.PICKAXE, id("iron")), item("silky_jewel"));
                ModifierEffects.applyEnchantments(stack, helper.getLevel().registryAccess());
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
                helper.getLevel().setBlockAndUpdate(pos, Blocks.COAL_ORE.defaultBlockState());
                helper.assertTrue(player.gameMode.destroyBlock(pos), "Survival Silk Touch mining removes ore");
                helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1)).stream()
                                .anyMatch(entity -> entity.getItem().is(Items.COAL_ORE)), "Actual block loot respects Silk Touch");
                helper.getLevel().getServer().getPlayerList().remove(player);
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void combatModifiersApplyOriginalDamageHealingAndStatusEffects(GameTestHelper helper) {
                var attacker = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
                var undead = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 1));
                var spider = helper.spawn(EntityType.SPIDER, new BlockPos(3, 1, 1));
                ItemStack stack = modified(TLTools.Type.BROADSWORD, ToolModifiers.SMITE, 24);
                helper.assertValueEqual(ModifierEffects.modifyDamage(stack, attacker, undead, 3), 10F, "Smite adds seven damage per completed level");
                helper.assertValueEqual(ModifierEffects.modifyDamage(stack, attacker, spider, 3), 3F, "Smite does not affect arthropods");
                ToolModifiers.setPoints(stack, ToolModifiers.BANE, 12);
                helper.assertValueEqual(ModifierEffects.modifyDamage(stack, attacker, spider, 3), 6.5F, "Bane partial progress scales damage");
                ToolModifiers.setPoints(stack, ToolModifiers.NECROTIC, 2);
                ToolModifiers.setPoints(stack, ToolModifiers.SHULKING, 50);
                ToolModifiers.setPoints(stack, ToolModifiers.WEBBED, 3);
                attacker.setHealth(10);
                ModifierEffects.beforeHit(stack, attacker, spider);
                ModifierEffects.afterHit(stack, attacker, spider, 6);
                helper.assertTrue(Math.abs(attacker.getHealth() - 11.2F) < 0.001F, "Necrotic heals a fifth of actual damage");
                helper.assertValueEqual(spider.getEffect(MobEffects.LEVITATION).getDuration(), 35, "Shulking original duration");
                helper.assertValueEqual(spider.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getDuration(), 60, "Webbed scales duration");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void reinforcedFivePreservesTheFinalDurabilityPoint(GameTestHelper helper) {
                ItemStack stack = modified(TLTools.Type.PICKAXE, ToolModifiers.REINFORCED, 5);
                stack.setDamageValue(stack.getMaxDamage() - 1);
                ToolState.hurt(stack, 10000);
                helper.assertTrue(!ToolState.broken(stack), "Unbreakable stops central durability loss before the broken threshold");
                helper.assertValueEqual(stack.getDamageValue(), stack.getMaxDamage() - 1, "Existing absolute wear is preserved");
                helper.assertValueEqual(ModifierEffects.adjustDamage(stack, 10000, null), 0, "Level five is deterministic damage immunity");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void mossStoresOriginalCapsAndHealingPreservesRepairCount(GameTestHelper helper) {
                ItemStack stack = modified(TLTools.Type.PICKAXE, ToolModifiers.MENDING_MOSS, 3);
                helper.assertValueEqual(ModifierEffects.storeMossXp(stack, 1000), 900, "Level three stores at most nine hundred XP");
                helper.assertValueEqual(ModifierEffects.storeMossXp(stack, 1), 0, "Full moss leaves XP for the player");
                stack.setDamageValue(20);
                stack.set(TLDataComponents.REPAIR_COUNT, 7);
                stack.set(TLDataComponents.MOSS_LAST_HEAL, helper.getLevel().getGameTime() - 151);
                var entity = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
                ModifierEffects.inventoryTick(stack, helper.getLevel(), entity, true);
                helper.assertValueEqual(ToolState.damage(stack), 15, "Level three converts one XP to five durability");
                helper.assertValueEqual(stack.get(TLDataComponents.MOSS_XP), 899, "One stored XP consumed");
                helper.assertValueEqual(stack.get(TLDataComponents.REPAIR_COUNT), 7, "Passive healing never increases repair fatigue");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void soulboundMovesTheSameToolThroughDeathAndClone(GameTestHelper helper) {
                var oldPlayer = helper.makeMockServerPlayerInLevel();
                var newPlayer = helper.makeMockServerPlayerInLevel();
                ItemStack stack = modified(TLTools.Type.PICKAXE, ToolModifiers.SOULBOUND, 1);
                ToolState.initialize(stack, true);
                var identity = stack.get(TLDataComponents.TOOL_ID);
                List<ItemEntity> drops = new ArrayList<>();
                drops.add(new ItemEntity(helper.getLevel(), 0, 0, 0, stack));
                ToolModifierEvents.death(new LivingDropsEvent(oldPlayer, oldPlayer.damageSources().generic(), drops, false));
                helper.assertTrue(drops.isEmpty(), "Soulbound removes the world drop");
                ToolModifierEvents.clone(new PlayerEvent.Clone(newPlayer, oldPlayer, true));
                helper.assertTrue(newPlayer.getInventory().items.stream().anyMatch(item -> identity.equals(item.get(TLDataComponents.TOOL_ID))), "The same identity transfers to the respawned player");
                helper.assertTrue(oldPlayer.getInventory().items.stream().noneMatch(item -> identity.equals(item.get(TLDataComponents.TOOL_ID))), "The corpse no longer retains a duplicate");
                helper.getLevel().getServer().getPlayerList().remove(oldPlayer);
                helper.getLevel().getServer().getPlayerList().remove(newPlayer);
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void cleaverAndPurchasedBeheadingShareOneTenLevelCap(GameTestHelper helper) {
                ItemStack stack = modified(TLTools.Type.CLEAVER, ToolModifiers.BEHEADING, 8);
                var player = helper.makeMockServerPlayerInLevel();
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
                List<ItemEntity> drops = new ArrayList<>();
                ToolCombatAbilities.behead(new LivingDropsEvent(zombie, zombie.damageSources().playerAttack(player), drops, true));
                helper.assertTrue(drops.stream().anyMatch(entity -> entity.getItem().is(Items.ZOMBIE_HEAD)), "Eight purchased levels plus two intrinsic levels guarantee a head");
                helper.assertTrue(ToolModifiers.modify(stack, List.of(new ItemStack(Items.ENDER_PEARL), new ItemStack(Items.OBSIDIAN))).isEmpty(), "Cleaver cannot exceed ten total Beheading levels");
                helper.getLevel().getServer().getPlayerList().remove(player);
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void expandersUseClassicBasicMattockAndAdvancedShapes(GameTestHelper helper) {
                ItemStack pickaxe = modified(TLTools.Type.PICKAXE, ToolModifiers.HARVEST_WIDTH, 1);
                helper.assertValueEqual(ToolAreaActions.width(pickaxe), 2, "Basic horizontal expander adds one column");
                helper.assertValueEqual(ToolAreaActions.height(pickaxe), 1, "Basic width does not add height");
                ItemStack mattock = modified(TLTools.Type.MATTOCK, ToolModifiers.HARVEST_WIDTH, 1);
                helper.assertValueEqual(ToolAreaActions.width(mattock), 2, "Mattock adds both dimensions");
                helper.assertValueEqual(ToolAreaActions.height(mattock), 2, "Mattock adds both dimensions");
                ToolModifiers.setPoints(mattock, ToolModifiers.HARVEST_HEIGHT, 1);
                helper.assertValueEqual(ToolAreaActions.width(mattock), 3, "Both mattock expanders give three by three");
                ItemStack hammer = modified(TLTools.Type.HAMMER, ToolModifiers.HARVEST_WIDTH, 1);
                helper.assertValueEqual(ToolAreaActions.width(hammer), 5, "Advanced width adds two columns");
                BlockPos origin = BlockPos.ZERO;
                List<BlockPos> left = ToolAreaActions.box(pickaxe, new BlockHitResult(new Vec3(0.25, 0.5, 0), Direction.NORTH, origin, false), Direction.NORTH, 1);
                List<BlockPos> right = ToolAreaActions.box(pickaxe, new BlockHitResult(new Vec3(0.75, 0.5, 0), Direction.NORTH, origin, false), Direction.NORTH, 1);
                helper.assertTrue(left.contains(origin.west()) && right.contains(origin.east()), "Even-width expansion follows the half of the targeted face");
                helper.succeed();
        }
        @GameTest(template = "empty")
        public static void projectileLuckControlsBothLootCountsAndRareDropChance(GameTestHelper helper) {
                var player = helper.makeMockServerPlayerInLevel();
                ItemStack ammo = modified(TLTools.Type.ARROW, ToolModifiers.LUCK, 360);
                var projectile = new TLToolProjectile(helper.getLevel(), player, ammo, ItemStack.EMPTY, 3, false);
                var target = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
                var params = new LootParams.Builder(helper.getLevel())
                                .withParameter(LootContextParams.THIS_ENTITY, target)
                                .withParameter(LootContextParams.ORIGIN, target.position())
                                .withParameter(LootContextParams.DAMAGE_SOURCE, target.damageSources().arrow(projectile, player))
                                .withParameter(LootContextParams.ATTACKING_ENTITY, player)
                                .withParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, projectile)
                                .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, player)
                                .create(LootContextParamSets.ENTITY);
                var context = new LootContext.Builder(params).create(Optional.empty());
                var function = EnchantedCountIncreaseFunction.lootingMultiplier(helper.getLevel().registryAccess(), ConstantValue.exactly(1)).build();
                ItemStack drop = function.apply(new ItemStack(Items.ROTTEN_FLESH), context);
                helper.assertValueEqual(drop.getCount(), 4, "Ammo Luck III applies even when the player's hand is empty");
                var chance = LootItemRandomChanceWithEnchantedBonusCondition.randomChanceAndLootingBoost(helper.getLevel().registryAccess(), 0, 1).build();
                helper.assertTrue(chance.test(context), "Ammo Luck also affects original rare-drop looting conditions");
                helper.getLevel().getServer().getPlayerList().remove(player);
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void mergedOrbsRetainTheValueOfUncollectedPickups(GameTestHelper helper) {
                var player = helper.makeMockServerPlayerInLevel();
                ItemStack moss = modified(TLTools.Type.PICKAXE, ToolModifiers.MENDING_MOSS, 1);
                player.setItemInHand(InteractionHand.MAIN_HAND, moss);
                var orb = new ExperienceOrb(helper.getLevel(), player.getX(), player.getY(), player.getZ(), 7);
                CompoundTag saved = orb.saveWithoutId(new CompoundTag());
                saved.putInt("Count", 2);
                orb.readAdditionalSaveData(saved);
                player.takeXpDelay = 0;
                orb.playerTouch(player);
                helper.assertValueEqual(moss.get(TLDataComponents.MOSS_XP), 7, "First merged pickup stores its seven XP");
                helper.assertValueEqual(orb.value, 7, "Remaining pickup keeps its original value");
                player.takeXpDelay = 0;
                orb.playerTouch(player);
                helper.assertValueEqual(moss.get(TLDataComponents.MOSS_XP), 14, "Second pickup is not lost to the first absorption");
                helper.assertTrue(orb.isRemoved(), "The orb disappears only after both pickups");
                helper.getLevel().getServer().getPlayerList().remove(player);
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void mossAwakeningUsesBookshelfPowerAndTenLevels(GameTestHelper helper) {
                var player = helper.makeMockServerPlayerInLevel();
                player.setGameMode(GameType.SURVIVAL);
                player.experienceLevel = 10;
                player.setItemInHand(InteractionHand.MAIN_HAND, item("ball_of_moss"));
                BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
                helper.getLevel().setBlockAndUpdate(pos, Blocks.BOOKSHELF.defaultBlockState());
                var event = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos,
                                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
                ToolModifierEvents.awakenMoss(event);
                helper.assertTrue(event.isCanceled(), "Successful awakening consumes the block interaction");
                helper.assertValueEqual(player.experienceLevel, 0, "Original recipe spends ten XP levels");
                helper.assertTrue(player.getInventory().items.stream().anyMatch(stack -> stack.is(item("mending_moss").getItem())), "The result reaches the player inventory");
                helper.getLevel().getServer().getPlayerList().remove(player);
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void fieryAndFinsApplyOriginalFireAndWaterEffects(GameTestHelper helper) {
                var attacker = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
                var target = helper.spawn(EntityType.PIG, new BlockPos(2, 1, 1));
                ItemStack fiery = modified(TLTools.Type.BROADSWORD, ToolModifiers.FIERY, 30);
                float before = target.getHealth();
                ModifierEffects.beforeHit(fiery, attacker, target);
                helper.assertTrue(target.isOnFire(), "Fiery ignites before primary damage and loot");
                helper.assertValueEqual(before - target.getHealth(), 2F, "Thirty blaze powder adds two secondary fire damage");
                ItemStack fins = modified(TLTools.Type.ARROW, ToolModifiers.FINS, 1);
                var projectile = new TLToolProjectile(TLToolEntities.PROJECTILE.get(), helper.getLevel()) {
                        @Override
                        public boolean isInWater() { return true; }
                };
                projectile.setDeltaMovement(0.6, 0, 0);
                ModifierEffects.projectileMovement(fins, projectile);
                helper.assertTrue(Math.abs(projectile.getDeltaMovement().x - 0.992) < 0.00001, "Fins replaces water drag with eighty percent of normal slowdown");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void glowingLightRequiresSupportAndFallsAwayWithIt(GameTestHelper helper) {
                GlowBlock glow = (GlowBlock) BuiltInRegistries.BLOCK.get(id("glow"));
                BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
                helper.getLevel().setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
                helper.assertTrue(glow.addGlow(helper.getLevel(), pos), "Glowing places on a solid support");
                helper.assertValueEqual(helper.getLevel().getBlockState(pos).getLightEmission(), 14, "Original glow emits light level fourteen");
                helper.getLevel().setBlockAndUpdate(pos.below(), Blocks.AIR.defaultBlockState());
                helper.assertTrue(helper.getLevel().getBlockState(pos).isAir(), "Glow disappears when its support is removed");
                helper.succeed();
        }
        @GameTest(template = "empty")
        public static void ammoMatchingIgnoresPartialProgressButRetainsModifierLevels(GameTestHelper helper) {
                ItemStack fired = modified(TLTools.Type.ARROW, ToolModifiers.LUCK, 40);
                ItemStack advanced = fired.copy();
                ToolModifiers.setPoints(advanced, ToolModifiers.LUCK, 41);
                helper.assertTrue(ToolModifiers.sameModifiers(fired, advanced), "A natural Luck point does not strand fired arrows");
                ToolModifiers.setPoints(advanced, ToolModifiers.LUCK, 60);
                helper.assertTrue(ToolModifiers.sameModifiers(fired, advanced), "Completing an existing level keeps original modifier equality");
                ToolModifiers.setPoints(advanced, ToolModifiers.LUCK, 61);
                helper.assertTrue(!ToolModifiers.sameModifiers(fired, advanced), "Starting another modifier level changes the original ammo match");
                helper.succeed();
        }
}
