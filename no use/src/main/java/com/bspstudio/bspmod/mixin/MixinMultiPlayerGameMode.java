package com.bspstudio.bspmod.mixin;

import com.bspstudio.bspmod.FlightMusicManager;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public abstract class MixinMultiPlayerGameMode {
    @Inject(method = "attack", at = @At("HEAD"))
    private void bsp$onAttack(Player player, Entity target, CallbackInfo ci) {
        if (target instanceof EnderDragon || target instanceof EnderDragonPart) {
            FlightMusicManager.triggerDragonrend();
        }
    }
}
