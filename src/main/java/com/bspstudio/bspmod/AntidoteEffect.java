package com.bspstudio.bspmod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * 解毒（防中毒）效果：效果期间免疫中毒——持续清除中毒效果。
 * 效果名称「解毒」，药水名称「防中毒药水」，颜色灰色。
 */
public class AntidoteEffect extends MobEffect {
    public AntidoteEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x808080);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        // 免疫中毒：只要身上有中毒，立即清除
        if (entity.hasEffect(MobEffects.POISON)) {
            entity.removeEffect(MobEffects.POISON);
        }
        return true;
    }
}
