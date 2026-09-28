package com.bspstudio.bspmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.*;

public class ThanksScreen extends Screen {
    private final Screen parent;
    private static final String ACTIVATION_KEY = "3t4qi5drf04jk39";

    static final List<String[]> THANKS = List.of(
        new String[]{"screen.bspmod.thanks.p1_name", "screen.bspmod.thanks.p1_reason"},
        new String[]{"screen.bspmod.thanks.p2_name", "screen.bspmod.thanks.p2_reason"},
        new String[]{"screen.bspmod.thanks.p3_name", "screen.bspmod.thanks.p3_reason"},
        new String[]{"screen.bspmod.thanks.p4_name", "screen.bspmod.thanks.p4_reason"},
        new String[]{"screen.bspmod.thanks.p5_name", "screen.bspmod.thanks.p5_reason"},
        new String[]{"screen.bspmod.thanks.p6_name", "screen.bspmod.thanks.p6_reason"},
        new String[]{"screen.bspmod.thanks.p7_name", "screen.bspmod.thanks.p7_reason"},
        new String[]{"screen.bspmod.thanks.p8_name", "screen.bspmod.thanks.p8_reason"}
    );

    private static final int TITLE_Y = 6;
    private static final int LIST_TOP = 54;
    private static final int INPUT_AREA_H = 106;
    private static final int ROW_H = 24;
    private static final int NAME_W = 110;
    private static final int REASON_W = 200;
    private static final int GAP = 8;
    private int scroll = 0;

    private EditBox redeemField;
    private EditBox activateField;
    private String statusMsg = "";
    private boolean statusOk = true;
    private int fieldX, redeemY, activateY;

    public ThanksScreen(Screen parent) {
        super(Component.translatable("screen.bspmod.thanks.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        String savedRedeem = redeemField != null ? redeemField.getValue() : "";
        String savedActivate = activateField != null ? activateField.getValue() : "";

        clearWidgets();

        int listMaxY = this.height - INPUT_AREA_H;

        // Thanks list
        int pairW = NAME_W + GAP + REASON_W;
        int leftX = (this.width - pairW) / 2;
        for (int i = 0; i < THANKS.size(); i++) {
            int y = LIST_TOP + i * ROW_H - scroll;
            if (y < LIST_TOP - ROW_H || y > listMaxY) continue;

            addRenderableWidget(Button.builder(
                    Component.translatable(THANKS.get(i)[0]), btn -> {})
                    .pos(leftX, y).width(NAME_W).build());

            addRenderableWidget(Button.builder(
                    Component.translatable(THANKS.get(i)[1]), btn -> {})
                    .pos(leftX + NAME_W + GAP, y).width(REASON_W).build());
        }

        // Input area (fixed, anchored to bottom)
        fieldX = (this.width - 200) / 2;
        redeemY = this.height - 100;
        activateY = redeemY + 26;

        redeemField = new EditBox(this.font, fieldX, redeemY, 200, 20, Component.translatable("screen.bspmod.thanks.redeem"));
        redeemField.setMaxLength(64);
        redeemField.setValue(savedRedeem);
        addRenderableWidget(redeemField);

        activateField = new EditBox(this.font, fieldX, activateY, 200, 20, Component.translatable("screen.bspmod.thanks.activate"));
        activateField.setMaxLength(64);
        activateField.setValue(savedActivate);
        addRenderableWidget(activateField);

        addRenderableWidget(Button.builder(Component.translatable("screen.bspmod.thanks.activate_btn"), this::onActivate)
                .pos(this.width / 2 - 80, this.height - 26).width(70).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.bspmod.thanks.back"), btn -> onClose())
                .pos(this.width / 2 + 10, this.height - 26).width(70).build());
    }

    private void setStatus(String msg, boolean ok) {
        this.statusMsg = msg;
        this.statusOk = ok;
    }

    private void setStatus(Component msg, boolean ok) {
        this.statusMsg = msg.getString();
        this.statusOk = ok;
    }

    private void onActivate(Button btn) {
        if (KeyManager.isActivated()) {
            setStatus(Component.translatable("screen.bspmod.thanks.already_activated"), false);
            return;
        }
        String redeem = redeemField.getValue().trim();
        String activate = activateField.getValue().trim();
        if (redeem.isEmpty() || activate.isEmpty()) {
            setStatus(Component.translatable("screen.bspmod.thanks.empty_fields"), false);
            return;
        }
        if (!activate.equals(ACTIVATION_KEY)) {
            setStatus(Component.translatable("screen.bspmod.thanks.wrong_activate"), false);
            return;
        }
        if (!KeyManager.isValidCode(redeem)) {
            setStatus(Component.translatable("screen.bspmod.thanks.invalid_code"), false);
            return;
        }
        if (KeyManager.activate(redeem)) {
            setStatus(Component.translatable("screen.bspmod.thanks.success"), true);
        } else {
            setStatus(Component.translatable("screen.bspmod.thanks.failed"), false);
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        super.render(g, mx, my, delta);

        g.drawCenteredString(this.font, Component.translatable("screen.bspmod.thanks.title"), this.width / 2, TITLE_Y, 0x55FF55);
        g.drawCenteredString(this.font, Component.translatable("screen.bspmod.thanks.subtitle"), this.width / 2, 28, 0xAAAAAA);

        // Input labels
        g.drawString(this.font, Component.translatable("screen.bspmod.thanks.redeem"), fieldX - 52, redeemY + 6, 0xAAAAAA);
        g.drawString(this.font, Component.translatable("screen.bspmod.thanks.activate"), fieldX - 52, activateY + 6, 0xAAAAAA);

        // Status message (rendered last, above the buttons, always visible)
        if (!statusMsg.isEmpty()) {
            int c = statusOk ? 0x55FF55 : 0xFF5555;
            g.drawCenteredString(this.font, Component.literal(statusMsg), this.width / 2, this.height - 50, c);
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        int visRows = (this.height - INPUT_AREA_H - LIST_TOP) / ROW_H;
        int maxS = Math.max(0, THANKS.size() * ROW_H - visRows * ROW_H);
        scroll = Math.clamp(scroll - (int)(sy * 16), 0, maxS);
        init();
        return true;
    }

    @Override
    public void onClose() { Minecraft.getInstance().setScreen(parent); }
}
