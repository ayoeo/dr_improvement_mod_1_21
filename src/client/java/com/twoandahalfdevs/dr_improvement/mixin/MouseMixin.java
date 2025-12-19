package com.twoandahalfdevs.dr_improvement.mixin;

import com.twoandahalfdevs.dr_improvement.client.DrImprovementModClientKt;
import com.twoandahalfdevs.dr_improvement.client.ModConfig;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {
  @Inject(method = "onMouseButton", at = @At("HEAD"))
  private void onMouseInput(long window, MouseInput input, int action, CallbackInfo ci) {
    if (action == 1 && ModConfig.INSTANCE.getSettings().getShowLatencyFlash()) {
      DrImprovementModClientKt.lastClickTime = System.currentTimeMillis();
    }
  }
}
