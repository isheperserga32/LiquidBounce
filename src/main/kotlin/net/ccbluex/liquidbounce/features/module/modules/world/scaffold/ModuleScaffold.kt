/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold

import it.unimi.dsi.fastutil.ints.IntObjectPair
import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.config.NoneChoice
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.BlockCountChangeEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.SimulatedTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleSafeWalk
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.NoFallBlink
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold.ScaffoldRotationConfigurable.RotationTimingMode.*
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold.ScaffoldRotationConfigurable.considerInventory
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold.ScaffoldRotationConfigurable.rotationTiming
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ScaffoldBlockItemSelection.isValidBlock
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.*
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.*
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.normal.ScaffoldDownFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.normal.ScaffoldEagleFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower.ScaffoldTowerKarhu
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower.ScaffoldTowerMotion
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower.ScaffoldTowerPulldown
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower.ScaffoldTowerVulcan
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.withFixedYaw
import net.ccbluex.liquidbounce.utils.block.SwingMode
import net.ccbluex.liquidbounce.utils.block.doPlacement
import net.ccbluex.liquidbounce.utils.block.getCenterDistanceSquared
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTarget
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.combat.ClickScheduler
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.item.*
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.kotlin.component1
import net.ccbluex.liquidbounce.utils.kotlin.component2
import net.ccbluex.liquidbounce.utils.math.geometry.Line
import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.math.toVec3d
import net.ccbluex.liquidbounce.utils.movement.copy
import net.ccbluex.liquidbounce.utils.movement.hasNoMovement
import net.ccbluex.liquidbounce.utils.render.placement.PlacementRenderer
import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain
import net.minecraft.entity.EntityPose
import net.minecraft.item.*
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.shape.VoxelShapes
import kotlin.math.abs

/**
 * Scaffold module
 *
 * Places blocks under you.
 */
@Suppress("TooManyFunctions")
object ModuleScaffold : Module("Scaffold", Category.WORLD) {

    private var delay by intRange("Delay", 0..0, 0..40, "ticks")
    private val minDist by float("MinDist", 0.0f, 0.0f..0.25f)
    private val timer by float("Timer", 1f, 0.01f..10f)

    init {
        tree(ScaffoldAutoBlockFeature)
        tree(ScaffoldMovementPrediction)
    }

    internal val technique = choices<ScaffoldTechnique>(
        "Technique",
        ScaffoldNormalTechnique,
        arrayOf(
            ScaffoldNormalTechnique,
            ScaffoldExpandTechnique,
            ScaffoldGodBridgeTechnique,
            ScaffoldBreezilyTechnique
        )
    ).apply { tagBy(this) }

    private val sameYMode by enumChoice("SameY", SameYMode.OFF)

    enum class SameYMode(
        override val choiceName: String,
        val getTargetedBlockPos: (BlockPos) -> BlockPos?
    ) : NamedChoice {

        OFF("Off", { null }),

        /**
         * Places blocks at the same Y level as the player
         */
        ON("On", { blockPos -> BlockPos(blockPos.x, placementY, blockPos.z) }),

        /**
         * Places blocks at the same Y level as the player, but only if the player is not falling
         */
        FALLING("Falling", { blockPos ->
            BlockPos(blockPos.x, placementY, blockPos.z).takeIf { player.velocity.y < 0.2 }
        }),

        /**
         * Similar to FALLING, but only when a certain velocity is triggered and after
         * 2 jumps
         */
        HYPIXEL("Hypixel", { blockPos ->
            if (ModuleScaffold.player.velocity.y == -0.15233518685055708 && jumps >= 2) {
                jumps = 0

                BlockPos(blockPos.x, startY, blockPos.z)
            } else {
                BlockPos(blockPos.x, startY - 1, blockPos.z)
            }
        })

    }

    @Suppress("UnusedPrivateProperty")
    val towerMode = choices<Choice>("Tower", {
        it.choices[0] // None
    }) {
        arrayOf(NoneChoice(it), ScaffoldTowerMotion, ScaffoldTowerPulldown, ScaffoldTowerKarhu, ScaffoldTowerVulcan)
    }

    val isTowering: Boolean
        get() = towerMode.choices.indexOf(towerMode.activeChoice) != 0 && mc.options.jumpKey.isPressed

    // SafeWalk feature - uses the SafeWalk module as a base
    @Suppress("unused")
    private val safeWalkMode = choices("SafeWalk", {
        it.choices[1] // Safe mode
    }, ModuleSafeWalk::createChoices)

    internal object ScaffoldRotationConfigurable : RotationsConfigurable(this) {

        val considerInventory by boolean("ConsiderInventory", false)
        val rotationTiming by enumChoice("RotationTiming", NORMAL)

        enum class RotationTimingMode(override val choiceName: String) : NamedChoice {

            /**
             * Rotates the player before the block is placed
             */
            NORMAL("Normal"),

            /**
             * Rotates the player on the tick the block is placed
             */
            ON_TICK("OnTick"),

            /**
             * Similar to ON_TICK, but the player will keep the rotation after placing
             */
            ON_TICK_SNAP("OnTickSnap")

        }

    }

    private var currentTarget: BlockPlacementTarget? = null

    private var swingMode by enumChoice("Swing", SwingMode.DO_NOT_HIDE)

    object SimulatePlacementAttempts : ToggleableConfigurable(this, "SimulatePlacementAttempts", false) {

        internal val clickScheduler = tree(ClickScheduler(ModuleScaffold, false, maxCps = 100))
        val failedAttemptsOnly by boolean("FailedAttemptsOnly", true)
    }

    init {
        tree(ScaffoldRotationConfigurable)
        tree(SimulatePlacementAttempts)
        tree(ScaffoldSlowFeature)
        tree(ScaffoldJumpStrafe)
        tree(ScaffoldSpeedLimiterFeature)
        tree(ScaffoldBlinkFeature)
    }

    private var ledge by boolean("Ledge", true)

    private val renderer = tree(PlacementRenderer("Render", true, this, keep = false))

    private var placementY = 0
    private var forceSneak = 0
    private var startY = 0
    private var jumps = 0

    val blockCount: Int
        get() {
            val blockInMainHand = player.inventory.getStack(player.inventory.selectedSlot)
            val blockInOffHand = player.offHandStack

            val blocks = hashSetOf(blockInMainHand, blockInOffHand)

            if (ScaffoldAutoBlockFeature.enabled) {
                findPlaceableSlots().mapTo(blocks) { it.value() }
            }

            return blocks.sumOf { if (isValidBlock(it)) it.count else 0 }
        }

    val isBlockBelow: Boolean
        get() {
            // Check if there is a collision box below the player
            // In this case we expand the bounding box by 0.5 in all directions and check if there is a collision
            // This might cause for "Spider-like" behavior, but it's the most reliable way to check
            // and usually the scaffold should start placing blocks
            return world.getBlockCollisions(
                player,
                player.boundingBox.expand(0.5, 0.0, 0.5).offset(0.0, -1.05, 0.0)
            ).any { shape -> shape != VoxelShapes.empty() }
        }

    /**
     * This comparator will estimate the value of a block. If this comparator says that Block A > Block B, Scaffold will
     * prefer Block A over Block B.
     * The chain will prefer the block that is solid. If both are solid, it goes to the next criteria
     * (in this case full cube) and so on
     */
    val BLOCK_COMPARATOR_FOR_HOTBAR =
        ComparatorChain(
            PreferFavourableBlocks,
            PreferSolidBlocks,
            PreferFullCubeBlocks,
            PreferWalkableBlocks,
            PreferAverageHardBlocks,
            PreferStackSize(higher = false),
        )
    val BLOCK_COMPARATOR_FOR_INVENTORY =
        ComparatorChain(
            PreferFavourableBlocks,
            PreferSolidBlocks,
            PreferFullCubeBlocks,
            PreferWalkableBlocks,
            PreferAverageHardBlocks,
            PreferStackSize(higher = false),
        )

    override fun enable() {
        // Placement Y is the Y coordinate of the block below the player
        placementY = player.blockPos.y - 1
        startY = player.blockPos.y
        jumps = 2

        ScaffoldMovementPlanner.reset()
        ScaffoldMovementPrediction.reset()

        super.enable()
    }

    override fun disable() {
        NoFallBlink.waitUntilGround = false
        ScaffoldMovementPlanner.reset()
        SilentHotbar.resetSlot(this)
        updateRenderCount()
    }

    private fun updateRenderCount(count: Int? = null) = EventManager.callEvent(BlockCountChangeEvent(count))

    @Suppress("unused")
    val rotationUpdateHandler = handler<SimulatedTickEvent> { event ->
        NoFallBlink.waitUntilGround = true

        val blockInHotbar = findBestValidHotbarSlotForTarget()

        val bestStack = if (blockInHotbar == null) {
            ItemStack(Items.SANDSTONE, 64)
        } else {
            player.inventory.getStack(blockInHotbar)
        }

        val optimalLine = this.currentOptimalLine

        val predictedPos = ScaffoldMovementPrediction.getPredictedPlacementPos(optimalLine) ?: player.pos
        // Check if the player is probably going to sneak at the predicted position
        val predictedPose =
            if (ScaffoldEagleFeature.enabled && ScaffoldEagleFeature.shouldEagle(player.input.playerInput)) {
                EntityPose.CROUCHING
            } else {
                EntityPose.STANDING
            }

        ModuleDebug.debugGeometry(
            ModuleScaffold,
            "predictedPos",
            ModuleDebug.DebuggedPoint(predictedPos, Color4b(0, 255, 0, 255), size = 0.1)
        )

        val technique = if (isTowering) {
            ScaffoldNormalTechnique
        } else {
            technique.activeChoice
        }

        val target = technique.findPlacementTarget(predictedPos, predictedPose, optimalLine, bestStack)
            .also { this.currentTarget = it }

        // Debug stuff
        if (optimalLine != null && target != null) {
            val b = target.placedBlock.toVec3d().add(0.5, 1.0, 0.5)
            val a = optimalLine.getNearestPointTo(b)

            // Debug the line a-b
            ModuleDebug.debugGeometry(
                ModuleScaffold,
                "lineToBlock",
                ModuleDebug.DebuggedLineSegment(
                    from = a,
                    to = b,
                    Color4b(255, 0, 0, 255),
                ),
            )
        }

        // Do not aim yet in SKIP mode, since we want to aim at the block only when we are about to place it
        if (rotationTiming == NORMAL) {
            val rotation = technique.getRotations(target)

            // Ledge feature - AutoJump and AutoSneak
            if (ledge) {
                val ledgeRotation = rotation ?: RotationManager.currentRotation ?: player.rotation
                val (requiresJump, requiresSneak) = ledge(
                    event.simulatedPlayer,
                    target,
                    ledgeRotation,
                    technique as? ScaffoldLedgeExtension
                )

                if (requiresJump) {
                    event.movementEvent.input = event.movementEvent.input.copy(jump = true)
                }

                if (requiresSneak > 0) {
                    event.movementEvent.input = event.movementEvent.input.copy(sneak = true)
                    forceSneak = requiresSneak
                }
            }

            RotationManager.aimAt(
                rotation ?: return@handler,
                considerInventory = considerInventory,
                configurable = ScaffoldRotationConfigurable,
                provider = this@ModuleScaffold,
                priority = Priority.IMPORTANT_FOR_PLAYER_LIFE
            )
        }
    }

    var currentOptimalLine: Line? = null

    @Suppress("unused")
    private val handleMovementInput = handler<MovementInputEvent> { event ->
        this.currentOptimalLine = null

        val currentInput = event.input

        if (currentInput.hasNoMovement) {
            return@handler
        }

        this.currentOptimalLine = ScaffoldMovementPlanner.getOptimalMovementLine(event.input)
    }

    @Suppress("unused")
    private val movementInputHandler = handler<MovementInputEvent>(priority = EventPriorityConvention.SAFETY_FEATURE) {
        if (forceSneak > 0) {
            it.sneaking = true
            forceSneak--
        }
    }

    @Suppress("unused")
    val timerHandler = repeatable {
        if (timer != 1f) {
            Timer.requestTimerSpeed(timer, Priority.IMPORTANT_FOR_USAGE_1, this@ModuleScaffold)
        }
    }

    @Suppress("unused")
    val tickHandler = repeatable {
        updateRenderCount(blockCount)

        if (player.isOnGround) {
            // Placement Y is the Y coordinate of the block below the player
            placementY = player.blockPos.y - 1
            jumps++
        }

        if (mc.options.jumpKey.isPressed) {
            startY = player.blockPos.y
            jumps = 2
        }

        val target = currentTarget

        val currentRotation = if ((rotationTiming == ON_TICK || rotationTiming == ON_TICK_SNAP) && target != null) {
            target.rotation
        } else {
            RotationManager.serverRotation
        }
        val currentCrosshairTarget = technique.activeChoice.getCrosshairTarget(target, currentRotation)
        val currentDelay = delay.random()

        var hasBlockInMainHand = isValidBlock(player.inventory.getStack(player.inventory.selectedSlot))
        val hasBlockInOffHand = isValidBlock(player.offHandStack)

        if (ScaffoldAutoBlockFeature.alwaysHoldBlock) {
            hasBlockInMainHand = handleSilentBlockSelection(hasBlockInMainHand, hasBlockInOffHand)
        }

        // Prioritize by all means the main hand if it has a block
        val suitableHand =
            arrayOf(Hand.MAIN_HAND, Hand.OFF_HAND).firstOrNull { isValidBlock(player.getStackInHand(it)) }

        if (simulatePlacementAttempts(currentCrosshairTarget, suitableHand) && player.moving
            && SimulatePlacementAttempts.clickScheduler.goingToClick
        ) {
            SimulatePlacementAttempts.clickScheduler.clicks {
                doPlacement(currentCrosshairTarget!!, suitableHand!!, swingMode = swingMode)
                true
            }
        }

        if (target == null || currentCrosshairTarget == null) {
            return@repeatable
        }

        // Does the crosshair target meet the requirements?
        if (!target.doesCrosshairTargetFullFillRequirements(currentCrosshairTarget) ||
            !isValidCrosshairTarget(currentCrosshairTarget)
        ) {
            return@repeatable
        }

        if (!ScaffoldAutoBlockFeature.alwaysHoldBlock) {
            hasBlockInMainHand = handleSilentBlockSelection(hasBlockInMainHand, hasBlockInOffHand)
        }

        if (!hasBlockInMainHand && !hasBlockInOffHand) {
            return@repeatable
        }

        val handToInteractWith = if (hasBlockInMainHand) Hand.MAIN_HAND else Hand.OFF_HAND
        var wasSuccessful = false

        if (rotationTiming == ON_TICK || rotationTiming == ON_TICK_SNAP) {
            // Check if server rotation matches the current rotation
            if (currentRotation != RotationManager.serverRotation) {
                network.sendPacket(
                    Full(
                        player.x, player.y, player.z, currentRotation.yaw, currentRotation.pitch,
                        player.isOnGround
                    )
                )

                if (rotationTiming == ON_TICK_SNAP) {
                    RotationManager.aimAt(
                        currentRotation,
                        considerInventory = considerInventory,
                        configurable = ScaffoldRotationConfigurable,
                        provider = this@ModuleScaffold,
                        priority = Priority.IMPORTANT_FOR_PLAYER_LIFE
                    )
                }

            }
        }

        // Take the fall off position before placing the block
        val previousFallOffPos = currentOptimalLine?.let { l -> ScaffoldMovementPrediction.getFallOffPositionOnLine(l) }

        renderer.addBlock(target.placedBlock)
        doPlacement(currentCrosshairTarget, handToInteractWith, {
            ScaffoldMovementPlanner.trackPlacedBlock(target)
            currentTarget = null

            wasSuccessful = true
            true
        }, swingMode = swingMode)

        if (rotationTiming == ON_TICK && RotationManager.serverRotation != player.rotation) {
            network.sendPacket(
                Full(
                    player.x, player.y, player.z, player.withFixedYaw(currentRotation),
                    player.pitch, player.isOnGround
                )
            )
        }

        if (wasSuccessful) {
            ScaffoldMovementPrediction.onPlace(currentOptimalLine, previousFallOffPos)
            ScaffoldEagleFeature.onBlockPlacement()
            ScaffoldBlinkFeature.onBlockPlacement()

            waitTicks(currentDelay)
        }
    }

    private fun findPlaceableSlots() = buildList<IntObjectPair<ItemStack>> {
        for (i in 0..8) {
            val stack = player.inventory.getStack(i)

            if (isValidBlock(stack)) {
                add(IntObjectPair.of(i, stack))
            }
        }
    }

    private fun findBestValidHotbarSlotForTarget(): Int? {
        val placeableSlots = findPlaceableSlots()
        val doNotUseBelowCount = ScaffoldAutoBlockFeature.doNotUseBelowCount

        val (slot, _) = placeableSlots
            .filter { (_, stack) -> stack.count > doNotUseBelowCount }
            .maxWithOrNull { o1, o2 -> BLOCK_COMPARATOR_FOR_HOTBAR.compare(o1.value(), o2.value()) }
            ?: placeableSlots.maxWithOrNull { o1, o2 -> BLOCK_COMPARATOR_FOR_HOTBAR.compare(o1.value(), o2.value()) }
            ?: return null

        return slot
    }

    internal fun isValidCrosshairTarget(rayTraceResult: BlockHitResult): Boolean {
        val diff = rayTraceResult.pos - player.eyes

        val side = rayTraceResult.side

        // Apply minDist
        if (side.axis != Direction.Axis.Y) {
            val dist = if (side == Direction.NORTH || side == Direction.SOUTH) diff.z else diff.x

            if (abs(dist) < minDist) {
                return false
            }
        }

        return true
    }

    internal fun getTargetedPosition(blockPos: BlockPos): BlockPos {
        if (ScaffoldDownFeature.handleEvents() && ScaffoldDownFeature.shouldGoDown) {
            return blockPos.add(0, -2, 0)
        }

        if (!isTowering) {
            sameYMode.getTargetedBlockPos(blockPos)?.let { return it }
        } else if (towerMode.activeChoice == ScaffoldTowerMotion &&
            ScaffoldTowerMotion.placeOffOnNoInput && !player.moving
        ) {
            // Find the block closest to the player
            val blocks = arrayOf(
                blockPos.add(0, 0, 1),
                blockPos.add(0, 0, -1),
                blockPos.add(1, 0, 0),
                blockPos.add(-1, 0, 0)
            )

            val blockOffset = blocks.minByOrNull {
                it.getCenterDistanceSquared()
            }?.add(0, -1, 0) ?: blockPos

            // Check if block next to the player is solid
            if (!blockOffset.getState()!!.isSolidBlock(world, blockOffset)) {
                return blockOffset
            }
        }

        return blockPos.add(0, -1, 0)
    }

    private fun simulatePlacementAttempts(
        hitResult: BlockHitResult?,
        suitableHand: Hand?,
    ): Boolean {
        val stack = if (suitableHand == Hand.MAIN_HAND) {
            player.mainHandStack
        } else {
            player.offHandStack
        }

        val option = SimulatePlacementAttempts

        if (hitResult == null || suitableHand == null || !option.enabled) {
            return false
        }

        if (hitResult.type != HitResult.Type.BLOCK) {
            return false
        }

        val context = ItemUsageContext(player, suitableHand, hitResult)

        val canPlaceOnFace = (stack.item as BlockItem).getPlacementState(ItemPlacementContext(context)) != null

        return when {
            SimulatePlacementAttempts.failedAttemptsOnly -> {
                !canPlaceOnFace
            }

            sameYMode != SameYMode.OFF -> {
                context.blockPos.y == placementY && (hitResult.side != Direction.UP || !canPlaceOnFace)
            }

            else -> {
                val isTargetUnderPlayer = context.blockPos.y <= player.blockY - 1
                val isTowering =
                    context.blockPos.y == player.blockY - 1 &&
                        canPlaceOnFace &&
                        context.side == Direction.UP

                isTargetUnderPlayer && !isTowering
            }
        }
    }

    private fun handleSilentBlockSelection(hasBlockInMainHand: Boolean, hasBlockInOffHand: Boolean): Boolean {
        // Handle silent block selection
        if (ScaffoldAutoBlockFeature.enabled && !hasBlockInMainHand && !hasBlockInOffHand) {
            val bestMainHandSlot = findBestValidHotbarSlotForTarget()

            if (bestMainHandSlot != null) {
                SilentHotbar.selectSlotSilently(
                    this, bestMainHandSlot,
                    ScaffoldAutoBlockFeature.slotResetDelay
                )

                return true
            } else {
                SilentHotbar.resetSlot(this)
            }
        } else {
            SilentHotbar.resetSlot(this)
        }

        return hasBlockInMainHand
    }

}
