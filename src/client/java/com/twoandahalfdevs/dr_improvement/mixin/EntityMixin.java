package com.twoandahalfdevs.dr_improvement.mixin;

import com.twoandahalfdevs.dr_improvement.client.ModConfig;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {
  @Inject(method = "getTeamColorValue", at = @At(value = "RETURN"), cancellable = true)
  private void colorBlindGlowTime(CallbackInfoReturnable<Integer> cir) {
    if (!ModConfig.INSTANCE.getSettings().getColorBlindOutline()) return;

    var color = cir.getReturnValue();
    switch (color) {
      case 16777045:
        cir.setReturnValue(0xff45a5);
        break;
      // Rare
      case 5636095:
        cir.setReturnValue(0x90D5FF);
        break;
      case 5635925:
        cir.setReturnValue(0x3b632f);
        break;
      case 16755200:
        cir.setReturnValue(0xff0303);
        break;
    }
  }
}
