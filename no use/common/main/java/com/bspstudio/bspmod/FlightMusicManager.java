package com.bspstudio.bspmod;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class FlightMusicManager {
    private static final Random RANDOM = new Random();
    private static FlightMusicInstance currentTrack = null;
    private static final int CHECK_INTERVAL = 40;
    private static int tickCounter = 0;

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        boolean hasFlight = player.hasEffect(BspMod.FLIGHT_EFFECT);
        if (!hasFlight) {
            stop();
            return;
        }

        if (currentTrack != null) {
            if (mc.getSoundManager().isActive(currentTrack)) return;
            currentTrack = null;
        }

        tickCounter++;
        if (tickCounter < CHECK_INTERVAL) return;
        tickCounter = 0;

        trySelectTrack(mc, player);
    }

    private static void trySelectTrack(Minecraft mc, LocalPlayer player) {
        ClientLevel level = mc.level;
        if (level == null) return;
        BlockPos pos = player.blockPosition();

        List<WeightedTrack> candidates = new ArrayList<>();

        // 1. Swap morning sunshine — swamp, 37.5%
        if (isBiome(level, pos, Biomes.SWAMP))
            add(candidates, "swap-morning-sunshine", 37.5);

        // 2-3-5-7-10: End general (no dragon nearby, no end city)
        if (isInEndGeneral(player, level)) {
            add(candidates, "shining-end-stone", 20);
            add(candidates, "echo-of-the-end", 20);
            add(candidates, "end-stone-piece", 20);
            add(candidates, "voids-lament", 20);
            add(candidates, "obsid-echo", 100);
        }

        // 4. Dragon split — shulker/shulker box nearby in End, 100%
        if (player.level().dimension() == Level.END && isShulkerNearby(player, 32))
            add(candidates, "dragon-split", 100);

        // 6. Copper forest
        boolean isTrial = isTrialChambers(level, pos);
        boolean lotsCopper = countCopper(level, pos, 32) > 400;
        if (isTrial) add(candidates, "copper-forest", 10);
        if (lotsCopper) add(candidates, "copper-forest", 75);

        // 8. Purple coneflower — end highlands, 100%
        if (isBiome(level, pos, Biomes.END_HIGHLANDS))
            add(candidates, "purple-coneflower", 100);

        // 9. Citecho void — end city/ship, 100%
        if (player.level().dimension() == Level.END && isEndCityBlocks(level, pos, 64))
            add(candidates, "citecho-void", 100);

        // 11-12-13. Triky glom
        boolean inLush = isBiome(level, pos, Biomes.LUSH_CAVES);
        if (isTrial) {
            add(candidates, "triky-glom", 20);
            add(candidates, "triky-glom-2", 20);
            add(candidates, "triky-glom-3", 20);
        }
        if (inLush) {
            add(candidates, "triky-glom", 6.25);
            add(candidates, "triky-glom-2", 6.25);
            add(candidates, "triky-glom-3", 6.25);
        }

        // 14. Village sunset
        if (isVillageNearby(level, pos, 128) && isSunset(level))
            add(candidates, "village-sunset", 87.5);

        // 15. Lightly lash cave — lush, 25%
        if (inLush) add(candidates, "lightly-lash-cave", 25);

        // 16. Drip drop cave
        boolean inDripstone = isBiome(level, pos, Biomes.DRIPSTONE_CAVES);
        if (inDripstone) add(candidates, "drip-drop-cave", 75);
        if (inLush) add(candidates, "drip-drop-cave", 12.5);

        // 17. Oak track
        if (isBiome(level, pos, Biomes.FOREST)) add(candidates, "oak-track", 12.5);
        if (isBiome(level, pos, Biomes.BIRCH_FOREST)) add(candidates, "oak-track", 3.125);
        if (isBiome(level, pos, Biomes.WARM_OCEAN) && !player.isSwimming())
            add(candidates, "oak-track", 1.25);

        // 18. Buzzle bees
        if (player.level().dimension() == Level.OVERWORLD && hasBeesNearby(level, pos, 64))
            add(candidates, "buzzle-bees", 8.75);

        if (candidates.isEmpty()) return;

        // Weighted random
        double totalWeight = candidates.stream().mapToDouble(w -> w.weight).sum();
        double roll = RANDOM.nextDouble() * totalWeight;
        double cumulative = 0;
        SoundEvent selected = null;
        for (WeightedTrack wt : candidates) {
            cumulative += wt.weight;
            if (roll <= cumulative) { selected = wt.sound; break; }
        }
        if (selected == null) selected = candidates.get(candidates.size() - 1).sound;

        playTrack(mc, selected);
    }

    private static void add(List<WeightedTrack> list, String name, double weight) {
        SoundEvent s = BspMod.FLIGHT_MUSIC_MAP.get(name);
        if (s != null && weight > 0) list.add(new WeightedTrack(s, weight));
    }

    private static void playTrack(Minecraft mc, SoundEvent sound) {
        stop();
        // 阻止原版背景音乐
        mc.getMusicManager().stopPlaying();
        currentTrack = new FlightMusicInstance(sound);
        mc.getSoundManager().play(currentTrack);
        // 音乐弹窗：显示作者
        if (EasterEggConfig.isMusicToastEnabled()) {
            mc.gui.setTitle(Component.literal("BSP Studios"));
            mc.gui.setSubtitle(Component.literal(sound.location().getPath()));
        }
    }

    /** Dragonrend anthem：攻击末影龙时 50.67% 概率触发 */
    public static void triggerDragonrend() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;
        if (RANDOM.nextDouble() < 0.5067) {
            SoundEvent s = BspMod.FLIGHT_MUSIC_MAP.get("dragonrend-anthem");
            if (s != null) playTrack(mc, s);
        }
    }

    /** Voidcry crescendo：播放 22# 唱片时 100% 触发（弹窗 + 阻止原版音乐，唱片本体声音由 jukebox 播放） */
    public static void notifyDisc22Played() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;
        mc.getMusicManager().stopPlaying();
        if (EasterEggConfig.isMusicToastEnabled()) {
            mc.gui.setTitle(Component.literal("BSP Studios"));
            mc.gui.setSubtitle(Component.literal("Voidcry Crescendo"));
        }
    }

    private static void stop() {
        if (currentTrack != null) {
            Minecraft.getInstance().getSoundManager().stop(currentTrack);
            currentTrack = null;
        }
    }

    public static boolean isCustomMusicPlaying() {
        if (currentTrack == null) return false;
        Minecraft mc = Minecraft.getInstance();
        return mc.getSoundManager() != null && mc.getSoundManager().isActive(currentTrack);
    }

    // --- Condition helpers ---

    private static boolean isBiome(ClientLevel level, BlockPos pos, net.minecraft.resources.ResourceKey<Biome> key) {
        return level.getBiome(pos).is(key);
    }

    private static boolean isInEndGeneral(LocalPlayer player, ClientLevel level) {
        if (player.level().dimension() != Level.END) return false;
        if (player.isCreative()) return true;
        // Dragon nearby → exclude
        for (Entity e : player.level().getEntitiesOfClass(EnderDragon.class,
                player.getBoundingBox().inflate(128))) return false;
        // End city nearby → exclude (check for purpur blocks)
        if (isEndCityBlocks(level, player.blockPosition(), 128)) return false;
        return true;
    }

    private static boolean isShulkerNearby(LocalPlayer player, double range) {
        return !player.level().getEntitiesOfClass(Shulker.class,
                player.getBoundingBox().inflate(range)).isEmpty();
    }

    private static boolean isEndCityBlocks(ClientLevel level, BlockPos center, int radius) {
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        int step = 8;
        for (int x = -radius; x <= radius; x += step) {
            for (int y = -radius; y <= radius; y += step) {
                for (int z = -radius; z <= radius; z += step) {
                    mp.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    BlockState st = level.getBlockState(mp);
                    if (st.is(Blocks.PURPUR_BLOCK) || st.is(Blocks.END_STONE_BRICKS))
                        return true;
                }
            }
        }
        return false;
    }

    private static boolean isTrialChambers(ClientLevel level, BlockPos center) {
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        int step = 8;
        int radius = 64;
        int count = 0;
        for (int x = -radius; x <= radius; x += step) {
            for (int y = -radius; y <= radius; y += step) {
                for (int z = -radius; z <= radius; z += step) {
                    mp.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    BlockState st = level.getBlockState(mp);
                    if (st.is(Blocks.TUFF_BRICKS) || st.is(Blocks.CHISELED_TUFF) ||
                        st.is(Blocks.CHISELED_TUFF_BRICKS) || st.is(Blocks.TUFF)) count++;
                    if (count >= 5) return true;
                }
            }
        }
        return false;
    }

    private static int countCopper(ClientLevel level, BlockPos center, int radius) {
        int count = 0;
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        int step = 4;
        for (int x = -radius; x <= radius; x += step) {
            for (int y = -radius; y <= radius; y += step) {
                for (int z = -radius; z <= radius; z += step) {
                    mp.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    Block b = level.getBlockState(mp).getBlock();
                    if (b == Blocks.COPPER_BLOCK || b == Blocks.CUT_COPPER ||
                        b == Blocks.EXPOSED_COPPER || b == Blocks.WEATHERED_COPPER ||
                        b == Blocks.OXIDIZED_COPPER || b == Blocks.WAXED_COPPER_BLOCK ||
                        b == Blocks.WAXED_CUT_COPPER || b == Blocks.WAXED_EXPOSED_COPPER ||
                        b == Blocks.WAXED_WEATHERED_COPPER || b == Blocks.WAXED_OXIDIZED_COPPER ||
                        b == Blocks.CHISELED_COPPER || b == Blocks.EXPOSED_CHISELED_COPPER ||
                        b == Blocks.WEATHERED_CHISELED_COPPER || b == Blocks.OXIDIZED_CHISELED_COPPER ||
                        b == Blocks.COPPER_GRATE || b == Blocks.EXPOSED_COPPER_GRATE ||
                        b == Blocks.WEATHERED_COPPER_GRATE || b == Blocks.OXIDIZED_COPPER_GRATE ||
                        b.getDescriptionId().contains("copper"))
                        count++;
                }
            }
        }
        return count;
    }

    private static boolean isVillageNearby(ClientLevel level, BlockPos center, int radius) {
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        int step = 16;
        for (int x = -radius; x <= radius; x += step) {
            for (int z = -radius; z <= radius; z += step) {
                mp.set(center.getX() + x, center.getY(), center.getZ() + z);
                // Scan down to surface
                while (mp.getY() > -64 && level.getBlockState(mp).isAir()) {
                    mp.move(0, -1, 0);
                }
                BlockState st = level.getBlockState(mp);
                if (st.is(Blocks.BELL) || st.is(Blocks.LECTERN)) return true;
                // Also check for hay bales and campfires
                mp.set(center.getX() + x, center.getY(), center.getZ() + z);
                for (int dy = -8; dy <= 8; dy += 2) {
                    mp.setY(center.getY() + dy);
                    if (level.getBlockState(mp).is(Blocks.HAY_BLOCK) ||
                        level.getBlockState(mp).is(Blocks.CAMPFIRE)) return true;
                }
            }
        }
        return false;
    }

    private static boolean isSunset(ClientLevel level) {
        long time = level.getDayTime() % 24000;
        return time >= 12200 && time <= 13600;
    }

    private static boolean hasBeesNearby(ClientLevel level, BlockPos center, int radius) {
        for (Entity e : level.entitiesForRendering()) {
            if (e instanceof Bee && e.distanceToSqr(center.getX(), center.getY(), center.getZ()) < radius * radius)
                return true;
        }
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        int step = 8;
        for (int x = -radius; x <= radius; x += step) {
            for (int y = -radius; y <= radius; y += step) {
                for (int z = -radius; z <= radius; z += step) {
                    mp.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    if (level.getBlockState(mp).getBlock() instanceof BeehiveBlock) {
                        var be = level.getBlockEntity(mp);
                        if (be instanceof BeehiveBlockEntity hive && !hive.isEmpty())
                            return true;
                    }
                }
            }
        }
        return false;
    }

    // --- Inner classes ---

    private static class WeightedTrack {
        final SoundEvent sound;
        final double weight;
        WeightedTrack(SoundEvent s, double w) { this.sound = s; this.weight = w; }
    }

    private static class FlightMusicInstance extends AbstractTickableSoundInstance {
        FlightMusicInstance(SoundEvent sound) {
            super(sound, SoundSource.MUSIC, RandomSource.create());
            this.volume = 0.5f;
            this.pitch = 1.0f;
            this.looping = false;
            this.relative = true;
        }

        @Override public void tick() {}
    }
}
