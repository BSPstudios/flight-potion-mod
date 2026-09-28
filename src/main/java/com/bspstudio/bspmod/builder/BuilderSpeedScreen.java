package com.bspstudio.bspmod.builder;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * 速度设置。按键（默认 B）打开速度屏幕，选档后发包到服务端保存。
 */
public class BuilderSpeedScreen {

    public static KeyMapping speedKey;

    // 网络包：速度档位 (0=慢 1=正常 2=快 3=命令)
    public record SpeedPayload(int speedIndex) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SpeedPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("bspmod", "builder_speed"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SpeedPayload> CODEC =
                StreamCodec.composite(ByteBufCodecs.INT, SpeedPayload::speedIndex, SpeedPayload::new);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(SpeedPayload.TYPE, SpeedPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(SpeedPayload.TYPE, (payload, ctx) -> {
            ServerPlayer player = ctx.player();
            ctx.server().execute(() -> {
                BuilderEntity b = findNearestBuilder(player);
                if (b != null) {
                    float speed = switch (payload.speedIndex()) {
                        case 0 -> 0.5f;
                        case 2 -> 2.0f;
                        case 3 -> 10.0f;
                        default -> 1.0f;
                    };
                    if (speed == 10.0f && !player.isCreative()) {
                        speed = 2.0f;
                    }
                    b.setSpeedMultiplier(speed);
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§a建筑者 " + b.getBuilderName() + " 速度已设为 " + speedLabel(speed)));
                }
            });
        });

        KeyMapping.Category category = KeyMapping.Category.register(
                ResourceLocation.fromNamespaceAndPath("bspmod", "builder"));
        speedKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.bspmod.builder_speed",
                InputConstants.Type.KEYSYM,
                org.lwjgl.glfw.GLFW.GLFW_KEY_B,
                category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (speedKey.consumeClick()) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    mc.setScreen(new BuilderSpeedSelectScreen());
                }
            }
        });
    }

    private static BuilderEntity findNearestBuilder(ServerPlayer player) {
        var level = player.level();
        var list = level.getEntities(BspModEntities.BUILDER, player.getBoundingBox().inflate(64), e -> true);
        if (list.isEmpty()) return null;
        list.sort(java.util.Comparator.comparingDouble(b -> b.distanceToSqr(player)));
        return list.get(0);
    }

    public static String speedLabel(float v) {
        if (v <= 0.5f) return "慢速";
        if (v <= 1.0f) return "正常";
        if (v <= 2.0f) return "快速";
        return "命令速度";
    }
}
