package com.bspstudio.bspmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;

public class RainbowTrailHandler {
    // 16 colors
    private static final int[] COLORS = {
        0xFF0000, 0xFF4500, 0xFF8C00, 0xFFAA00, 0xFFFF00, 0xADFF2F,
        0x00FF00, 0x00FFAA, 0x0000FF, 0x4B0082, 0x8A2BE2, 0x800080,
        0xFF00FF, 0xFF1493, 0xFF69B4, 0xFF1493
    };
    private static final int TRAIL_LENGTH = 16;

    private static final Vec3[] prevPositions = new Vec3[TRAIL_LENGTH];
    private static int writeIndex = 0;
    private static int tickCounter = 0;

    public static boolean shouldRender(LocalPlayer player) {
        return EasterEggConfig.isEnabled();
    }

    public static void tick(LocalPlayer player) {
        if (!shouldRender(player)) return;

        tickCounter++;
        if (tickCounter >= 2) {
            tickCounter = 0;
            prevPositions[writeIndex] = player.position().add(0, 1, 0);
            writeIndex = (writeIndex + 1) % TRAIL_LENGTH;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        for (int i = 0; i < TRAIL_LENGTH; i++) {
            int idx = (writeIndex - 1 - i + TRAIL_LENGTH) % TRAIL_LENGTH;
            Vec3 pos = prevPositions[idx];
            if (pos == null) continue;

            int rgb = COLORS[i % COLORS.length];
            // DustParticleOptions(int color, float scale)
            ParticleOptions particle = new DustParticleOptions(rgb, 1.5f);
            mc.level.addParticle(particle, pos.x, pos.y, pos.z, 0, 0, 0);
        }
    }
}
