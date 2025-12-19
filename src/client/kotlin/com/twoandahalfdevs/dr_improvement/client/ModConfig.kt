package com.twoandahalfdevs.dr_improvement.client

import com.google.gson.GsonBuilder
import net.fabricmc.loader.api.FabricLoader
import java.io.FileReader
import java.io.FileWriter

data class ConfigData(
  var reduceInputLag: Boolean = true,
  var fixDashRotation: Boolean = true,
  var autoCastAbilities: Boolean = false,
  var guildNametags: Boolean = true,
  var showLatencyFlash: Boolean = false,
  var colorBlindOutline: Boolean = false,
  var abilityNametags: Boolean = true,
  var showPotionInfo: Boolean = true,
  var showCooldownInfo: Boolean = true,
  var showCombatInfo: Boolean = true,
  var showRogueInfo: Boolean = true,
  var showBleedingInfo: Boolean = true,
  var showBleedingNametags: Boolean = true,
  var filterNPCsFromMinimap: Boolean = true,
  var fixNamesForMinimap: Boolean = true,
  var mode1440p: Boolean = false,
  var removeNauseaOverlay: Boolean = true,
  var energyBarLyingFactor: Int = 0,
  var minimapNameScale: Int = 0,

  var shadowmeldPoints: Int = 0,
  var justMyNaturePoints: Int = 0,
)

object ModConfig {
  private val GSON = GsonBuilder().setPrettyPrinting().create()
  private val CONFIG_FILE = FabricLoader.getInstance().configDir.resolve("dr_improvement_mod.json").toFile()

  var settings: ConfigData = ConfigData()
    private set

  fun save() {
    try {
      FileWriter(CONFIG_FILE).use { writer ->
        GSON.toJson(settings, writer)
      }
    } catch (_: Exception) {
    }
  }

  fun load() {
    if (CONFIG_FILE.exists()) {
      try {
        FileReader(CONFIG_FILE).use { reader ->
          settings = GSON.fromJson(reader, ConfigData::class.java)
        }
      } catch (_: Exception) {
        settings = ConfigData()
        save()
      }
    } else {
      settings = ConfigData()
      save()
    }
  }
}