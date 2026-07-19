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
    public static final Holder<MobEffect> FLIGHT_EFFECT =
            Registry.registerForHolder(
                    BuiltInRegistries.MOB_EFFECT,
                    ResourceLocation.fromNamespaceAndPath("bsp", "flight"),
                    new FlightEffect()
            );

    public static final Holder<Potion> FLIGHT_POTION =
            Registry.registerForHolder(
                    BuiltInRegistries.POTION,
                    ResourceLocation.fromNamespaceAndPath("bsp", "flight_potion"),
                    new Potion(new MobEffectInstance(FLIGHT_EFFECT, 3600))
            );

    public static final Holder<Potion> LONG_FLIGHT_POTION =
            Registry.registerForHolder(
                    BuiltInRegistries.POTION,
                    ResourceLocation.fromNamespaceAndPath("bsp", "long_flight_potion"),
                    new Potion(new MobEffectInstance(FLIGHT_EFFECT, 18000))
            );

    public static final Holder<Potion> STRONG_FLIGHT_POTION =
            Registry.registerForHolder(
                    BuiltInRegistries.POTION,
                    ResourceLocation.fromNamespaceAndPath("bsp", "strong_flight_potion"),
                    new Potion(new MobEffectInstance(FLIGHT_EFFECT, 3600, 1))
            );

    public static final Holder<Potion> LONG_STRONG_FLIGHT_POTION =
            Registry.registerForHolder(
                    BuiltInRegistries.POTION,
                    ResourceLocation.fromNamespaceAndPath("bsp", "long_strong_flight_potion"),
                    new Potion(new MobEffectInstance(FLIGHT_EFFECT, 18000, 1))
            );

    public static void registerBrewingRecipes() {
        PotionBrewing.addMix(Potions.AWKWARD, Items.NETHER_STAR, FLIGHT_POTION);
        PotionBrewing.addMix(FLIGHT_POTION, Items.REDSTONE, LONG_FLIGHT_POTION);
        PotionBrewing.addMix(FLIGHT_POTION, Items.GLOWSTONE_DUST, STRONG_FLIGHT_POTION);
        PotionBrewing.addMix(STRONG_FLIGHT_POTION, Items.REDSTONE, LONG_STRONG_FLIGHT_POTION);
        PotionBrewing.addMix(LONG_FLIGHT_POTION, Items.GLOWSTONE_DUST, LONG_STRONG_FLIGHT_POTION);
    }
}
