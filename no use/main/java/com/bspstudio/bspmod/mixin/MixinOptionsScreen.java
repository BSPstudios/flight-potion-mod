package com.bspstudio.bspmod.mixin;

import com.bspstudio.bspmod.ThanksScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OptionsScreen.class)
public abstract class MixinOptionsScreen extends Screen {
    protected MixinOptionsScreen(Component title) { super(title); }

    @Inject(method = "init", at = @At("TAIL"))
    private void bsp$addThanksButton(CallbackInfo ci) {
        this.addRenderableWidget(Button.builder(Component.translatable("screen.bspmod.option.thanks"),
                btn -> Minecraft.getInstance().setScreen(new ThanksScreen(this)))
                .pos(10, this.height - 26)
                .width(60)
                .build());
    }
}
