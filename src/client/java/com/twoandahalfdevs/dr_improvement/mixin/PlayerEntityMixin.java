package com.twoandahalfdevs.dr_improvement.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
  @Redirect(method = "addAttackParticlesAndSounds", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;addCritParticles(Lnet/minecraft/entity/Entity;)V"))
  private void modifyCrittingParticles(PlayerEntity instance, Entity target) {
    // No client-side crit particles hehe
  }
}
