package com.twoandahalfdevs.dr_improvement.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.function.Function;

@Mixin(targets = "com.mojang.serialization.DataResult$Error", remap = false)
public abstract class DataResultErrorMixin {
  @Shadow
  public abstract String message();

  @Shadow
  public abstract Optional<Object> partialValue();

  @Inject(method = "getOrThrow(Ljava/util/function/Function;)Ljava/lang/Object;", at = @At("HEAD"), cancellable = true)
  private void safeGetOrThrow(Function<String, Throwable> exceptionSupplier, CallbackInfoReturnable<Object> cir) {
    String errorMsg = this.message();

    if (errorMsg != null && isBadPickaxeTime(errorMsg)) {
      var partialValue = this.partialValue();
      partialValue.ifPresent(cir::setReturnValue);
    }
  }

  @Unique
  private static boolean isBadPickaxeTime(String errorMsg) {
    return errorMsg.contains("missed input: {tool:{rules:")
      || errorMsg.contains("missed input: {tool: {rules:");
  }
}
