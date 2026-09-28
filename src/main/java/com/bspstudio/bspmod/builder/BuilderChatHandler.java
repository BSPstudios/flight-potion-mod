package com.bspstudio.bspmod.builder;

import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;

/**
 * 监听聊天消息，处理 @建筑者 指令。
 */
public class BuilderChatHandler {

    public static void register() {
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            if (sender == null) return;
            PlayerChatMessage pcm = message;
            String content = pcm.signedContent();
            if (content == null || content.isEmpty()) return;

            if (BuilderCommands.handleChat(sender, content)) {
                // 已作为建筑者指令处理。这里不取消原消息（聊天消息仍显示），
                // 因为取消聊天消息需要 ALLOW_CHAT_MESSAGE 阶段。
            }
        });
    }
}
