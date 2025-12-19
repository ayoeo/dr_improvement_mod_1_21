package com.twoandahalfdevs.dr_improvement.mixin;

import net.minecraft.component.ComponentHolder;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
  @Unique
  private final Pattern originRegex = Pattern.compile("(.*) \\((.*)/.*\\)");

  @Inject(method = "getTooltip", at = @At("RETURN"))
  private void getTooltip(Item.TooltipContext context, @Nullable PlayerEntity player, TooltipType type, CallbackInfoReturnable<List<Text>> cir) {
    List<Text> tooltipList = cir.getReturnValue();
    List<Text> additionalList = new ArrayList<>();

    ItemStack stack = (ItemStack) (Object) this;

    var customData = stack.get(DataComponentTypes.CUSTOM_DATA);

    if (customData != null) {
      NbtCompound nbt = customData.copyNbt();

      if (nbt.contains("origin")) {
        var origin = nbt.getString("origin");
        if (origin.isPresent()) {
          Matcher matcher = originRegex.matcher(origin.get());
          if (matcher.find()) {
            if (matcher.groupCount() == 2) {
              String originType = matcher.group(1);
              String playerStr = matcher.group(2);

              additionalList.add(Text.of(Formatting.GRAY + "Origin: " + originType + " - " + Formatting.ITALIC + playerStr));
            }
          }
        }
      }
    }

    if (!additionalList.isEmpty()) {
      tooltipList.add(Text.of(""));
      tooltipList.addAll(additionalList);
    }
  }
}