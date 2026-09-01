package com.bspstudio.bspmod.mixin;

import com.bspstudio.bspmod.BspMod;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class MixinPlayer {
    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void bsp$cancelFall(double fallDistance, float multiplier, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Player player) {
            if (BspMod.FLIGHT_EFFECT != null && player.hasEffect(BspMod.FLIGHT_EFFECT)) {
                cir.setReturnValue(false);
                return;
            }
            // 落地宽限期：效果刚结束、玩家还在下落时，免疫摔落伤害
            if (player.level().getGameTime() <= BspMod.fallProtectUntil) {
                cir.setReturnValue(false);
            }
        }
    }
}
