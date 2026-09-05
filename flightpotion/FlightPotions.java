package com.example.flightpotion;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.fabricmc.fabric.api.registry.FabricBrewingRecipeRegistry;

import java.util.function.Supplier;

public class FlightPotions {
    // 注册表
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, "flightpotion");
    public static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(ForgeRegistries.POTIONS, "flightpotion");

    // 注册自定义效果
    public static final RegistryObject<MobEffect> FLIGHT_EFFECT = EFFECTS.register("flight", FlightEffect::new);

    // 注册自定义药水
    public static final RegistryObject<Potion> FLIGHT_POTION = POTIONS.register("flight", 
        () -> new Potion(new MobEffectInstance(FLIGHT_EFFECT.getHolder().get(), 1200))); // 1:00
    public static final RegistryObject<Potion> LONG_FLIGHT_POTION = POTIONS.register("long_flight", 
        () -> new Potion(new MobEffectInstance(FLIGHT_EFFECT.getHolder().get(), 3600))); // 3:00
    public static final RegistryObject<Potion> STRONG_FLIGHT_POTION = POTIONS.register("strong_flight", 
        () -> new Potion("flight", new MobEffectInstance(FLIGHT_EFFECT.getHolder().get(), 1200, 1))); // II 1:00
    public static final RegistryObject<Potion> LONG_STRONG_FLIGHT_POTION = POTIONS.register("long_strong_flight", 
        () -> new Potion("flight", new MobEffectInstance(FLIGHT_EFFECT.getHolder().get(), 3600, 1))); // II 3:00

    // 注册酿造配方
    public static void registerPotionRecipes(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // --- 请在此处选择一种方法，并注释/删除另一种 ---
            // 方法 1: Forge 专用 (使用 BrewingRecipeRegistry)
            BrewingRecipeRegistry.addRecipe(Ingredient.of(Potions.SLOW_FALLING), Ingredient.of(Items.NETHER_STAR), new ItemStack(Items.POTION));
            
            // 方法 2: Fabric 专用 (使用 FabricBrewingRecipeRegistry)
            // FabricBrewingRecipeRegistry.registerPotionRecipe(net.minecraft.potion.Potions.SLOW_FALLING, net.minecraft.item.Items.NETHER_STAR, FLIGHT_POTION.get());
            
            // 扩展配方 (Forge 示例，Fabric 类似)
            BrewingRecipeRegistry.addRecipe(Ingredient.of(FLIGHT_POTION.get()), Ingredient.of(Items.REDSTONE), new ItemStack(Items.POTION));
            BrewingRecipeRegistry.addRecipe(Ingredient.of(FLIGHT_POTION.get()), Ingredient.of(Items.GLOWSTONE_DUST), new ItemStack(Items.POTION));
            BrewingRecipeRegistry.addRecipe(Ingredient.of(STRONG_FLIGHT_POTION.get()), Ingredient.of(Items.REDSTONE), new ItemStack(Items.POTION));
        });
    }
}