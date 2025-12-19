package com.twoandahalfdevs.dr_improvement.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public class ItemRendererMixin {
  @Shadow
  private float equipProgressMainHand;

  @Shadow
  private float equipProgressOffHand;

  @Shadow
  private ItemStack mainHand;

  @Shadow
  private ItemStack offHand;

  @Shadow
  @Final
  private MinecraftClient client;

  @Shadow
  private float lastEquipProgressMainHand;

  @Shadow
  private float lastEquipProgressOffHand;

  @Inject(method = "updateHeldItems", at = @At("TAIL"))
  private void modifyEquipProgress(CallbackInfo ci) {
    // No more annoying held item visual equip time reset when hitting stuff
    if (client.player == null) return;

    mainHand = client.player.getMainHandStack();
    offHand = client.player.getOffHandStack();
    equipProgressMainHand = lastEquipProgressMainHand = 1;
    equipProgressOffHand = lastEquipProgressOffHand = 1;
  }
}

