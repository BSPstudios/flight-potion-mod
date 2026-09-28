package com.bspstudio.bspmod.mixin;

import com.bspstudio.bspmod.EasterEggConfig;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在视频设置页（VideoSettingsScreen）追加「音乐弹窗」与「彩蛋」两个开关按钮。
 * 注意：1.21.9 中 Screen.init(Minecraft,int,int) 是 public final，无法注入；
 * 改为注入 OptionsSubScreen.init()（protected，非 final），再判断具体页面类型。
 */
@Mixin(OptionsSubScreen.class)
public abstract class MixinVideoSettingsScreen {

    @Inject(method = "init()V", at = @At("TAIL"))
    private void bsp$onSubScreenInit(CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        if (!(self instanceof VideoSettingsScreen)) return;

        boolean toast = EasterEggConfig.isMusicToastEnabled();
        boolean egg = EasterEggConfig.isEnabled();

        ((MixinScreen) (Object) this).bsp$addWidget(Button.builder(
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

        ((MixinScreen) (Object) this).bsp$addWidget(Button.builder(
                Component.translatable("screen.bspmod.option.egg")
                        .append(Component.translatable(egg ? "screen.bspmod.option.egg_on" : "screen.bspmod.option.egg_off")),
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
