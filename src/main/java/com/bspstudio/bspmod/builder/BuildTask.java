package com.bspstudio.bspmod.builder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 一个建筑任务。可以是挖掘、放置、替换。
 * 区域任务用两个角点表示最小包围盒；单方块任务 min == max。
 */
public class BuildTask {

    public enum Kind { MINE, PLACE, REPLACE }

    public Kind kind;
    public BlockPos min;
    public BlockPos max;
    public String blockId;      // 放置目标 / 替换后的新方块
    public String fromBlockId;  // 替换的原方块（仅 REPLACE 用）

    public BuildTask() {}

    public static BuildTask mine(BlockPos a, BlockPos b) {
        BuildTask t = new BuildTask();
        t.kind = Kind.MINE;
        t.min = new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
        t.max = new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
        return t;
    }

    public static BuildTask place(String blockId, BlockPos a, BlockPos b) {
        BuildTask t = new BuildTask();
        t.kind = Kind.PLACE;
        t.blockId = blockId;
        t.min = new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
        t.max = new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
        return t;
    }

    public static BuildTask replace(String from, String to, BlockPos a, BlockPos b) {
        BuildTask t = new BuildTask();
        t.kind = Kind.REPLACE;
        t.fromBlockId = from;
        t.blockId = to;
        t.min = new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
        t.max = new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
        return t;
    }

    public int volume() {
        int dx = max.getX() - min.getX() + 1;
        int dy = max.getY() - min.getY() + 1;
        int dz = max.getZ() - min.getZ() + 1;
        long v = (long) dx * dy * dz;
        return v > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) v;
    }

    public void write(ValueOutput out) {
        out.putString("kind", kind.name());
        out.putInt("minx", min.getX());
        out.putInt("miny", min.getY());
        out.putInt("minz", min.getZ());
        out.putInt("maxx", max.getX());
        out.putInt("maxy", max.getY());
        out.putInt("maxz", max.getZ());
        if (blockId != null) out.putString("block", blockId);
        if (fromBlockId != null) out.putString("from", fromBlockId);
    }

    public static BuildTask read(ValueInput in) {
        BuildTask t = new BuildTask();
        String k = in.getStringOr("kind", "");
        try { t.kind = Kind.valueOf(k); } catch (Exception e) { return null; }
        t.min = new BlockPos(in.getIntOr("minx", 0), in.getIntOr("miny", 0), in.getIntOr("minz", 0));
        t.max = new BlockPos(in.getIntOr("maxx", 0), in.getIntOr("maxy", 0), in.getIntOr("maxz", 0));
        t.blockId = in.getStringOr("block", "");
        t.fromBlockId = in.getStringOr("from", "");
        return t;
    }
}
