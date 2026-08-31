package com.flightpotion.neoforge;

import com.flightpotion.FlightPotions;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod("bspmod")
public class FlightPotionNeoForge {
    public FlightPotionNeoForge(IEventBus modEventBus) {
        modEventBus.addListener(this::setup);
        NeoForge.EVENT_BUS.register(new NeoForgeEvents());
    }

    private void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(FlightPotions::registerBrewingRecipes);
    }
}
