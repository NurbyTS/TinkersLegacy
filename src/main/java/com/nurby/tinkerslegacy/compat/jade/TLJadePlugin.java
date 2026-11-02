package com.nurby.tinkerslegacy.compat.jade;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.registry.TLTags;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin
public final class TLJadePlugin implements IWailaPlugin {

        @Override
        public void registerClient(IWailaClientRegistration registration) {
                registration.registerBlockComponent(new MiningRequirement(), Block.class);
        }

        // Mining Level
        private static final class MiningRequirement implements IBlockComponentProvider {
                private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(
                                TinkersLegacy.MODID, "mining_requirement");

                @Override
                public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
                        if (!accessor.getBlockState().is(TLTags.Blocks.NEEDS_COBALT_TOOL)) return;
                        // Jade selects Netherite from the ore's incorrect-for-diamond tag.
                        // Keep its icon and harvest check beside the original Cobalt level name.
                        boolean harvestable = accessor.getPlayer().hasInfiniteMaterials()
                                        || accessor.getPlayer().hasCorrectToolForDrops(accessor.getBlockState());
                        tooltip.add(Component.translatable("tooltip.tinkerslegacy.mining_requirement",
                                        Component.translatable("tinkerslegacy.mininglevel.4"))
                                        .withStyle(harvestable ? ChatFormatting.GREEN : ChatFormatting.RED), ID);
                }

                @Override
                public ResourceLocation getUid() {
                        return ID;
                }

                @Override
                public int getDefaultPriority() {
                        return 10000;
                }
        }
}
