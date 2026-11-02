package com.nurby.tinkerslegacy.item;

import com.nurby.tinkerslegacy.library.material.MaterialDefinition;
import com.nurby.tinkerslegacy.registry.TLDataComponents;
import com.nurby.tinkerslegacy.registry.TLRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class MaterialShardItem extends Item {

        public MaterialShardItem(Properties properties) {
                super(properties);
        }

        // Variants
        public ItemStack withMaterial(ResourceLocation material, int count) {
                return withMaterial(material, count, com.nurby.tinkerslegacy.library.station.MaterialInputs.SHARD);
        }

        public ItemStack withMaterial(ResourceLocation material, int count, int units) {
                ItemStack stack = new ItemStack(this, count);
                stack.set(TLDataComponents.MATERIAL_ID, material);
                if (units != com.nurby.tinkerslegacy.library.station.MaterialInputs.SHARD) {
                        stack.set(TLDataComponents.MATERIAL_UNITS, units);
                }
                return stack;
        }

        public List<ItemStack> variants() {
                return TLRegistries.MATERIALS.stream()
                                .filter(MaterialDefinition::hasShard)
                                .map(material -> withMaterial(material.id(), 1))
                                .toList();
        }

        // Tooltips
        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
                        net.minecraft.world.item.TooltipFlag flag) {
                super.appendHoverText(stack, context, tooltip, flag);
                if (!stack.has(TLDataComponents.MATERIAL_UNITS)) return;
                int units = stack.get(TLDataComponents.MATERIAL_UNITS);
                int divisor = units;
                int denominator = 144;
                while (denominator != 0) {
                        int remainder = divisor % denominator;
                        divisor = denominator;
                        denominator = remainder;
                }
                String value = units % 144 == 0 ? Integer.toString(units / 144)
                                : (units / divisor) + "/" + (144 / divisor);
                tooltip.add(Component.translatable("tooltip.tinkerslegacy.material_units", value)
                                .withStyle(net.minecraft.ChatFormatting.GRAY));
        }

        // Getters
        public MaterialDefinition material(ItemStack stack) {
                ResourceLocation id = stack.get(TLDataComponents.MATERIAL_ID);
                return id != null && TLRegistries.MATERIALS.containsKey(id)
                                ? TLRegistries.MATERIALS.get(id) : null;
        }

        @Override
        public Component getName(ItemStack stack) {
                var material = stack.get(TLDataComponents.MATERIAL_ID);
                return material == null ? super.getName(stack)
                                : Component.translatable(stack.has(TLDataComponents.MATERIAL_UNITS)
                                                ? "item.tinkerslegacy.material_fragment.named" : "item.tinkerslegacy.material_shard.named",
                                                Component.translatable(material.getNamespace() + ".material." + material.getPath()));
        }
}
