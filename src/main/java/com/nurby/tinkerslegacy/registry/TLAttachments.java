package com.nurby.tinkerslegacy.registry;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.api.tool.ToolCombatCredit;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class TLAttachments {
        public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(
                        NeoForgeRegistries.Keys.ATTACHMENT_TYPES, TinkersLegacy.MODID);
        public static final DeferredHolder<AttachmentType<?>, AttachmentType<ToolCombatCredit>> COMBAT_CREDIT = ATTACHMENTS.register(
                        "combat_credit", () -> AttachmentType.builder(() -> ToolCombatCredit.EMPTY)
                                        .serialize(ToolCombatCredit.CODEC, credit -> !credit.entries().isEmpty()).build());

        private TLAttachments() {
        }

        // Registration
        public static void register(IEventBus bus) {
                ATTACHMENTS.register(bus);
        }
}
