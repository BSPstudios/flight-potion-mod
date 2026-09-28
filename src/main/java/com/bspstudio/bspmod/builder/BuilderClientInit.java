package com.bspstudio.bspmod.builder;

import com.bspstudio.bspmod.FlightClientHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * 建筑者/工作者客户端渲染注册。
 */
public class BuilderClientInit implements ClientModInitializer {

    public static final ModelLayerLocation BUILDER_LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("bspmod", "builder"), "main");
    public static final ModelLayerLocation WORKER_LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("bspmod", "worker"), "main");

    @Override
    public void onInitializeClient() {
        // 模型层：复用 HumanoidModel 的玩家网格
        EntityModelLayerRegistry.registerModelLayer(BUILDER_LAYER,
                () -> LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f), 64, 64));
        EntityModelLayerRegistry.registerModelLayer(WORKER_LAYER,
                () -> LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f), 64, 64));

        // 渲染器
        EntityRendererRegistry.register(BspModEntities.BUILDER, BuilderRenderer::new);
        EntityRendererRegistry.register(BspModEntities.WORKER, WorkerRenderer::new);

        // 速度设置按键 + 屏幕
        BuilderSpeedScreen.register();
    }
}
