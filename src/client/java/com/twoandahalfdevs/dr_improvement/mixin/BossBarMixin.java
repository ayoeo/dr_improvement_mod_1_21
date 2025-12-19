package com.twoandahalfdevs.dr_improvement.mixin;

import com.twoandahalfdevs.dr_improvement.client.DrImprovementModClientKt;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.entity.boss.BossBar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BossBarHud.class)
public class BossBarMixin {
  @ModifyVariable(method = "render", at = @At(value = "STORE", ordinal = 1), index = 4)
  private int modifyHeight(int height) {
    // If there are more than 2 boss bars I'm gonna freak out
    return height + 14;
  }

  @Inject(method = "renderBossBar(Lnet/minecraft/client/gui/DrawContext;IILnet/minecraft/entity/boss/BossBar;)V", at = @At("HEAD"), cancellable = true)
  private void renderBossBarHead(DrawContext context, int x, int y, BossBar bossBar, CallbackInfo ci) {
    // Don't render the bar for player hp, just the text
    var plainText = bossBar.getName().getString();
    if (plainText.contains("Lv ") && plainText.contains(" - HP ")) {
      try {
        var split = plainText.split("-");
        DrImprovementModClientKt.setClas(split[0].trim());
      } catch (Exception ignored) {
      }

      ci.cancel();
    }

  }
}
