package com.nurby.tinkerslegacy.library.tool;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record ToolCombatCredit(List<Entry> entries) {
        public static final ToolCombatCredit EMPTY = new ToolCombatCredit(List.of());
        public static final Codec<ToolCombatCredit> CODEC = Entry.CODEC.listOf().xmap(ToolCombatCredit::new, ToolCombatCredit::entries);

        public ToolCombatCredit {
                entries = List.copyOf(entries);
        }

        // Contributions
        public ToolCombatCredit add(UUID player, UUID tool, double damage) {
                if (!Double.isFinite(damage) || damage <= 0) return this;
                List<Entry> updated = new ArrayList<>(entries);
                for (int index = 0; index < updated.size(); index++) {
                        Entry entry = updated.get(index);
                        if (entry.player().equals(player) && entry.tool().equals(tool)) {
                                updated.set(index, new Entry(player, tool, Math.min(Long.MAX_VALUE, entry.damage() + damage)));
                                return new ToolCombatCredit(updated);
                        }
                }
                updated.add(new Entry(player, tool, Math.min(Long.MAX_VALUE, damage)));
                return new ToolCombatCredit(updated);
        }

        public record Entry(UUID player, UUID tool, double damage) {
                public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                                UUIDUtil.CODEC.fieldOf("player").forGetter(Entry::player),
                                UUIDUtil.CODEC.fieldOf("tool").forGetter(Entry::tool),
                                Codec.doubleRange(0, Long.MAX_VALUE).fieldOf("damage").forGetter(Entry::damage))
                                .apply(instance, Entry::new));

                public Entry {
                        if (!Double.isFinite(damage) || damage < 0) throw new IllegalArgumentException("Invalid combat credit");
                }
        }
}
