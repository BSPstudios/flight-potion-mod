package com.bspstudio.bspmod.builder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 任务执行核心逻辑：挖掘 / 放置 / 替换，红石块区域扫描，工作者分配与召唤。
 * 采用增量进度跟踪：每个任务维护一个扁平索引（taskProgress / blockOffset），
 * 每 tick 处理一定数量的方块，处理完后任务出队。
 */
public class TaskExecutor {

    public static final int WORK_PER_BLOCK = 1;      // 每处理一个方块 = 1 工作量
    public static final int WORK_PER_WORKER = 50;    // 每 50 工作量召唤一个工作者
    public static final int SEARCH_RADIUS = 16;      // 红石块搜索半径

    // ---------- 红石块区域扫描 ----------

    /** 在建筑者周围搜索红石块，返回坐标列表（按距建筑者距离排序） */
    public static List<BlockPos> findRedstoneBlocks(BuilderEntity builder, ServerLevel level) {
        List<BlockPos> result = new ArrayList<>();
        BlockPos center = builder.blockPosition();
        int r = SEARCH_RADIUS;
        for (int x = center.getX() - r; x <= center.getX() + r; x++) {
            for (int y = center.getY() - r; y <= center.getY() + r; y++) {
                for (int z = center.getZ() - r; z <= center.getZ() + r; z++) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (level.getBlockState(p).is(Blocks.REDSTONE_BLOCK)) {
                        result.add(p);
                    }
                }
            }
        }
        result.sort(Comparator.comparingDouble(p -> p.distSqr(center)));
        return result;
    }

    /** 根据红石块顶点列表计算最小包围盒；数量不是 2/4/8 返回 null */
    public static BlockPos[] redstoneBoundingBox(List<BlockPos> redstones) {
        int n = redstones.size();
        if (n != 2 && n != 4 && n != 8) return null;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos p : redstones) {
            minX = Math.min(minX, p.getX());
            minY = Math.min(minY, p.getY());
            minZ = Math.min(minZ, p.getZ());
            maxX = Math.max(maxX, p.getX());
            maxY = Math.max(maxY, p.getY());
            maxZ = Math.max(maxZ, p.getZ());
        }
        return new BlockPos[]{ new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ) };
    }

    // ---------- 扁平化工具 ----------

    private static int volume(BuildTask t) {
        int dx = t.max.getX() - t.min.getX() + 1;
        int dy = t.max.getY() - t.min.getY() + 1;
        int dz = t.max.getZ() - t.min.getZ() + 1;
        long v = (long) dx * dy * dz;
        return v > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) v;
    }

    private static BlockPos indexToPos(BuildTask t, int index) {
        int dx = t.max.getX() - t.min.getX() + 1;
        int dz = t.max.getZ() - t.min.getZ() + 1;
        int plane = dx * dz;
        int y = index / plane;
        int rem = index % plane;
        int x = rem / dz;
        int z = rem % dz;
        return new BlockPos(t.min.getX() + x, t.min.getY() + y, t.min.getZ() + z);
    }

    // ---------- 建筑者 tick：增量执行 ----------

    public static void tickExecutor(BuilderEntity builder, ServerLevel level) {
        List<BuildTask> queue = builder.getTaskQueue();
        if (queue.isEmpty()) {
            onTasksComplete(builder);
            return;
        }

        BuildTask t = queue.get(0);
        if (t == null) { queue.remove(0); builder.setTaskProgress(0); return; }

        boolean creative = builder.getSyncedGameType() == GameType.CREATIVE;
        int budget = creative ? Integer.MAX_VALUE : blocksPerTick(builder);

        int total = volume(t);
        int processed = 0;
        int idx = builder.getTaskProgress();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        while (idx < total && processed < budget) {
            BlockPos p = indexToPos(t, idx);
            pos.set(p);
            if (applyAt(builder, level, t, pos)) {
                processed++;
            }
            idx++;
        }
        builder.setTaskProgress(idx);
        builder.addWork(processed);

        maybeSpawnWorker(builder, level);

        if (idx >= total) {
            // 任务完成
            queue.remove(0);
            builder.setTaskProgress(0);
            // REPLACE 任务完成后清除剩余红石块
            if (t.kind == BuildTask.Kind.REPLACE) {
                clearRedstoneInRegion(builder, level, t);
            }
        }
    }

    private static int blocksPerTick(BuilderEntity builder) {
        float speed = builder.getSpeedMultiplier();
        if (speed <= 0.5f) return 1;
        if (speed <= 1.0f) return 1;
        if (speed <= 2.0f) return 2;
        return 4;
    }

    private static boolean applyAt(BuilderEntity builder, ServerLevel level, BuildTask t, BlockPos pos) {
        BlockState cur = level.getBlockState(pos);
        switch (t.kind) {
            case MINE:
                if (cur.isAir()) return false;
                level.destroyBlock(pos, false, builder, 512);
                return true;
            case PLACE:
                if (!cur.isAir() && !cur.is(Blocks.REDSTONE_BLOCK)) return false;
                BlockState place = BlockRegistryUtil.resolveState(t.blockId);
                if (place == null) return false;
                level.setBlock(pos, place, 3);
                return true;
            case REPLACE:
                if (cur.is(Blocks.REDSTONE_BLOCK)) return false; // 跳过红石块
                BlockState from = BlockRegistryUtil.resolveState(t.fromBlockId);
                BlockState to = BlockRegistryUtil.resolveState(t.blockId);
                if (from == null || to == null) return false;
                if (!cur.getBlock().equals(from.getBlock())) return false;
                level.setBlock(pos, to, 3);
                return true;
        }
        return false;
    }

    private static void clearRedstoneInRegion(BuilderEntity builder, ServerLevel level, BuildTask t) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = t.min.getX(); x <= t.max.getX(); x++) {
            for (int y = t.min.getY(); y <= t.max.getY(); y++) {
                for (int z = t.min.getZ(); z <= t.max.getZ(); z++) {
                    pos.set(x, y, z);
                    if (level.getBlockState(pos).is(Blocks.REDSTONE_BLOCK)) {
                        level.destroyBlock(pos, false, builder, 512);
                    }
                }
            }
        }
    }

    private static void onTasksComplete(BuilderEntity builder) {
        builder.setDialogueState(BuilderEntity.State.TASK_DONE);
        if (builder.getOwnerUuid() != null && builder.level() instanceof ServerLevel level) {
            var p = level.getServer().getPlayerList().getPlayer(builder.getOwnerUuid());
            if (p != null) {
                p.sendSystemMessage(Component.literal("§e[建筑者] §f任务已全部完成！需要保存这次建筑吗？（回复「保存」或「不保存」）"));
            }
        }
    }

    // ---------- 工作者召唤 ----------

    public static void maybeSpawnWorker(BuilderEntity builder, ServerLevel level) {
        int should = builder.getWorkDone() / WORK_PER_WORKER;
        if (should > builder.getWorkersSpawned()) {
            int newCount = should - builder.getWorkersSpawned();
            for (int i = 0; i < newCount; i++) {
                builder.setWorkersSpawned(builder.getWorkersSpawned() + 1);
                spawnWorker(builder, level);
                if (builder.getOwnerUuid() != null) {
                    var p = level.getServer().getPlayerList().getPlayer(builder.getOwnerUuid());
                    if (p != null) {
                        p.sendSystemMessage(Component.literal("§a[建筑者] §f已完成 " + builder.getWorkDone()
                                + " 个工作量，召唤第 " + builder.getWorkersSpawned() + " 个帮手协助。"));
                    }
                }
            }
        }
    }

    public static WorkerEntity spawnWorker(BuilderEntity builder, ServerLevel level) {
        WorkerEntity w = BspModEntities.WORKER.spawn(level, builder.blockPosition(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        w.setOwnerBuilder(builder.getUUID());
        builder.getWorkerUuids().add(w.getUUID());
        return w;
    }

    // ---------- 任务分配：平均分配 ----------

    /** 把 builder 的剩余任务平均分配给所有工作者（工作者复制任务执行） */
    public static void distributeTasks(BuilderEntity builder) {
        List<WorkerEntity> workers = new ArrayList<>();
        ServerLevel level = (ServerLevel) builder.level();
        for (UUID u : builder.getWorkerUuids()) {
            var e = level.getEntity(u);
            if (e instanceof WorkerEntity w && !w.isRemoved() && !w.isDying()) {
                workers.add(w);
            }
        }
        if (workers.isEmpty()) return;

        List<BuildTask> all = new ArrayList<>(builder.getTaskQueue());
        if (all.isEmpty()) return;

        int idx = 0;
        for (BuildTask t : all) {
            WorkerEntity w = workers.get(idx % workers.size());
            w.getTasks().add(t);
            idx++;
        }
    }

    // ---------- 工作者 tick：执行自己的任务 ----------

    public static void tickWorker(WorkerEntity worker, ServerLevel level) {
        List<BuildTask> tasks = worker.getTasks();
        if (tasks.isEmpty()) {
            returnToBuilder(worker, level);
            return;
        }
        BuildTask t = tasks.get(0);
        // 工作者高效执行，每 tick 处理 4 块
        int budget = 4;
        int total = volume(t);
        int processed = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        // WorkerEntity 用 blockOffset 记录进度，但这里简化：直接把整个区域一次执行完（最多区域不会太大）
        // 为避免卡顿，这里每 tick 只处理 budget 块，进度用 WorkerEntity 的字段
        while (worker.blockOffset < total && processed < budget) {
            BlockPos p = indexToPos(t, worker.blockOffset);
            pos.set(p);
            if (applyAtWorker(worker, level, t, pos)) {
                processed++;
            }
            worker.blockOffset++;
        }
        if (worker.blockOffset >= total) {
            tasks.remove(0);
            worker.blockOffset = 0;
            if (t.kind == BuildTask.Kind.REPLACE) {
                clearRedstoneInRegionWorker(worker, level, t);
            }
        }
    }

    private static void returnToBuilder(WorkerEntity worker, ServerLevel level) {
        if (worker.getOwnerBuilderUuid() != null) {
            var b = level.getEntity(worker.getOwnerBuilderUuid());
            if (b != null) {
                worker.getNavigation().moveTo(b.getX(), b.getY(), b.getZ(), 1.0);
            }
        }
    }

    private static boolean applyAtWorker(WorkerEntity worker, ServerLevel level, BuildTask t, BlockPos pos) {
        BlockState cur = level.getBlockState(pos);
        switch (t.kind) {
            case MINE:
                if (cur.isAir()) return false;
                level.destroyBlock(pos, false, worker, 512);
                return true;
            case PLACE:
                if (!cur.isAir() && !cur.is(Blocks.REDSTONE_BLOCK)) return false;
                BlockState place = BlockRegistryUtil.resolveState(t.blockId);
                if (place == null) return false;
                level.setBlock(pos, place, 3);
                return true;
            case REPLACE:
                if (cur.is(Blocks.REDSTONE_BLOCK)) return false;
                BlockState from = BlockRegistryUtil.resolveState(t.fromBlockId);
                BlockState to = BlockRegistryUtil.resolveState(t.blockId);
                if (from == null || to == null) return false;
                if (!cur.getBlock().equals(from.getBlock())) return false;
                level.setBlock(pos, to, 3);
                return true;
        }
        return false;
    }

    private static void clearRedstoneInRegionWorker(WorkerEntity worker, ServerLevel level, BuildTask t) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = t.min.getX(); x <= t.max.getX(); x++) {
            for (int y = t.min.getY(); y <= t.max.getY(); y++) {
                for (int z = t.min.getZ(); z <= t.max.getZ(); z++) {
                    pos.set(x, y, z);
                    if (level.getBlockState(pos).is(Blocks.REDSTONE_BLOCK)) {
                        level.destroyBlock(pos, false, worker, 512);
                    }
                }
            }
        }
    }
}
