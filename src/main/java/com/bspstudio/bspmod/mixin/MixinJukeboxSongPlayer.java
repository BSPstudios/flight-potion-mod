package com.bspstudio.bspmod.mixin;

import com.bspstudio.bspmod.BspMod;
import com.bspstudio.bspmod.FlightMusicManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.JukeboxSongPlayer;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JukeboxSongPlayer.class)
public abstract class MixinJukeboxSongPlayer {
    @Inject(method = "play", at = @At("HEAD"))
    private void bsp$onPlay(LevelAccessor world, Holder<JukeboxSong> song, CallbackInfo ci) {
        if (!(world instanceof ClientLevel)) return;
        if (BspMod.DISC_22_SONG_KEY == null) return;
        if (song.is(BspMod.DISC_22_SONG_KEY)) {
            FlightMusicManager.notifyDisc22Played();
        }
    }
}
