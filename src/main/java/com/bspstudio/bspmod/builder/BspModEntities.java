package com.bspstudio.bspmod.builder;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * 注册建筑者与工作者实体类型。
 */
public class BspModEntities {

    public static final String MOD_ID = "bspmod";

    public static EntityType<BuilderEntity> BUILDER;
    public static EntityType<WorkerEntity> WORKER;

    public static void register() {
        BUILDER = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(MOD_ID, "builder"),
                EntityType.Builder.of(BuilderEntity::new, MobCategory.MISC)
                        .sized(0.6f, 1.8f)
                        .clientTrackingRange(10)
                        .build(ResourceKey.create(
                                net.minecraft.core.registries.Registries.ENTITY_TYPE,
                                ResourceLocation.fromNamespaceAndPath(MOD_ID, "builder")))
        );

        WORKER = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(MOD_ID, "worker"),
                EntityType.Builder.of(WorkerEntity::new, MobCategory.MISC)
                        .sized(0.4f, 1.2f)
                        .clientTrackingRange(10)
                        .build(ResourceKey.create(
                                net.minecraft.core.registries.Registries.ENTITY_TYPE,
                                ResourceLocation.fromNamespaceAndPath(MOD_ID, "worker")))
        );

        // 属性注册
        FabricDefaultAttributeRegistry.register(BUILDER, builderAttributes());
        FabricDefaultAttributeRegistry.register(WORKER, workerAttributes());
    }

    private static AttributeSupplier.Builder builderAttributes() {
        return AttributeSupplier.builder()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    private static AttributeSupplier.Builder workerAttributes() {
        return AttributeSupplier.builder()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }
}
