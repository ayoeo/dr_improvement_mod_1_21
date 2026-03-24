package com.twoandahalfdevs.dr_improvement.mixin;

import com.twoandahalfdevs.dr_improvement.client.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.misc.Misc;
import xaero.hud.minimap.element.render.MinimapElementGraphics;
import xaero.hud.minimap.element.render.MinimapElementRenderInfo;
import xaero.hud.render.util.RenderBufferUtil;
import xaero.lib.client.graphics.XaeroBufferProvider;

@Mixin(value = xaero.hud.minimap.radar.render.element.RadarRenderer.class, remap = false)
public class MixinRadarRenderer {
  @Shadow
  private double labelScale;

  @Shadow
  private int displayY;

  @Shadow
  private VertexConsumer labelBgBuilder;

  @Shadow
  private XaeroBufferProvider minimapBufferSource;

  @Inject(method = "renderElement(Lnet/minecraft/entity/Entity;ZZDFDDLxaero/hud/minimap/element/render/MinimapElementRenderInfo;Lxaero/hud/minimap/element/render/MinimapElementGraphics;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;)Z", at = @At("HEAD"), cancellable = true)
  private void filterNPCs(Entity entity, boolean highlighted, boolean outOfBounds, double optionalDepth, float optionalScale, double partialX, double partialY, MinimapElementRenderInfo renderInfo, MinimapElementGraphics guiGraphics, VertexConsumerProvider.Immediate vanillaBufferSource, CallbackInfoReturnable<Boolean> cir) {
    if (!(entity instanceof PlayerEntity player) || !ModConfig.INSTANCE.getSettings().getFilterNPCsFromMinimap())
      return;

    String playerName = player.getGameProfile().name();
    if (playerName.startsWith("NPC@")) {
      cir.cancel();
    }
  }

  @Inject(method = "renderLabel", at = @At("HEAD"), cancellable = true)
  private void fixNametags(Entity e, Entity renderEntity, boolean name, double optionalScale, MatrixStack matrixStack, CallbackInfo ci) {
    if (!ModConfig.INSTANCE.getSettings().getFixNamesForMinimap() || !(e instanceof PlayerEntity)) return;

    // Do it ourselves
    ci.cancel();

    double dotNameScale = this.labelScale * optionalScale * (1.0 + (ModConfig.INSTANCE.getSettings().getMinimapNameScale() * 0.01f));
    matrixStack.scale((float) dotNameScale, (float) dotNameScale, 1.0F);
    String yValueString = null;
    if (this.displayY > 0) {
      int yInt = (int) Math.floor(e.getY());
      int pYInt = (int) Math.floor(renderEntity.getY());
      if (this.displayY == 1) {
        yValueString = "" + yInt;
      } else if (this.displayY == 2) {
        yValueString = "" + (yInt - pYInt);
      } else {
        yValueString = "";
      }

      if (yValueString.isEmpty()) {
        yValueString = "-";
      }
    }

    var font = MinecraftClient.getInstance().textRenderer;
    MutableText label = null;
    if (name) {
      var displayName = e.getDisplayName();
      if (displayName == null) {
        return;
      }
      label = displayName.copy();
      if (this.displayY > 0) {
        label.append(Text.of(" (" + yValueString + ")").copy().setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
      }
    } else if (this.displayY > 0) {
      label = MutableText.of(Text.of(yValueString).getContent());
    }

    if (label != null) {
      int labelW = font.getWidth(label);
      RenderBufferUtil.addColoredRect(matrixStack.peek().getPositionMatrix(), this.labelBgBuilder, (float) (-labelW / 2 - 2), -1.0F, labelW + 3, 10, 0.0F, 0.0F, 0.0F, 0.4F);
      Misc.drawNormalText(matrixStack, label, (float) (-labelW / 2), 0.0F, -1, false, this.minimapBufferSource);
    }
  }
}
