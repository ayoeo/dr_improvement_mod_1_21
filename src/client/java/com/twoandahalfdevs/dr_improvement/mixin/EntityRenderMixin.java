package com.twoandahalfdevs.dr_improvement.mixin;

import com.twoandahalfdevs.dr_improvement.client.DrImprovementModClientKt;
import com.twoandahalfdevs.dr_improvement.client.ModConfig;
import net.minecraft.client.network.ClientPlayerLikeEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public class EntityRenderMixin<T extends Entity, S extends EntityRenderState> {
  @Inject(method = "updateRenderState(Lnet/minecraft/entity/PlayerLikeEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V", at = @At("TAIL"))
  private void updateRenderState(PlayerLikeEntity player, PlayerEntityRenderState state, float tickDelta, CallbackInfo ci) {
    if (state.displayName == null || !(player instanceof ClientPlayerLikeEntity clientPlayer)) {
      state.playerName = null;
      return;
    }

    state.playerName = clientPlayer.getMannequinName();

    if (ModConfig.INSTANCE.getSettings().getShowBleedingNametags()) {
      var bleedTime = DrImprovementModClientKt.INSTANCE.bleedString(state.id);
      if (bleedTime != null && state.playerName != null) {
        state.playerName = Text.literal("§7(" + bleedTime + "§7)§r ").append(state.playerName.copy());
      }
    }
  }

  @Inject(method = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V", at = @At(value = "HEAD"))
  private void renderLabelIfPresent(PlayerEntityRenderState state, MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, CameraRenderState cameraRenderState, CallbackInfo ci) {
    if (state.entityType != EntityType.PLAYER) return;

    if (ModConfig.INSTANCE.getSettings().getAbilityNametags()) {
      var cds = DrImprovementModClientKt.INSTANCE.cdString(state.id);
      if (cds != null && state.displayName != null) {
        state.displayName = state.displayName.copy().append(" §7(" + cds + "§7)");
      }
    }
  }
}
