package com.nurby.tinkerslegacy.registry;

import com.nurby.tinkerslegacy.TinkersLegacy;
import com.nurby.tinkerslegacy.api.material.MaterialBuilder;
import com.nurby.tinkerslegacy.api.material.MaterialDefinition;
import com.nurby.tinkerslegacy.api.material.recipe.CastingRecipeInfo;
import com.nurby.tinkerslegacy.api.material.recipe.LegacyRecipeInfo;
import com.nurby.tinkerslegacy.api.material.stat.MaterialStat;
import com.nurby.tinkerslegacy.client.datagen.texture.render_info.CompositeRenderInfo;
import com.nurby.tinkerslegacy.client.datagen.texture.render_info.MetalRenderInfo;
import com.nurby.tinkerslegacy.client.datagen.texture.render_info.TintMaterialRenderInfo;
import com.nurby.tinkerslegacy.registry.tools.ToolStatTypes;
import com.nurby.tinkerslegacy.util.TinkersUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;

public final class TLMaterials {
        public static final DeferredRegister<MaterialDefinition> MATERIALS = DeferredRegister.create(
                        TLRegistries.MATERIAL_KEY,
                        TinkersLegacy.MODID);

        // Natural
        public static final DeferredHolder<MaterialDefinition, MaterialDefinition> WOOD = add(
                        "wood", 0x8E661B,
                        material -> material
                                        .representativeItem(ResourceLocation.withDefaultNamespace("oak_planks"))
                                        .stats(
                                                        head(35, 2.0F, 2.0F, 0),
                                                        handle(1.0F, 25),
                                                        extra(15),
                                                        bow(1.0F, 1.0F, 0),
                                                        shaft(1.0F, 0))
                                        .trait(id("ecological"), TLStatTypes.GENERAL.getId()));

        public static final DeferredHolder<MaterialDefinition, MaterialDefinition> STONE = add(
                        "stone", 0x999999,
                        material -> material
                                        .representativeItem(ResourceLocation.withDefaultNamespace("cobblestone"))
                                        .stats(
                                                        head(120, 4.0F, 3.0F, 1),
                                                        handle(0.5F, -50),
                                                        extra(20),
                                                        bow(0.2F, 0.4F, -1.0F))
                                        .trait(id("cheapskate"), ToolStatTypes.HEAD.getId())
                                        .trait(id("cheap"), TLStatTypes.GENERAL.getId()));

        public static final DeferredHolder<MaterialDefinition, MaterialDefinition> FLINT = add(
                        "flint", 0x696969,
                        material -> material
                                        .representativeItem(ResourceLocation.withDefaultNamespace("flint"))
                                        .stats(
                                                        head(150, 5.0F, 2.9F, 1),
                                                        handle(0.6F, -60),
                                                        extra(40),
                                                        bow(0.2F, 0.4F, -1.0F))
                                        .trait(id("crude2"), ToolStatTypes.HEAD.getId())
                                        .trait(id("crude1"), TLStatTypes.GENERAL.getId()));

        public static final DeferredHolder<MaterialDefinition, MaterialDefinition> CACTUS = add(
                        "cactus", 0x00A10F,
                        material -> material
                                        .representativeItem(ResourceLocation.withDefaultNamespace("cactus"))
                                        .stats(
                                                        head(210, 4.0F, 3.4F, 1),
                                                        handle(0.85F, 20),
                                                        extra(50),
                                                        bow(1.05F, 0.9F, 0))
                                        .trait(id("prickly"), ToolStatTypes.HEAD.getId())
                                        .trait(id("spiky"), TLStatTypes.GENERAL.getId()));

        public static final DeferredHolder<MaterialDefinition, MaterialDefinition> BONE = add(
                        "bone", 0xEDE6BF,
                        material -> material
                                        .representativeItem(ResourceLocation.withDefaultNamespace("bone"))
                                        .stats(
                                                        head(200, 5.09F, 2.5F, 1),
                                                        handle(1.1F, 50),
                                                        extra(65),
                                                        bow(0.95F, 1.15F, 0),
                                                        shaft(0.9F, 5))
                                        .trait(id("splintering"), ToolStatTypes.HEAD.getId())
                                        .trait(id("splitting"), ToolStatTypes.ARROW_SHAFT.getId())
                                        .trait(id("fractured"), TLStatTypes.GENERAL.getId()));

        public static final DeferredHolder<MaterialDefinition, MaterialDefinition> OBSIDIAN = add(
                        "obsidian", 0x601CC4,
                        material -> material
                                        .recipeInfo(new CastingRecipeInfo(id("obsidian"), true))
                                        .representativeItem(ResourceLocation.withDefaultNamespace("obsidian"))
                                        .stats(
                                                        head(139, 7.07F, 4.2F, 4),
                                                        handle(0.9F, -100),
                                                        extra(90),
                                                        bow(0.2F, 0.4F, -1.0F))
                                        .trait(id("duritos"), TLStatTypes.GENERAL.getId()));

        public static final DeferredHolder<MaterialDefinition, MaterialDefinition> PRISMARINE = add(
                        "prismarine", 0x7EDEBC,
                        material -> material
                                        .representativeItem(ResourceLocation.withDefaultNamespace("prismarine"))
                                        .stats(
                                                        head(430, 5.5F, 6.2F, 1),
                                                        handle(0.6F, -150),
                                                        extra(100),
                                                        bow(0.2F, 0.4F, -1.0F))
                                        .trait(id("jagged"), ToolStatTypes.HEAD.getId())
                                        .trait(id("aquadynamic"), ToolStatTypes.HEAD.getId())
                                        .trait(id("aquadynamic"), TLStatTypes.GENERAL.getId()));

        public static final DeferredHolder<MaterialDefinition, MaterialDefinition> ENDSTONE = add(
                        "endstone", 0xE0D890,
                        material -> material
                                        .representativeItem(ResourceLocation.withDefaultNamespace("end_stone"))
                                        .stats(
                                                        head(420, 3.23F, 3.23F, 3),
                                                        handle(0.85F, 0),
                                                        extra(42),
                                                        bow(0.2F, 0.4F, -1.0F))
                                        .trait(id("alien"), ToolStatTypes.HEAD.getId())
                                        .trait(id("enderference"), TLStatTypes.GENERAL.getId())
                                        .trait(id("enderference"), ToolStatTypes.PROJECTILE.getId()));

        public static final DeferredHolder<MaterialDefinition, MaterialDefinition> PAPER = add(
                        "paper", 0xFFFFFF,
                        material -> material
                                        .representativeItem(ResourceLocation.withDefaultNamespace("paper"))
                                        .stats(
                                                        head(12, 0.51F, 0.05F, 0),
                                                        handle(0.1F, 5),
                                                        extra(15),
                                                        bow(1.5F, 0.4F, -2.0F))
                                        .trait(id("writable2"), ToolStatTypes.HEAD.getId())
                                        .trait(id("writable1"), TLStatTypes.GENERAL.getId()));

        public static final DeferredHolder<MaterialDefinition, MaterialDefinition> SPONGE = add(
                        "sponge", 0xCACC4E,
                        material -> material
                                        .representativeItem(ResourceLocation.withDefaultNamespace("sponge"))
                                        .stats(
                                                        head(1050, 3.02F, 0.0F, 0),
                                                        handle(1.2F, 250),
                                                        extra(250),
                                                        bow(1.15F, 0.75F, 0))
                                        .trait(id("squeaky"), TLStatTypes.GENERAL.getId()));

        public static final DeferredHolder<MaterialDefinition, MaterialDefinition> FIREWOOD = add(
                        "firewood", 0xCC5300,
                        material -> material
                                        .stats(
                                                        head(550, 6.0F, 5.5F, 0),
                                                        handle(1.0F, -200),
                                                        extra(150),
                                                        bow(1.0F, 1.0F, 0.0F))
                                        .trait(id("autosmelt"), TLStatTypes.GENERAL.getId()));

        // Slime
        public static final DeferredHolder<MaterialDefinition, MaterialDefinition> KNIGHTSLIME = add(
                        "knightslime", 0xF18FF0,
                        material -> material
                                        .recipeInfo(new CastingRecipeInfo(id("knightslime"), true))
                                        .renderInfo(new MetalRenderInfo(0xF18FF0, 0.0F, 0.3F, 0.0F))
                                        .stats(
                                                        head(850, 5.8F, 5.1F, 3),
                                                        handle(0.5F, 500),
                                                        extra(125),
                                                        bow(0.4F, 2.0F, 2.0F))
                                        .trait(id("crumbling"), ToolStatTypes.HEAD.getId())
                                        .trait(id("unnatural"), TLStatTypes.GENERAL.getId()));

        public static final DeferredHolder<MaterialDefinition, MaterialDefinition> SLIME = add(
                        "slime", 0x82C873,
                        material -> material
                                        .stats(
                                                        head(1000, 4.24F, 1.8F, 0),
                                                        handle(0.7F, 0),
                                                        extra(350),
                                                        bow(0.85F, 1.3F, 0))
                                        .trait(id("slimey_green"), TLStatTypes.GENERAL.getId()));

        public static final DeferredHolder<MaterialDefinition, MaterialDefinition> BLUESLIME = add(
                        "blueslime", 0x74C8C7,
                        material -> material
                                        .stats(
                                                        head(780, 4.03F, 1.8F, 0),
                                                        handle(1.3F, -50),
                                                        extra(200),
                                                        bow(1.05F, 1.0F, 0))
                                        .trait(id("slimey_blue"), TLStatTypes.GENERAL.getId()));

        public static final DeferredHolder<MaterialDefinition, MaterialDefinition> MAGMASLIME = add(
                        "magmaslime", 0xFF960D,
                        material -> material
                                        .stats(
                                                        head(600, 2.1F, 7.0F, 0),
                                                        handle(0.85F, -200),
                                                        extra(150),
                                                        bow(1.1F, 1.05F, 1.0F))
                                        .trait(id("superheat"), ToolStatTypes.HEAD.getId())
                                        .trait(id("flammable"), TLStatTypes.GENERAL.getId()));

        // Metals
        public static final DeferredHolder<MaterialDefinition, MaterialDefinition> IRON = add(
                        "iron", 0xCACACA,
                        material -> material
                                        .recipeInfo(new CastingRecipeInfo(id("iron")))
                                        .renderInfo(new MetalRenderInfo(0xCACACA, 0.0F, 0.3F, 0.0F))
                                        .representativeItem(ResourceLocation.withDefaultNamespace("iron_ingot"))
                                        .stats(
                                                        head(204, 6.0F, 4.0F, 2),
                                                        handle(0.85F, 60),
                                                        extra(50),
                                                        bow(0.5F, 1.5F, 7.0F))
                                        .trait(id("magnetic2"), ToolStatTypes.HEAD.getId())
                                        .trait(id("magnetic1"), TLStatTypes.GENERAL.getId()));

        public static final DeferredHolder<MaterialDefinition, MaterialDefinition> PIGIRON = add(
                        "pigiron", 0xEF9E9B,
                        material -> material
                                        .recipeInfo(new CastingRecipeInfo(id("pigiron")))
                                        .renderInfo(new MetalRenderInfo(0xEF9E9B, 0.0F, 0.3F, 0.0F))
                                        .stats(
                                                        head(380, 6.2F, 4.5F, 2),
                                                        handle(1.2F, 0),
                                                        extra(170),
                                                        bow(0.6F, 1.4F, 7.0F))
                                        .trait(id("baconlicious"), ToolStatTypes.HEAD.getId())
                                        .trait(id("tasty"), ToolStatTypes.HEAD.getId())
                                        .trait(id("tasty"), TLStatTypes.GENERAL.getId()));

        // Nether
        public static final DeferredHolder<MaterialDefinition, MaterialDefinition> NETHERRACK = add(
                        "netherrack", 0xB84F4F,
                        material -> material
                                        .renderInfo(new CompositeRenderInfo(ResourceLocation.withDefaultNamespace("block/netherrack")))
                                        .representativeItem(ResourceLocation.withDefaultNamespace("netherrack"))
                                        .stats(
                                                        head(270, 4.5F, 3.0F, 1),
                                                        handle(0.85F, -150),
                                                        extra(75),
                                                        bow(0.2F, 0.4F, -1.0F))
                                        .trait(id("aridiculous"), ToolStatTypes.HEAD.getId())
                                        .trait(id("hellish"), ToolStatTypes.HEAD.getId())
                                        .trait(id("hellish"), TLStatTypes.GENERAL.getId()));

        public static final DeferredHolder<MaterialDefinition, MaterialDefinition> ARDITE = add(
                        "ardite", 0xD14210,
                        material -> material
                                        .recipeInfo(new CastingRecipeInfo(id("ardite")))
                                        .renderInfo(new MetalRenderInfo(0xD14210, 0.0F, 0.3F, 0.0F))
                                        .stats(
                                                        head(990, 3.5F, 3.6F, 4),
                                                        handle(1.4F, -200),
                                                        extra(450),
                                                        bow(0.45F, 0.8F, 1.0F))
                                        .trait(id("stonebound"), ToolStatTypes.HEAD.getId())
                                        .trait(id("petramor"), TLStatTypes.GENERAL.getId()));

        public static final DeferredHolder<MaterialDefinition, MaterialDefinition> COBALT = add(
                        "cobalt", 0x2882D4,
                        material -> material
                                        .recipeInfo(new CastingRecipeInfo(id("cobalt")))
                                        .renderInfo(new MetalRenderInfo(0x2882D4, 0.0F, 0.3F, 0.0F))
                                        .stats(
                                                        head(780, 12.0F, 4.1F, 4),
                                                        handle(0.9F, 100),
                                                        extra(300),
                                                        bow(0.75F, 1.3F, 3.0F))
                                        .trait(id("momentum"), ToolStatTypes.HEAD.getId())
                                        .trait(id("lightweight"), TLStatTypes.GENERAL.getId()));

        public static final DeferredHolder<MaterialDefinition, MaterialDefinition> MANYULLYN = add(
                        "manyullyn", 0xA15CF8,
                        material -> material
                                        .recipeInfo(new CastingRecipeInfo(id("manyullyn")))
                                        .renderInfo(new MetalRenderInfo(0xA15CF8, 0.0F, 0.3F, 0.0F))
                                        .stats(
                                                        head(820, 7.02F, 8.72F, 4),
                                                        handle(0.5F, 250),
                                                        extra(50),
                                                        bow(0.65F, 1.2F, 4.0F))
                                        .trait(id("insatiable"), ToolStatTypes.HEAD.getId())
                                        .trait(id("coldblooded"), TLStatTypes.GENERAL.getId()));

        // Integration
        public static final DeferredHolder<MaterialDefinition, MaterialDefinition> COPPER = add(
                        "copper", 0xED9F07,
                        material -> material
                                        .recipeInfo(new CastingRecipeInfo(id("copper")))
                                        .renderInfo(new MetalRenderInfo(0xED9F07, 0.0F, 0.3F, 0.0F))
                                        .representativeItem(ResourceLocation.withDefaultNamespace("copper_ingot"))
                                        .stats(
                                                        head(210, 5.3F, 3.0F, 1),
                                                        handle(1.05F, 30),
                                                        extra(100),
                                                        bow(0.6F, 1.45F, 5.0F))
                                        .trait(id("established"), TLStatTypes.GENERAL.getId()));

        public static final DeferredHolder<MaterialDefinition, MaterialDefinition> BRONZE = add(
                        "bronze", 0xE3BD68,
                        material -> material
                                        .recipeInfo(new CastingRecipeInfo(id("bronze")))
                                        .renderInfo(new MetalRenderInfo(0xE3BD68, 0.0F, 0.3F, 0.0F))
                                        .stats(
                                                        head(430, 6.8F, 3.5F, 2),
                                                        handle(1.1F, 70),
                                                        extra(80),
                                                        bow(0.55F, 1.5F, 6.0F))
                                        .trait(id("dense"), TLStatTypes.GENERAL.getId()));

        public static final DeferredHolder<MaterialDefinition, MaterialDefinition> LEAD = add(
                        "lead", 0x4D4968,
                        material -> material
                                        .recipeInfo(new CastingRecipeInfo(id("lead")))
                                        .renderInfo(new MetalRenderInfo(0x4D4968, 0.0F, 0.3F, 0.0F))
                                        .stats(
                                                        head(434, 5.25F, 3.5F, 1),
                                                        handle(0.7F, -50),
                                                        extra(100),
                                                        bow(0.4F, 1.3F, 3.0F))
                                        .trait(id("poisonous"), TLStatTypes.GENERAL.getId())
                                        .trait(id("heavy"), TLStatTypes.GENERAL.getId()));

        public static final DeferredHolder<MaterialDefinition, MaterialDefinition> SILVER = add(
                        "silver", 0xD1ECF6,
                        material -> material
                                        .recipeInfo(new CastingRecipeInfo(id("silver")))
                                        .renderInfo(new MetalRenderInfo(0xD1ECF6, 0.0F, 0.3F, 0.0F))
                                        .stats(
                                                        head(250, 5.0F, 5.0F, 1),
                                                        handle(0.95F, 50),
                                                        extra(150),
                                                        bow(1.2F, 0.8F, 2.0F))
                                        .trait(id("holy"), TLStatTypes.GENERAL.getId()));

        public static final DeferredHolder<MaterialDefinition, MaterialDefinition> ELECTRUM = add(
                        "electrum", 0xE8DB49,
                        material -> material
                                        .recipeInfo(new CastingRecipeInfo(id("electrum")))
                                        .renderInfo(new MetalRenderInfo(0xE8DB49, 0.0F, 0.3F, 0.0F))
                                        .stats(
                                                        head(50, 12.0F, 3.0F, 1),
                                                        handle(1.1F, -25),
                                                        extra(250),
                                                        bow(1.5F, 1.0F, 4.0F))
                                        .trait(id("shocking"), TLStatTypes.GENERAL.getId()));

        public static final DeferredHolder<MaterialDefinition, MaterialDefinition> STEEL = add(
                        "steel", 0xA7A7A7,
                        material -> material
                                        .recipeInfo(new CastingRecipeInfo(id("steel")))
                                        .renderInfo(new MetalRenderInfo(0xA7A7A7, 0.0F, 0.3F, 0.0F))
                                        .stats(
                                                        head(540, 7.0F, 6.0F, 3),
                                                        handle(0.9F, 150),
                                                        extra(25),
                                                        bow(0.4F, 2.0F, 9.0F))
                                        .trait(id("sharp"), ToolStatTypes.HEAD.getId())
                                        .trait(id("stiff"), TLStatTypes.GENERAL.getId()));

        // Bowstrings
        public static final DeferredHolder<MaterialDefinition, MaterialDefinition> STRING = add(
                        "string", 0xEEEEEE,
                        material -> material
                                        .representativeItem(ResourceLocation.withDefaultNamespace("string"))
                                        .stats(
                                                        bowstring(1.0F)));

        public static final DeferredHolder<MaterialDefinition, MaterialDefinition> VINE = add(
                        "vine", 0x40A10F,
                        material -> material
                                        .representativeItem(ResourceLocation.withDefaultNamespace("vine"))
                                        .stats(
                                                        bowstring(1.0F)));

        public static final DeferredHolder<MaterialDefinition, MaterialDefinition> SLIMEVINE_BLUE = add(
                        "slimevine_blue", 0x74C8C7,
                        material -> material
                                        .stats(
                                                        bowstring(1.0F)));

        public static final DeferredHolder<MaterialDefinition, MaterialDefinition> SLIMEVINE_PURPLE = add(
                        "slimevine_purple", 0xC873C8,
                        material -> material
                                        .stats(
                                                        bowstring(1.0F)));

        // Shafts
        public static final DeferredHolder<MaterialDefinition, MaterialDefinition> BLAZE = add(
                        "blaze", 0xFFC100,
                        material -> material
                                        .representativeItem(ResourceLocation.withDefaultNamespace("blaze_rod"))
                                        .stats(
                                                        shaft(0.8F, 3))
                                        .trait(id("hovering"), TLStatTypes.GENERAL.getId()));

        public static final DeferredHolder<MaterialDefinition, MaterialDefinition> REED = add(
                        "reed", 0xAADB74,
                        material -> material
                                        .representativeItem(ResourceLocation.withDefaultNamespace("sugar_cane"))
                                        .stats(
                                                        shaft(1.5F, 20))
                                        .trait(id("breakable"), TLStatTypes.GENERAL.getId()));

        public static final DeferredHolder<MaterialDefinition, MaterialDefinition> ICE = add(
                        "ice", 0x97D7E0,
                        material -> material
                                        .representativeItem(ResourceLocation.withDefaultNamespace("packed_ice"))
                                        .stats(
                                                        shaft(0.95F, 0))
                                        .trait(id("freezing"), TLStatTypes.GENERAL.getId()));

        public static final DeferredHolder<MaterialDefinition, MaterialDefinition> ENDROD = add(
                        "endrod", 0xE8FFD6,
                        material -> material
                                        .representativeItem(ResourceLocation.withDefaultNamespace("end_rod"))
                                        .stats(
                                                        shaft(0.7F, 1))
                                        .trait(id("endspeed"), TLStatTypes.GENERAL.getId()));

        // Fletching
        public static final DeferredHolder<MaterialDefinition, MaterialDefinition> FEATHER = add(
                        "feather", 0xEEEEEE,
                        material -> material
                                        .representativeItem(ResourceLocation.withDefaultNamespace("feather"))
                                        .stats(
                                                        fletching(1.0F, 1.0F)));

        public static final DeferredHolder<MaterialDefinition, MaterialDefinition> LEAF = add(
                        "leaf", 0x1D730C,
                        material -> material
                                        .representativeItem(ResourceLocation.withDefaultNamespace("oak_leaves"))
                                        .stats(
                                                        fletching(0.5F, 1.5F)));

        public static final DeferredHolder<MaterialDefinition, MaterialDefinition> SLIMELEAF_BLUE = add(
                        "slimeleaf_blue", 0x74C8C7,
                        material -> material
                                        .stats(
                                                        fletching(0.8F, 1.25F)));

        public static final DeferredHolder<MaterialDefinition, MaterialDefinition> SLIMELEAF_ORANGE = add(
                        "slimeleaf_orange", 0xFF960D,
                        material -> material
                                        .stats(
                                                        fletching(0.8F, 1.25F)));

        public static final DeferredHolder<MaterialDefinition, MaterialDefinition> SLIMELEAF_PURPLE = add(
                        "slimeleaf_purple", 0xC873C8,
                        material -> material
                                        .stats(
                                                        fletching(0.8F, 1.25F)));

        // Saved Materials
        public static final DeferredHolder<MaterialDefinition, MaterialDefinition> DIAMOND = add(
                        "diamond", 0x5DE0D0,
                        material -> material
                                        .recipeInfo(new LegacyRecipeInfo())
                                        .renderInfo(new MetalRenderInfo(0x5DE0D0, 0.2F, 0.15F, 0.0F))
                                        .stats(
                                                        head(750, 8.0F, 7.0F, 3),
                                                        handle(1.2F, 75),
                                                        extra(100)));

        // Adders
        private static DeferredHolder<MaterialDefinition, MaterialDefinition> add(
                        String name, int color, Function<MaterialBuilder, MaterialBuilder> factory) {
                return MATERIALS.register(name, () -> factory.apply(
                                MaterialBuilder.material(id(name))
                                                .color(color)
                                                .renderInfo(new TintMaterialRenderInfo(color))).build());
        }

        // Registration
        public static void register(IEventBus bus) {
                MATERIALS.register(bus);
        }

        // Stats
        private static MaterialStat head(int durability, float speed, float attack, int level) {
                return TinkersUtils.createMaterialStat(ToolStatTypes.HEAD,
                                "durability", durability,
                                "mining_speed", speed,
                                "attack_damage", attack,
                                "harvest_level", level);
        }

        private static MaterialStat handle(float modifier, int durability) {
                return TinkersUtils.createMaterialStat(ToolStatTypes.HANDLE,
                                "modifier", modifier,
                                "durability", durability);
        }

        private static MaterialStat extra(int durability) {
                return TinkersUtils.createMaterialStat(ToolStatTypes.EXTRA,
                                "durability", durability);
        }

        private static MaterialStat bow(float speed, float range, float damage) {
                return TinkersUtils.createMaterialStat(ToolStatTypes.BOW,
                                "draw_speed", speed,
                                "range", range,
                                "bonus_damage", damage);
        }

        private static MaterialStat bowstring(float modifier) {
                return TinkersUtils.createMaterialStat(ToolStatTypes.BOWSTRING,
                                "modifier", modifier);
        }

        private static MaterialStat shaft(float modifier, int ammo) {
                return TinkersUtils.createMaterialStat(ToolStatTypes.ARROW_SHAFT,
                                "modifier", modifier,
                                "bonus_ammo", ammo);
        }

        private static MaterialStat fletching(float accuracy, float modifier) {
                return TinkersUtils.createMaterialStat(ToolStatTypes.FLETCHING,
                                "accuracy", accuracy,
                                "modifier", modifier);
        }

        private static ResourceLocation id(String path) {
                return ResourceLocation.fromNamespaceAndPath(TinkersLegacy.MODID, path);
        }
}
