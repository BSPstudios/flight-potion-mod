package com.flightpotion.fabric;

import com.flightpotion.ChestCommandHandler;
import com.flightpotion.FlightMusic;
import com.flightpotion.FlightPotions;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLivingEntityEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.functions.SetPotionFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FlightPotionFabric implements ModInitializer {

    private static final Random RANDOM = new Random();

    @Override
    public void onInitialize() {
        FlightPotions.registerBrewingRecipes();

        // 战利品注入
        LootTableEvents.MODIFY.register((key, tableBuilder, source) -> {
            if (source.isBuiltin() && key.location().equals(ResourceLocation.withDefaultNamespace("chests/shipwreck_supply"))) {
                LootPool pool = LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .add(LootItem.lootTableItem(Items.SPLASH_POTION)
                                .apply(SetPotionFunction.setPotion(FlightPotions.FLIGHT_POTION.value()))
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 1)))
                                .setWeight(1))
                        .build();
                tableBuilder.pool(pool);
            }
        });

        // 命令注册
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("chest")
                    .requires(s -> s.hasPermission(3))

                    .then(Commands.literal("load")
                            .executes(ctx -> execLoad(ctx, null, null))
                            .then(Commands.argument("loot_table", StringArgumentType.string())
                                    .executes(ctx -> execLoad(ctx, ctx.getArgument("loot_table", String.class), null))
                                    .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                            .executes(ctx -> execLoad(ctx, ctx.getArgument("loot_table", String.class), ctx.getArgument("pos", BlockPos.class)))
                                    )
                            )
                            .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                    .executes(ctx -> execLoad(ctx, null, ctx.getArgument("pos", BlockPos.class)))
                            )
                            .then(createDetectBlock(1))
                    )

                    .then(Commands.literal("add")
                            .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                    .then(Commands.argument("item", ItemArgument.item(registryAccess))
                                            .executes(ctx -> execAdd(ctx, 1))
                                            .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                                    .executes(ctx -> execAdd(ctx, ctx.getArgument("count", Integer.class)))
                                                    .then(createDetectBlock(1))
                                            )
                                    )
                            )
                    )

                    .then(Commands.literal("remove")
                            .then(Commands.argument("source", StringArgumentType.string())
                                    .then(Commands.argument("target", StringArgumentType.string())
                                            .then(Commands.argument("item", ItemArgument.item(registryAccess))
                                                    .executes(ctx -> execRemove(ctx, 1))
                                                    .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                                            .executes(ctx -> execRemove(ctx, ctx.getArgument("count", Integer.class)))
                                                            .then(createDetectBlock(1))
                                                    )
                                            )
                                    )
                            )
                    )

                    .then(Commands.literal("clear")
                            .executes(ctx -> {
                                ctx.getSource().getPlayerOrException().getInventory().clearContent();
                                ctx.getSource().sendSuccess(() -> Component.literal("背包已清空"), true);
                                return 1;
                            })
                            .then(Commands.argument("target", StringArgumentType.string())
                                    .executes(ctx -> {
                                        Container cont = ChestCommandHandler.resolveContainer(ctx.getSource().getLevel(), ctx.getArgument("target", String.class));
                                        if (cont == null) { ctx.getSource().sendFailure(Component.literal("目标无效")); return 0; }
                                        ChestCommandHandler.clearContainer(cont);
                                        ctx.getSource().sendSuccess(() -> Component.literal("容器已清空"), true);
                                        return 1;
                                    })
                            )
                    )

                    .then(Commands.literal("minus")
                            .then(Commands.argument("target", StringArgumentType.string())
                                    .then(Commands.argument("item", ItemArgument.item(registryAccess))
                                            .executes(ctx -> execMinus(ctx, 1))
                                            .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                                    .executes(ctx -> execMinus(ctx, ctx.getArgument("count", Integer.class)))
                                                    .then(createDetectBlock(1))
                                            )
                                    )
                            )
                    )

                    .then(Commands.literal("detect")
                            .then(Commands.argument("target", StringArgumentType.string())
                                    .then(Commands.argument("item", ItemArgument.item(registryAccess))
                                            .executes(ctx -> execDetect(ctx, 1, false))
                                            .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                                    .executes(ctx -> execDetect(ctx, ctx.getArgument("count", Integer.class), false))
                                                    .then(Commands.argument("redstone", BoolArgumentType.bool())
                                                            .executes(ctx -> execDetect(ctx, ctx.getArgument("count", Integer.class), ctx.getArgument("redstone", Boolean.class)))
                                                    )
                                            )
                                            .then(Commands.argument("redstone", BoolArgumentType.bool())
                                                    .executes(ctx -> execDetect(ctx, 1, ctx.getArgument("redstone", Boolean.class)))
                                            )
                                    )
                            )
                    )
            );
        });

        // 女巫受到伤害时喝/喷飞行药水
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, amount, applied, blocked) -> {
            if (entity instanceof Witch witch && !witch.hasEffect(FlightPotions.FLIGHT_EFFECT)) {
                if (amount >= 10.0 && amount <= 17.0) {
                    if (RANDOM.nextBoolean()) {
                        Potion potion = getRandomFlightPotion();
                        MobEffectInstance effect = new MobEffectInstance(potion.getEffects().getFirst());
                        witch.addEffect(effect);
                        witch.playSound(SoundEvents.WITCH_DRINK, 1.0F, 1.0F);
                    } else {
                        Potion potion = getRandomFlightPotion();
                        ItemStack splashPotion = new ItemStack(Items.SPLASH_POTION);
                        // 1.21 药水数据组件
                        splashPotion.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));
                        ThrownPotion thrownPotion = new ThrownPotion(witch.level(), witch);
                        thrownPotion.setItem(splashPotion);
                        thrownPotion.setPos(witch.getX(), witch.getEyeY() - 0.1, witch.getZ());
                        thrownPotion.shoot(0, -0.5, 0, 0.5F, 0);
                        witch.level().addFreshEntity(thrownPotion);
                        witch.playSound(SoundEvents.WITCH_THROW, 1.0F, 1.0F);
                    }
                }
            }
        });

        // 客户端音乐逻辑
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            ClientTickEvents.END_CLIENT_TICK.register(client -> FlightMusic.tryPlayFlightMusic());
        }
    }

    /** 随机返回一种飞行药水 */
    private static Potion getRandomFlightPotion() {
        return switch (RANDOM.nextInt(4)) {
            case 0 -> FlightPotions.FLIGHT_POTION.value();
            case 1 -> FlightPotions.LONG_FLIGHT_POTION.value();
            case 2 -> FlightPotions.STRONG_FLIGHT_POTION.value();
            default -> FlightPotions.LONG_STRONG_FLIGHT_POTION.value();
        };
    }

    private static ArgumentBuilder<CommandSourceStack, ?> createDetectBlock(int level) {
        String targetArg = "detectTarget" + level;
        String itemArg = "detectItem" + level;
        String countArg = "detectCount" + level;

        var detectNode = Commands.literal("detect")
                .then(Commands.argument(targetArg, StringArgumentType.string())
                        .then(Commands.argument(itemArg, ItemArgument.item(net.minecraft.core.registries.BuiltInRegistries.ITEM))
                                .executes(ctx -> checkConditionsAndExecute(ctx, level, true))
                                .then(Commands.argument(countArg, IntegerArgumentType.integer(1))
                                        .executes(ctx -> checkConditionsAndExecute(ctx, level, true))
                                        .then(Commands.literal("while")
                                                .executes(ctx -> checkConditionsAndExecute(ctx, level, true))
                                                .then(createDetectBlock(level + 1))
                                        )
                                        .then(Commands.literal("until")
                                                .executes(ctx -> checkConditionsAndExecute(ctx, level, false))
                                                .then(createDetectBlock(level + 1))
                                        )
                                )
                                .then(Commands.literal("while")
                                        .executes(ctx -> checkConditionsAndExecute(ctx, level, true))
                                        .then(createDetectBlock(level + 1))
                                )
                                .then(Commands.literal("until")
                                        .executes(ctx -> checkConditionsAndExecute(ctx, level, false))
                                        .then(createDetectBlock(level + 1))
                                )
                        )
                );

        return Commands.literal("{")
                .then(detectNode)
                .then(Commands.literal("}")
                        .executes(ctx -> checkConditionsAndExecute(ctx, level, true))
                        .then(Commands.literal("{")
                                .then(createDetectBlock(level + 1))
                        )
                );
    }

    private static int checkConditionsAndExecute(CommandContext<CommandSourceStack> ctx, int maxLevel, boolean lastMode) {
        ServerLevel world = ctx.getSource().getLevel();
        List<Condition> conditions = new ArrayList<>();

        for (int i = 1; i <= maxLevel; i++) {
            String targetArg = "detectTarget" + i;
            String itemArg = "detectItem" + i;
            String countArg = "detectCount" + i;
            try {
                String target = ctx.getArgument(targetArg, String.class);
                ItemInput input = ctx.getArgument(itemArg, ItemInput.class);
                int count = 1;
                try { count = ctx.getArgument(countArg, Integer.class); } catch (IllegalArgumentException ignored) {}
                ItemStack stack = input.createItemStack(count, false);

                boolean expected = (i == maxLevel) ? lastMode : true;
                conditions.add(new Condition(target, stack, count, expected));
            } catch (IllegalArgumentException e) { break; }
        }

        boolean allPassed = conditions.stream().allMatch(c -> {
            Container cont = ChestCommandHandler.resolveContainer(world, c.target);
            if (cont == null) return false;
            boolean found = ChestCommandHandler.detectItem(cont, c.stack, c.count);
            return c.expected == found;
        });

        if (!allPassed) return 0;

        return executeOriginalCommand(ctx);
    }

    private static int executeOriginalCommand(CommandContext<CommandSourceStack> ctx) {
        try {
            if (ctx.getInput().contains(" add ")) {
                BlockPos pos = ctx.getArgument("pos", BlockPos.class);
                int count = 1;
                try { count = ctx.getArgument("count", Integer.class); } catch (IllegalArgumentException ignored) {}
                return execAdd(ctx, count);
            } else if (ctx.getInput().contains(" remove ")) {
                int count = 1;
                try { count = ctx.getArgument("count", Integer.class); } catch (IllegalArgumentException ignored) {}
                return execRemove(ctx, count);
            } else if (ctx.getInput().contains(" minus ")) {
                int count = 1;
                try { count = ctx.getArgument("count", Integer.class); } catch (IllegalArgumentException ignored) {}
                return execMinus(ctx, count);
            } else if (ctx.getInput().contains(" load ")) {
                return execLoad(ctx, null, null);
            }
        } catch (Exception e) {}
        return 0;
    }

    private static class Condition {
        final String target;
        final ItemStack stack;
        final int count;
        final boolean expected;
        Condition(String t, ItemStack s, int c, boolean e) { target = t; stack = s; count = c; expected = e; }
    }

    private static int execLoad(CommandContext<CommandSourceStack> ctx, String lootStr, BlockPos pos) {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        BlockPos target = (pos != null) ? pos : ChestCommandHandler.getLookedContainer(player);
        ResourceLocation loot = (lootStr != null) ? ResourceLocation.tryParse(lootStr) : null;
        int res = ChestCommandHandler.refreshContainer(player.serverLevel(), target, loot);
        ctx.getSource().sendSuccess(() -> Component.literal("操作完成"), true);
        return res >= 0 ? 1 : 0;
    }

    private static int execAdd(CommandContext<CommandSourceStack> ctx, int count) {
        BlockPos pos = ctx.getArgument("pos", BlockPos.class);
        ItemInput input = ctx.getArgument("item", ItemInput.class);
        Container cont = (Container) ctx.getSource().getLevel().getBlockEntity(pos);
        if (cont == null) { ctx.getSource().sendFailure(Component.literal("目标不是容器")); return 0; }
        ChestCommandHandler.addToContainer(cont, input.createItemStack(count, false));
        ctx.getSource().sendSuccess(() -> Component.literal("物品已添加"), true);
        return 1;
    }

    private static int execRemove(CommandContext<CommandSourceStack> ctx, int count) {
        String src = ctx.getArgument("source", String.class);
        String tgt = ctx.getArgument("target", String.class);
        ItemInput input = ctx.getArgument("item", ItemInput.class);
        Container srcCont = ChestCommandHandler.resolveContainer(ctx.getSource().getLevel(), src);
        Container tgtCont = ChestCommandHandler.resolveContainer(ctx.getSource().getLevel(), tgt);
        if (srcCont == null || tgtCont == null) { ctx.getSource().sendFailure(Component.literal("源或目标无效")); return 0; }
        ItemStack stack = input.createItemStack(count, false);
        if (ChestCommandHandler.removeItems(srcCont, stack) > 0) {
            ctx.getSource().sendFailure(Component.literal("源物品不足")); return 0;
        }
        ChestCommandHandler.addToContainer(tgtCont, stack);
        ctx.getSource().sendSuccess(() -> Component.literal("转移成功"), true);
        return 1;
    }

    private static int execMinus(CommandContext<CommandSourceStack> ctx, int count) {
        String target = ctx.getArgument("target", String.class);
        ItemInput input = ctx.getArgument("item", ItemInput.class);
        Container cont = ChestCommandHandler.resolveContainer(ctx.getSource().getLevel(), target);
        if (cont == null) { ctx.getSource().sendFailure(Component.literal("目标无效")); return 0; }
        int remaining = ChestCommandHandler.removeItems(cont, input.createItemStack(count, false));
        ctx.getSource().sendSuccess(() -> Component.literal("已减去物品，剩余未扣除: " + remaining), true);
        return 1;
    }

    private static int execDetect(CommandContext<CommandSourceStack> ctx, int count, boolean redstone) {
        String target = ctx.getArgument("target", String.class);
        ItemInput input = ctx.getArgument("item", ItemInput.class);
        Container cont = ChestCommandHandler.resolveContainer(ctx.getSource().getLevel(), target);
        if (cont == null) { ctx.getSource().sendFailure(Component.literal("目标无效")); return 0; }
        boolean found = ChestCommandHandler.detectItem(cont, input.createItemStack(count, false), count);
        ctx.getSource().sendSuccess(() -> Component.literal(found ? "1" : "0"), redstone);
        return found ? 1 : 0;
    }
}
