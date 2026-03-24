package com.twoandahalfdevs.dr_improvement.mixin;

import com.twoandahalfdevs.dr_improvement.client.DrImprovementModClientKt;
import com.twoandahalfdevs.dr_improvement.client.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.client.network.ClientConnectionState;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPosition;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.s2c.play.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;
import java.util.Set;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin extends ClientCommonNetworkHandler {
  @Shadow
  private static boolean setPosition(EntityPosition pos, Set<PositionFlag> flags, Entity entity, boolean bl) {
    return false;
  }

  protected ClientPlayNetworkHandlerMixin(MinecraftClient client, ClientConnection connection, ClientConnectionState connectionState) {
    super(client, connection, connectionState);
  }


  @Inject(method = "onExperienceBarUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V", shift = At.Shift.AFTER), cancellable = true)
  private void experienceBarUpdateHead(ExperienceBarUpdateS2CPacket packet, CallbackInfo ci) {
    DrImprovementModClientKt.setExpUpdateTime(System.nanoTime());
    DrImprovementModClientKt.setPrevExp(DrImprovementModClientKt.getLatestExp());
    DrImprovementModClientKt.setLatestExp(packet.getBarProgress());
  }

  @Inject(method = "onPlayerPositionLook", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V", shift = At.Shift.AFTER), cancellable = true)
  public void saveOldRotation(PlayerPositionLookS2CPacket packet, CallbackInfo ci) {
    var player = this.client.player;
    var pos = packet.change().position();
    if (ModConfig.INSTANCE.getSettings().getFixDashRotation() && player != null && DrImprovementModClientKt.recentPositions.stream().anyMatch(vec3d -> vec3d.squaredDistanceTo(pos) < 0.0025)) {
      if (!player.hasVehicle()) {
        setPosition(packet.change().withRotation(player.getYaw(), player.getPitch()), packet.relatives(), player, false);
      }

      this.connection.send(new TeleportConfirmC2SPacket(packet.teleportId()));
      this.connection.send(new PlayerMoveC2SPacket.Full(player.getX(), player.getY(), player.getZ(), packet.change().yaw(), packet.change().pitch(), false, false));
      ci.cancel();
    }
  }

  @Inject(method = "onGameMessage", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V", shift = At.Shift.AFTER), cancellable = true)
  public void onGameMessageHead(GameMessageS2CPacket packet, CallbackInfo ci) {
    var content = packet.content();
    if (!packet.overlay() && content != null) {
      DrImprovementModClientKt.INSTANCE.onChatMessage(content.getString());
    }
  }

  @Inject(method = "onChatMessage", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V", shift = At.Shift.AFTER), cancellable = true)
  public void onChatMessageHead(ChatMessageS2CPacket packet, CallbackInfo ci) {
    DrImprovementModClientKt.INSTANCE.onChatMessage(packet.body().content());
  }

  @Inject(method = "onScoreboardScoreUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V", shift = At.Shift.AFTER), cancellable = true)
  public void onScoreboardPlayerUpdateHead(ScoreboardScoreUpdateS2CPacket packet, CallbackInfo ci) {
    if (!Objects.equals(packet.objectiveName(), "health") || client.world == null) {
      return;
    }

    DrImprovementModClientKt.getScoreWasUpdated().put(packet.scoreHolderName(), packet.score());

    // In case the player is under 5% health, we need to display SOME info
    var player = client.world.getPlayers()
      .stream()
      .filter(p -> p.getStringifiedName().equals(packet.scoreHolderName()))
      .findFirst();
    if (player.isEmpty()) return;

    var maxHealth = DrImprovementModClientKt.getMaxHealthValues().get(packet.scoreHolderName());
    if (maxHealth != null && player.get() != client.player) {
      player.get().setHealth(((float) packet.score() * 20f) / (float) maxHealth);
    }

    // We have to wait for an update haha
    DrImprovementModClientKt.getLatestCurrentHealth().remove(packet.scoreHolderName());
  }

  @Inject(method = "onEntityTrackerUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V", shift = At.Shift.AFTER), cancellable = true)
  public void onEntityTrackerUpdateHead(EntityTrackerUpdateS2CPacket packet, CallbackInfo ci) {
    if (client.world == null) return;
    var player = client.world.getEntityById(packet.id());
    if (player instanceof PlayerEntity) {
      var packetHealth = packet.trackedValues().stream().filter(
        i -> i.id() == 9
      ).findFirst();

      if (packetHealth.isPresent() && packetHealth.get().value() != null) {
        var health = (float) packetHealth.get().value();

        if (health != 1.0f) {
          // Update the health now : )
          DrImprovementModClientKt.getLatestCurrentHealth().put(player.getStringifiedName(), health);
        }
      }
    }
  }
}
