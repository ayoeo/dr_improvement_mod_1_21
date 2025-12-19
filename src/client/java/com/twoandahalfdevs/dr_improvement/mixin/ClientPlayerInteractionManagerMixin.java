package com.twoandahalfdevs.dr_improvement.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {
  @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
  private void eee(ClientPlayerEntity clientPlayerEntity, Hand hand, BlockHitResult blockHitResult, CallbackInfoReturnable<ActionResult> cir) {
    // Disable client-side block place for ability heads
    if (clientPlayerEntity.getStackInHand(hand).getItem().getTranslationKey().equals("block.minecraft.player_head")) {
      cir.setReturnValue(ActionResult.CONSUME);
    }
  }

  // Fix stupid 1.8 thing
  @Inject(method = "hasLimitedAttackSpeed", at = @At("HEAD"), cancellable = true)
  private void aaa(CallbackInfoReturnable<Boolean> cir) {
    cir.setReturnValue(false);
  }
}
