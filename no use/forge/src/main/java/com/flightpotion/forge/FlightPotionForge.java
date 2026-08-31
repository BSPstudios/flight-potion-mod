package com.flightpotion.forge;

import com.flightpotion.FlightPotions;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.common.MinecraftForge;

@Mod("bspmod")
public class FlightPotionForge {
    public FlightPotionForge() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        MinecraftForge.EVENT_BUS.register(new ForgeEvents());
    }

    private void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(FlightPotions::registerBrewingRecipes);
    }
}
