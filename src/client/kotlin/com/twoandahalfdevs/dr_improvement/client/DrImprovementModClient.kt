package com.twoandahalfdevs.dr_improvement.client

import com.google.common.collect.EvictingQueue
import com.twoandahalfdevs.dr_improvement.mixin.ItemCooldownEntryAccessor
import com.twoandahalfdevs.dr_improvement.mixin.ItemCooldownManagerAccessor
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderTickCounter
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.BowItem
import net.minecraft.item.ItemStack
import net.minecraft.item.PotionItem
import net.minecraft.item.SplashPotionItem
import net.minecraft.registry.tag.ItemTags
import net.minecraft.scoreboard.ScoreHolder
import net.minecraft.util.Identifier
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.Vec3d
import org.joml.Math
import java.math.RoundingMode
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.*

@JvmField
var lastClickTime = 0L

@JvmField
var recentPositions: EvictingQueue<Vec3d> = EvictingQueue.create(20)

var latestExp = 0f
var prevExp = 0f
var expUpdateTime = 0L
var clas = "???"

var scoreWasUpdated: MutableMap<String, Int> = ConcurrentHashMap()
var latestCurrentHealth: MutableMap<String, Float> = ConcurrentHashMap()
var maxHealthValues: MutableMap<String, Int> = ConcurrentHashMap()

fun interpolatedExp(): Float {
  val expDelta: Float = latestExp - prevExp

  val deltaTimeMs: Float = (System.nanoTime() - expUpdateTime) / 1000000f

  val noLying = 1f + (ModConfig.settings.energyBarLyingFactor * 0.1f)

  // If we're going up, interpolate slower
  return if (expDelta > 0f) {
    (prevExp + expDelta * (deltaTimeMs / 50f).coerceIn(0f, 1f)).pow(noLying)
  } else {
    (prevExp + expDelta * (deltaTimeMs / 10f).coerceIn(0f, 1f)).pow(noLying)
  }
}

lateinit var INSTANCE: DrImprovementModClient


class DrImprovementModClient : ClientModInitializer, ClientTickEvents.StartTick, ClientTickEvents.EndTick {
  override fun onInitializeClient() {
    INSTANCE = this
    ModConfig.load()
    ClientTickEvents.START_CLIENT_TICK.register(this)
    ClientTickEvents.END_CLIENT_TICK.register(this)

    HudElementRegistry.addLast(
      Identifier.of(
        "dr_improvement",
        "latency_flash"
      )
    ) { context: DrawContext, _: RenderTickCounter ->
      if (!ModConfig.settings.showLatencyFlash) {
        return@addLast
      }

      val timeSinceClick = System.currentTimeMillis() - lastClickTime

      if (timeSinceClick in 0 until 100) {
        val client = MinecraftClient.getInstance()
        val width = client.window.scaledWidth
        val height = client.window.scaledHeight

        context.fill(0, 0, width, height, 0xFFFFFFFF.toInt())
      }
    }

    HudElementRegistry.addFirst(
      Identifier.of(
        "dr_improvement",
        "dr_info"
      )
    ) { context: DrawContext, _: RenderTickCounter ->
      renderInfo(context)
    }
  }

  fun renderInfo(context: DrawContext) {
    val minecraft = MinecraftClient.getInstance()
    val xCenter = minecraft.window.scaledWidth / 2
    val yCenter = minecraft.window.scaledHeight / 2
    val textRenderer = minecraft.textRenderer

    val xOffset = 8
    val yOffset = 0
    val yOffsetBleed = if (ModConfig.settings.mode1440p) 14 else 11
    val ySpreadAbilPots = if (ModConfig.settings.mode1440p) -34 else -26
    val textColor = 0x8CFFFFFF.toInt()
    val ySpreadCombat = if (ModConfig.settings.mode1440p) 36 else 27
    val xOffsetBottom = 12

    val lowDurabilityLabels = minecraft.player?.let(::lowDurabilityItems).orEmpty()

    if (lowDurabilityLabels.isNotEmpty()) {
      val warningText = "§lLow Durability: ${lowDurabilityLabels.joinToString(", ")}"
      context.drawTextWithShadow(
        textRenderer,
        warningText,
        xCenter - textRenderer.getWidth(warningText) / 2,
        minecraft.window.scaledHeight / 6,
        0xADD13F3F.toInt(),
      )
    }

    // Potions!!!
    if (ModConfig.settings.showPotionInfo) {
      context.drawTextWithShadow(
        textRenderer,
        potsStr,
        xCenter + xOffset,
        yCenter - yOffset + ySpreadAbilPots,
        potsStrColor.toInt(),
      )
    }

    // Cooldown!!!
    if (ModConfig.settings.showCooldownInfo) {
      context.drawTextWithShadow(
        textRenderer,
        cdStr,
        xCenter - minecraft.textRenderer.getWidth(cdStr) - xOffset,
        yCenter - yOffset + ySpreadAbilPots,
        textColor,
      )
    }

    val bleedSecs = 5.0 - (System.currentTimeMillis() - lastBleedTime) / 1000.0
    if (ModConfig.settings.showBleedingInfo && bleedSecs >= 0.0) {
      val bleedStr = "§4Bleed: ${bleedSecs.toBigDecimal().setScale(1, RoundingMode.UP).toDouble()}"
      var y = yCenter - yOffset + ySpreadAbilPots
      if (ModConfig.settings.showPotionInfo || ModConfig.settings.showCooldownInfo) {
        y -= yOffsetBleed
      }
      context.drawTextWithShadow(
        textRenderer,
        bleedStr,
        xCenter - minecraft.textRenderer.getWidth(bleedStr) / 2,
        y,
        textColor,
      )
    }

    val probablyCombatTimer =
      (combatTimer - (System.currentTimeMillis() - lastUpdatedCombatTime) / 1000.0).coerceAtLeast(
        0.0
      )

    val probablyBonusTimer =
      (bonusTimer - (System.currentTimeMillis() - lastUpdatedBonusTime) / 1000.0).coerceAtLeast(
        0.0
      )

    val combatStr = if (probablyCombatTimer > 0) "§c${
      probablyCombatTimer.toBigDecimal().setScale(1, RoundingMode.UP).toDouble()
    }s" else "§a:)"

    val combatstrWidth = minecraft.textRenderer.getWidth(combatStr)
    if (ModConfig.settings.showCombatInfo) {
      context.drawTextWithShadow(
        textRenderer,
        combatStr,
        xCenter - (if (clas.contains("Rogue") && ModConfig.settings.showRogueInfo) combatstrWidth + xOffsetBottom else combatstrWidth / 2),
        yCenter - yOffset + ySpreadCombat,
        textColor,
      )
    }

    val bonusStr = if (probablyCombatTimer <= 0) {
      "§a(:"
    } else {
      if (probablyBonusTimer > 0) "§a${
        probablyBonusTimer.toBigDecimal().setScale(1, RoundingMode.UP).toDouble()
      }s" else "§c):"
    }

    if (clas.contains("Rogue") && ModConfig.settings.showRogueInfo) {
      val bonusStrWidth = minecraft.textRenderer.getWidth(bonusStr)
      val xPos = if (ModConfig.settings.showCombatInfo) {
        xCenter + xOffsetBottom
      } else {
        xCenter - bonusStrWidth / 2
      }

      context.drawTextWithShadow(
        textRenderer,
        bonusStr,
        xPos,
        yCenter - yOffset + ySpreadCombat,
        textColor,
      )
    }
  }

  var switchBackSlot = 0
  var switchBackTicks: Int? = null

  private var pots = 5
  private var potCd: Int? = null
  private var totalPots = 0

  var actionBarMsg: String? = ""
  var actionBarTime = 0

  private var cdActive = false
  private var cd: Int? = null
  private var lastUpdatedCdTime = System.currentTimeMillis()
  private var lastUpdatedPotCdTime = System.currentTimeMillis()
  private var cdStr = ""
  private var potsStr = ""
  private var potsStrColor = 0x8CFFFFFF

  private val ultCdReg =
    """§(.)(?:The Fast|Berserk|Divine Protection|Deaths Grasp): \[(?:([0-9]*)m)? ?(?:([0-9]*)s)?]""".toRegex()
  private val potReg = """\[([0-9]*)/5] Potions: \[([0-9]*)s]""".toRegex()

  private val guildReg = """\[.+]""".toRegex()

  private val abilityReg = """(.*) has activated The Fast""".toRegex()
  private val debugDmg = """[0-9]+ \S*DMG -> (.+) \[[0-9]+ HP]|-[0-9]+ \S*HP \((.+)\)""".toRegex()
  private val notRealCombat = listOf("FALL")
  private val reflectReg = """\*\s+(OPPONENT\s+)?REFLECTED.*\[\d+]""".toRegex()
  private val bleedingReg = """^\s*\*\sTARGET\s+BLEEDING\s\*\s*$""".toRegex()
  private val imBleedingReg = """^\s*\*\sBLEEDING\s\*\s*$""".toRegex()
  private val anyActiveStartReg =
    """^([^:]+) has activated (The Fast|Berserk|Divine Protection|Death's Grasp)""".toRegex()

  val playerCdMap = hashMapOf<Int, Pair<String, Long>>()
  val playerBleedMap = hashMapOf<Int, Long>()

  private val combatBonusTime
    get() = 4 + 0.5 * ModConfig.settings.justMyNaturePoints

  private val basePveTime = 8.0
  private val combatPvETime: Double
    get() = if (clas.contains("Rogue")) {
      val shadowmeldMult = 1.0 - ModConfig.settings.shadowmeldPoints * 0.035
      basePveTime * shadowmeldMult
    } else {
      basePveTime
    }

  private val basePvpTime = 15.0
  private val combatPvPTime: Double
    get() = if (clas.contains("Rogue")) {
      val shadowmeldMult = 1.0 - ModConfig.settings.shadowmeldPoints * 0.035
      basePvpTime * shadowmeldMult
    } else {
      basePvpTime
    }

  private fun durationFromAbility(ability: String?) = when (ability) {
    "The Fast" -> 8
    "Berserk" -> 10
    "Divine Protection" -> 10
    "Death's Grasp" -> 5
    else -> null
  }

  private fun isPotion(stack: ItemStack): Boolean {
    if (stack.isEmpty) return false
    return stack.item is PotionItem || stack.item is SplashPotionItem
  }

  private fun lowDurabilityItems(player: PlayerEntity): List<String> {
    val lowItems = mutableListOf<String>()

    addLowDurabilityLabel(lowItems, "Helmet", player.getEquippedStack(EquipmentSlot.HEAD))
    addLowDurabilityLabel(lowItems, "Chestplate", player.getEquippedStack(EquipmentSlot.CHEST))
    addLowDurabilityLabel(lowItems, "Leggings", player.getEquippedStack(EquipmentSlot.LEGS))
    addLowDurabilityLabel(lowItems, "Boots", player.getEquippedStack(EquipmentSlot.FEET))

    val weapon = player.inventory.getStack(0)
    if (isWeapon(weapon)) {
      addLowDurabilityLabel(lowItems, "Weapon", weapon)
    }

    val bow = player.inventory.getStack(1)
    if (bow.item is BowItem) {
      addLowDurabilityLabel(lowItems, "Bow", bow)
    }

    return lowItems
  }

  private fun addLowDurabilityLabel(lowItems: MutableList<String>, label: String, stack: ItemStack) {
    val nbt = ItemNbtStuff.customData(stack)
    val durabilityPercent = ItemNbtStuff.durabilityPercent(nbt) ?: return
    val rarity = ItemNbtStuff.itemRarity(nbt)
    val tier = ItemNbtStuff.itemTier(nbt)

    // Ignore <rare t1 and common t2+
    val uncommonOrLower = rarity.equals("COMMON", true) ||
      rarity.equals("UNCOMMON", true)

    val ignore = rarity.equals("COMMON", true) || (tier == 1 && uncommonOrLower)
    if (!ignore && durabilityPercent <= ItemNbtStuff.LOW_DURABILITY_THRESHOLD) {
      lowItems.add(label)
    }
  }

  private fun isWeapon(stack: ItemStack): Boolean {
    return stack.isIn(ItemTags.SWORDS)
      || stack.isIn(ItemTags.AXES)
      || stack.isIn(ItemTags.HOES)
      || stack.isIn(ItemTags.SHOVELS)
      || stack.item is BowItem
  }

  private fun vanillaPotionCooldownSecs(player: PlayerEntity): Int? {
    val cooldownManager = player.itemCooldownManager
    val cooldownAccessor = cooldownManager as ItemCooldownManagerAccessor
    val potionStacks = sequenceOf(
      player.inventory.mainStacks.asSequence(),
      sequenceOf(player.offHandStack),
    ).flatten()

    potionStacks.forEach { stack ->
      if (!isPotion(stack) || !cooldownManager.isCoolingDown(stack)) return null

      val group = cooldownManager.getGroup(stack)
      val cooldown = cooldownAccessor.entries()[group] as? ItemCooldownEntryAccessor ?: return@forEach
      val remainingTicks = (cooldown.endTick() - cooldownAccessor.tick()).coerceAtLeast(1)
      return ceil(remainingTicks / 20.0).toInt()
    }

    return null
  }

  fun cdString(playerId: Int): String? {
    val (abil, activationTime) = playerCdMap[playerId] ?: return null
    val dur = durationFromAbility(abil) ?: return null
    val secsSinceActivation = (System.currentTimeMillis() - activationTime) / 1000.0

    return if (secsSinceActivation < dur) {
      // Still active
      "§a§l${dur - ceil(secsSinceActivation).roundToInt()}"
    } else {
      null
    }
  }

  fun bleedString(playerId: Int): String? {
    val bleedStartTime = playerBleedMap[playerId] ?: return null
    val secsSinceBleed = (System.currentTimeMillis() - bleedStartTime) / 1000.0

    return if (secsSinceBleed <= 5) {
      "§4§l${ceil(5.0 - secsSinceBleed).roundToInt()}"
    } else {
      null
    }
  }

  // Might need to rollback if we get a reflect in there
  var rollbackLastUpdatedBonusTime = System.currentTimeMillis()
  var rollbackLastUpdatedCombatTime = System.currentTimeMillis()
  var rollbackBonusTimer = 0.0
  var rollbackCombatTimer = 0.0

  // Combat tracking stuff
  var lastUpdatedBonusTime = System.currentTimeMillis()
    set(value) {
      if (!rollingBack)
        rollbackLastUpdatedBonusTime = field
      field = value
    }

  var lastUpdatedCombatTime = System.currentTimeMillis()
    set(value) {
      if (!rollingBack)
        rollbackLastUpdatedCombatTime = field
      field = value
    }

  var bonusTimer = 0.0
    set(value) {
      if (!rollingBack)
        rollbackBonusTimer = field
      field = value
    }

  var combatTimer = 0.0
    set(value) {
      if (!rollingBack)
        rollbackCombatTimer = field
      field = value
    }

  var rollingBack = false
  var lastBleedTime = System.currentTimeMillis()
  var bleedNextTarget = false

  private fun rollBackCombatTimers() {
    rollingBack = true
    lastUpdatedBonusTime = rollbackLastUpdatedBonusTime
    lastUpdatedCombatTime = rollbackLastUpdatedCombatTime
    bonusTimer = rollbackBonusTimer
    combatTimer = rollbackCombatTimer
    rollingBack = false
  }

  fun onChatMessage(msg: String) {
    val minecraft = MinecraftClient.getInstance()

    if (bleedingReg.find(msg) != null) {
      bleedNextTarget = true
    }

    if (imBleedingReg.find(msg) != null) {
      lastBleedTime = System.currentTimeMillis()
    }

    // If we reflect, we're not really in combat
    val reflectMatches = reflectReg.find(msg)
    if (reflectMatches != null) {
      rollBackCombatTimers()
      return
    }

    val world = minecraft.world ?: return
    val activeStartMatches = anyActiveStartReg.find(msg)
    if (activeStartMatches != null) {
      val name = activeStartMatches.groupValues.getOrNull(1)
      val abil = activeStartMatches.groupValues.getOrNull(2)
      val p = world.players.find {
        name?.endsWith(it.name.string) == true
      }

      if (p != null && abil != null) {
        playerCdMap[p.id] = Pair(abil, System.currentTimeMillis())
      }
    }

    val abilMatches = abilityReg.find(msg)
    val matches = abilMatches?.groupValues?.getOrNull(1)
    if (matches?.endsWith(minecraft.player!!.name.string) == true) {
      // Ability does NOT take us out of combat now
      lastUpdatedBonusTime = System.currentTimeMillis()
      bonusTimer = combatBonusTime
    }

    val dmgMatches = debugDmg.find(msg)
    val attacked = dmgMatches?.groupValues?.getOrNull(1)
    val attacker = dmgMatches?.groupValues?.getOrNull(2)
    val probablyCombatTimer =
      (combatTimer - (System.currentTimeMillis() - lastUpdatedCombatTime) / 1000.0).coerceAtLeast(
        0.0
      )

    if (!attacked.isNullOrEmpty()) {
      val attackedPlayer = world.players.find {
        attacked.endsWith(it.name.string)
      }

      val attackedIsPlayer =
        attacked.contains(guildReg) || attackedPlayer != null

      // Attacked is player pvp combat
      if (attackedIsPlayer) {
        // COMBAT BONUS WHOAHHO
        if (probablyCombatTimer <= 0.0) {
          lastUpdatedBonusTime = System.currentTimeMillis()
          bonusTimer = combatBonusTime
        }

        lastUpdatedCombatTime = System.currentTimeMillis()
        combatTimer = max(combatPvPTime, probablyCombatTimer)

        // Bleed this guy!
        if (bleedNextTarget && attackedPlayer != null) {
          playerBleedMap[attackedPlayer.id] = System.currentTimeMillis()
        }
      } else {
        // Attacked is monster pve combat

        // COMBAT BONUS WHOAHHO
        if (probablyCombatTimer <= 0.0) {
          lastUpdatedBonusTime = System.currentTimeMillis()
          bonusTimer = combatBonusTime
        }

        lastUpdatedCombatTime = System.currentTimeMillis()
        combatTimer = max(combatPvETime, probablyCombatTimer)
      }

      // They're bleeeedin now
      bleedNextTarget = false
    } else if (!attacker.isNullOrEmpty()) {
      // MONSTER??
      if (attacker !in notRealCombat) {
        // COMBAT BONUS WHOAHHO
        if (probablyCombatTimer <= 0.0) {
          lastUpdatedBonusTime = System.currentTimeMillis()
          bonusTimer = combatBonusTime
        }

        val playersContainsAttacker = world.players.any {
          it.name.string == attacker
        }

        lastUpdatedCombatTime = System.currentTimeMillis()
        combatTimer = if (playersContainsAttacker) {
          max(combatPvPTime, probablyCombatTimer)
        } else {
          max(combatPvETime, probablyCombatTimer)
        }
      }
    }
  }

  override fun onStartTick(client: MinecraftClient) {
    switchBackTicks?.let { ticks ->
      if (ticks == 0) {
        client.player?.getInventory()?.selectedSlot = switchBackSlot
        switchBackTicks = null
      } else {
        switchBackTicks = ticks - 1
      }
    }

    val world = client.world ?: return

    // Update health
    val iter = scoreWasUpdated.entries.iterator()
    while (iter.hasNext()) {
      val entry = iter.next()
      val player = world.getPlayers()
        .firstOrNull { p -> p.stringifiedName == entry.key }
      val health = latestCurrentHealth.getOrDefault(entry.key, null)
      if (player != null && health != null && health > 0f) {
        val clientMaxHealth = player.maxHealth
        if (clientMaxHealth > 0f) {
          val ratio = clientMaxHealth / health
          val maxHealth = entry.value.toDouble() * ratio
          if (!maxHealth.isNaN() && maxHealth > 0f) {
            maxHealthValues[player.stringifiedName] = round(maxHealth).toInt()
          }
        }
        iter.remove()
      }
    }

    // Update the scoreboard to reflect real health values
    val scoreboard = world.scoreboard ?: return
    val healthObjective = scoreboard.getNullableObjective("health") ?: return
    val scores = scoreboard.getScoreboardEntries(healthObjective)
    for (score in scores) {
      val maxHealth = maxHealthValues[score.owner] ?: continue
      val player = world.getPlayers()
        .firstOrNull { p -> p.stringifiedName == score.owner() }

      if (player != null) {
        val clientMaxHealth = player.maxHealth
        if (clientMaxHealth > 0) {
          val ratio = player.health / clientMaxHealth
          val newValue = Math.round(maxHealth.toDouble() * ratio).toInt()

          val scoreHolder = ScoreHolder.fromName(score.owner())
          val scoreAccess = scoreboard.getOrCreateScore(scoreHolder, healthObjective)
          scoreAccess.score = newValue
        }
      }
    }
  }

  override fun onEndTick(p0: MinecraftClient) {
    // Cooldown
    if (actionBarTime > 0 && actionBarMsg != null) {
      val cdMatches = ultCdReg.find(actionBarMsg!!)
      if (cdMatches != null) {
        try {
          val col = cdMatches.groupValues.getOrNull(1)
          val min = cdMatches.groupValues.getOrNull(2)
          val sec = cdMatches.groupValues.getOrNull(3)
          val minutes = if (!min.isNullOrEmpty()) min.toInt() else 0
          val seconds = if (!sec.isNullOrEmpty()) sec.toInt() else 0
          cd = minutes * 60 + seconds
          cdActive = col == "a"
          lastUpdatedCdTime = System.currentTimeMillis() - ((60 - actionBarTime) * 50)
        } catch (e: NumberFormatException) {
          e.printStackTrace()
        }
      } else {
        cd = null
      }

      val potMatches = potReg.find(actionBarMsg!!)
      if (potMatches != null) {
        pots = potMatches.groupValues.getOrNull(1)?.toInt() ?: 0
        potCd = potMatches.groupValues.getOrNull(2)?.toInt()
        lastUpdatedPotCdTime =
          System.currentTimeMillis() - ((60 - actionBarTime) * 50)
      } else {
        // Nothing on the bar, no pot cooldown.
        pots = 5
        potCd = null
      }
    } else {
      cd = null
      pots = 5
      potCd = null
      potsStr = ""
      cdStr = ""
    }

    val minecraft = MinecraftClient.getInstance()
    val player = minecraft.player ?: return

    val vanillaPotCd = vanillaPotionCooldownSecs(player)

    totalPots = player.inventory.mainStacks.count {
      it.item.translationKey.equals("item.minecraft.potion") || it.item.translationKey.equals("item.minecraft.splash_potion")
    }

    val probablyTheCoolDownNow = cd?.let {
      (it - (System.currentTimeMillis() - lastUpdatedCdTime) / 1000)
    }

    // Update pots str
    potsStrColor = 0x8cfa5043
    val usablePots = pots.coerceAtMost(totalPots)
    if (usablePots >= 5) potsStrColor = 0x8c52ff80
    else if (usablePots >= 3) potsStrColor = 0x8ceeff6b
    else if (usablePots >= 1) potsStrColor = 0x8cff8e38
    potsStr = "$usablePots / $totalPots"
    val actionBarPotCdNow = potCd?.let {
      (it - (System.currentTimeMillis() - lastUpdatedPotCdTime) / 1000).coerceAtLeast(0)
    }
    val displayedPotCd = vanillaPotCd ?: actionBarPotCdNow
    val potCdColor = if (vanillaPotCd != null) "§c" else "§7"
    if (displayedPotCd != null) {
      potsStr += " $potCdColor(${displayedPotCd}s)"
    }

    // Update cd str
    val cdAboveZero = probablyTheCoolDownNow != null &&
      // cd over zero OR it's been less than a second since our last update
      (probablyTheCoolDownNow > 0 || (System.currentTimeMillis() - lastUpdatedCdTime < 1000))
    cdStr = if (cdAboveZero) "§${if (cdActive) "a" else "c"}${probablyTheCoolDownNow}s" else "§aReady"
  }
}

fun getBlockHitPosition(): BlockHitResult? {
  val maxBlockHitDistance = 4.5
  val camera = MinecraftClient.getInstance().cameraEntity
  val tickProgress = MinecraftClient.getInstance().renderTickCounter.getTickProgress(true)
  if (camera == null) return null

  val cameraPos = camera.getCameraPosVec(tickProgress)
  val hitResult = camera.raycast(maxBlockHitDistance, tickProgress, false)

  if (!cameraPos.isInRange(cameraPos, maxBlockHitDistance) || hitResult.type != HitResult.Type.BLOCK) return null
  return hitResult as BlockHitResult
}
