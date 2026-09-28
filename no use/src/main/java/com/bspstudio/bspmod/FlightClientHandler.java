package com.bspstudio.bspmod;

import com.bspstudio.bspmod.mixin.MixinOptionsScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class FlightClientHandler implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            LocalPlayer player = client.player;
            if (player == null) return;

            FlightMusicManager.tick();

            if (!player.hasEffect(BspMod.FLIGHT_EFFECT)) return;

            if (RainbowTrailHandler.shouldRender(player)) {
                RainbowTrailHandler.tick(player);
            }

            if (client.options.keyJump.isDown()) {
                player.setDeltaMovement(
                        player.getDeltaMovement().x,
                        0.08,
                        player.getDeltaMovement().z
                );
            }
            if (client.options.keyShift.isDown()) {
                player.setDeltaMovement(
                        player.getDeltaMovement().x,
                        -0.08,
                        player.getDeltaMovement().z
                );
            }

            player.fallDistance = 0;
        });
    }
}
