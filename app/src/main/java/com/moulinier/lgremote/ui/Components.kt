package com.moulinier.lgremote.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * A pressable surface that fires on touch-down (like a real remote), with haptics,
 * a press animation and optional auto-repeat while held.
 */
@Composable
fun RemoteKey(
    onPress: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    color: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    repeat: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val haptics = LocalHaptics.current
    val scope = rememberCoroutineScope()
    val action by rememberUpdatedState(onPress)
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, label = "scale")
    val bg by animateColorAsState(if (pressed) lerp(color, contentColor, 0.16f) else color, label = "bg")

    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(bg)
            .pointerInput(repeat) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    haptics?.tap()
                    action()
                    val job = if (repeat) scope.launch {
                        delay(450)
                        while (true) {
                            action()
                            haptics?.tick()
                            delay(120)
                        }
                    } else null
                    waitForUpOrCancellation()
                    job?.cancel()
                    pressed = false
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) { content() }
    }
}

@Composable
fun IconKey(
    icon: ImageVector,
    description: String,
    onPress: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    color: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    shape: Shape = CircleShape,
    repeat: Boolean = false,
) {
    RemoteKey(onPress, modifier.size(size), shape, color, contentColor, repeat) {
        Icon(icon, contentDescription = description, modifier = Modifier.size(size * 0.45f))
    }
}

/** Circular directional pad with a central OK key. */
@Composable
fun DPad(
    onUp: () -> Unit,
    onDown: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onOk: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val d = minOf(maxWidth, maxHeight)
        val key = d * 0.32f
        val offset = d * 0.33f
        Box(
            Modifier
                .size(d)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            val c = MaterialTheme.colorScheme.surfaceContainerHigh
            IconKey(Icons.Rounded.KeyboardArrowUp, "Haut", onUp, Modifier.offset(y = -offset), key, c, repeat = true)
            IconKey(Icons.Rounded.KeyboardArrowDown, "Bas", onDown, Modifier.offset(y = offset), key, c, repeat = true)
            IconKey(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Gauche", onLeft, Modifier.offset(x = -offset), key, c, repeat = true)
            IconKey(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Droite", onRight, Modifier.offset(x = offset), key, c, repeat = true)
            RemoteKey(
                onOk,
                Modifier.size(d * 0.36f),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Text("OK", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** Trackpad: drag to move the TV's magic-remote pointer, tap to click, right strip to scroll. */
@Composable
fun TouchPad(
    onMove: (Int, Int) -> Unit,
    onClick: () -> Unit,
    onScroll: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHaptics.current
    val move by rememberUpdatedState(onMove)
    val click by rememberUpdatedState(onClick)
    val scroll by rememberUpdatedState(onScroll)
    Box(
        modifier
            .clip(RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(end = 56.dp)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        var travelled = 0f
                        var accX = 0f
                        var accY = 0f
                        val start = down.uptimeMillis
                        var lastUp = start
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) { lastUp = change.uptimeMillis; break }
                            val delta = change.positionChange()
                            travelled += abs(delta.x) + abs(delta.y)
                            accX += delta.x * 0.9f
                            accY += delta.y * 0.9f
                            val dx = accX.toInt()
                            val dy = accY.toInt()
                            if (dx != 0 || dy != 0) {
                                move(dx, dy)
                                accX -= dx; accY -= dy
                            }
                            change.consume()
                        }
                        if (travelled < viewConfiguration.touchSlop && lastUp - start < 300) {
                            haptics?.tap()
                            click()
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Rounded.TouchApp, null,
                    Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
                Text(
                    "Glissez pour déplacer · Touchez pour cliquer",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        }
        // Scroll strip
        var acc by remember { mutableFloatStateOf(0f) }
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(48.dp)
                .padding(vertical = 12.dp, horizontal = 4.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            acc += change.positionChange().y
                            if (abs(acc) > 24f) {
                                scroll(if (acc > 0) 1 else -1)
                                haptics?.tick()
                                acc = 0f
                            }
                            change.consume()
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier.fillMaxHeight().padding(vertical = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Rounded.KeyboardArrowUp, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(Icons.Rounded.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Vertical pill with + / − (volume, channels). */
@Composable
fun Rocker(
    label: String,
    onUp: () -> Unit,
    onDown: () -> Unit,
    modifier: Modifier = Modifier,
    upIcon: ImageVector = Icons.Rounded.Add,
    downIcon: ImageVector = Icons.Rounded.Remove,
) {
    Column(
        modifier
            .width(72.dp)
            .clip(RoundedCornerShape(36.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val c = MaterialTheme.colorScheme.surfaceContainerHigh
        RemoteKey(onUp, Modifier.fillMaxWidth().aspectRatio(1f), color = c, repeat = true) {
            Icon(upIcon, "$label +")
        }
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        RemoteKey(onDown, Modifier.fillMaxWidth().aspectRatio(1f), color = c, repeat = true) {
            Icon(downIcon, "$label −")
        }
    }
}
