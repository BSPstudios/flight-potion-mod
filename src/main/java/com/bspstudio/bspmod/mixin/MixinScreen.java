package com.bspstudio.bspmod.mixin;

import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** 提供对 Screen.addRenderableWidget（protected）的调用入口。 */
@Mixin(Screen.class)
public abstract class MixinScreen {

    @Invoker("addRenderableWidget")
    public abstract <T extends GuiEventListener & Renderable & NarratableEntry> T bsp$addWidget(T widget);
}
