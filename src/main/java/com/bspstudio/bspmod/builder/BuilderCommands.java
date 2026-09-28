package com.bspstudio.bspmod.builder;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * 建筑者指令系统 + 聊天 @名称 对话。
 */
public class BuilderCommands {

    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("builder")
                    .then(literal("summon")
                            .then(argument("name", StringArgumentType.word())
                                    .executes(ctx -> summonBuilder(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                    .then(literal("cleanup")
                            .executes(ctx -> cleanupWorkers(ctx.getSource())))
            );
        });
    }

    private static int summonBuilder(CommandSourceStack src, String name) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Only players can summon a builder"));
            return 0;
        }
        ServerLevel level = src.getLevel();
        BuilderEntity b = BspModEntities.BUILDER.spawn(level, player.blockPosition(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        b.setOwner(player);
        b.setBuilderName(name);
        b.syncGameMode(player.gameMode());
        src.sendSuccess(() -> Component.literal("Builder " + name + " summoned, game mode synced to "
                + (player.gameMode().isCreative() ? "creative" : "survival")), false);
        return 1;
    }

    private static int cleanupWorkers(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Only players can clean up workers"));
            return 0;
        }
        ServerLevel level = src.getLevel();
        BuilderEntity nearest = findNearestBuilder(player);
        if (nearest == null) {
            src.sendFailure(Component.literal("No builder nearby"));
            return 0;
        }
        int count = cleanupWorkersOfBuilder(nearest, level, true);
        src.sendSuccess(() -> Component.literal("Cleaned up " + count + " workers"), false);
        return count;
    }

    public static int cleanupWorkersOfBuilder(BuilderEntity builder, ServerLevel level, boolean silent) {
        List<WorkerEntity> workers = new ArrayList<>();
        for (var uuid : new ArrayList<>(builder.getWorkerUuids())) {
            Entity e = level.getEntity(uuid);
            if (e instanceof WorkerEntity w) {
                workers.add(w);
            }
        }
        if (!silent && builder.getOwnerUuid() != null) {
            var p = level.getServer().getPlayerList().getPlayer(builder.getOwnerUuid());
            if (p != null) {
                p.sendSystemMessage(Component.literal("The helpers are done, time to rest."));
            }
        }
        int count = 0;
        for (WorkerEntity w : workers) {
            w.beginCleanup();
            count++;
        }
        builder.getWorkerUuids().clear();
        return count;
    }

    // ---------- 聊天消息处理 ----------

    public static boolean handleChat(ServerPlayer player, String raw) {
        String msg = raw.trim();
        if (!msg.startsWith("@")) return false;

        String rest = msg.substring(1).trim();
        String targetName = null;
        String command;

        if (rest.isEmpty()) return false;
        int space = rest.indexOf(' ');
        if (space < 0) {
            targetName = rest;
            command = "";
        } else {
            String first = rest.substring(0, space);
            String tail = rest.substring(space + 1).trim();
            if (isCommandKeyword(first)) {
                targetName = null;
                command = rest;
            } else {
                targetName = first;
                command = tail;
            }
        }

        ServerLevel level = player.level();
        BuilderEntity builder = findBuilder(player, targetName);
        if (builder == null) {
            player.sendSystemMessage(Component.literal("No builder found (use /builder summon <name> first)"));
            return true;
        }

        if (command.isEmpty()) {
            showBuilderStatus(player, builder);
            return true;
        }

        return executeCommand(player, builder, command);
    }

    private static boolean isCommandKeyword(String s) {
        switch (s) {
            case "mine": case "place": case "replace":
            case "follow": case "stop": case "cleanup":
            case "save": case "nosave":
            case "挖掘": case "放置": case "替换":
            case "跟随我": case "停下": case "清理":
            case "保存": case "不保存":
                return true;
            default:
                return false;
        }
    }

    private static BuilderEntity findBuilder(ServerPlayer player, String name) {
        if (name == null) return findNearestBuilder(player);
        ServerLevel level = player.level();
        AABB box = player.getBoundingBox().inflate(64);
        List<BuilderEntity> list = level.getEntities(BspModEntities.BUILDER, box, e -> true);
        for (BuilderEntity b : list) {
            if (b.getBuilderName().equalsIgnoreCase(name)) return b;
        }
        return null;
    }

    private static BuilderEntity findNearestBuilder(ServerPlayer player) {
        ServerLevel level = player.level();
        AABB box = player.getBoundingBox().inflate(64);
        List<BuilderEntity> list = level.getEntities(BspModEntities.BUILDER, box, e -> true);
        if (list.isEmpty()) return null;
        list.sort(Comparator.comparingDouble(b -> b.distanceToSqr(player)));
        return list.get(0);
    }

    public static void showBuilderStatus(ServerPlayer player, BuilderEntity b) {
        player.sendSystemMessage(Component.literal("[" + b.getBuilderName() + "] tasks: " + b.getTaskQueue().size()
                + ", work: " + b.getWorkDone() + ", workers: " + b.getWorkersSpawned()
                + ", speed: " + formatSpeed(b.getSpeedMultiplier())));
    }

    private static String formatSpeed(float v) {
        if (v <= 0.5f) return "slow";
        if (v <= 1.0f) return "normal";
        if (v <= 2.0f) return "fast";
        return "command";
    }

    // ---------- 指令执行 ----------

    private static boolean executeCommand(ServerPlayer player, BuilderEntity b, String command) {
        String[] parts = command.trim().split("\\s+");
        if (parts.length == 0) return false;

        String action = parts[0];
        ServerLevel level = player.level();

        switch (action) {
            case "挖掘": case "mine": {
                BlockPos[] region = parseRegion(player, b, level, parts, 1);
                if (region == null) return true;
                b.getTaskQueue().add(BuildTask.mine(region[0], region[1]));
                b.setDialogueState(BuilderEntity.State.EXECUTING);
                reply(player, b, "mine task added");
                return true;
            }
            case "放置": case "place": {
                if (parts.length < 2) { reply(player, b, "usage: place <blockId> <coords/redstone>"); return true; }
                String blockId = parts[1];
                BlockPos[] region = parseRegion(player, b, level, parts, 2);
                if (region == null) return true;
                if (BlockRegistryUtil.resolveBlock(blockId) == null) {
                    reply(player, b, "unknown block: " + blockId);
                    return true;
                }
                b.getTaskQueue().add(BuildTask.place(blockId, region[0], region[1]));
                b.setDialogueState(BuilderEntity.State.EXECUTING);
                reply(player, b, "place task added");
                return true;
            }
            case "替换": case "replace": {
                if (parts.length < 4) { reply(player, b, "usage: replace <from> 为 <to> <coords>"); return true; }
                String from = parts[1];
                if (!parts[2].equals("为") && !parts[2].equals("with")) {
                    reply(player, b, "usage: replace <from> 为 <to> <coords>");
                    return true;
                }
                String to = parts[3];
                BlockPos[] region = parseRegion(player, b, level, parts, 4);
                if (region == null) return true;
                if (BlockRegistryUtil.resolveBlock(from) == null || BlockRegistryUtil.resolveBlock(to) == null) {
                    reply(player, b, "unknown block: " + from + " or " + to);
                    return true;
                }
                b.getTaskQueue().add(BuildTask.replace(from, to, region[0], region[1]));
                b.setDialogueState(BuilderEntity.State.EXECUTING);
                reply(player, b, "replace task added");
                return true;
            }
            case "跟随我": case "follow": {
                b.getNavigation().moveTo(player, 1.0);
                reply(player, b, "following");
                return true;
            }
            case "停下": case "stop": {
                b.getNavigation().stop();
                reply(player, b, "stopped");
                return true;
            }
            case "清理": case "cleanup": {
                int n = cleanupWorkersOfBuilder(b, level, false);
                reply(player, b, "cleaned up " + n + " workers");
                return true;
            }
            case "保存": case "save": {
                if (b.getDialogueState() == BuilderEntity.State.TASK_DONE) {
                    b.setDialogueState(BuilderEntity.State.ASKING_SAVE_ID);
                    reply(player, b, "please tell me the building ID");
                } else {
                    reply(player, b, "no building to save");
                }
                return true;
            }
            case "不保存": case "nosave": {
                if (b.getDialogueState() == BuilderEntity.State.TASK_DONE) {
                    b.setDialogueState(BuilderEntity.State.IDLE);
                    reply(player, b, "ok, not saved");
                }
                return true;
            }
        }
        return false;
    }

    /** 解析区域：支持坐标 x1 y1 z1 到 x2 y2 z2，或红石块顶点，或单方块 x y z。 */
    private static BlockPos[] parseRegion(ServerPlayer player, BuilderEntity b, ServerLevel level, String[] parts, int startIdx) {
        int remaining = parts.length - startIdx;
        if (remaining >= 7 && parts[startIdx + 3].equals("到")) {
            try {
                int x1 = Integer.parseInt(parts[startIdx]);
                int y1 = Integer.parseInt(parts[startIdx + 1]);
                int z1 = Integer.parseInt(parts[startIdx + 2]);
                int x2 = Integer.parseInt(parts[startIdx + 4]);
                int y2 = Integer.parseInt(parts[startIdx + 5]);
                int z2 = Integer.parseInt(parts[startIdx + 6]);
                return new BlockPos[]{ new BlockPos(x1, y1, z1), new BlockPos(x2, y2, z2) };
            } catch (NumberFormatException e) {
                reply(player, b, "coords must be integers");
                return null;
            }
        }

        if (remaining == 3) {
            try {
                int x = Integer.parseInt(parts[startIdx]);
                int y = Integer.parseInt(parts[startIdx + 1]);
                int z = Integer.parseInt(parts[startIdx + 2]);
                return new BlockPos[]{ new BlockPos(x, y, z), new BlockPos(x, y, z) };
            } catch (NumberFormatException e) {
                // fall through to redstone
            }
        }

        List<BlockPos> reds = TaskExecutor.findRedstoneBlocks(b, level);
        BlockPos[] box = TaskExecutor.redstoneBoundingBox(reds);
        if (box == null) {
            reply(player, b, "redstone block count incorrect (need 2/4/8, found " + reds.size() + ")");
            return null;
        }
        return box;
    }

    private static void reply(ServerPlayer player, BuilderEntity b, String msg) {
        player.sendSystemMessage(Component.literal("[" + b.getBuilderName() + "] " + msg));
    }
}
