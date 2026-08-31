package com.flightpotion.mixin;

import com.flightpotion.FlightPotions;
import net.minecraft.world.entity.monster.Witch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(Witch.class)
public class WitchMixin {

    @Unique
    private int flightTick = 0;

    @Inject(method = "mobTick", at = @At("HEAD"))
    private void onMobTick(CallbackInfo ci) {
        Witch witch = (Witch) (Object) this;
        if (witch.hasEffect(FlightPotions.FLIGHT_EFFECT)) {
            witch.setNoGravity(true);
            flightTick++;
            if (flightTick % 20 == 0) {
                Random rand = witch.getRandom();
                double x = witch.getX() + (rand.nextDouble() - 0.5) * 16;
                double y = witch.getY() + rand.nextDouble() * 8 - 4;
                double z = witch.getZ() + (rand.nextDouble() - 0.5) * 16;
                y = Math.max(y, 0);
                witch.getMoveControl().setWantedPosition(x, y, z, 1.0);
            }
        } else {
            witch.setNoGravity(false);
        }
    }
}