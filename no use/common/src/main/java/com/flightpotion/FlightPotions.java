package com.flightpotion;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.Potions;

public class FlightPotions {
    public static final Holder<MobEffect> FLIGHT_EFFECT;
    public static final Holder<Potion> FLIGHT_POTION;
    public static final Holder<Potion> LONG_FLIGHT_POTION;
    public static final Holder<Potion> STRONG_FLIGHT_POTION;
    public static final Holder<Potion> LONG_STRONG_FLIGHT_POTION;

    static {
        FLIGHT_EFFECT = Registry.registerForHolder(
                BuiltInRegistries.MOB_EFFECT,
                ResourceLocation.fromNamespaceAndPath("bspmod", "flight"),
                new FlightEffect()
        );

        FLIGHT_POTION = Registry.registerForHolder(
                BuiltInRegistries.POTION,
                ResourceLocation.fromNamespaceAndPath("bspmod", "flight_potion"),
                new Potion(new MobEffectInstance(FLIGHT_EFFECT, 3600))
        );
        LONG_FLIGHT_POTION = Registry.registerForHolder(
                BuiltInRegistries.POTION,
                ResourceLocation.fromNamespaceAndPath("bspmod", "long_flight_potion"),
                new Potion(new MobEffectInstance(FLIGHT_EFFECT, 18000))
        );
        STRONG_FLIGHT_POTION = Registry.registerForHolder(
                BuiltInRegistries.POTION,
                ResourceLocation.fromNamespaceAndPath("bspmod", "strong_flight_potion"),
                new Potion(new MobEffectInstance(FLIGHT_EFFECT, 3600, 1))
        );
        LONG_STRONG_FLIGHT_POTION = Registry.registerForHolder(
                BuiltInRegistries.POTION,
                ResourceLocation.fromNamespaceAndPath("bspmod", "long_strong_flight_potion"),
                new Potion(new MobEffectInstance(FLIGHT_EFFECT, 18000, 1))
        );
    }

    public static void registerBrewingRecipes() {
        PotionBrewing.addMix(Potions.AWKWARD, Items.NETHER_STAR, FLIGHT_POTION);
        PotionBrewing.addMix(FLIGHT_POTION, Items.REDSTONE, LONG_FLIGHT_POTION);
        PotionBrewing.addMix(FLIGHT_POTION, Items.GLOWSTONE_DUST, STRONG_FLIGHT_POTION);
        PotionBrewing.addMix(STRONG_FLIGHT_POTION, Items.REDSTONE, LONG_STRONG_FLIGHT_POTION);
        PotionBrewing.addMix(LONG_FLIGHT_POTION, Items.GLOWSTONE_DUST, LONG_STRONG_FLIGHT_POTION);
    }
}
