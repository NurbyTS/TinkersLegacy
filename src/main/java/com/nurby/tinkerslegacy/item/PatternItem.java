package com.nurby.tinkerslegacy.item;

import com.nurby.tinkerslegacy.registry.TLDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class PatternItem extends Item {
        public PatternItem(Properties properties) {
                super(properties);
        }

        @Override
        public Component getName(ItemStack stack) {
                var part = stack.get(TLDataComponents.PART);
                return part == null ? super.getName(stack)
                                : Component.translatable("item.tinkerslegacy.pattern.named",
                                                Component.translatable("part." + part.getNamespace() + "." + part.getPath()));
        }
}
