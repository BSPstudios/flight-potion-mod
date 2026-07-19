package com.flightpotion;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

public class FlightMusic {
    private static final List<ResourceLocation> FLIGHT_TRACKS = List.of(
            ResourceLocation.withDefaultNamespace("music/game/memories"),
            ResourceLocation.withDefaultNamespace("music/game/ebb"),
            ResourceLocation.withDefaultNamespace("music/game/home"),
            ResourceLocation.withDefaultNamespace("music/game/shores"),
            ResourceLocation.withDefaultNamespace("music/game/nightly"),
            ResourceLocation.withDefaultNamespace("music/game/bounce")
    );

    private static final ResourceLocation PIGSTEP = ResourceLocation.withDefaultNamespace("music/game/pigstep");
    private static final ResourceLocation TEARS   = ResourceLocation.withDefaultNamespace("music/game/tears");

    private static final ResourceLocation CITECHO_VOID =
            ResourceLocation.fromNamespaceAndPath("bsp", "music/end/citecho_void");

    public static void tryPlayFlightMusic() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        Holder<MobEffect> flightEffect = BuiltInRegistries.MOB_EFFECT.get(
                ResourceLocation.fromNamespaceAndPath("bsp", "flight")
        ).orElse(null);
        if (flightEffect == null) return;

        MobEffectInstance effect = player.getEffect(flightEffect);
        if (effect == null || !player.getAbilities().flying) return;

        SoundManager soundManager = mc.getSoundManager();

        if (player.level().dimension() == Level.OVERWORLD) {
            if (player.level().random.nextFloat() < 0.0161F) {
                ResourceLocation chosen = FLIGHT_TRACKS.get(player.level().random.nextInt(FLIGHT_TRACKS.size()));
                soundManager.play(new SimpleSoundInstance(
                        chosen, SoundSource.MUSIC, 1.0F, 1.0F,
                        player.level().random, false, 0,
                        SimpleSoundInstance.Attenuation.NONE,
                        0.0, 0.0, 0.0, true
                ));
            }
        } else if (player.level().dimension() == Level.NETHER) {
            float roll = player.level().random.nextFloat();
            if (roll < 0.07F) {
                soundManager.play(new SimpleSoundInstance(
                        PIGSTEP, SoundSource.MUSIC, 1.0F, 1.0F,
                        player.level().random, false, 0,
                        SimpleSoundInstance.Attenuation.NONE,
                        0.0, 0.0, 0.0, true
                ));
            } else if (roll < 0.14F) {
                soundManager.play(new SimpleSoundInstance(
                        TEARS, SoundSource.MUSIC, 1.0F, 1.0F,
                        player.level().random, false, 0,
                        SimpleSoundInstance.Attenuation.NONE,
                        0.0, 0.0, 0.0, true
                ));
            }
        } else if (player.level().dimension() == Level.END && isInEndCity(player)) {
            soundManager.play(new SimpleSoundInstance(
                    CITECHO_VOID, SoundSource.MUSIC, 1.0F, 1.0F,
                    player.level().random, false, 0,
                    SimpleSoundInstance.Attenuation.NONE,
                    0.0, 0.0, 0.0, true
            ));
        }
    }

    private static boolean isInEndCity(LocalPlayer player) {
        BlockPos center = player.blockPosition();
        int radius = 16;
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {
            var block = player.level().getBlockState(pos).getBlock();
            if (block == Blocks.PURPUR_BLOCK || block == Blocks.END_STONE_BRICKS || block == Blocks.PURPUR_PILLAR) {
                return true;
            }
        }
        return false;
    }
}
