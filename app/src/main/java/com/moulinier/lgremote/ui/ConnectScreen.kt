package com.moulinier.lgremote.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.WifiFind
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moulinier.lgremote.RemoteViewModel
import com.moulinier.lgremote.tv.ConnectionState

@Composable
fun ConnectScreen(vm: RemoteViewModel, state: ConnectionState) {
    val found by vm.discovered.collectAsStateWithLifecycle()
    val scanning by vm.scanning.collectAsStateWithLifecycle()
    val tvName by vm.tvName.collectAsStateWithLifecycle()
    val mac by vm.mac.collectAsStateWithLifecycle()
    val waking by vm.waking.collectAsStateWithLifecycle()
    var manual by rememberSaveable { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(96.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Tv, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Text("LG Remote", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Connectez votre téléviseur LG webOS. Le téléphone et la TV doivent être sur le même réseau Wi-Fi.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        // Status of the last / current connection attempt
        when {
            waking -> StatusCard(
                "Allumage de ${tvName ?: vm.lastHost ?: "la TV"}…",
                "Connexion dès que la TV est prête.",
                progress = true,
            )
            state is ConnectionState.PoweredOff -> StatusCard(
                "${tvName ?: state.host} est éteinte",
                if (mac != null) null else "Adresse MAC inconnue : allumez la TV avec sa télécommande, puis reconnectez-vous.",
                icon = Icons.Rounded.PowerSettingsNew,
                tone = StatusTone.Neutral,
                actions = {
                    TextButton(onClick = vm::forget) { Text("Oublier") }
                    if (mac != null) {
                        Button(onClick = vm::powerOn) {
                            Icon(Icons.Rounded.PowerSettingsNew, null, Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text("Allumer la TV")
                        }
                    } else {
                        Button(onClick = vm::reconnect) { Text("Reconnecter") }
                    }
                },
            )
            // Offer power-on right away: a TV that is off only fails after the connect timeouts.
            state is ConnectionState.Connecting -> StatusCard(
                "Connexion à ${tvName ?: state.host}…",
                if (mac != null) "La TV est éteinte ? Allumez-la sans attendre." else null,
                progress = true,
                actions = if (mac != null) {
                    {
                        Button(onClick = vm::powerOn) {
                            Icon(Icons.Rounded.PowerSettingsNew, null, Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text("Allumer la TV")
                        }
                    }
                } else null,
            )
            state is ConnectionState.Failed -> StatusCard(
                "Impossible de joindre ${tvName ?: state.host}",
                state.message,
                actions = {
                    TextButton(onClick = vm::forget) { Text("Oublier") }
                    if (mac != null) FilledTonalButton(onClick = vm::powerOn) { Text("Allumer") }
                    Button(onClick = vm::reconnect) { Text("Réessayer") }
                },
            )
            else -> {}
        }

        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer)) {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Téléviseurs détectés", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = vm::scan, enabled = !scanning) {
                    Icon(Icons.Rounded.Refresh, null, Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Rechercher")
                }
            }
            if (scanning) LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp))
            if (found.isEmpty() && !scanning) {
                ListItem(
                    headlineContent = { Text("Aucune TV trouvée") },
                    supportingContent = { Text("Vérifiez que la TV est allumée, ou saisissez son adresse IP ci-dessous.") },
                    leadingContent = { Icon(Icons.Rounded.WifiFind, null) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                )
            }
            found.forEach { tv ->
                ListItem(
                    headlineContent = { Text(tv.name) },
                    supportingContent = { Text(tv.host) },
                    leadingContent = { Icon(Icons.Rounded.Tv, null, tint = MaterialTheme.colorScheme.primary) },
                    trailingContent = { Icon(Icons.Rounded.ChevronRight, null) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.clickable { vm.connect(tv.host, tv.name) },
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Connexion manuelle", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = manual,
                    onValueChange = { manual = it },
                    label = { Text("Adresse IP de la TV") },
                    placeholder = { Text("192.168.1.42") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { vm.connect(manual) }),
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(onClick = { vm.connect(manual) }, enabled = manual.isNotBlank(), modifier = Modifier.align(Alignment.End)) {
                    Text("Connecter")
                }
            }
        }
    }
}

private enum class StatusTone { Neutral, Error }

@Composable
private fun StatusCard(
    title: String,
    detail: String?,
    progress: Boolean = false,
    icon: ImageVector = Icons.Rounded.ErrorOutline,
    tone: StatusTone = StatusTone.Error,
    actions: (@Composable () -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                progress -> cs.secondaryContainer
                tone == StatusTone.Neutral -> cs.surfaceContainerHigh
                else -> cs.errorContainer
            },
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!progress) Icon(icon, null, Modifier.padding(end = 12.dp))
                Text(title, style = MaterialTheme.typography.titleSmall)
            }
            if (progress) LinearProgressIndicator(Modifier.fillMaxWidth())
            detail?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            if (actions != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) { actions() }
            }
        }
    }
}
