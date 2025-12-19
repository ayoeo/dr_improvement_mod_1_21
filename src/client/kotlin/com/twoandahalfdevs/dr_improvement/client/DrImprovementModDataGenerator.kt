package com.twoandahalfdevs.dr_improvement.client

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator

class DrImprovementModDataGenerator : DataGeneratorEntrypoint {
  override fun onInitializeDataGenerator(fabricDataGenerator: FabricDataGenerator) {
    val pack = fabricDataGenerator.createPack()
  }
}
