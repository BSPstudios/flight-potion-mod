package com.bspstudio.bspmod.builder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 保存的建筑：一个区域 + 方块数据。
 * 这里以「区域范围 + 方块 ID 列表」形式保存，简化实现：
 * 保存时记录整块区域每个坐标的方块 ID，重建时按坐标放置。
 */
public class SavedBuilding {

    public String id;
    public BlockPos min;
    public BlockPos max;
    // 展平后的方块 ID 列表，按 x,y,z 顺序存储（从 min 到 max）
    public String[] blocks;

    public SavedBuilding() {}

    public void write(ValueOutput out) {
        out.putString("id", id == null ? "" : id);
        out.putInt("minx", min.getX());
        out.putInt("miny", min.getY());
        out.putInt("minz", min.getZ());
        out.putInt("maxx", max.getX());
        out.putInt("maxy", max.getY());
        out.putInt("maxz", max.getZ());
        int[] lens = new int[blocks.length];
        for (int i = 0; i < blocks.length; i++) lens[i] = blocks[i].length();
        out.putIntArray("lens", lens);
        StringBuilder sb = new StringBuilder();
        for (String s : blocks) sb.append(s).append('\u0000');
        out.putString("data", sb.toString());
    }

    public static SavedBuilding read(ValueInput in) {
        SavedBuilding b = new SavedBuilding();
        b.id = in.getStringOr("id", "");
        b.min = new BlockPos(in.getIntOr("minx", 0), in.getIntOr("miny", 0), in.getIntOr("minz", 0));
        b.max = new BlockPos(in.getIntOr("maxx", 0), in.getIntOr("maxy", 0), in.getIntOr("maxz", 0));
        int[] lens = in.getIntArray("lens").orElse(new int[0]);
        String data = in.getStringOr("data", "");
        String[] parts = data.split("\u0000", -1);
        b.blocks = new String[lens.length];
        for (int i = 0; i < lens.length; i++) {
            b.blocks[i] = i < parts.length ? parts[i] : "";
        }
        return b;
    }
}
