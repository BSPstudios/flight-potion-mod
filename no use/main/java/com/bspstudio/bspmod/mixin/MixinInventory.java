package com.bspstudio.bspmod.mixin;

import com.bspstudio.bspmod.BspMod;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Inventory.class)
public abstract class MixinInventory {
    @Shadow @Final public Player player;

    @Inject(method = "add(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("RETURN"))
    private void bsp$onAddDisc22(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() == null || !cir.getReturnValue()) return;
        if (BspMod.DISC_22 == null) return;
        if (!stack.is(BspMod.DISC_22)) return;
        if (!(player instanceof ServerPlayer sp)) return;

        // 检查是否已经穿过末地折跃门
        ResourceLocation gateId = ResourceLocation.fromNamespaceAndPath("minecraft", "end/enter_end_gateway");
        AdvancementHolder gate = sp.level().getServer().getAdvancements().get(gateId);
        if (gate != null && sp.getAdvancements().getOrStartProgress(gate).isDone()) {
            return; // 已穿过折跃门，不授予
        }

        // 授予进度「末地城，我飞来啦」
        ResourceLocation advId = ResourceLocation.fromNamespaceAndPath(BspMod.MOD_ID, "end/end_city_fly");
        AdvancementHolder adv = sp.level().getServer().getAdvancements().get(advId);
        if (adv != null) {
            sp.getAdvancements().award(adv, "disc_22");
        }
    }
}
