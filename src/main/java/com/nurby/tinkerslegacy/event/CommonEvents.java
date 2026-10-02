package com.nurby.tinkerslegacy.event;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.registry.TLTags;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import com.nurby.tinkerslegacy.command.ToolCommand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = TinkersLegacy.MODID)
public class CommonEvents {
    @SubscribeEvent
    public static void onSlimeGroundJump(LivingEvent.LivingJumpEvent event) {
        var entity = event.getEntity();
        BlockPos pos = entity.blockPosition();
        if (entity.level().isEmptyBlock(pos)) pos = pos.below();
        if (entity.level().getBlockState(pos).is(TLTags.Blocks.SLIMY_GROUND)) {
            entity.setDeltaMovement(entity.getDeltaMovement().add(0, 0.06, 0));
            entity.playSound(SoundEvents.SLIME_SQUISH, 0.56F, 1.0F);
        }
    }

    @SubscribeEvent
    public static void onCommandRegister(RegisterCommandsEvent event) {
        ToolCommand.register(event);
    }
}
