package com.twoandahalfdevs.dr_improvement.mixin;

import com.twoandahalfdevs.dr_improvement.client.DrImprovementModClientKt;
import com.twoandahalfdevs.dr_improvement.client.ModConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
  @Shadow
  private Text overlayMessage;

  @Shadow
  private int overlayRemaining;

  @Unique
  private static int oldTime = 0;

  @Inject(method = "renderNauseaOverlay", at = @At("HEAD"), cancellable = true)
  private void renderNausea(DrawContext context, float nauseaStrength, CallbackInfo ci) {
    if (ModConfig.INSTANCE.getSettings().getRemoveNauseaOverlay()) {
      ci.cancel();
    }
  }

  @Inject(method = "render", at = @At("HEAD"))
  private void renderHead(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
    if (this.overlayMessage != null) {
      DrImprovementModClientKt.INSTANCE.setActionBarMsg(this.overlayMessage.getString());
    } else {
      DrImprovementModClientKt.INSTANCE.setActionBarMsg(null);
    }
    DrImprovementModClientKt.INSTANCE.setActionBarTime(this.overlayRemaining);

    oldTime = this.overlayRemaining;

    DrImprovementModClientKt.INSTANCE.renderInfo(context);
  }

  @Inject(method = "render", at = @At("TAIL"))
  private void renderTail(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
    this.overlayRemaining = oldTime;
  }
}