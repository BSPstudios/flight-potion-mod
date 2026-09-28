package com.bspstudio.bspmod.mixin;

import com.bspstudio.bspmod.FlightMusicManager;
import net.minecraft.client.sounds.MusicManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MusicManager.class)
public abstract class MixinMusicManager {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void bsp$blockVanillaMusic(CallbackInfo ci) {
        // 自定义音乐播放时，阻止原版背景音乐启动
        if (FlightMusicManager.isCustomMusicPlaying()) {
            ci.cancel();
        }
    }
}
