package com.example.bluetooth

import kotlin.math.hypot

/** One complete state: releasing one finger never releases another control. */
class ControllerInput {
    private val held = mutableSetOf<String>()
    var lx = 0f; var ly = 0f; var rx = 0f; var ry = 0f
    var lt = 0f; var rt = 0f
    var hat = 8
    fun button(name: String, down: Boolean) {
        require(name in buttons) { "Unknown controller button: $name" }
        if (down) held.add(name) else held.remove(name)
    }
    fun reset() { held.clear(); lx=0f; ly=0f; rx=0f; ry=0f; lt=0f; rt=0f; hat=8 }
    fun report(deadzonePct: Int): ByteArray {
        fun stick(x: Float, y: Float): Pair<Float, Float> {
            if (!x.isFinite() || !y.isFinite()) return 0f to 0f
            val length = hypot(x, y)
            val deadzone = deadzonePct.coerceIn(0, 25) / 100f
            if (length <= deadzone || length == 0f) return 0f to 0f
            val scale = (length.coerceAtMost(1f) - deadzone) / (1f - deadzone) / length
            return x * scale to y * scale
        }
        val left=stick(lx,ly); val right=stick(rx,ry)
        return GamepadReport.encode(held.fold(0) { bits, name -> bits or buttons.getValue(name) },
            hat,left.first,left.second,right.first,right.second,lt,rt)
    }
    companion object {
        val buttons = mapOf("A" to GamepadReport.A, "B" to GamepadReport.B,
            "X" to GamepadReport.X, "Y" to GamepadReport.Y,
            "LB" to GamepadReport.L1, "RB" to GamepadReport.R1,
            "SELECT" to GamepadReport.SELECT, "START" to GamepadReport.START,
            "L3" to GamepadReport.L3, "R3" to GamepadReport.R3)
    }
}
