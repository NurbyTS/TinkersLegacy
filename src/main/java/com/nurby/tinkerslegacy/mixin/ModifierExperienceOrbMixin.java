package com.nurby.tinkerslegacy.mixin;

import com.nurby.tinkerslegacy.api.tool.modifier.ToolModifierEvents;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExperienceOrb.class)
public abstract class ModifierExperienceOrbMixin {
        @Shadow public int value;

        // Merged Orbs
        @Inject(method = "playerTouch", at = @At("HEAD"))
        private void tinkerslegacy$beginPickup(Player player, CallbackInfo callback) {
                ((ExperienceOrb) (Object) this).getPersistentData().remove(ToolModifierEvents.MOSS_PICKUP_VALUE);
        }

        @Inject(method = "playerTouch", at = @At("RETURN"))
        private void tinkerslegacy$finishPickup(Player player, CallbackInfo callback) {
                var data = ((ExperienceOrb) (Object) this).getPersistentData();
                // Restore only moss's per-pickup change, leaving other XP listeners alone.
                if (data.contains(ToolModifierEvents.MOSS_PICKUP_VALUE)) {
                        value = data.getInt(ToolModifierEvents.MOSS_PICKUP_VALUE);
                        data.remove(ToolModifierEvents.MOSS_PICKUP_VALUE);
                }
        }
}
