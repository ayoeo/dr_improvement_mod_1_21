package com.twoandahalfdevs.dr_improvement.client

import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.DepthTestFunction
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.UniformType
import net.minecraft.client.render.VertexFormats
import org.lwjgl.system.MemoryUtil
import java.util.*

object OverlayShader {
  private var pipeline: RenderPipeline? = null
  private var uniformBuffer: GpuBuffer? = null
  private const val UNIFORM_SIZE = 20

  fun init() {
    if (pipeline != null) return

    try {
      pipeline = RenderPipeline.builder()
        .withLocation("dr_improvement/overlay")
        .withVertexShader("core/overlay")
        .withFragmentShader("core/overlay")
        .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withDepthWrite(false)
        .withBlend(BlendFunction.TRANSLUCENT)
        .withUniform("OverlayData", UniformType.UNIFORM_BUFFER)
        .build()

      uniformBuffer = RenderSystem.getDevice().createBuffer(
        { "Overlay Uniforms" },
        GpuBuffer.USAGE_UNIFORM or GpuBuffer.USAGE_COPY_DST,
        UNIFORM_SIZE.toLong(),
      )

    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  @JvmStatic
  fun draw() {
    val client = MinecraftClient.getInstance()
    val player = client.player ?: return
    val window = client.window

    if (pipeline == null) init()
    val pipe = pipeline ?: return
    val ubo = uniformBuffer ?: return

    if (client.options.hudHidden) return

    val mem = MemoryUtil.memAlloc(UNIFORM_SIZE)
    try {
      mem.putFloat(0, interpolatedExp())
      mem.putFloat(4, player.health / player.maxHealth)
      mem.putFloat(8, player.experienceLevel / 100f)
      mem.putFloat(12, window.framebufferWidth.toFloat() / window.framebufferHeight)
      mem.putFloat(16, if (ModConfig.settings.mode1440p) 0.0f else 1.0f)

      val device = RenderSystem.getDevice()
      val encoder = device.createCommandEncoder()

      encoder.writeToBuffer(ubo.slice(), mem)

      val target = client.framebuffer.colorAttachmentView

      encoder.createRenderPass({ "DR Improvement Mod Overlay Pass" }, target, OptionalInt.empty()).use { pass ->
        pass.setPipeline(pipe)
        pass.setUniform("OverlayData", ubo)
        pass.draw(0, 3)
      }
    } finally {
      MemoryUtil.memFree(mem)
    }
  }
}