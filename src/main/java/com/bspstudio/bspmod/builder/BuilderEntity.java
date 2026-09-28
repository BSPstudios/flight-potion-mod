package com.bspstudio.bspmod.builder;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 建筑者实体。玩家通过命令召唤，通过聊天框 @名称 指令 交互。
 * 负责维护任务列表、对话状态机，并将任务分配给工作者。
 */
public class BuilderEntity extends PathfinderMob {

    // 对话状态
    public enum State { IDLE, ASKING_MORE, CONFIRMING, EXECUTING, TASK_DONE, ASKING_SAVE, ASKING_SAVE_ID }

    private static final EntityDataAccessor<Integer> DATA_STATE =
            SynchedEntityData.defineId(BuilderEntity.class, EntityDataSerializers.INT);

    private String builderName = "";
    private UUID ownerUuid = null;
    private String ownerName = "";
    private GameType syncedGameType = GameType.SURVIVAL;

    // 任务与工作进度
    private final List<BuildTask> taskQueue = new ArrayList<>();
    private int taskReplyCount = 0;      // 每 5~10 次回复后汇总确认
    private int workDone = 0;            // 累计工作量（每50召唤一个工作者）
    private int workersSpawned = 0;      // 已召唤工作者数量
    private final List<UUID> workerUuids = new ArrayList<>();
    private int taskProgress = 0;        // 当前任务已处理方块数（扁平索引）

    // 速度档位: 0.5 / 1.0 / 2.0 / 10.0
    private float speedMultiplier = 1.0f;

    // 建筑保存
    private final List<SavedBuilding> savedBuildings = new ArrayList<>();

    public BuilderEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        try {
            this.setPersistenceRequired();
            this.setCustomNameVisible(true);
            System.out.println("[bspmod] BuilderEntity constructed OK, level=" + (level != null));
        } catch (Throwable t) {
            System.out.println("[bspmod] BuilderEntity constructor FAILED: " + t);
            t.printStackTrace();
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STATE, State.IDLE.ordinal());
    }

    // ---------- 名称与所有者 ----------

    public String getBuilderName() { return builderName; }
    public void setBuilderName(String name) {
        this.builderName = name;
        this.setCustomName(Component.literal(name));
    }
    public UUID getOwnerUuid() { return ownerUuid; }
    public void setOwner(ServerPlayer player) {
        this.ownerUuid = player.getUUID();
        this.ownerName = player.getGameProfile().name();
    }
    public String getOwnerName() { return ownerName; }

    public boolean isOwnedBy(ServerPlayer player) {
        return ownerUuid != null && ownerUuid.equals(player.getUUID());
    }

    // ---------- 游戏模式同步 ----------

    public void syncGameMode(GameType mode) {
        this.syncedGameType = mode;
    }
    public GameType getSyncedGameType() { return syncedGameType; }

    // ---------- 速度档位 ----------

    public float getSpeedMultiplier() { return speedMultiplier; }
    public void setSpeedMultiplier(float v) { this.speedMultiplier = v; }

    // ---------- 对话状态机 ----------

    public State getDialogueState() { return State.values()[this.entityData.get(DATA_STATE)]; }
    public void setDialogueState(State s) { this.entityData.set(DATA_STATE, s.ordinal()); }

    // ---------- 任务队列 ----------

    public List<BuildTask> getTaskQueue() { return taskQueue; }
    public int getWorkDone() { return workDone; }
    public void addWork(int n) { this.workDone += n; }
    public int getWorkersSpawned() { return workersSpawned; }
    public void setWorkersSpawned(int n) { this.workersSpawned = n; }
    public List<UUID> getWorkerUuids() { return workerUuids; }

    public int getTaskReplyCount() { return taskReplyCount; }
    public void setTaskReplyCount(int n) { this.taskReplyCount = n; }

    public int getTaskProgress() { return taskProgress; }
    public void setTaskProgress(int n) { this.taskProgress = n; }

    public List<SavedBuilding> getSavedBuildings() { return savedBuildings; }

    // ---------- 交互（点击） ----------

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide() && player instanceof ServerPlayer sp) {
            BuilderCommands.showBuilderStatus(sp, this);
        }
        return InteractionResult.SUCCESS;
    }

    // ---------- 主 tick：执行任务 ----------

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) return;
        if (!(this.level() instanceof ServerLevel sl)) return;

        if (getDialogueState() == State.EXECUTING) {
            TaskExecutor.tickExecutor(this, sl);
        }
    }

    @Override
    protected void registerGoals() {
        // 建筑者不需要 AI 目标
    }

    // ---------- 持久化 ----------

    @Override
    public void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putString("BspBuilderName", builderName);
        if (ownerUuid != null) out.putString("BspBuilderOwner", ownerUuid.toString());
        out.putString("BspBuilderOwnerName", ownerName);
        out.putInt("BspBuilderState", getDialogueState().ordinal());
        out.putInt("BspWorkDone", workDone);
        out.putInt("BspWorkersSpawned", workersSpawned);
        out.putInt("BspTaskProgress", taskProgress);
        out.putFloat("BspSpeed", speedMultiplier);
        out.putInt("BspGameType", syncedGameType.getId());

        // 任务列表
        var tasks = out.childrenList("BspTasks");
        for (BuildTask t : taskQueue) {
            var child = tasks.addChild();
            t.write(child);
        }

        // 工作者 UUID 列表
        var workers = out.childrenList("BspWorkers");
        for (UUID u : workerUuids) {
            var c = workers.addChild();
            c.putString("uuid", u.toString());
        }

        // 保存的建筑列表
        var saves = out.childrenList("BspBuildings");
        for (SavedBuilding b : savedBuildings) {
            var c = saves.addChild();
            b.write(c);
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        this.builderName = in.getStringOr("BspBuilderName", "");
        String owner = in.getStringOr("BspBuilderOwner", "");
        if (!owner.isEmpty()) { try { this.ownerUuid = UUID.fromString(owner); } catch (Exception ignored) {} }
        this.ownerName = in.getStringOr("BspBuilderOwnerName", "");
        int st = in.getIntOr("BspBuilderState", 0);
        if (st >= 0 && st < State.values().length) this.setDialogueState(State.values()[st]);
        this.workDone = in.getIntOr("BspWorkDone", 0);
        this.workersSpawned = in.getIntOr("BspWorkersSpawned", 0);
        this.taskProgress = in.getIntOr("BspTaskProgress", 0);
        this.speedMultiplier = in.getFloatOr("BspSpeed", 1.0f);
        int gt = in.getIntOr("BspGameType", GameType.SURVIVAL.getId());
        this.syncedGameType = GameType.byId(gt);
        if (!builderName.isEmpty()) this.setCustomName(Component.literal(builderName));

        // 任务列表
        this.taskQueue.clear();
        var tasks = in.childrenListOrEmpty("BspTasks");
        for (ValueInput c : tasks) {
            BuildTask t = BuildTask.read(c);
            if (t != null) this.taskQueue.add(t);
        }

        // 工作者
        this.workerUuids.clear();
        var workers = in.childrenListOrEmpty("BspWorkers");
        for (ValueInput c : workers) {
            String u = c.getStringOr("uuid", "");
            if (!u.isEmpty()) { try { this.workerUuids.add(UUID.fromString(u)); } catch (Exception ignored) {} }
        }

        // 建筑
        this.savedBuildings.clear();
        var saves = in.childrenListOrEmpty("BspBuildings");
        for (ValueInput c : saves) {
            SavedBuilding b = SavedBuilding.read(c);
            if (b != null) this.savedBuildings.add(b);
        }
    }
}
