package com.moulinier.lgremote

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.moulinier.lgremote.tv.ConnectionState
import com.moulinier.lgremote.ui.App
import com.moulinier.lgremote.ui.theme.LgRemoteTheme

class MainActivity : ComponentActivity() {
    private val vm: RemoteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { LgRemoteTheme { App(vm) } }
    }

    override fun onResume() {
        super.onResume()
        val s = vm.state.value
        if (s is ConnectionState.Failed) vm.reconnect()
    }

    /** Phone volume keys control the TV volume while connected. */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (vm.state.value is ConnectionState.Connected) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> { vm.volumeUp(); return true }
                KeyEvent.KEYCODE_VOLUME_DOWN -> { vm.volumeDown(); return true }
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}
