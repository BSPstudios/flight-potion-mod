package com.bspstudio.bspmod.builder;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 建筑者速度选择屏幕。
 */
public class BuilderSpeedSelectScreen extends Screen {

    public BuilderSpeedSelectScreen() {
        super(Component.literal("建筑者速度设置"));
    }

    @Override
    protected void init() {
        int w = 200;
        int x = (this.width - w) / 2;
        int y = this.height / 2 - 60;

        addButton(x, y, "慢速（50%）", 0);
        addButton(x, y + 26, "正常（100%）", 1);
        addButton(x, y + 52, "快速（200%）", 2);
        addButton(x, y + 78, "命令速度（1000%，仅创造）", 3);
        addButton(x, y + 110, "取消", -1);
    }

    private void addButton(int x, int y, String label, int index) {
        this.addRenderableWidget(Button.builder(Component.literal(label), btn -> {
            if (index >= 0) {
                ClientPlayNetworking.send(new BuilderSpeedScreen.SpeedPayload(index));
            }
            this.onClose();
        }).bounds(x, y, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        super.render(g, mouseX, mouseY, partialTick);
    }
}
