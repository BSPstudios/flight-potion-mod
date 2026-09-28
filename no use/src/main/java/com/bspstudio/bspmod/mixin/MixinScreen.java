package com.bspstudio.bspmod.mixin;

import com.bspstudio.bspmod.EasterEggConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class MixinScreen {
    @Invoker("addRenderableWidget")
    public abstract <T extends GuiEventListener & Renderable & NarratableEntry> T callAddRenderableWidget(T widget);

    @Inject(method = "init(Lnet/minecraft/client/Minecraft;II)V", at = @At("TAIL"))
    private void bsp$onScreenInit(CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        if (!(self instanceof VideoSettingsScreen)) return;

        boolean on = EasterEggConfig.isEnabled();
        boolean toast = EasterEggConfig.isMusicToastEnabled();
        callAddRenderableWidget(Button.builder(
                Component.translatable("screen.bspmod.option.music_toast")
                        .append(Component.translatable(toast ? "screen.bspmod.option.egg_on" : "screen.bspmod.option.egg_off")),
                btn -> {
                    EasterEggConfig.setMusicToastEnabled(!EasterEggConfig.isMusicToastEnabled());
                    btn.setMessage(Component.translatable("screen.bspmod.option.music_toast")
                            .append(Component.translatable(EasterEggConfig.isMusicToastEnabled() ? "screen.bspmod.option.egg_on" : "screen.bspmod.option.egg_off")));
                })
                .pos(self.width / 2 - 50, self.height - 52)
                .width(100)
                .build());
        callAddRenderableWidget(Button.builder(
                Component.translatable("screen.bspmod.option.egg")
                        .append(Component.translatable(on ? "screen.bspmod.option.egg_on" : "screen.bspmod.option.egg_off")),
                btn -> {
                    EasterEggConfig.toggle();
                    btn.setMessage(Component.translatable("screen.bspmod.option.egg")
                            .append(Component.translatable(EasterEggConfig.isEnabled() ? "screen.bspmod.option.egg_on" : "screen.bspmod.option.egg_off")));
                })
                .pos(self.width / 2 - 50, self.height - 30)
                .width(100)
                .build());
    }
}
