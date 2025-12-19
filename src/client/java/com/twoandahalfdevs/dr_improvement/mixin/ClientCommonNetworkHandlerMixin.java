package com.twoandahalfdevs.dr_improvement.mixin;

import com.twoandahalfdevs.dr_improvement.client.DrImprovementModClient;
import com.twoandahalfdevs.dr_improvement.client.DrImprovementModClientKt;
import com.twoandahalfdevs.dr_improvement.client.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonNetworkHandler.class)
public abstract class ClientCommonNetworkHandlerMixin {
  @Shadow
  public abstract void sendPacket(Packet<?> packet);

  @Unique
  private RequestCommandCompletionsC2SPacket latestCompletion;

  @Unique
  private long lastCompletionTime = 0L;

  @Unique
  private int prevSlot = 0;

  @Inject(method = "sendPacket", at = @At("HEAD"), cancellable = true)
  public void setPosition(Packet<?> packet, CallbackInfo ci) {
    if (packet instanceof PlayerMoveC2SPacket movePacket) {
      if (movePacket.changesPosition()) {
        var pos = new Vec3d(movePacket.getX(0), movePacket.getY(0), movePacket.getZ(0));
        DrImprovementModClientKt.recentPositions.add(pos);
      }
    } else if (packet instanceof RequestCommandCompletionsC2SPacket) {
      if (packet == latestCompletion) {
        // We're resending it's fine
        latestCompletion = null;
      } else {
        latestCompletion = (RequestCommandCompletionsC2SPacket) packet;
        ci.cancel();
      }
    }
  }


  @Inject(method = "sendPacket", at = @At("TAIL"))
  private void sendTail(Packet<?> packet, CallbackInfo ci) {
    if (packet instanceof UpdateSelectedSlotC2SPacket) {
      var player = MinecraftClient.getInstance().player;

      if (player != null && ModConfig.INSTANCE.getSettings().getAutoCastAbilities()) {
        var newSlot = player.getInventory().getSelectedSlot();

        var item = player.getMainHandStack();
        var key = item.getItem().getTranslationKey();
        if (key.equals("block.minecraft.player_head")) {
          // Click it
          var interactionManager = MinecraftClient.getInstance().interactionManager;
          if (interactionManager != null) {
            var hitBlock = DrImprovementModClientKt.getBlockHitPosition();
            if (hitBlock != null) {
              // Interact with the block
              ((InteractionManagerAccessor) MinecraftClient.getInstance().interactionManager)
                .invokeSendSequencedPacket((ClientWorld) player.getEntityWorld(), seq ->
                  new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hitBlock, seq));
            }

            // Interact with ITEM
            ((InteractionManagerAccessor) MinecraftClient.getInstance().interactionManager)
              .invokeSendSequencedPacket((ClientWorld) player.getEntityWorld(), seq ->
                new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, seq, player.getYaw(), player.getPitch()));

            // Switch back, but not RIGHT AWAY
            DrImprovementModClientKt.INSTANCE.setSwitchBackSlot(prevSlot);
            DrImprovementModClientKt.INSTANCE.setSwitchBackTicks(0);
          }
        }

        prevSlot = newSlot;
      }
    }
  }

  @Inject(method = "sendQueuedPackets", at = @At("HEAD"))
  private void tickHead(CallbackInfo ci) {
    if (latestCompletion != null && System.currentTimeMillis() - lastCompletionTime > 400L) {
      lastCompletionTime = System.currentTimeMillis();
      sendPacket(latestCompletion);
    }
  }
}