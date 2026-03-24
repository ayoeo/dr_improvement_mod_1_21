package com.twoandahalfdevs.dr_improvement.mixin;

import com.twoandahalfdevs.dr_improvement.client.ModConfig;
import com.twoandahalfdevs.dr_improvement.client.OverlayShader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
  @Shadow
  @Final
  private MinecraftClient client;

  @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V"))
  private void postRenderWorld(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
    if (!this.client.skipGameRender) {
      OverlayShader.draw();
    }
  }

  @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;updateCamera(Lnet/minecraft/client/render/RenderTickCounter;)V"))
  public void preUpdateLookDir(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
    if (ModConfig.INSTANCE.getSettings().getReduceInputLag()) {
      GLFW.glfwPollEvents();
    }
  }
}
