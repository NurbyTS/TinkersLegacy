package com.nurby.tinkerslegacy.datagen;

import com.nurby.tinkerslegacy.block.SlimyGrassBlock;
import com.nurby.tinkerslegacy.block.SlimyLeavesBlock;
import com.nurby.tinkerslegacy.block.SlimyVineBlock;
import com.nurby.tinkerslegacy.block.GlowBlock;
import com.nurby.tinkerslegacy.registry.TLBlocks;
import com.nurby.tinkerslegacy.registry.TLItems;
import com.nurby.tinkerslegacy.registry.TLOreFamilies;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.AlternativesEntry;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.predicates.BonusLevelTableCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;

import java.util.HashSet;
import java.util.Set;

public class TLBlockLootProvider extends BlockLootSubProvider {
        public TLBlockLootProvider(HolderLookup.Provider lookupProvider) {
                super(Set.of(), FeatureFlags.DEFAULT_FLAGS, lookupProvider);
        }

        @Override
        protected void generate() {
                // Ores
                Set<Block> ores = new HashSet<>();

                for (var family : TLOreFamilies.ALL) {
                        for (var variant : family.variants()) {
                                Block ore = variant.get();

                                if (!ores.add(ore)) {
                                        throw new IllegalStateException("Duplicate ore variant: " + ore);
                                }

                                add(ore, createOreDrop(ore, family.rawItem().get()));
                        }
                }

                // Other Blocks
                for (Block block : getKnownBlocks()) {
                        if (ores.contains(block)) {
                                continue;
                        }

                        if (block instanceof GlowBlock) {
                                add(block, LootTable.lootTable());
                        } else if (block instanceof com.nurby.tinkerslegacy.block.StationBlock) {
                                add(block, createSingleItemTable(block).apply(
                                                net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction.copyComponents(
                                                                net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction.Source.BLOCK_ENTITY)
                                                                .include(com.nurby.tinkerslegacy.registry.TLDataComponents.STATION_BASE.get())));
                        } else if (block instanceof SlimyVineBlock) {
                                add(block, createShearsOnlyDrop(block));
                        } else if (block instanceof SlimyLeavesBlock) {
                                add(block, slimeLeaves(block));
                        } else if (block instanceof SlimyGrassBlock grass) {
                                add(block, createSingleItemTableWithSilkTouch(block, grass.soil()));
                        } else {
                                dropSelf(block);
                        }
                }
        }

        // Foliage
        private LootTable.Builder slimeLeaves(Block block) {
                LootPoolEntryContainer.Builder<?> drop = EmptyLootItem.emptyItem();
                if (block == TLBlocks.block(TLBlocks.SlimyPlants.Type.BLUE_LEAVES)) {
                        drop = AlternativesEntry.alternatives(
                                        LootItem.lootTableItem(TLItems.item(TLItems.Items.Slimeballs.Type.BLUE))
                                                        .when(LootItemRandomChanceCondition.randomChance(1.0F / 3)),
                                        LootItem.lootTableItem(Items.SLIME_BALL));
                } else if (block == TLBlocks.block(TLBlocks.SlimyPlants.Type.PURPLE_LEAVES)) {
                        drop = LootItem.lootTableItem(TLItems.item(TLItems.Items.Slimeballs.Type.PURPLE));
                }
                var fortune = registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE);
                return createSilkTouchOrShearsDispatchTable(block, applyExplosionCondition(block, drop)
                                .when(BonusLevelTableCondition.bonusLevelFlatChance(fortune,
                                                0.005F, 0.0055555557F, 0.00625F, 0.008333334F, 0.025F)));
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
                return TLBlocks.BLOCKS.getEntries()
                                .stream()
                                .map(holder -> (Block) holder.get())
                                .toList();
        }
}
