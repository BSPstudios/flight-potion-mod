package com.bspstudio.bspmod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class FlightEffect extends MobEffect {
    public FlightEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x5CE3FF);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (entity instanceof Player player) {
            // 只赋予飞行权限，不强制 flying 状态
            // 实际飞行由 FlightClientHandler 通过 Velocity 控制
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }
            // 免疫漂浮效果
            if (player.hasEffect(MobEffects.LEVITATION)) {
                player.removeEffect(MobEffects.LEVITATION);
            }
        }
        return true;
    }
}
