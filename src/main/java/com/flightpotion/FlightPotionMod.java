package com.flightpotion;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.registry.FabricBrewingRecipeRegistry;
import net.minecraft.core.Holder;
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

    // 飞行效果（返回 Holder<MobEffect>）
    public static final Holder<MobEffect> FLIGHT_EFFECT = Registry.registerForHolder(
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
                public void onMobRemoved(LivingEntity entity, int amplifier, MobEffectInstance.Callback callback) {
                    if (entity instanceof Player player && !player.isCreative() && !player.isSpectator()) {
                        player.getAbilities().mayfly = false;
                        player.getAbilities().flying = false;
                        player.getAbilities().setFlyingSpeed(0.05F);
                        player.fallDistance = 0.0F;
                        player.onUpdateAbilities();
                    }
                    super.onMobRemoved(entity, amplifier, callback);
                }
            }
    );

    // 药水（返回 Holder<Potion>）
    public static final Holder<Potion> FLIGHT_POTION = Registry.registerForHolder(
            BuiltInRegistries.POTION,
            ResourceLocation.fromNamespaceAndPath("bsp", "flight_potion"),
            new Potion(new MobEffectInstance(FLIGHT_EFFECT, 3600))
    );

    public static final Holder<Potion> LONG_FLIGHT_POTION = Registry.registerForHolder(
            BuiltInRegistries.POTION,
            ResourceLocation.fromNamespaceAndPath("bsp", "long_flight_potion"),
            new Potion(new MobEffectInstance(FLIGHT_EFFECT, 18000))
    );

    public static final Holder<Potion> STRONG_FLIGHT_POTION = Registry.registerForHolder(
            BuiltInRegistries.POTION,
            ResourceLocation.fromNamespaceAndPath("bsp", "strong_flight_potion"),
            new Potion(new MobEffectInstance(FLIGHT_EFFECT, 3600, 1))
    );

    public static final Holder<Potion> LONG_STRONG_FLIGHT_POTION = Registry.registerForHolder(
            BuiltInRegistries.POTION,
            ResourceLocation.fromNamespaceAndPath("bsp", "long_strong_flight_potion"),
            new Potion(new MobEffectInstance(FLIGHT_EFFECT, 18000, 1))
    );

    @Override
    public void onInitialize() {
        FabricBrewingRecipeRegistry.registerPotionRecipe(Potions.AWKWARD, Items.NETHER_STAR, FLIGHT_POTION);
        FabricBrewingRecipeRegistry.registerPotionRecipe(FLIGHT_POTION, Items.REDSTONE, LONG_FLIGHT_POTION);
        FabricBrewingRecipeRegistry.registerPotionRecipe(FLIGHT_POTION, Items.GLOWSTONE_DUST, STRONG_FLIGHT_POTION);
        FabricBrewingRecipeRegistry.registerPotionRecipe(STRONG_FLIGHT_POTION, Items.REDSTONE, LONG_STRONG_FLIGHT_POTION);
        FabricBrewingRecipeRegistry.registerPotionRecipe(LONG_FLIGHT_POTION, Items.GLOWSTONE_DUST, LONG_STRONG_FLIGHT_POTION);
    }
}
