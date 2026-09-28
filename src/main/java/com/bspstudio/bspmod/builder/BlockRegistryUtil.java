package com.bspstudio.bspmod.builder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 方块 ID 解析工具。支持 "minecraft:stone" 或 "stone" 或带状态 "oak_log[axis=y]"。
 */
public class BlockRegistryUtil {

    /** 解析方块 ID（忽略方括号状态部分），返回 Block，找不到返回 null */
    public static Block resolveBlock(String id) {
        if (id == null) return null;
        String s = id.trim();
        int bracket = s.indexOf('[');
        if (bracket >= 0) s = s.substring(0, bracket);
        s = s.trim();
        if (s.isEmpty()) return null;

        ResourceLocation rl;
        if (s.contains(":")) {
            rl = ResourceLocation.tryParse(s);
        } else {
            rl = ResourceLocation.tryParse("minecraft:" + s);
        }
        if (rl == null) return null;

        return BuiltInRegistries.BLOCK.getOptional(rl).orElse(null);
    }

    /** 解析带状态的方块（含 [key=value]），返回默认状态；找不到返回 null */
    public static BlockState resolveState(String id) {
        Block b = resolveBlock(id);
        if (b == null) return null;
        BlockState state = b.defaultBlockState();
        int bracket = id.indexOf('[');
        if (bracket >= 0 && id.endsWith("]")) {
            String props = id.substring(bracket + 1, id.length() - 1);
            String[] pairs = props.split(",");
            for (String pair : pairs) {
                String[] kv = pair.split("=", 2);
                if (kv.length != 2) continue;
                String key = kv[0].trim();
                String val = kv[1].trim();
                var prop = b.getStateDefinition().getProperty(key);
                if (prop != null) {
                    var opt = prop.getValue(val);
                    if (opt.isPresent()) {
                        state = setValueRaw(state, prop, opt.get());
                    }
                }
            }
        }
        return state;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState setValueRaw(BlockState state, net.minecraft.world.level.block.state.properties.Property prop, Comparable value) {
        return state.setValue(prop, value);
    }

    /** 获取方块的稳定 ID 字符串（用于保存） */
    public static String getBlockId(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    }
}
