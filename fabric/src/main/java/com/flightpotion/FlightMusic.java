package com.flightpotion;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Random;

public class FlightMusic {
    private static final Random RANDOM = new Random();

    // 主世界 66.66% 概率使用的自定义音乐列表
    private static final List<ResourceLocation> FLIGHT_TRACKS = List.of(
        ResourceLocation.withDefaultNamespace("music/game/memories"),
        ResourceLocation.withDefaultNamespace("music/game/ebb"),
        ResourceLocation.withDefaultNamespace("music/game/home"),
        ResourceLocation.withDefaultNamespace("music/game/shores"),
        ResourceLocation.withDefaultNamespace("music/game/nightly"),
        ResourceLocation.withDefaultNamespace("music/game/bounce")
    );

    // 下界专用曲目
    private static final ResourceLocation PIGSTEP = ResourceLocation.withDefaultNamespace("music/game/pigstep");
    private static final ResourceLocation TEARS   = ResourceLocation.withDefaultNamespace("music/game/tears");

    /**
     * 在客户端 tick 中调用。当玩家拥有飞行效果且正在飞行时，
     * 根据所在维度以不同概率播放指定音乐。
     */
    public static void tryPlayFlightMusic() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        Holder<MobEffect> flightEffect = BuiltInRegistries.MOB_EFFECT.get(
            ResourceLocation.fromNamespaceAndPath("bspmod", "flight")
        );
        if (flightEffect == null) return;

        MobEffectInstance effect = player.getEffect(flightEffect.value());
        if (effect == null || !player.getAbilities().flying) return;

        SoundManager soundManager = mc.getSoundManager();
        // 避免同时播放多首音乐
        if (soundManager.isActive(null)) return;

        // 主世界
        if (player.level().dimension() == Level.OVERWORLD) {
            if (RANDOM.nextFloat() < 0.0166F) {
                ResourceLocation chosen = FLIGHT_TRACKS.get(RANDOM.nextInt(FLIGHT_TRACKS.size()));
                soundManager.play(new SimpleSoundInstance(
                    chosen,
                    SoundSource.MUSIC,
                    1.0F, 1.0F,
                    RANDOM,
                    false, 0,
                    net.minecraft.client.resources.sounds.SoundInstance.Attenuation.NONE,
                    0.0, 0.0, 0.0,
                    true
                ));
            }
        }
        // 下界
        else if (player.level().dimension() == Level.NETHER) {
            float roll = RANDOM.nextFloat();
            if (roll < 0.07F) {
                soundManager.play(new SimpleSoundInstance(
                    PIGSTEP,
                    SoundSource.MUSIC,
                    1.0F, 1.0F,
                    RANDOM,
                    false, 0,
                    net.minecraft.client.resources.sounds.SoundInstance.Attenuation.NONE,
                    0.0, 0.0, 0.0,
                    true
                ));
            } else if (roll < 0.14F) { // 0.07 + 0.07
                soundManager.play(new SimpleSoundInstance(
                    TEARS,
                    SoundSource.MUSIC,
                    1.0F, 1.0F,
                    RANDOM,
                    false, 0,
                    net.minecraft.client.resources.sounds.SoundInstance.Attenuation.NONE,
                    0.0, 0.0, 0.0,
                    true
                ));
            }
            // 其余情况由原版音乐接管
        }
        // 其他维度（如末地）完全不干预，纯原版
    }
}
