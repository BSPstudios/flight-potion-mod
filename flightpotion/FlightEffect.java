package com.example.flightpotion;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class FlightEffect extends MobEffect {
    public FlightEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x98D982);
    }

    @Override
    public boolean applyEffectTick(LivingEntity livingEntity, int amplifier) {
        if (livingEntity instanceof Player player) {
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }
            player.getAbilities().flying = true;
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public void onEffectRemoved(LivingEntity livingEntity, int amplifier, boolean ambient, boolean showParticles) {
        if (livingEntity instanceof Player player) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
        super.onEffectRemoved(livingEntity, amplifier, ambient, showParticles);
    }
}