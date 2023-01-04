@file:OptIn(ExperimentalComposeUiApi::class)

package splineeditor

import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.unit.toSize
import kotlin.math.max
import kotlin.math.min


internal suspend fun PointerInputScope.detectPointerGestures(
    onPress: (Offset, PointerKeyboardModifiers) -> Unit = { _, _ -> },
    onDragStart: (Offset, PointerKeyboardModifiers) -> Unit = { _, _ -> },
    onDrag: (Offset, PointerKeyboardModifiers) -> Unit = { _, _ -> },
    onDragEnd: (Offset) -> Unit = {},
    onClick: (Offset, PointerKeyboardModifiers) -> Unit = { _, _ -> },
    onDoubleClick: (Offset, PointerKeyboardModifiers) -> Unit = { _, _ -> }
) {
    forEachGesture {
        awaitPointerEventScope {
            val firstDown: PointerEvent = awaitPress()
            onPress(firstDown.position, firstDown.keyboardModifiers)
            firstDown.change.consume()
            
            // wait for far enough movement or pointer up
            val nextEvent = awaitGestureContinuing(firstDown)
            
            if (nextEvent.isMouseRelease()) {
                performClicks(nextEvent, onClick, onDoubleClick)
            } else if (nextEvent.isDrag()) {
                performDrag(nextEvent, onDragStart, onDrag, onDragEnd)
            }
        }
    }
}

private suspend fun AwaitPointerEventScope.performDrag(
    startDragEvent: PointerEvent,
    onDragStart: (Offset, PointerKeyboardModifiers) -> Unit,
    onDrag: (Offset, PointerKeyboardModifiers) -> Unit,
    onDragEnd: (Offset) -> Unit
) {
    onDragStart(startDragEvent.position, startDragEvent.keyboardModifiers)
    
    var nextEvent: PointerEvent
    do {
        nextEvent = awaitPointerEvent()
        if (nextEvent.isDrag()) {
            onDrag(nextEvent.position.clamped(size.toSize().toRect()), nextEvent.keyboardModifiers)
            nextEvent.change.consume()
        }
    } while (!nextEvent.isMouseRelease())
    
    onDragEnd(nextEvent.position.clamped(size.toSize().toRect()))
    
    nextEvent.change.consume()
}

private fun Offset.clamped(bounds: Rect): Offset {
    return Offset(
        x = x.clamped(bounds.left, bounds.right),
        y = y.clamped(bounds.top, bounds.bottom)
    )
}

private fun Float.clamped(min: Float, max: Float): Float {
    return max(min, min(max, this))
}

private suspend fun AwaitPointerEventScope.performClicks(
    firstRelease: PointerEvent,
    onClick: (Offset, PointerKeyboardModifiers) -> Unit,
    onDoubleClick: (Offset, PointerKeyboardModifiers) -> Unit
) {
    onClick(firstRelease.position, firstRelease.keyboardModifiers)
    
    try {
        withTimeout(250) {
            while (true) {
                val pressEvent = awaitPress()
                pressEvent.change.consume()
                val releaseEvent = awaitRelease()
                onDoubleClick(releaseEvent.position, releaseEvent.keyboardModifiers)
                releaseEvent.change.consume()
            }
        }
    } catch (e: PointerEventTimeoutCancellationException) {
        // Double-click ain't happening!
    }
}

private suspend fun AwaitPointerEventScope.awaitPress(): PointerEvent {
    var event: PointerEvent
    do {
        event = awaitPointerEvent()
    } while (!event.isMousePress())
    
    return event
}

private suspend fun AwaitPointerEventScope.awaitRelease(): PointerEvent {
    var event: PointerEvent
    do {
        event = awaitPointerEvent()
    } while (!event.isMouseRelease())
    
    return event
}

private val PointerEvent.position
    get() = change.position

private fun PointerEvent.isMousePress(): Boolean =
    type == PointerEventType.Press
        && changes.all { it.type == PointerType.Mouse }
        && buttons.isPrimaryPressed

private fun PointerEvent.isMouseRelease(): Boolean =
    type == PointerEventType.Release
        && changes.all { it.type == PointerType.Mouse }
        && !buttons.isPrimaryPressed

private suspend fun AwaitPointerEventScope.awaitGestureContinuing(firstDown: PointerEvent): PointerEvent {
    var nextEvent: PointerEvent
    do {
        nextEvent = awaitPointerEvent()
    } while (
        !(nextEvent.isDrag(minDistance = 8f, from = firstDown.position)
            || nextEvent.isMouseRelease())
    )
    
    return nextEvent
}

private val PointerEvent.change
    get() =
        changes.single { it.type == PointerType.Mouse }

fun PointerEvent.isDrag(minDistance: Float, from: Offset): Boolean =
    isDrag() && hasMoved(minDistance, from)

private fun PointerEvent.isDrag() =
    type == PointerEventType.Move && buttons.isPrimaryPressed

private fun PointerEvent.hasMoved(minDistance: Float, from: Offset) =
    (change.position - from).getDistance() >= minDistance
