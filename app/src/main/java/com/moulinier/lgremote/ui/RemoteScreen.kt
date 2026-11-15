package com.moulinier.lgremote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Gamepad
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Input
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moulinier.lgremote.RemoteViewModel

private enum class Sheet { None, Apps, Inputs, Keyboard, Numbers, Settings }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteScreen(vm: RemoteViewModel) {
    val tvName by vm.tvName.collectAsStateWithLifecycle()
    val volume by vm.volume.collectAsStateWithLifecycle()
    val muted by vm.muted.collectAsStateWithLifecycle()
    var sheet by rememberSaveable { mutableStateOf(Sheet.None) }
    var touchMode by rememberSaveable { mutableStateOf(false) }
    val cs = MaterialTheme.colorScheme

    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Header
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    tvName ?: "TV LG",
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    buildString {
                        append("Connectée")
                        volume?.let { append(" · Volume $it") }
                        if (muted) append(" · Muet")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.primary,
                )
            }
            IconButton(onClick = { sheet = Sheet.Settings }) { Icon(Icons.Rounded.Settings, "Réglages") }
            IconKey(
                Icons.Rounded.PowerSettingsNew, "Éteindre", vm::powerOff,
                size = 48.dp, color = cs.errorContainer, contentColor = cs.onErrorContainer,
            )
        }

        // Quick actions
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            QuickAction(Icons.Rounded.Input, "Source") { vm.loadInputs(); sheet = Sheet.Inputs }
            QuickAction(Icons.Rounded.Apps, "Apps") { vm.loadApps(); sheet = Sheet.Apps }
            QuickAction(Icons.Rounded.Keyboard, "Clavier") { sheet = Sheet.Keyboard }
            QuickAction(Icons.Rounded.Dialpad, "123") { sheet = Sheet.Numbers }
        }

        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = !touchMode, onClick = { touchMode = false },
                shape = SegmentedButtonDefaults.itemShape(0, 2),
                icon = { SegmentedButtonDefaults.Icon(!touchMode) { Icon(Icons.Rounded.Gamepad, null, Modifier.size(18.dp)) } },
            ) { Text("Croix") }
            SegmentedButton(
                selected = touchMode, onClick = { touchMode = true },
                shape = SegmentedButtonDefaults.itemShape(1, 2),
                icon = { SegmentedButtonDefaults.Icon(touchMode) { Icon(Icons.Rounded.TouchApp, null, Modifier.size(18.dp)) } },
            ) { Text("Pavé tactile") }
        }

        // Navigation area
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            if (touchMode) {
                TouchPad(
                    onMove = vm.client::move,
                    onClick = vm.client::click,
                    onScroll = { vm.client.scroll(0, it * 2) },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                DPad(
                    onUp = { vm.button("UP") },
                    onDown = { vm.button("DOWN") },
                    onLeft = { vm.button("LEFT") },
                    onRight = { vm.button("RIGHT") },
                    onOk = { vm.button("ENTER") },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        // Back / Home / Settings
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            IconKey(Icons.AutoMirrored.Rounded.ArrowBack, "Retour", { vm.button("BACK") })
            IconKey(Icons.Rounded.Home, "Accueil", { vm.button("HOME") }, color = cs.secondaryContainer, contentColor = cs.onSecondaryContainer)
            IconKey(Icons.Rounded.Tune, "Réglages TV", { vm.button("MENU") })
        }

        // Volume / channel rockers
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Rocker("VOL", vm::volumeUp, vm::volumeDown)
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                IconKey(
                    if (muted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                    "Muet", vm::toggleMute,
                    color = if (muted) cs.tertiaryContainer else cs.surfaceContainerHighest,
                    contentColor = if (muted) cs.onTertiaryContainer else cs.onSurface,
                )
                IconKey(Icons.Rounded.Info, "Infos", { vm.button("INFO") })
            }
            Rocker("CH", vm::channelUp, vm::channelDown, upIcon = Icons.Rounded.KeyboardArrowUp, downIcon = Icons.Rounded.KeyboardArrowDown)
        }

        // Media controls
        Row(
            Modifier.fillMaxWidth().height(56.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val shape = RoundedCornerShape(18.dp)
            val m = Modifier.weight(1f).fillMaxWidth().height(56.dp)
            MediaKey(Icons.Rounded.FastRewind, "Retour rapide", vm::rewind, m, shape)
            MediaKey(Icons.Rounded.PlayArrow, "Lecture", vm::play, m, shape, primary = true)
            MediaKey(Icons.Rounded.Pause, "Pause", vm::pause, m, shape)
            MediaKey(Icons.Rounded.Stop, "Stop", vm::stop, m, shape)
            MediaKey(Icons.Rounded.FastForward, "Avance rapide", vm::fastForward, m, shape)
        }
    }

    when (sheet) {
        Sheet.Apps -> AppsSheet(vm) { sheet = Sheet.None }
        Sheet.Inputs -> InputsSheet(vm) { sheet = Sheet.None }
        Sheet.Keyboard -> KeyboardSheet(vm) { sheet = Sheet.None }
        Sheet.Numbers -> NumbersSheet(vm) { sheet = Sheet.None }
        Sheet.Settings -> SettingsSheet(vm) { sheet = Sheet.None }
        Sheet.None -> {}
    }
}

@Composable
private fun QuickAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        RemoteKey(
            onClick,
            Modifier.size(width = 72.dp, height = 48.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) { Icon(icon, label) }
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MediaKey(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier,
    shape: androidx.compose.ui.graphics.Shape,
    primary: Boolean = false,
) {
    val cs = MaterialTheme.colorScheme
    RemoteKey(
        onClick, modifier, shape,
        color = if (primary) cs.primary else cs.surfaceContainerHigh,
        contentColor = if (primary) cs.onPrimary else cs.onSurface,
    ) { Icon(icon, description) }
}
