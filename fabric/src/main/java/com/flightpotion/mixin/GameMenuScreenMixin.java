package com.flightpotion.mixin;

import com.flightpotion.screen.CreditsScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.GameMenuScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
public class GameMenuScreenMixin extends Screen {
    protected GameMenuScreenMixin(Component title) { super(title); }

    @Inject(method = "init", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        this.addRenderableWidget(Button.builder(Component.literal("鸣谢"), btn -> {
            this.minecraft.setScreen(new CreditsScreen(this));
        }).bounds(this.width / 2 - 100, this.height / 4 + 108, 200, 20).build());
    }
}
