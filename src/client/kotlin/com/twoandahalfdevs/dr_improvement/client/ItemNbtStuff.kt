package com.twoandahalfdevs.dr_improvement.client

import net.minecraft.component.DataComponentTypes
import net.minecraft.item.ItemStack
import net.minecraft.nbt.AbstractNbtNumber
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtString
import net.minecraft.text.Text
import net.minecraft.text.TextColor
import kotlin.math.roundToInt

object ItemNbtStuff {
  const val MAX_DURABILITY = 2500
  const val LOW_DURABILITY_THRESHOLD = 35.0
  const val LOW_DURABILITY_COLOR = 0xD13F3F

  @JvmStatic
  fun customData(stack: ItemStack): NbtCompound? {
    val customData = stack.get(DataComponentTypes.CUSTOM_DATA)
    return customData?.copyNbt()
  }

  @JvmStatic
  fun durabilityPercent(nbt: NbtCompound?): Double? {
    val durabilityNbt = nbt?.get("durability") as? AbstractNbtNumber ?: return null
    val durability = durabilityNbt.intValue()
    val clampedDurability = durability.coerceIn(0, MAX_DURABILITY)
    return (clampedDurability * 100.0) / MAX_DURABILITY
  }

  @JvmStatic
  fun itemTier(nbt: NbtCompound?): Int? {
    val tierNbt = nbt?.get("tier") as? AbstractNbtNumber ?: return null
    return tierNbt.intValue()
  }

  @JvmStatic
  fun itemRarity(nbt: NbtCompound?): String? {
    val rarityNbt = nbt?.get("rarity") as? NbtString ?: return null
    return rarityNbt.toString()
  }

  @JvmStatic
  fun durabilityText(nbt: NbtCompound?): Text? {
    val durabilityPercent = durabilityPercent(nbt) ?: return null
    val displayedPercent = durabilityPercent.roundToInt()
    val color = durabilityColor(durabilityPercent)
    val bold = durabilityPercent <= LOW_DURABILITY_THRESHOLD

    return Text.literal("Durability: $displayedPercent%")
      .styled { style -> style.withColor(TextColor.fromRgb(color)).withBold(bold) }
  }

  @JvmStatic
  fun durabilityColor(durabilityPercent: Double): Int {
    if (durabilityPercent > 50) {
      return 0x52FF80
    }
    if (durabilityPercent > LOW_DURABILITY_THRESHOLD) {
      return 0xFF8E38
    }
    return LOW_DURABILITY_COLOR
  }
}
