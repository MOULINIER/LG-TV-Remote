package com.moulinier.lgremote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.PanTool
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** One-handed mode: shrink the remote into the bottom corner reachable by the thumb. */
enum class Handedness(val label: String) {
    Off("Désactivé"), Left("Gaucher"), Right("Droitier");

    companion object {
        const val MIN_SCALE = 0.6f
        const val MAX_SCALE = 0.9f
        const val DEFAULT_SCALE = 0.75f
        fun parse(name: String?) = entries.firstOrNull { it.name == name } ?: Off
    }
}

/**
 * Lays [content] out at full size, then scales it down towards the bottom-left
 * (left-handed) or bottom-right (right-handed) corner. Touch input follows the
 * scaling. The freed side strip holds a button to switch hands.
 */
@Composable
fun OneHandedBox(
    handedness: Handedness,
    scale: Float,
    onSwitchSide: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (handedness == Handedness.Off) {
        Box(modifier.fillMaxSize()) { content() }
        return
    }
    val left = handedness == Handedness.Left
    BoxWithConstraints(modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(if (left) 0f else 1f, 1f)
            },
        ) { content() }

        // Switch-hand button, centred in the freed side strip.
        val strip = maxWidth * (1f - scale)
        Box(
            Modifier
                .align(if (left) Alignment.BottomEnd else Alignment.BottomStart)
                .width(strip)
                .fillMaxHeight(scale),
            contentAlignment = Alignment.Center,
        ) {
            if (strip >= 48.dp) {
                FilledTonalIconButton(onClick = onSwitchSide) {
                    Icon(
                        if (left) Icons.AutoMirrored.Rounded.KeyboardArrowRight else Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        "Changer de main",
                    )
                }
            }
        }
    }
}

@Composable
internal fun OneHandedSetting(
    handedness: Handedness,
    scale: Float,
    onHandedness: (Handedness) -> Unit,
    onScale: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Rounded.PanTool, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.weight(1f)) {
                Text("Mode une main", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Réduit la télécommande dans le coin inférieur, à portée du pouce",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            val entries = Handedness.entries
            entries.forEachIndexed { i, h ->
                SegmentedButton(
                    selected = h == handedness,
                    onClick = { onHandedness(h) },
                    shape = SegmentedButtonDefaults.itemShape(i, entries.size),
                    icon = {},
                ) { Text(h.label, maxLines = 1) }
            }
        }
        if (handedness != Handedness.Off) {
            // Local state while dragging; persisted when the finger lifts.
            var value by remember(scale) { mutableFloatStateOf(scale) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Taille", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = value,
                    onValueChange = { value = it },
                    onValueChangeFinished = { onScale(value) },
                    valueRange = Handedness.MIN_SCALE..Handedness.MAX_SCALE,
                    steps = 5,
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                )
                Text("${(value * 100).roundToInt()} %", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
