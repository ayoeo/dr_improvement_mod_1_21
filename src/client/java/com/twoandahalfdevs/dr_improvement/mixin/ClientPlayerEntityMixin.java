package com.twoandahalfdevs.dr_improvement.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin extends PlayerEntity {
  public ClientPlayerEntityMixin(World world, GameProfile profile) {
    super(world, profile);
  }

  @Redirect(method = "dropSelectedItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;dropSelectedItem(Z)Lnet/minecraft/item/ItemStack;"))
  private ItemStack dropSelectedItem(PlayerInventory instance, boolean entireStack) {
    // Don't do client-side drops anymore
    return instance.getSelectedStack();
  }
}

