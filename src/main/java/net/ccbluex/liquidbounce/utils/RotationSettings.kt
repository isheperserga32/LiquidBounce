/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue

@Suppress("MemberVisibilityCanBePrivate")
open class RotationSettings(owner: Module, generalApply: () -> Boolean = { true }) {

    open val rotationsValue = BoolValue("Rotations", true) { generalApply() }

    val smootherModeValue = ListValue("SmootherMode",
        arrayOf("Linear", "Relative"),
        "Relative"
    ) { rotationsActive && generalApply() }
    val applyServerSideValue = BoolValue("ApplyServerSide", true) { rotationsActive && generalApply() }
    val simulateShortStopValue = BoolValue("SimulateShortStop", false) { rotationsActive && generalApply() }
    val strafeValue = BoolValue("Strafe", false) { rotationsActive && applyServerSide && generalApply() }
    val strictValue = BoolValue("Strict", false) { strafeValue.isActive() && generalApply() }
    val keepRotationValue = BoolValue("KeepRotation",
        true
    ) { rotationsActive && applyServerSide && generalApply() }
    val resetTicksValue = object : IntegerValue("ResetTicks", 1, 1..20) {
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(minimum)
        override fun isSupported() = rotationsActive && applyServerSide && generalApply()
    }

    val startRotatingSlowValue = BoolValue("StartRotatingSlow", false) { rotationsActive && generalApply() }
    val slowDownOnDirectionChangeValue = BoolValue("SlowDownOnDirectionChange",
        false
    ) { rotationsActive && generalApply() }
    val useStraightLinePathValue = BoolValue("UseStraightLinePath", true) { rotationsActive && generalApply() }
    val maxHorizontalAngleChangeValue: FloatValue = object : FloatValue("MaxHorizontalAngleChange",
        180f,
        1f..180f
    ) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtLeast(minHorizontalAngleChange)
        override fun isSupported() = rotationsActive && generalApply()
    }

    val minHorizontalAngleChangeValue: FloatValue = object : FloatValue("MinHorizontalAngleChange",
        180f,
        1f..180f
    ) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtMost(maxHorizontalAngleChange)
        override fun isSupported() = !maxHorizontalAngleChangeValue.isMinimal() && rotationsActive && generalApply()
    }

    val maxVerticalAngleChangeValue: FloatValue = object : FloatValue("MaxVerticalAngleChange", 180f, 1f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtLeast(minVerticalAngleChange)
        override fun isSupported() = rotationsActive && generalApply()
    }

    val minVerticalAngleChangeValue: FloatValue = object : FloatValue("MinVerticalAngleChange", 180f, 1f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtMost(maxVerticalAngleChange)
        override fun isSupported() = !maxVerticalAngleChangeValue.isMinimal() && rotationsActive && generalApply()
    }

    val angleResetDifferenceValue = FloatValue("AngleResetDifference",
        5f,
        0.1f..180f
    ) { rotationsActive && applyServerSide && generalApply() }

    val minRotationDifferenceValue = FloatValue("MinRotationDifference",
        0f,
        0f..1f
    ) { rotationsActive && generalApply() }

    // Variables for easier access
    val rotations by rotationsValue
    val smootherMode by smootherModeValue
    val applyServerSide by applyServerSideValue
    val simulateShortStop by simulateShortStopValue
    val strafe by strafeValue
    val strict by strictValue
    val keepRotation by keepRotationValue
    val resetTicks by resetTicksValue
    val startRotatingSlow by startRotatingSlowValue
    val slowDownOnDirectionChange by slowDownOnDirectionChangeValue
    val useStraightLinePath by useStraightLinePathValue
    val maxHorizontalAngleChange by maxHorizontalAngleChangeValue
    val minHorizontalAngleChange by minHorizontalAngleChangeValue
    val maxVerticalAngleChange by maxVerticalAngleChangeValue
    val minVerticalAngleChange by minVerticalAngleChangeValue
    val angleResetDifference by angleResetDifferenceValue
    val minRotationDifference by minRotationDifferenceValue

    var prioritizeRequest = false
    var immediate = false
    var instant = false

    open val rotationsActive
        get() = rotations

    val horizontalSpeed
        get() = minHorizontalAngleChange..maxHorizontalAngleChange

    val verticalSpeed
        get() = minVerticalAngleChange..maxVerticalAngleChange

    fun withoutKeepRotation(): RotationSettings {
        keepRotationValue.isSupported = { false }

        return this
    }

    init {
        owner.addConfigurable(this)
    }
}

class RotationSettingsWithRotationModes(
    owner: Module, listValue: ListValue, generalApply: () -> Boolean = { true },
) : RotationSettings(owner, generalApply) {

    override val rotationsValue: BoolValue
        get() = super.rotationsValue.apply { isSupported = { false } }

    val rotationModeValue = listValue.apply { isSupported = generalApply }

    val rotationMode by rotationModeValue

    override val rotationsActive: Boolean
        get() = rotationMode != "Off"

    init {
        owner.addConfigurable(this)
    }
}