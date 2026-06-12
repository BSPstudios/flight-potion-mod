package com.flightpotion;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.registry.FabricBrewingRecipeRegistry;
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
import net.minecraft.world.item.alchemy.Potions;

public class FlightPotionMod implements ModInitializer {

    public static final MobEffect FLIGHT_EFFECT = Registry.register(
            BuiltInRegistries.MOB_EFFECT,
            ResourceLocation.fromNamespaceAndPath("flightpotion", "flight"),
            new MobEffect(MobEffectCategory.BENEFICIAL, 0x98D982) {
                @Override
                public boolean applyEffectTick(LivingEntity entity, int amplifier) {
                    if (entity instanceof Player player) {
                        if (!player.getAbilities().mayfly) {
                            player.getAbilities().mayfly = true;
                            player.onUpdateAbilities();
                        }
                        player.getAbilities().flying = true;
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
                        player.onUpdateAbilities();
                    }
                }
            }
    );

    public static final Potion FLIGHT_POTION = Registry.register(
            BuiltInRegistries.POTION,
            ResourceLocation.fromNamespaceAndPath("flightpotion", "flight_potion"),
            new Potion(new MobEffectInstance(FLIGHT_EFFECT, 1200))
    );

    public static final Potion LONG_FLIGHT_POTION = Registry.register(
            BuiltInRegistries.POTION,
            ResourceLocation.fromNamespaceAndPath("flightpotion", "long_flight_potion"),
            new Potion(new MobEffectInstance(FLIGHT_EFFECT, 3600))
    );

    public static final Potion STRONG_FLIGHT_POTION = Registry.register(
            BuiltInRegistries.POTION,
            ResourceLocation.fromNamespaceAndPath("flightpotion", "strong_flight_potion"),
            new Potion(new MobEffectInstance(FLIGHT_EFFECT, 1200, 1))
    );

    public static final Potion LONG_STRONG_FLIGHT_POTION = Registry.register(
            BuiltInRegistries.POTION,
            ResourceLocation.fromNamespaceAndPath("flightpotion", "long_strong_flight_potion"),
            new Potion(new MobEffectInstance(FLIGHT_EFFECT, 3600, 1))
    );

    @Override
    public void onInitialize() {
        // 粗制的药水 + 下界之星 → 飞行药水 (1:00)
        FabricBrewingRecipeRegistry.registerPotionRecipe(Potions.AWKWARD, Items.NETHER_STAR, FLIGHT_POTION);
        // 飞行药水 (1:00) + 红石 → 延长版 (3:00)
        FabricBrewingRecipeRegistry.registerPotionRecipe(FLIGHT_POTION, Items.REDSTONE, LONG_FLIGHT_POTION);
        // 飞行药水 (1:00) + 萤石粉 → 等级 II (1:00)
        FabricBrewingRecipeRegistry.registerPotionRecipe(FLIGHT_POTION, Items.GLOWSTONE_DUST, STRONG_FLIGHT_POTION);
        // 飞行药水 II (1:00) + 红石 → 延长版 II (3:00)
        FabricBrewingRecipeRegistry.registerPotionRecipe(STRONG_FLIGHT_POTION, Items.REDSTONE, LONG_STRONG_FLIGHT_POTION);
    }
}
