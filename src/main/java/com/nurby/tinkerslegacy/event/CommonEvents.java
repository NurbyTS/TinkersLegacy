package com.nurby.tinkerslegacy.event;

import com.nurby.tinkerslegacy.TinkersLegacy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import com.nurby.tinkerslegacy.command.ToolCommand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = TinkersLegacy.MODID)
public class CommonEvents {
    private static final TagKey<Block> SLIMY_GROUND = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, "slimy_ground"));

    @SubscribeEvent
    public static void onSlimeGroundJump(LivingEvent.LivingJumpEvent event) {
        var entity = event.getEntity();
        BlockPos pos = entity.blockPosition();
        if (entity.level().isEmptyBlock(pos)) pos = pos.below();
        if (entity.level().getBlockState(pos).is(SLIMY_GROUND)) {
            entity.setDeltaMovement(entity.getDeltaMovement().add(0, 0.06, 0));
            entity.playSound(SoundEvents.SLIME_SQUISH, 0.56F, 1.0F);
        }
    }

    @SubscribeEvent
    public static void onCommandRegister(RegisterCommandsEvent event) {
        ToolCommand.register(event);
    }
}
