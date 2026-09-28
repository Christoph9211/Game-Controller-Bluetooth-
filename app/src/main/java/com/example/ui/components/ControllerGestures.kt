package com.example.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize

/** Own one pointer until up/cancel; other controls can own other fingers concurrently. */
fun Modifier.controllerGesture(label: String, onPosition: (Offset?, IntSize) -> Unit): Modifier = composed {
    val current by rememberUpdatedState(onPosition)
    pointerInput(label) {
        awaitEachGesture {
            val down=awaitFirstDown()
            down.consume()
            try {
                current(down.position,size)
                while (true) {
                    val change=awaitPointerEvent().changes.firstOrNull { it.id==down.id } ?: break
                    if (!change.pressed || change.isConsumed) break
                    current(change.position,size)
                    change.consume()
                }
            } finally { current(null,size) }
        }
    }.semantics { contentDescription=label }
}

fun Modifier.controllerButton(label: String, onDown: (Boolean) -> Unit): Modifier = composed {
    val current by rememberUpdatedState(onDown)
    controllerGesture(label) { position, _ -> current(position != null) }
        .semantics {
            role=Role.Button
            onClick { current(true); current(false); true }
        }
}
