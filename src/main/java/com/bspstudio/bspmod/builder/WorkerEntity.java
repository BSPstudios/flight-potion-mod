package com.bspstudio.bspmod.builder;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 工作者实体。比建筑者小的灰色人形，只执行挖掘/放置/替换，不参与对话。
 */
public class WorkerEntity extends PathfinderMob {

    private UUID ownerBuilderUuid = null;
    private final List<BuildTask> tasks = new ArrayList<>();
    private int taskIndex = 0;      // 当前任务在 tasks 中的下标
    int blockOffset = 0;            // 当前任务内已处理到第几块（扁平索引）

    // 死亡特效状态
    private boolean dying = false;
    private int deathTicks = -1;

    public WorkerEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        try {
            this.setPersistenceRequired();
            System.out.println("[bspmod] WorkerEntity constructed OK, level=" + (level != null));
        } catch (Throwable t) {
            System.out.println("[bspmod] WorkerEntity constructor FAILED: " + t);
            t.printStackTrace();
        }
    }

    public void setOwnerBuilder(UUID uuid) { this.ownerBuilderUuid = uuid; }
    public UUID getOwnerBuilderUuid() { return ownerBuilderUuid; }
    public List<BuildTask> getTasks() { return tasks; }

    public boolean isDying() { return dying; }

    /** 触发清理死亡特效：播放图腾粒子+音效，20 tick 后消失，无掉落 */
    public void beginCleanup() {
        if (dying) return;
        dying = true;
        deathTicks = 20;
        this.setNoAi(true);
        this.setInvulnerable(true);
        if (this.level() instanceof ServerLevel sl) {
            double x = this.getX();
            double y = this.getY() + 1.0;
            double z = this.getZ();
            sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y, z, 60, 0.5, 1.0, 0.5, 0.1);
            this.level().playSound(null, this.blockPosition(), SoundEvents.TOTEM_USE,
                    SoundSource.NEUTRAL, 1.0f, 1.0f);
        }
    }

    @Override
    public void tick() {
        if (dying) {
            super.tick();
            if (!this.level().isClientSide()) {
                deathTicks--;
                if (deathTicks <= 0) {
                    this.discard();
                }
            }
            return;
        }
        super.tick();
        if (this.level().isClientSide()) return;
        if (!(this.level() instanceof ServerLevel sl)) return;

        TaskExecutor.tickWorker(this, sl);
    }

    @Override
    protected void registerGoals() {
        // 无 AI 目标
    }

    @Override
    public boolean shouldDropExperience() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        if (ownerBuilderUuid != null) out.putString("BspWorkerOwner", ownerBuilderUuid.toString());
        out.putInt("BspWorkerTaskIndex", taskIndex);
        out.putInt("BspWorkerBlockOffset", blockOffset);
        var list = out.childrenList("BspWorkerTasks");
        for (BuildTask t : tasks) {
            list.addChild().putString("t", t.kind.name() + "|" + t.min.getX() + "," + t.min.getY() + "," + t.min.getZ()
                    + "|" + t.max.getX() + "," + t.max.getY() + "," + t.max.getZ()
                    + "|" + (t.blockId == null ? "" : t.blockId)
                    + "|" + (t.fromBlockId == null ? "" : t.fromBlockId));
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        String owner = in.getStringOr("BspWorkerOwner", "");
        if (!owner.isEmpty()) { try { this.ownerBuilderUuid = UUID.fromString(owner); } catch (Exception ignored) {} }
        this.taskIndex = in.getIntOr("BspWorkerTaskIndex", 0);
        this.blockOffset = in.getIntOr("BspWorkerBlockOffset", 0);
        this.tasks.clear();
        var list = in.childrenListOrEmpty("BspWorkerTasks");
        for (ValueInput c : list) {
            String s = c.getStringOr("t", "");
            BuildTask t = parseTaskString(s);
            if (t != null) tasks.add(t);
        }
    }

    private static BuildTask parseTaskString(String s) {
        String[] parts = s.split("\\|", -1);
        if (parts.length < 5) return null;
        BuildTask t = new BuildTask();
        try { t.kind = BuildTask.Kind.valueOf(parts[0]); } catch (Exception e) { return null; }
        String[] a = parts[1].split(",");
        String[] b = parts[2].split(",");
        if (a.length != 3 || b.length != 3) return null;
        try {
            t.min = new net.minecraft.core.BlockPos(Integer.parseInt(a[0]), Integer.parseInt(a[1]), Integer.parseInt(a[2]));
            t.max = new net.minecraft.core.BlockPos(Integer.parseInt(b[0]), Integer.parseInt(b[1]), Integer.parseInt(b[2]));
        } catch (NumberFormatException e) { return null; }
        t.blockId = parts[3].isEmpty() ? null : parts[3];
        t.fromBlockId = parts[4].isEmpty() ? null : parts[4];
        return t;
    }
}
