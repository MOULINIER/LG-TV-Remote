package com.moulinier.lgremote.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.KeyboardReturn
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Input
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.moulinier.lgremote.RemoteViewModel
import com.moulinier.lgremote.tv.ConnectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemoteSheet(title: String, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            content()
        }
    }
}

@Composable
fun AppsSheet(vm: RemoteViewModel, onDismiss: () -> Unit) {
    val apps by vm.apps.collectAsStateWithLifecycle()
    RemoteSheet("Applications", onDismiss) {
        if (apps.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(88.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(480.dp),
            ) {
                items(apps, key = { it.id }) { app ->
                    Column(
                        Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { vm.launch(app); onDismiss() }
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                        ) {
                            if (app.icon != null) {
                                AsyncImage(
                                    model = app.icon,
                                    contentDescription = app.title,
                                    contentScale = ContentScale.Crop,
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Tv, null) }
                            }
                        }
                        Text(
                            app.title,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InputsSheet(vm: RemoteViewModel, onDismiss: () -> Unit) {
    val inputs by vm.inputs.collectAsStateWithLifecycle()
    RemoteSheet("Source", onDismiss) {
        if (inputs.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn {
                item {
                    ListItem(
                        headlineContent = { Text("Télévision") },
                        leadingContent = { Icon(Icons.Rounded.Tv, null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable {
                            vm.client.send("ssap://system.launcher/launch", org.json.JSONObject().put("id", "com.webos.app.livetv"))
                            onDismiss()
                        },
                    )
                }
                items(inputs, key = { it.id }) { input ->
                    ListItem(
                        headlineContent = { Text(input.label) },
                        supportingContent = { Text(input.id) },
                        leadingContent = { Icon(Icons.Rounded.Input, null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { vm.switchInput(input); onDismiss() },
                    )
                }
            }
        }
    }
}

@Composable
fun KeyboardSheet(vm: RemoteViewModel, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    RemoteSheet("Clavier", onDismiss) {
        Column(Modifier.padding(horizontal = 24.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Placez le curseur dans un champ de texte sur la TV, puis tapez ici.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Texte à envoyer") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { vm.sendText(text); vm.sendEnter(); text = "" }),
                trailingIcon = {
                    FilledTonalIconButton(onClick = { vm.sendText(text); text = "" }, enabled = text.isNotEmpty()) {
                        Icon(Icons.AutoMirrored.Rounded.Send, "Envoyer")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { vm.deleteChars(1) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.AutoMirrored.Rounded.Backspace, null, Modifier.size(18.dp))
                    Text("  Effacer")
                }
                Button(onClick = { vm.sendEnter() }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardReturn, null, Modifier.size(18.dp))
                    Text("  Entrée")
                }
            }
        }
    }
}

@Composable
fun NumbersSheet(vm: RemoteViewModel, onDismiss: () -> Unit) {
    RemoteSheet("Pavé numérique", onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"))
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    row.forEach { n -> NumKey(n) { vm.button(n) } }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                IconKey(Icons.Rounded.Remove, "Tiret", { vm.button("DASH") }, size = 64.dp)
                NumKey("0") { vm.button("0") }
                IconKey(Icons.Rounded.ViewList, "Liste des chaînes", { vm.button("LIST") }, size = 64.dp)
            }
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ColorKey(Color(0xFFE53935)) { vm.button("RED") }
                ColorKey(Color(0xFF43A047)) { vm.button("GREEN") }
                ColorKey(Color(0xFFFDD835)) { vm.button("YELLOW") }
                ColorKey(Color(0xFF1E88E5)) { vm.button("BLUE") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { vm.button("QMENU") }) { Text("Menu rapide") }
                OutlinedButton(onClick = { vm.button("EXIT") }) {
                    Icon(Icons.Rounded.Close, null, Modifier.size(18.dp)); Text(" Quitter")
                }
            }
        }
    }
}

@Composable
private fun NumKey(label: String, onPress: () -> Unit) {
    RemoteKey(onPress, Modifier.size(64.dp)) {
        Text(label, style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
private fun ColorKey(color: Color, onPress: () -> Unit) {
    RemoteKey(onPress, Modifier.size(width = 64.dp, height = 28.dp), shape = RoundedCornerShape(14.dp), color = color) {}
}

@Composable
fun SettingsSheet(vm: RemoteViewModel, onDismiss: () -> Unit) {
    val mac by vm.mac.collectAsStateWithLifecycle()
    val tvName by vm.tvName.collectAsStateWithLifecycle()
    val state by vm.state.collectAsStateWithLifecycle()
    val hapticLevel by vm.hapticLevel.collectAsStateWithLifecycle()
    val handedness by vm.handedness.collectAsStateWithLifecycle()
    val oneHandScale by vm.oneHandScale.collectAsStateWithLifecycle()
    var macField by remember(mac) { mutableStateOf(mac ?: "") }
    RemoteSheet("Réglages", onDismiss) {
        Column(Modifier.padding(horizontal = 24.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ListItem(
                headlineContent = { Text(tvName ?: "TV LG") },
                supportingContent = { Text(vm.lastHost ?: "—") },
                leadingContent = { Icon(Icons.Rounded.Tv, null) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
            HapticSetting(
                hapticLevel,
                onChange = vm::setHapticLevel,
                problem = remember { vm.haptics.problem() },
            )
            OneHandedSetting(handedness, oneHandScale, vm::setHandedness, vm::setOneHandScale)
            OutlinedTextField(
                value = macField,
                onValueChange = { macField = it },
                label = { Text("Adresse MAC (allumage Wake-on-LAN)") },
                placeholder = { Text("AA:BB:CC:DD:EE:FF") },
                supportingText = { Text("Détectée automatiquement si possible. Activez « Allumer via Wi-Fi » sur la TV.") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { vm.setMac(macField) }, modifier = Modifier.weight(1f)) { Text("Enregistrer MAC") }
                Button(onClick = { vm.toast("Bonjour depuis LG TV Remote 👋") }, enabled = state is ConnectionState.Connected, modifier = Modifier.weight(1f)) {
                    Text("Tester")
                }
            }
            OutlinedButton(onClick = { onDismiss(); vm.forget() }, modifier = Modifier.fillMaxWidth()) {
                Text("Déconnecter et oublier cette TV", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
internal fun HapticSetting(
    level: HapticLevel,
    onChange: (HapticLevel) -> Unit,
    problem: String?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Rounded.Vibration, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.weight(1f)) {
                Text("Retour haptique", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Vibration à chaque appui sur une touche",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            val levels = HapticLevel.entries
            levels.forEachIndexed { i, l ->
                SegmentedButton(
                    selected = l == level,
                    onClick = { onChange(l) },
                    shape = SegmentedButtonDefaults.itemShape(i, levels.size),
                    icon = {},
                ) { Text(l.label, maxLines = 1) }
            }
        }
        if (problem != null && level != HapticLevel.Off) {
            Text(problem, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}
