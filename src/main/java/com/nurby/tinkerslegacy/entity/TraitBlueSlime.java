package com.nurby.tinkerslegacy.entity;

import com.nurby.tinkerslegacy.registry.TLItems;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class TraitBlueSlime extends Slime {
        public TraitBlueSlime(EntityType<? extends Slime> type, Level level) {
                super(type, level);
        }

        // Particles
        @Override
        protected ParticleOptions getParticleType() {
                return new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(TLItems.item(TLItems.Items.Slimeballs.Type.BLUE)));
        }
}
