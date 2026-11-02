package com.nurby.tinkerslegacy.test;

import com.mojang.serialization.JsonOps;
import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.item.dynamic.DynamicTool;
import com.nurby.tinkerslegacy.library.material.layer.MaterialLayer;
import com.nurby.tinkerslegacy.library.tool.ToolState;
import com.nurby.tinkerslegacy.library.tool.traits.ToolTraitData;
import com.nurby.tinkerslegacy.library.tool.traits.ToolTraitEffects;
import com.nurby.tinkerslegacy.library.tool.traits.ToolTraitEvents;
import com.nurby.tinkerslegacy.library.tool.traits.ToolTraits;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLRegistries;
import com.nurby.tinkerslegacy.registry.TLTools;
import com.nurby.tinkerslegacy.registry.ToolStatTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.nurby.tinkerslegacy.test.ToolAssemblyGameTests.id;
import static com.nurby.tinkerslegacy.test.ToolAssemblyGameTests.tool;

@GameTestHolder(TinkersLegacy.MODID)
@PrefixGameTestTemplate(false)
public final class ToolTraitGameTests {
        // Material Roles
        @GameTest(template = "empty")
        public static void roleTraitsOverrideDefaultsAndEveryMaterialTraitHasBehavior(GameTestHelper helper) {
                helper.assertValueEqual(ToolTraits.materialTraits(id("stone"), ToolStatTypes.HEAD.getId()), List.of(id("cheapskate")), "Head trait overrides the material default");
                helper.assertValueEqual(ToolTraits.materialTraits(id("stone"), ToolStatTypes.HANDLE.getId()), List.of(id("cheap")), "Handle falls back to the default trait");
                for (var material : TLRegistries.MATERIALS) {
                        for (var traits : material.traits().values()) {
                                for (ResourceLocation trait : traits) helper.assertTrue(ToolTraits.definitions().containsKey(trait), "Registered trait behavior for " + trait);
                        }
                }
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void writableUsesDistinctRoleTraitsWithoutSpendingModifiers(GameTestHelper helper) {
                ItemStack paper = tool(TLTools.Type.PICKAXE, id("paper"));
                helper.assertValueEqual(ToolTraits.bonusSlots(paper), 2, "Writable1 and writable2 each contribute exactly one slot");
                helper.assertValueEqual(ToolState.freeModifiers(paper), 5, "Paper slots add to the three starting slots");
                helper.assertTrue(paper.get(TLDataComponents.MODIFIERS).isEmpty(), "Material traits are separate from purchased modifiers");
                paper.set(TLDataComponents.EMBOSSMENT, paper.get(TLDataComponents.MATERIALS));
                helper.assertValueEqual(ToolTraits.bonusSlots(paper), 2, "Repeated and embossed roles cannot duplicate a trait");
                ItemStack mixed = tool(TLTools.Type.PICKAXE, id("iron"));
                List<MaterialLayer> layers = new ArrayList<>(mixed.get(TLDataComponents.MATERIALS));
                layers.set(0, new MaterialLayer(ToolStatTypes.HEAD.getId(), id("paper")));
                mixed.set(TLDataComponents.MATERIALS, List.copyOf(layers));
                helper.assertValueEqual(ToolTraits.bonusSlots(mixed), 1, "Writable2 alone is one original bonus slot");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void projectileTraitsFollowPhysicalRolesWithoutChangingSavedLayers(GameTestHelper helper) {
                ItemStack arrow = tool(TLTools.Type.ARROW, id("endstone"));
                List<MaterialLayer> layers = arrow.get(TLDataComponents.MATERIALS);
                helper.assertTrue(ToolTraits.has(arrow, "alien") && ToolTraits.has(arrow, "enderference"),
                                "An endstone arrow head supplies both head and projectile traits");
                helper.assertValueEqual(layers.size(), 3, "Virtual projectile roles do not add saved material layers");
                ItemStack ironArrow = tool(TLTools.Type.ARROW, id("iron"));
                helper.assertValueEqual(ToolTraits.level(ironArrow, "magnetic"), 3, "Arrow heads combine magnetic head and projectile roles");
                ItemStack bolt = tool(TLTools.Type.BOLT, id("iron"));
                helper.assertValueEqual(ToolTraits.level(bolt, "magnetic"), 2, "The original bolt coating supplies only head traits");
                ironArrow.set(TLDataComponents.EMBOSSMENT, List.of(new MaterialLayer(ToolStatTypes.HEAD.getId(), id("endstone"))));
                helper.assertTrue(ToolTraits.has(ironArrow, "enderference"), "Embossed arrow heads include the projectile role");
                helper.assertValueEqual(arrow.get(TLDataComponents.MATERIALS), layers, "Trait lookup leaves rendering and saved materials unchanged");
                helper.succeed();
        }

        // Stats and State
        @GameTest(template = "empty")
        public static void traitRecalculationNeverCompoundsOrErasesProgress(GameTestHelper helper) {
                ItemStack bone = tool(TLTools.Type.PICKAXE, id("bone"));
                var item = (DynamicTool) bone.getItem();
                Map<String, Number> first = bone.get(TLDataComponents.MODIFIED_STATS);
                bone.setDamageValue(5);
                bone.set(TLDataComponents.TRAIT_DATA, ToolTraitData.EMPTY.with("charge", 37));
                for (int pass = 0; pass < 5; pass++) item.calculateStats(bone);
                helper.assertValueEqual(bone.get(TLDataComponents.MODIFIED_STATS), first, "Fractured only applies once to fresh base stats");
                helper.assertValueEqual(ToolState.damage(bone), 5, "Rebuilding preserves absolute wear");
                helper.assertValueEqual(ToolTraits.data(bone).get("charge"), 37.0, "Rebuilding preserves saved trait state");
                ItemStack stone = tool(TLTools.Type.PICKAXE, id("stone"));
                int base = stone.get(TLDataComponents.BASE_STATS).get("durability").intValue();
                helper.assertValueEqual(stone.getMaxDamage(), Math.max(1, base * 80 / 100), "Cheapskate applies its final 20 percent penalty");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void traitDataIsImmutableAndRoundTrips(GameTestHelper helper) {
                ToolTraitData empty = ToolTraitData.EMPTY;
                ToolTraitData progress = empty.with("alien_durability", 73).with("alien_speed", 0.7).with("charge", 87);
                var json = ToolTraitData.CODEC.encodeStart(JsonOps.INSTANCE, progress).getOrThrow();
                helper.assertValueEqual(ToolTraitData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(), progress, "Persistent trait data round trip");
                helper.assertTrue(empty.values().isEmpty(), "Updates do not mutate an existing component instance");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void passiveRepairsPreserveRepairCountAndCheapAddsFivePercent(GameTestHelper helper) {
                ItemStack stack = tool(TLTools.Type.PICKAXE, id("iron"));
                stack.setDamageValue(20);
                stack.set(TLDataComponents.REPAIR_COUNT, 7);
                ToolTraits.passiveHeal(stack, 5);
                helper.assertValueEqual(ToolState.damage(stack), 15, "Passive healing repairs actual damage");
                helper.assertValueEqual(stack.get(TLDataComponents.REPAIR_COUNT), 7, "Passive healing does not create repair fatigue");
                ItemStack stone = tool(TLTools.Type.PICKAXE, id("stone"));
                helper.assertValueEqual(ToolTraits.heal(stone, 100), 105, "Cheap repairs add the original five percent");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void alienWaitsForAnActiveMiningAttemptToFinish(GameTestHelper helper) {
                var player = helper.makeMockServerPlayerInLevel();
                player.setGameMode(GameType.SURVIVAL);
                BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
                player.moveTo(pos.getX() + 2, pos.getY(), pos.getZ() + 2, 0, 0);
                ItemStack stack = tool(TLTools.Type.PICKAXE, id("endstone"));
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
                helper.getLevel().setBlockAndUpdate(pos, Blocks.OBSIDIAN.defaultBlockState());
                player.tickCount = 72;
                try {
                        player.gameMode.handleBlockBreakAction(pos,
                                        net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                                        net.minecraft.core.Direction.UP, helper.getLevel().getMaxBuildHeight(), 0);
                        helper.assertTrue(((com.nurby.tinkerslegacy.mixin.ServerPlayerGameModeAccessor) player.gameMode)
                                        .tinkerslegacy$isDestroyingBlock(), "A slow block starts an actual server mining attempt");
                        ToolTraits.inventoryTick(stack, helper.getLevel(), player, true);
                        helper.assertTrue(!ToolTraits.data(stack).contains("alien_pool_durability"),
                                        "Alien leaves components untouched while the player mines");
                        player.gameMode.handleBlockBreakAction(pos,
                                        net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,
                                        net.minecraft.core.Direction.UP, helper.getLevel().getMaxBuildHeight(), 1);
                        ToolTraits.inventoryTick(stack, helper.getLevel(), player, true);
                        helper.assertTrue(ToolTraits.data(stack).contains("alien_pool_durability"),
                                        "Alien resumes its ordinary stat progression after mining stops");
                } finally {
                        helper.getLevel().getServer().getPlayerList().remove(player);
                }
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void secondaryTraitDamagePreservesArmorAndProjectileAttribution(GameTestHelper helper) {
                var player = helper.makeMockPlayer(GameType.SURVIVAL);
                var target = helper.spawnWithNoFreeWill(EntityType.HUSK, 1, 1, 1);
                target.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
                ItemStack tool = tool(TLTools.Type.PICKAXE, id("electrum"));
                tool.set(TLDataComponents.TRAIT_DATA, ToolTraitData.EMPTY.with("charge", 100));
                var projectile = new com.nurby.tinkerslegacy.entity.TLToolProjectile(
                                com.nurby.tinkerslegacy.registry.TLToolEntities.PROJECTILE.get(), helper.getLevel());
                projectile.setOwner(player);
                float health = target.getHealth();
                ToolTraits.beforeHit(tool, player, target, projectile);
                helper.assertTrue(target.getHealth() < health && target.getHealth() > health - 5,
                                "Armor reduces the original five-point Shocking discharge");
                helper.assertTrue(target.getLastDamageSource().getDirectEntity() == projectile,
                                "Immediate secondary damage retains its projectile for tool identity tracking");
                helper.assertValueEqual(target.invulnerableTime, 0, "A successful discharge permits the main hit to follow");
                helper.assertTrue(!ToolTraits.secondary(), "The nested-damage guard is released after the callback");
                health = target.getHealth();
                ToolTraits.bleedDamage(target, player, 3);
                helper.assertTrue(target.getHealth() > health - 3, "Bleeding respects armor too");
                helper.assertTrue(target.getLastDamageSource().getEntity() == player
                                && target.getLastDamageSource().getDirectEntity() == null,
                                "Delayed bleeding retains kill attribution without granting XP to a currently held tool");
                health = target.getHealth();
                ToolTraits.secondaryDamage(target, player, 2);
                helper.assertValueEqual(health - target.getHealth(), 2F, "Prickly-style secondary damage bypasses armor");
                helper.assertTrue(target.getLastDamageSource().getDirectEntity() == player,
                                "Projectile context never leaks into a later unrelated action");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void unnaturalUsesOnlyTheEffectiveHeadRole(GameTestHelper helper) {
                var player = helper.makeMockPlayer(GameType.SURVIVAL);
                ItemStack axe = tool(TLTools.Type.HATCHET, id("knightslime"));
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, axe);
                var event = new net.neoforged.neoforge.event.entity.player.PlayerEvent.BreakSpeed(
                                player, Blocks.STONE.defaultBlockState(), 1, helper.absolutePos(BlockPos.ZERO));
                ToolTraitEvents.miningSpeed(event);
                helper.assertValueEqual(event.getNewSpeed(), 1F, "An axe receives no Unnatural bonus against pickaxe blocks");
                ItemStack mattock = tool(TLTools.Type.MATTOCK, id("knightslime"));
                mattock.set(TLDataComponents.EFFECT_TRAITS, List.of(id("unnatural")));
                var stats = new java.util.LinkedHashMap<>(mattock.get(TLDataComponents.MODIFIED_STATS));
                stats.put("harvest_level_axe", 4);
                stats.put("harvest_level_shovel", 1);
                mattock.set(TLDataComponents.MODIFIED_STATS, Map.copyOf(stats));
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, mattock);
                event = new net.neoforged.neoforge.event.entity.player.PlayerEvent.BreakSpeed(
                                player, Blocks.DIRT.defaultBlockState(), 1, helper.absolutePos(BlockPos.ZERO));
                ToolTraitEvents.miningSpeed(event);
                helper.assertValueEqual(event.getNewSpeed(), 2F, "Dirt uses the shovel head's level, not the stronger axe head");
                helper.succeed();
        }

        // Actions
        @GameTest(template = "empty")
        public static void squeakySuppressesDamageAndColdbloodedOnlyBoostsFullHealth(GameTestHelper helper) {
                var player = helper.makeMockPlayer(GameType.SURVIVAL);
                var target = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 1, 1, 1);
                ItemStack sponge = tool(TLTools.Type.PICKAXE, id("sponge"));
                helper.assertValueEqual(ToolTraits.attackDamage(sponge, player, target, 10), 0F, "Squeaky always deals zero damage");
                ItemStack manyullyn = tool(TLTools.Type.PICKAXE, id("manyullyn"));
                helper.assertValueEqual(ToolTraits.attackDamage(manyullyn, player, target, 10), 15F, "Coldblooded adds 50 percent against a full-health target");
                target.setHealth(target.getMaxHealth() - 1);
                helper.assertValueEqual(ToolTraits.attackDamage(manyullyn, player, target, 10), 10F, "Coldblooded does not boost a wounded target");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void autosmeltTransformsRealDropsAndLeavesNonSmeltableDrops(GameTestHelper helper) {
                ItemStack stack = tool(TLTools.Type.PICKAXE, id("firewood"));
                BlockPos pos = helper.absolutePos(BlockPos.ZERO);
                List<ItemEntity> drops = new ArrayList<>();
                drops.add(new ItemEntity(helper.getLevel(), pos.getX(), pos.getY(), pos.getZ(), new ItemStack(Items.RAW_IRON, 3)));
                drops.add(new ItemEntity(helper.getLevel(), pos.getX(), pos.getY(), pos.getZ(), new ItemStack(Items.DIAMOND, 2)));
                BlockDropsEvent event = new BlockDropsEvent(helper.getLevel(), pos, Blocks.IRON_ORE.defaultBlockState(), null, drops, null, stack);
                ToolTraitEvents.drops(event);
                helper.assertTrue(drops.get(0).getItem().is(Items.IRON_INGOT), "Autosmelt follows the world's furnace recipes");
                helper.assertValueEqual(drops.get(0).getItem().getCount(), 3, "Autosmelt preserves original drop count");
                helper.assertTrue(drops.get(1).getItem().is(Items.DIAMOND), "Non-smeltable drops remain unchanged");
                helper.assertValueEqual(drops.get(1).getItem().getCount(), 2, "Non-smeltable drop count remains unchanged");
                helper.succeed();
        }

        @GameTest(template = "empty")
        public static void momentumAndMagneticHaveOriginalLevelsAndDurations(GameTestHelper helper) {
                var player = helper.makeMockPlayer(GameType.SURVIVAL);
                ItemStack cobalt = tool(TLTools.Type.PICKAXE, id("cobalt"));
                ToolTraits.afterBlockBreak(cobalt, helper.getLevel(), Blocks.STONE.defaultBlockState(), helper.absolutePos(BlockPos.ZERO), player);
                helper.assertValueEqual(ToolTraitEffects.level(player, ToolTraitEffects.MOMENTUM), 1, "First successful block starts momentum at one");
                ItemStack iron = tool(TLTools.Type.PICKAXE, id("iron"));
                ToolTraits.afterBlockBreak(iron, helper.getLevel(), Blocks.STONE.defaultBlockState(), helper.absolutePos(BlockPos.ZERO), player);
                helper.assertValueEqual(ToolTraitEffects.level(player, ToolTraitEffects.MAGNETIC), 3, "Distinct magnetic1 and magnetic2 combine");
                helper.assertValueEqual(player.getEffect(ToolTraitEffects.MAGNETIC).getDuration(), 30, "Original magnetic duration");
                helper.succeed();
        }
}
