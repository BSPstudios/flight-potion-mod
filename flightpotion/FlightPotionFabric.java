package com.example.flightpotion;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.fabricmc.fabric.api.registry.FabricBrewingRecipeRegistry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class FlightPotionFabric implements ModInitializer {
    public static final String MOD_ID = "flightpotion";
    
    public static final FlightEffect FLIGHT_EFFECT = new FlightEffect();
    public static final Potion FLIGHT_POTION = new Potion(new MobEffectInstance(FLIGHT_EFFECT, 1200));
    public static final Potion LONG_FLIGHT_POTION = new Potion(new MobEffectInstance(FLIGHT_EFFECT, 3600));
    public static final Potion STRONG_FLIGHT_POTION = new Potion(new MobEffectInstance(FLIGHT_EFFECT, 1200, 1));
    public static final Potion LONG_STRONG_FLIGHT_POTION = new Potion(new MobEffectInstance(FLIGHT_EFFECT, 3600, 1));

    @Override
    public void onInitialize() {
        Registry.register(BuiltInRegistries.MOB_EFFECT, new ResourceLocation(MOD_ID, "flight"), FLIGHT_EFFECT);
        Registry.register(BuiltInRegistries.POTION, new ResourceLocation(MOD_ID, "flight"), FLIGHT_POTION);
        Registry.register(BuiltInRegistries.POTION, new ResourceLocation(MOD_ID, "long_flight"), LONG_FLIGHT_POTION);
        Registry.register(BuiltInRegistries.POTION, new ResourceLocation(MOD_ID, "strong_flight"), STRONG_FLIGHT_POTION);
        Registry.register(BuiltInRegistries.POTION, new ResourceLocation(MOD_ID, "long_strong_flight"), LONG_STRONG_FLIGHT_POTION);

        FabricBrewingRecipeRegistry.registerPotionRecipe(Potions.SLOW_FALLING, Items.NETHER_STAR, FLIGHT_POTION);
        FabricBrewingRecipeRegistry.registerPotionRecipe(FLIGHT_POTION, Items.REDSTONE, LONG_FLIGHT_POTION);
        FabricBrewingRecipeRegistry.registerPotionRecipe(FLIGHT_POTION, Items.GLOWSTONE_DUST, STRONG_FLIGHT_POTION);
        FabricBrewingRecipeRegistry.registerPotionRecipe(STRONG_FLIGHT_POTION, Items.REDSTONE, LONG_STRONG_FLIGHT_POTION);
    }
}