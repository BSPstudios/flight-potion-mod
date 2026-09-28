package com.bspstudio.bspmod.mixin;

import com.bspstudio.bspmod.BspMod;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.MaceItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 飞行效果下使用重锤攻击：无视原版"需下落 1.5 格"的限制，触发重锤暴击，
 * 并因飞行时 fallDistance 恒为 0（原版据此算伤害），这里返回固定暴击加成。
 */
@Mixin(MaceItem.class)
public abstract class MixinMaceItem {

    /** 飞行重锤暴击的固定额外伤害（配合重锤基础 8 点 ≈ 24 点，接近原版满暴击） */
    private static final float FLIGHT_SMASH_BONUS = 16.0f;

    @Inject(method = "canSmashAttack", at = @At("HEAD"), cancellable = true)
    private static void bsp$forceSmash(LivingEntity attacker, CallbackInfoReturnable<Boolean> cir) {
        if (attacker instanceof Player player && BspMod.FLIGHT_EFFECT != null
                && player.hasEffect(BspMod.FLIGHT_EFFECT)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getAttackDamageBonus", at = @At("HEAD"), cancellable = true)
    private void bsp$flightSmashDamage(Entity target, float baseDamage, DamageSource source,
                                       CallbackInfoReturnable<Float> cir) {
        Entity direct = source.getDirectEntity();
        if (direct instanceof Player player && BspMod.FLIGHT_EFFECT != null
                && player.hasEffect(BspMod.FLIGHT_EFFECT)) {
            cir.setReturnValue(FLIGHT_SMASH_BONUS);
        }
    }
}
