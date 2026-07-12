package com.flightpotion.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.SplashScreen;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SplashScreen.class)
public class SplashScreenMixin {

    @Shadow @Final
    private Minecraft minecraft;

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureManager;bindForSetup(Lnet/minecraft/resources/ResourceLocation;)V", ordinal = 0))
    private void redirectLogoTexture(TextureManager textureManager, ResourceLocation original) {
        ResourceLocation customLogo = ResourceLocation.fromNamespaceAndPath("bspmod", "textures/gui/logo");
        if (this.minecraft.getResourceManager().getResource(customLogo).isPresent()) {
            textureManager.bindForSetup(customLogo);
        } else {
            textureManager.bindForSetup(original);
        }
    }
}
