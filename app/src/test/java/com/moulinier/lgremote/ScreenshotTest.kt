package com.moulinier.lgremote

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.moulinier.lgremote.tv.ConnectionState
import com.moulinier.lgremote.ui.ConnectScreen
import com.moulinier.lgremote.ui.RemoteScreen
import com.moulinier.lgremote.ui.theme.LgRemoteTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [30], qualifiers = "w400dp-h860dp-xxhdpi")
class ScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private fun vm() = RemoteViewModel(ApplicationProvider.getApplicationContext<Application>())

    private fun shot(name: String, dark: Boolean, content: @androidx.compose.runtime.Composable () -> Unit) {
        compose.setContent {
            LgRemoteTheme(dark = dark) {
                androidx.compose.material3.Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { content() }
            }
        }
        compose.onRoot().captureRoboImage("../docs/$name.png")
    }

    @Test fun remoteDark() { val vm = vm(); shot("remote_dark", true) { RemoteScreen(vm) } }
    @Test fun remoteLight() { val vm = vm(); shot("remote_light", false) { RemoteScreen(vm) } }
    @Test fun connectDark() {
        val vm = vm()
        shot("connect_dark", true) { ConnectScreen(vm, ConnectionState.Failed("192.168.1.42", "timeout")) }
    }
}
