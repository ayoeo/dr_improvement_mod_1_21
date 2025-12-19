package com.twoandahalfdevs.dr_improvement.mixin;

import com.twoandahalfdevs.dr_improvement.client.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(Team.class)
public abstract class TeamMixin {
  @Shadow
  public abstract Formatting getColor();

  @Shadow
  private Text prefix;

  @Shadow
  private Text suffix;

  @Inject(method = "decorateName(Lnet/minecraft/text/Text;)Lnet/minecraft/text/MutableText;", at = @At(value = "HEAD"), cancellable = true)
  private void decorateName(Text name, CallbackInfoReturnable<MutableText> cir) {
    var player = MinecraftClient.getInstance().player;
    Text selfPrefix = null;
    if (player != null && player.getScoreboardTeam() instanceof Team) {
      selfPrefix = player.getScoreboardTeam().getPrefix();
    }

    Text colChange = this.prefix;
    Text sufx = this.suffix;
    if (this.prefix.getString().startsWith("[") && selfPrefix != null && Objects.equals(this.prefix.getString(), selfPrefix.getString())) {
      colChange = this.prefix.copy();

      boolean isParty = false;
      for (Text sibling : colChange.getSiblings()) {
        var styleColor = sibling.getStyle().getColor();
        if (styleColor != null && "dark_green".equals(styleColor.getName())) {
          isParty = true;
          break;
        }
      }

      if (!isParty) {
        for (Text sibling : colChange.getSiblings()) {
          if (sibling instanceof MutableText mutableSibling) {
            mutableSibling.setStyle(mutableSibling.getStyle().withColor(Formatting.GREEN));
          }
        }

        sufx = this.suffix.copy().setStyle(this.suffix.getStyle().withColor(Formatting.GREEN));
      }
    }

    MutableText mutableText;
    if (ModConfig.INSTANCE.getSettings().getGuildNametags()) {
      mutableText = Text.empty().append(colChange).append(name).append(sufx);
    } else {
      mutableText = Text.empty().append(this.prefix).append(name).append(this.suffix);
    }

    Formatting formatting = this.getColor();
    if (formatting != Formatting.RESET) {
      mutableText.formatted(formatting);
    }

    cir.setReturnValue(mutableText);
  }
}
