package com.twoandahalfdevs.dr_improvement.client

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import me.shedaniel.clothconfig2.api.ConfigBuilder
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text

class ModMenuIntegration : ModMenuApi {
  override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
    return ConfigScreenFactory(ModConfigScreen::create)
  }
}

object ModConfigScreen {
  fun create(parent: Screen): Screen {
    val builder = ConfigBuilder.create().setParentScreen(parent).setTitle(Text.literal("DR Improvement Mod Settings"))

    builder.setSavingRunnable(ModConfig::save)

    val general = builder.getOrCreateCategory(Text.literal("General"))
    val entryBuilder = builder.entryBuilder()

    general.addEntry(
      entryBuilder.startBooleanToggle(
        Text.literal("Reduce Input Lag"),
        ModConfig.settings.reduceInputLag
      ).setDefaultValue(true)
        .setTooltip(Text.literal("Reduces input lag by polling for new inputs just before rendering the next frame."))
        .setSaveConsumer { newValue -> ModConfig.settings.reduceInputLag = newValue }.build()
    )


    general.addEntry(
      entryBuilder.startBooleanToggle(
        Text.literal("Fix Dash Rotation Reset"),
        ModConfig.settings.fixDashRotation
      ).setDefaultValue(true).setTooltip(Text.literal("Prevents dashes from resetting your rotation."))
        .setSaveConsumer { newValue -> ModConfig.settings.fixDashRotation = newValue }.build()
    )


    general.addEntry(
      entryBuilder.startBooleanToggle(
        Text.literal("Auto Cast Abilities"),
        ModConfig.settings.autoCastAbilities
      ).setDefaultValue(true)
        .setTooltip(Text.literal("Casts ability artifacts when you select them in your hotbar. DR has a toggle for this, but it doesn't work as well."))
        .setSaveConsumer { newValue -> ModConfig.settings.autoCastAbilities = newValue }.build()
    )

    general.addEntry(
      entryBuilder.startBooleanToggle(Text.literal("Guild Nametags"), ModConfig.settings.guildNametags)
        .setDefaultValue(true).setTooltip(Text.literal("Shows light green nametags for guild members."))
        .setSaveConsumer { newValue -> ModConfig.settings.guildNametags = newValue }.build()
    )

    general.addEntry(
      entryBuilder.startBooleanToggle(
        Text.literal("Bleeding Nametags"),
        ModConfig.settings.showBleedingNametags
      ).setDefaultValue(true).setTooltip(Text.literal("Shows enemy bleeding status in nametags."))
        .setSaveConsumer { newValue -> ModConfig.settings.showBleedingNametags = newValue }.build()
    )

    general.addEntry(
      entryBuilder.startBooleanToggle(
        Text.literal("Show Latency Flash"),
        ModConfig.settings.showLatencyFlash
      ).setDefaultValue(false).setTooltip(Text.literal("Flashes the screen white on click (for testing latency)."))
        .setSaveConsumer { newValue -> ModConfig.settings.showLatencyFlash = newValue }.build()
    )

    general.addEntry(
      entryBuilder.startBooleanToggle(
        Text.literal("Colorblind Glow"),
        ModConfig.settings.colorBlindOutline
      ).setDefaultValue(false).setTooltip(Text.literal("Changes outline colors for riph."))
        .setSaveConsumer { newValue -> ModConfig.settings.colorBlindOutline = newValue }.build()
    )

    general.addEntry(
      entryBuilder.startBooleanToggle(Text.literal("Ability Nametags"), ModConfig.settings.abilityNametags)
        .setDefaultValue(true)
        .setTooltip(Text.literal("Ability durations / cooldowns in the nametags."))
        .setSaveConsumer { newValue -> ModConfig.settings.abilityNametags = newValue }
        .build()
    )

    general.addEntry(
      entryBuilder.startBooleanToggle(
        Text.literal("Show Potion Info"),
        ModConfig.settings.showPotionInfo
      ).setDefaultValue(true).setTooltip(Text.literal("Shows potion count and cooldown near the crosshair."))
        .setSaveConsumer { newValue -> ModConfig.settings.showPotionInfo = newValue }.build()
    )

    general.addEntry(
      entryBuilder.startBooleanToggle(
        Text.literal("Show Combat Info"),
        ModConfig.settings.showCombatInfo
      ).setDefaultValue(true).setTooltip(Text.literal("Shows combat time near the crosshair."))
        .setSaveConsumer { newValue -> ModConfig.settings.showCombatInfo = newValue }.build()
    )

    general.addEntry(
      entryBuilder.startBooleanToggle(
        Text.literal("1440p Mode"),
        ModConfig.settings.mode1440p
      ).setDefaultValue(false).setTooltip(Text.literal("GUI scale 2 only though hah"))
        .setSaveConsumer { newValue -> ModConfig.settings.mode1440p = newValue }.build()
    )

    general.addEntry(
      entryBuilder.startBooleanToggle(
        Text.literal("Remove Nausea Overlay"),
        ModConfig.settings.removeNauseaOverlay
      ).setDefaultValue(false).setTooltip(Text.literal("The green is ugly and it breaks Exordium so"))
        .setSaveConsumer { newValue -> ModConfig.settings.removeNauseaOverlay = newValue }.build()
    )

    general.addEntry(
      entryBuilder.startBooleanToggle(
        Text.literal("Show Rogue Info"),
        ModConfig.settings.showRogueInfo
      ).setDefaultValue(true).setTooltip(Text.literal("Shows Rogue bonus timers near the crosshair."))
        .setSaveConsumer { newValue -> ModConfig.settings.showRogueInfo = newValue }.build()
    )

    general.addEntry(
      entryBuilder.startBooleanToggle(
        Text.literal("Show Bleeding Status"),
        ModConfig.settings.showBleedingInfo
      ).setDefaultValue(true).setTooltip(Text.literal("Shows your bleeding status near the crosshair."))
        .setSaveConsumer { newValue -> ModConfig.settings.showBleedingInfo = newValue }.build()
    )

    general.addEntry(
      entryBuilder.startBooleanToggle(
        Text.literal("Show Cooldown Info"),
        ModConfig.settings.showCooldownInfo
      ).setDefaultValue(true).setTooltip(Text.literal("Shows ability cooldown near the crosshair."))
        .setSaveConsumer { newValue -> ModConfig.settings.showCooldownInfo = newValue }.build()
    )

    general.addEntry(
      entryBuilder.startBooleanToggle(
        Text.literal("Filter NPCs from Minimap"), ModConfig.settings.filterNPCsFromMinimap
      ).setDefaultValue(true).setTooltip(Text.literal("Filters DR NPCs from Xaero's Minimap radar."))
        .setSaveConsumer { newValue -> ModConfig.settings.filterNPCsFromMinimap = newValue }.build()
    )

    general.addEntry(
      entryBuilder.startBooleanToggle(
        Text.literal("Fix Name Colors for Minimap"), ModConfig.settings.fixNamesForMinimap
      ).setDefaultValue(true).setTooltip(Text.literal("Fixes name colors for Xaero's Minimap radar."))
        .setSaveConsumer { newValue -> ModConfig.settings.fixNamesForMinimap = newValue }.build()
    )

    general.addEntry(
      entryBuilder.startIntSlider(
        Text.literal("Energy Lying Factor"),
        ModConfig.settings.energyBarLyingFactor,
        0,
        10
      ).setDefaultValue(0).setTooltip(Text.literal("Makes your energy bar appear lower than it should."))
        .setSaveConsumer { newValue -> ModConfig.settings.energyBarLyingFactor = newValue }.build()
    )

    general.addEntry(
      entryBuilder.startIntSlider(
        Text.literal("Minimap Name Scale"),
        ModConfig.settings.minimapNameScale,
        0,
        100
      ).setDefaultValue(0).setTooltip(Text.literal("Custom multiplier for Xaero's Minimap name scale."))
        .setSaveConsumer { newValue -> ModConfig.settings.minimapNameScale = newValue }.build()
    )

    general.addEntry(
      entryBuilder.startIntSlider(Text.literal("Shadowmeld Points"), ModConfig.settings.shadowmeldPoints, 0, 10)
        .setDefaultValue(5)
        .setTooltip(Text.literal("Sets Rogue 'Shadowmeld' node points."))
        .setSaveConsumer { newValue -> ModConfig.settings.shadowmeldPoints = newValue }
        .build()
    )

    general.addEntry(
      entryBuilder.startIntSlider(Text.literal("Just My Nature Points"), ModConfig.settings.justMyNaturePoints, 0, 5)
        .setDefaultValue(2)
        .setTooltip(Text.literal("Sets Rogue 'It's Just My Nature' node points."))
        .setSaveConsumer { newValue -> ModConfig.settings.justMyNaturePoints = newValue }
        .build()
    )

    return builder.build()
  }
}