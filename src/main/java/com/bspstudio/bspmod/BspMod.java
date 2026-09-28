package com.bspstudio.bspmod;

import java.util.*;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.functions.SetPotionFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.registry.FabricBrewingRecipeRegistryBuilder;

public class BspMod implements ModInitializer {
    public static final String MOD_ID = "bspmod";

    public static Holder<MobEffect> FLIGHT_EFFECT;
    public static Holder<MobEffect> ANTIDOTE_EFFECT;
    public static Holder<Potion> FLIGHT_POTION;
    public static Holder<Potion> LONG_FLIGHT;
    public static Holder<Potion> ANTIDOTE;
    public static Holder<Potion> LONG_ANTIDOTE;
    public static Map<String, SoundEvent> FLIGHT_MUSIC_MAP = new HashMap<>();

    /** 22# 唱片（音乐唱片）物品 */
    public static Item DISC_22;
    public static ResourceKey<JukeboxSong> DISC_22_SONG_KEY;

    /** 落地保护：效果结束后的宽限期内免疫摔落伤害（游戏时间 tick） */
    public static long fallProtectUntil = -1;

    private static final java.util.Random RANDOM = new java.util.Random();

    private static final String[] TRACK_NAMES = {
        "swap-morning-sunshine", "shining-end-stone", "echo-of-the-end",
        "dragon-split", "end-stone-piece", "copper-forest", "voids-lament",
        "purple-coneflower", "citecho-void", "obsid-echo",
        "triky-glom", "triky-glom-2", "triky-glom-3",
        "village-sunset", "lightly-lash-cave", "drip-drop-cave",
        "oak-track", "buzzle-bees"
    };

    @Override
    public void onInitialize() {
        // 注册建筑者与工作者实体
        com.bspstudio.bspmod.builder.BspModEntities.register();
        com.bspstudio.bspmod.builder.BuilderCommands.registerCommands();
        com.bspstudio.bspmod.builder.BuilderChatHandler.register();

        FLIGHT_EFFECT = Registry.registerForHolder(
                BuiltInRegistries.MOB_EFFECT,
                ResourceLocation.fromNamespaceAndPath(MOD_ID, "flight"),
                new FlightEffect()
        );

        // 1.2 时代：解毒（防中毒）效果，灰色，效果期间免疫中毒
        ANTIDOTE_EFFECT = Registry.registerForHolder(
                BuiltInRegistries.MOB_EFFECT,
                ResourceLocation.fromNamespaceAndPath(MOD_ID, "antidote"),
                new AntidoteEffect()
        );

        FLIGHT_POTION = Registry.registerForHolder(
                BuiltInRegistries.POTION,
                ResourceLocation.fromNamespaceAndPath(MOD_ID, "flight_potion"),
                new Potion("flight_potion", new MobEffectInstance(FLIGHT_EFFECT, 3600))
        );

        LONG_FLIGHT = Registry.registerForHolder(
                BuiltInRegistries.POTION,
                ResourceLocation.fromNamespaceAndPath(MOD_ID, "long_flight_potion"),
                new Potion("long_flight_potion", new MobEffectInstance(FLIGHT_EFFECT, 18000))
        );

        // 1.2 时代：防中毒药水（普通 + 延长），效果「解毒」
        ANTIDOTE = Registry.registerForHolder(
                BuiltInRegistries.POTION,
                ResourceLocation.fromNamespaceAndPath(MOD_ID, "antidote"),
                new Potion("antidote", new MobEffectInstance(ANTIDOTE_EFFECT, 3600))
        );

        LONG_ANTIDOTE = Registry.registerForHolder(
                BuiltInRegistries.POTION,
                ResourceLocation.fromNamespaceAndPath(MOD_ID, "long_antidote"),
                new Potion("long_antidote", new MobEffectInstance(ANTIDOTE_EFFECT, 7200))
        );

        FabricBrewingRecipeRegistryBuilder.BUILD.register(builder -> {
            builder.registerPotionRecipe(
                    Potions.AWKWARD,
                    Ingredient.of(Items.FEATHER),
                    FLIGHT_POTION
            );
            builder.registerPotionRecipe(
                    FLIGHT_POTION,
                    Ingredient.of(Items.REDSTONE),
                    LONG_FLIGHT
            );
            // 防中毒药水：粗制的药水 + 奶桶
            builder.registerPotionRecipe(
                    Potions.AWKWARD,
                    Ingredient.of(Items.MILK_BUCKET),
                    ANTIDOTE
            );
            builder.registerPotionRecipe(
                    ANTIDOTE,
                    Ingredient.of(Items.REDSTONE),
                    LONG_ANTIDOTE
            );
            // 1.2.2：防中毒药水 + 发酵蛛眼 → 中毒药水（腐化）
            builder.registerPotionRecipe(
                    ANTIDOTE,
                    Ingredient.of(Items.FERMENTED_SPIDER_EYE),
                    Potions.POISON
            );
            // 1.2.3：延长防中毒药水 + 发酵蛛眼 → 延长中毒药水
            builder.registerPotionRecipe(
                    LONG_ANTIDOTE,
                    Ingredient.of(Items.FERMENTED_SPIDER_EYE),
                    Potions.LONG_POISON
            );
        });

        // Shipwreck treasure: 0.203% chance of 1-2 flight potions or 1 water breathing
        registerShipwreckLoot();

        for (String name : TRACK_NAMES) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MOD_ID, name);
            SoundEvent se = Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
            FLIGHT_MUSIC_MAP.put(name, se);
        }

        // 新增两个独立音乐音效：Dragonrend anthem + Voidcry crescendo
        SoundEvent dragonrend = Registry.register(BuiltInRegistries.SOUND_EVENT,
                ResourceLocation.fromNamespaceAndPath(MOD_ID, "dragonrend-anthem"),
                SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MOD_ID, "dragonrend-anthem")));
        FLIGHT_MUSIC_MAP.put("dragonrend-anthem", dragonrend);

        SoundEvent voidcry = Registry.register(BuiltInRegistries.SOUND_EVENT,
                ResourceLocation.fromNamespaceAndPath(MOD_ID, "voidcry-crescendo"),
                SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MOD_ID, "voidcry-crescendo")));
        FLIGHT_MUSIC_MAP.put("voidcry-crescendo", voidcry);

        // 22# 唱片：JukeboxSong 通过 data/bspmod/jukebox_song/disc_22.json 数据驱动注册。
        // 这里只需注册物品，并关联到该歌曲的 ResourceKey。
        DISC_22_SONG_KEY = ResourceKey.create(Registries.JUKEBOX_SONG,
                ResourceLocation.fromNamespaceAndPath(MOD_ID, "disc_22"));

        // 22# 唱片物品：复用原版 Precipice 唱片贴图，稀有度「罕见」
        // 注意：1.21.9 中 Item 构造会读取 Properties.id（effectiveModel 需要），
        // 必须显式 setId，否则抛 "Item id not set" NPE。
        ResourceKey<Item> discKey = ResourceKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(MOD_ID, "disc_22"));
        DISC_22 = Registry.register(
                BuiltInRegistries.ITEM,
                discKey,
                new Item(new Item.Properties()
                        .setId(discKey)
                        .stacksTo(1)
                        .rarity(Rarity.RARE)
                        .jukeboxPlayable(DISC_22_SONG_KEY))
        );

        registerShulkerDrop();
        registerEndCityFlyCheck();

        // 把 22# 唱片加入创造模式「工具与实用物品」物品栏，否则玩家物品栏里看不到
        ItemGroupEvents.modifyEntriesEvent(
                ResourceKey.create(Registries.CREATIVE_MODE_TAB,
                        ResourceLocation.withDefaultNamespace("tools_and_utilities"))
        ).register(entries -> {
            entries.accept(DISC_22);
        });
    }

    /** 22# 唱片掉落：在飞行效果下用重锤击杀潜影贝，16% 概率掉落 */
    private void registerShulkerDrop() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (DISC_22 == null) return;
            if (!(entity instanceof Shulker)) return;

            Entity attacker = source.getEntity();
            if (!(attacker instanceof Player player)) return;
            if (!player.hasEffect(FLIGHT_EFFECT)) return;

            // 武器必须是重锤
            ItemStack weapon = source.getWeaponItem();
            if (weapon == null || !weapon.is(Items.MACE)) return;

            if (RANDOM.nextFloat() < 0.16f) {
                if (entity.level() instanceof ServerLevel serverLevel) {
                    entity.spawnAtLocation(serverLevel, new ItemStack(DISC_22));
                }
            }
        });
    }

    /**
     * 进度「末地城，我飞来啦」：获得 22# 唱片 且 从未穿过末地折跃门。
     *
     * 历史坑：原先挂在 Inventory.add(ItemStack) 的 Mixin 上，但创造模式物品栏
     * 取出物品走的是 setItem(slot, stack)（/give 同样不经过 add），钩子根本不触发，
     * 导致进度始终测不出来。改为每秒轮询体检，覆盖所有获取途径
     * （掉落拾取 / give / 创造模式拿取 / 合成 / 箱子搬运）。
     */
    private void registerEndCityFlyCheck() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (DISC_22 == null) return;
            if (server.getTickCount() % 20 != 0) return; // 每秒一次，开销可忽略

            ResourceLocation gateId = ResourceLocation.fromNamespaceAndPath("minecraft", "end/enter_end_gateway");
            ResourceLocation advId = ResourceLocation.fromNamespaceAndPath(MOD_ID, "end/end_city_fly");
            AdvancementHolder adv = server.getAdvancements().get(advId);
            if (adv == null) return;

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                // 未持有 22# 唱片 → 跳过
                if (!player.getInventory().contains(s -> s.is(DISC_22))) continue;
                // 已达成 → 跳过
                if (player.getAdvancements().getOrStartProgress(adv).isDone()) continue;
                // 已穿过末地折跃门 → 不授予
                AdvancementHolder gate = server.getAdvancements().get(gateId);
                if (gate != null && player.getAdvancements().getOrStartProgress(gate).isDone()) continue;

                player.getAdvancements().award(adv, "disc_22");
            }
        });
    }

    private static final int LOOT_PW = 203;
    private static final int LOOT_EW = 100000 - LOOT_PW;

    private void registerShipwreckLoot() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            if (!key.location().getPath().contains("shipwreck_treasure")) return;

            var potionReg = registries.lookupOrThrow(BuiltInRegistries.POTION.key());
            ResourceKey<Potion> wbKey = ResourceKey.create(Registries.POTION, ResourceLocation.withDefaultNamespace("water_breathing"));
            ResourceKey<Potion> lwbKey = ResourceKey.create(Registries.POTION, ResourceLocation.withDefaultNamespace("long_water_breathing"));
            Holder<Potion> wb = potionReg.getOrThrow(wbKey);
            Holder<Potion> lwb = potionReg.getOrThrow(lwbKey);

            // Flight: 6 variants (drink/splash/lingering × normal/long), 1-2 bottles each
            addPotionPool(tableBuilder, Items.POTION, FLIGHT_POTION, 1, 2);
            addPotionPool(tableBuilder, Items.POTION, LONG_FLIGHT, 1, 2);
            addPotionPool(tableBuilder, Items.SPLASH_POTION, FLIGHT_POTION, 1, 2);
            addPotionPool(tableBuilder, Items.SPLASH_POTION, LONG_FLIGHT, 1, 2);
            addPotionPool(tableBuilder, Items.LINGERING_POTION, FLIGHT_POTION, 1, 2);
            addPotionPool(tableBuilder, Items.LINGERING_POTION, LONG_FLIGHT, 1, 2);

            // Water breathing: 6 variants, 1 bottle each
            addPotionPool(tableBuilder, Items.POTION, wb, 1, 1);
            addPotionPool(tableBuilder, Items.POTION, lwb, 1, 1);
            addPotionPool(tableBuilder, Items.SPLASH_POTION, wb, 1, 1);
            addPotionPool(tableBuilder, Items.SPLASH_POTION, lwb, 1, 1);
            addPotionPool(tableBuilder, Items.LINGERING_POTION, wb, 1, 1);
            addPotionPool(tableBuilder, Items.LINGERING_POTION, lwb, 1, 1);
        });
    }

    private void addPotionPool(LootTable.Builder table, ItemLike item, Holder<Potion> potion, int min, int max) {
        table.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                .add(EmptyLootItem.emptyItem().setWeight(LOOT_EW))
                .add(LootItem.lootTableItem(item)
                        .apply(SetPotionFunction.setPotion(potion))
                        .apply(SetItemCountFunction.setCount(
                                min == max ? ConstantValue.exactly(min) : UniformGenerator.between(min, max)))
                        .setWeight(LOOT_PW)));
    }
}
