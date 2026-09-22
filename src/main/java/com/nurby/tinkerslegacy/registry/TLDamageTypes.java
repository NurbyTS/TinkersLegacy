package com.nurby.tinkerslegacy.registry;

import com.nurby.tinkerslegacy.TinkersLegacy;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.player.Player;

public class TLDamageTypes {

    public static final ResourceKey<DamageType> ARMOR_BYPASSING =
            ResourceKey.create(
                    Registries.DAMAGE_TYPE,
                    ResourceLocation.fromNamespaceAndPath(
                            TinkersLegacy.MODID,
                            "armor_bypassing"
                    )
            );

    public static DamageSource rapierDamageSource(Player player) {
        return new DamageSource(
                player.level()
                        .registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(ARMOR_BYPASSING),
                player
        );
    }
}