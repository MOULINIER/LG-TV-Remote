package com.moulinier.lgremote.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import coil.compose.LocalImageLoader
import com.moulinier.lgremote.RemoteViewModel
import com.moulinier.lgremote.tv.ConnectionState
import com.moulinier.lgremote.tv.WebOsClient

@Suppress("DEPRECATION")
@Composable
fun App(vm: RemoteViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val ctx = LocalContext.current
    val imageLoader = remember { ImageLoader.Builder(ctx).okHttpClient(WebOsClient.sharedHttp).build() }

    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }

    CompositionLocalProvider(LocalImageLoader provides imageLoader, LocalHaptics provides vm.haptics) {
        Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
            val connected = state is ConnectionState.Connected
            AnimatedContent(
                targetState = connected,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen",
                modifier = Modifier.padding(padding),
            ) { isConnected ->
                if (isConnected) RemoteScreen(vm) else ConnectScreen(vm, state)
            }
        }
    }

    if (state is ConnectionState.WaitingForPairing) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Autorisez sur la TV") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text(
                        "Une demande de connexion s'affiche sur votre téléviseur. " +
                            "Acceptez-la avec la télécommande d'origine.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            },
            confirmButton = { TextButton(onClick = { vm.client.disconnect() }) { Text("Annuler") } },
        )
    }
}

