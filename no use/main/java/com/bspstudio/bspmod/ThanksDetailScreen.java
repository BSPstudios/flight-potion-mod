package com.bspstudio.bspmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public class ThanksDetailScreen extends Screen {
    private final Screen parent;
    private final String name;
    private final String reason;

    public ThanksDetailScreen(Screen parent, String name, String reason) {
        super(Component.literal(name));
        this.parent = parent;
        this.name = name;
        this.reason = reason;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.literal("返回"),
                btn -> Minecraft.getInstance().setScreen(parent))
                .pos(this.width / 2 - 40, this.height / 2 + 40)
                .width(80)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(this.font, "感谢 " + name, this.width / 2, 30, 0xFFFF55);
        int y = 60;
        for (FormattedCharSequence line : this.font.split(Component.literal(reason), this.width - 60)) {
            graphics.drawCenteredString(this.font, line, this.width / 2, y, 0xFFFFFF);
            y += 14;
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
