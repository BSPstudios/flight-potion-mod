package com.flightpotion;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.Potions;

public class FlightPotionMod implements ModInitializer {

    public static final MobEffect FLIGHT_EFFECT = Registry.register(
            BuiltInRegistries.MOB_EFFECT,
            ResourceLocation.fromNamespaceAndPath("bsp", "flight"),
            new MobEffect(MobEffectCategory.BENEFICIAL, 0x4FCCE8) {
                @Override
                public boolean applyEffectTick(LivingEntity entity, int amplifier) {
                    if (entity instanceof Player player) {
                        if (!player.getAbilities().mayfly) {
                            player.getAbilities().mayfly = true;
                            player.onUpdateAbilities();
                        }
                        player.fallDistance = 0.0F;

                        if (player.isSprinting()) {
                            player.getAbilities().setFlyingSpeed(0.25F);
                        } else {
                            player.getAbilities().setFlyingSpeed(0.15F);
                        }
                    }
                    return true;
                }

                @Override
                public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
                    return true;
                }

                @Override
                public void onEffectRemoved(LivingEntity entity, int amplifier) {
                    if (entity instanceof Player player && !player.isCreative() && !player.isSpectator()) {
                        player.getAbilities().mayfly = false;
                        player.getAbilities().flying = false;
                        player.getAbilities().setFlyingSpeed(0.05F);
                        player.fallDistance = 0.0F;
                        player.onUpdateAbilities();
                    }
                }
            }
    );

    // 普通版：飞行 I，180秒
    public static final Potion FLIGHT_POTION = Registry.register(
            BuiltInRegistries.POTION,
            ResourceLocation.fromNamespaceAndPath("bsp", "flight_potion"),
            new Potion(new MobEffectInstance(FLIGHT_EFFECT, 3600))
    );

    // 加长版：飞行 I，900秒
    public static final Potion LONG_FLIGHT_POTION = Registry.register(
            BuiltInRegistries.POTION,
            ResourceLocation.fromNamespaceAndPath("bsp", "long_flight_potion"),
            new Potion(new MobEffectInstance(FLIGHT_EFFECT, 18000))
    );

    // 增强版：飞行 II，180秒
    public static final Potion STRONG_FLIGHT_POTION = Registry.register(
            BuiltInRegistries.POTION,
            ResourceLocation.fromNamespaceAndPath("bsp", "strong_flight_potion"),
            new Potion(new MobEffectInstance(FLIGHT_EFFECT, 3600, 1))
    );

    // 加长+增强版：飞行 II，900秒
    public static final Potion LONG_STRONG_FLIGHT_POTION = Registry.register(
            BuiltInRegistries.POTION,
            ResourceLocation.fromNamespaceAndPath("bsp", "long_strong_flight_potion"),
            new Potion(new MobEffectInstance(FLIGHT_EFFECT, 18000, 1))
    );

    @Override
    public void onInitialize() {
        // 粗制的药水 + 下界之星 → 飞行药水
        PotionBrewing.addMix(Potions.AWKWARD, Items.NETHER_STAR, FLIGHT_POTION);
        // 飞行药水 + 红石粉 → 加长飞行药水
        PotionBrewing.addMix(FLIGHT_POTION, Items.REDSTONE, LONG_FLIGHT_POTION);
        // 飞行药水 + 萤石粉 → 增强飞行药水
        PotionBrewing.addMix(FLIGHT_POTION, Items.GLOWSTONE_DUST, STRONG_FLIGHT_POTION);
        // 增强飞行药水 + 红石粉 → 加长增强飞行药水
        PotionBrewing.addMix(STRONG_FLIGHT_POTION, Items.REDSTONE, LONG_STRONG_FLIGHT_POTION);
        // 加长飞行药水 + 萤石粉 → 加长增强飞行药水
        PotionBrewing.addMix(LONG_FLIGHT_POTION, Items.GLOWSTONE_DUST, LONG_STRONG_FLIGHT_POTION);
    }
}
