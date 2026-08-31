package com.flightpotion.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class CreditsScreen extends Screen {
    private final Screen parent;

    public CreditsScreen(Screen parent) {
        super(Component.literal("鸣谢"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.addRenderableWidget(Button.builder(Component.literal("返回"), btn -> {
            this.minecraft.setScreen(this.parent);
        }).bounds(this.width / 2 - 100, this.height - 30, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(graphics, mouseX, mouseY, delta);
        super.render(graphics, mouseX, mouseY, delta);

        graphics.drawCenteredString(this.font, "盒芒拌凉屎工作室", this.width / 2, 40, 0x62D607);
        graphics.drawCenteredString(this.font, "感谢名单", this.width / 2, 70, 0xFFFFFF);
        graphics.drawCenteredString(this.font, "葱油拌面", this.width / 2, 90, 0xCCCCCC);
        graphics.drawCenteredString(this.font, "曹杰", this.width / 2, 110, 0xCCCCCC);
    }
}
