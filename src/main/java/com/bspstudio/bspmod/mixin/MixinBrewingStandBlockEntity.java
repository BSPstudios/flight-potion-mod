package com.bspstudio.bspmod.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 修复原版 doBrew 的一个缺陷：
 * 原版顺序是 {@code itemstack.shrink(1)} 然后 {@code itemstack.getItem().getCraftingRemainder()}。
 * 当原料数量本身就是 1（如奶桶）时，shrink 后数量归零，{@code ItemStack.getItem()} 会返回
 * {@code Items.AIR}，而 AIR 没有 crafting remainder —— 于是奶桶 → 空桶的剩余物直接丢失。
 * 原版没有「奶桶酿造」配方，所以这个缺陷一直没暴露；本模组用奶桶酿防中毒药水后，空桶会凭空消失。
 *
 * 修复方式：在 HEAD（原料尚未被消耗、数量仍 > 0）先取到真正的 crafting remainder；
 * 在 TAIL 若原料槽已被清空（说明原版因 AIR 缺陷丢掉了剩余物）则补回原料槽。
 * 其余情况原版已自行处理（堆叠 > 1 时掉落），不重复干预。
 */
@Mixin(BrewingStandBlockEntity.class)
public abstract class MixinBrewingStandBlockEntity {

    /** 暂存本次 doBrew 真正的 crafting remainder（doBrew 为静态方法，用 ThreadLocal 跨注入点传递）。 */
    private static final ThreadLocal<ItemStack> bsp$trueRemainder =
            ThreadLocal.withInitial(() -> ItemStack.EMPTY);

    @Inject(method = "doBrew", at = @At("HEAD"))
    private static void bsp$captureRemainder(Level level, BlockPos pos, NonNullList<ItemStack> items,
                                             CallbackInfo ci) {
        ItemStack ingredient = items.get(3);
        bsp$trueRemainder.set(ingredient.isEmpty()
                ? ItemStack.EMPTY
                : ingredient.getItem().getCraftingRemainder());
    }

    @Inject(method = "doBrew", at = @At("TAIL"))
    private static void bsp$restoreRemainder(Level level, BlockPos pos, NonNullList<ItemStack> items,
                                             CallbackInfo ci) {
        ItemStack remainder = bsp$trueRemainder.get();
        bsp$trueRemainder.remove();
        // 仅当原版确实丢掉了剩余物（原料槽被清空）时补回；否则原版已处理，勿重复。
        if (!remainder.isEmpty() && items.get(3).isEmpty()) {
            items.set(3, remainder.copy());
        }
    }
}
