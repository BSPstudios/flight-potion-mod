package com.flightpotion;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.function.Predicate;

public class ChestCommandHandler {

    public static int refreshContainer(ServerLevel world, BlockPos pos, ResourceLocation lootTable) {
        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof Container container)) return -1;
        if (lootTable == null && be instanceof ChestBlockEntity chest) {
            lootTable = chest.getLootTable();
        }
        if (lootTable == null) {
            container.clearContent();
            be.setChanged();
            world.sendBlockUpdated(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
            return 0;
        }
        container.clearContent();
        LootTable table = world.getServer().getLootData().getLootTable(lootTable);
        LootContext ctx = new LootContext.Builder(world)
                .withRandom(world.random)
                .create(LootContextParamSets.CHEST);
        table.fill(container, ctx);
        be.setChanged();
        world.sendBlockUpdated(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
        return 1;
    }

    public static int addToContainer(Container container, ItemStack stack) {
        for (int i = 0; i < container.getContainerSize() && !stack.isEmpty(); i++) {
            ItemStack slot = container.getItem(i);
            if (slot.isEmpty()) {
                container.setItem(i, stack.copy());
                stack.setCount(0);
                break;
            } else if (ItemStack.isSameItemSameComponents(slot, stack)) {
                int canAdd = Math.min(stack.getCount(), slot.getMaxStackSize() - slot.getCount());
                if (canAdd > 0) {
                    slot.grow(canAdd);
                    stack.shrink(canAdd);
                }
            }
        }
        return stack.isEmpty() ? 1 : 2;
    }

    public static int removeItems(Container container, ItemStack target) {
        Predicate<ItemStack> predicate = s -> ItemStack.isSameItemSameComponents(s, target);
        int remaining = target.getCount();
        for (int i = 0; i < container.getContainerSize() && remaining > 0; i++) {
            ItemStack slot = container.getItem(i);
            if (predicate.test(slot)) {
                int canRemove = Math.min(remaining, slot.getCount());
                slot.shrink(canRemove);
                remaining -= canRemove;
                if (slot.isEmpty()) container.setItem(i, ItemStack.EMPTY);
            }
        }
        return remaining;
    }

    public static void clearContainer(Container container) {
        container.clearContent();
    }

    public static boolean detectItem(Container container, ItemStack target, int minCount) {
        Predicate<ItemStack> predicate = s -> ItemStack.isSameItemSameComponents(s, target);
        int total = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack slot = container.getItem(i);
            if (predicate.test(slot)) {
                total += slot.getCount();
                if (total >= minCount) return true;
            }
        }
        return total >= minCount;
    }

    public static Container resolveContainer(ServerLevel world, String str) {
        ServerPlayer player = world.getServer().getPlayerList().getPlayerByName(str);
        if (player != null) return player.getInventory();

        try {
            String[] parts = str.split(" ");
            if (parts.length == 3) {
                int x = Integer.parseInt(parts[0]);
                int y = Integer.parseInt(parts[1]);
                int z = Integer.parseInt(parts[2]);
                BlockPos pos = new BlockPos(x, y, z);
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof Container c) return c;
            }
        } catch (NumberFormatException ignored) {}
        return null;
    }

    public static BlockPos getLookedContainer(net.minecraft.world.entity.player.Player player) {
        var hit = player.pick(5, 0, false);
        return hit.getBlockPos();
    }
}