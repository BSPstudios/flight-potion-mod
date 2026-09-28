package com.bspstudio.bspmod.mixin;

import com.bspstudio.bspmod.BspMod;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class MixinServerPlayer {
    private boolean bsp$hadFlightEffect = false;
    private boolean bsp$landingGrace = false;

    @Inject(method = "tick", at = @At("TAIL"))
    private void bsp$afterTick(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (BspMod.FLIGHT_EFFECT != null && player.hasEffect(BspMod.FLIGHT_EFFECT)) {
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }
            // 飞行中持续清零摔落距离
            player.fallDistance = 0;
            bsp$hadFlightEffect = true;
            bsp$landingGrace = false;
        } else {
            if (bsp$hadFlightEffect) {
                // 效果消失：非创造模式清除飞行权限
                if (!player.isCreative() && !player.isSpectator()) {
                    player.getAbilities().mayfly = false;
                    player.getAbilities().flying = false;
                    player.onUpdateAbilities();
                }
                bsp$hadFlightEffect = false;
                // 开启落地缓冲：持续清零摔落距离直到双脚着地，避免效果结束后摔死
                bsp$landingGrace = true;
            }
            if (bsp$landingGrace) {
                if (!player.onGround()) {
                    player.fallDistance = 0;
                    BspMod.fallProtectUntil = player.level().getGameTime() + 2;
                } else {
                    bsp$landingGrace = false;
                    BspMod.fallProtectUntil = -1;
                }
            }
        }
    }
}
