package com.moulinier.lgremote

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.moulinier.lgremote.tv.ConnectionState
import com.moulinier.lgremote.tv.DiscoveredTv
import com.moulinier.lgremote.tv.Discovery
import com.moulinier.lgremote.tv.WakeOnLan
import com.moulinier.lgremote.tv.WebOsClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

data class TvApp(val id: String, val title: String, val icon: String?)
data class TvInput(val id: String, val label: String)

class RemoteViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("tv", Context.MODE_PRIVATE)
    val client = WebOsClient(viewModelScope)
    val state: StateFlow<ConnectionState> = client.state

    private val _discovered = MutableStateFlow<List<DiscoveredTv>>(emptyList())
    val discovered = _discovered.asStateFlow()
    private val _scanning = MutableStateFlow(false)
    val scanning = _scanning.asStateFlow()

    private val _apps = MutableStateFlow<List<TvApp>>(emptyList())
    val apps = _apps.asStateFlow()
    private val _inputs = MutableStateFlow<List<TvInput>>(emptyList())
    val inputs = _inputs.asStateFlow()

    private val _volume = MutableStateFlow<Int?>(null)
    val volume = _volume.asStateFlow()
    private val _muted = MutableStateFlow(false)
    val muted = _muted.asStateFlow()

    private val _tvName = MutableStateFlow(prefs.getString("name", null))
    val tvName = _tvName.asStateFlow()
    private val _mac = MutableStateFlow(prefs.getString("mac", null))
    val mac = _mac.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    val lastHost: String? get() = prefs.getString("host", null)

    init {
        client.onClientKey = { host, key -> prefs.edit().putString("key_$host", key).apply() }
        client.onRegistered = {
            viewModelScope.launch {
                refreshVolume()
                fetchMac()
            }
        }
        lastHost?.let { connect(it, _tvName.value) } ?: scan()
    }

    fun scan() {
        if (_scanning.value) return
        viewModelScope.launch {
            _scanning.value = true
            _discovered.value = Discovery.scan(getApplication())
            _scanning.value = false
        }
    }

    fun connect(host: String, name: String? = null) {
        val h = host.trim()
        if (h.isEmpty()) return
        val previous = lastHost
        prefs.edit().putString("host", h).apply {
            if (name != null) putString("name", name)
            if (previous != h) remove("mac")
        }.apply()
        if (name != null) _tvName.value = name
        if (previous != h) _mac.value = null
        client.connect(h, prefs.getString("key_$h", null))
    }

    fun reconnect() { lastHost?.let { connect(it) } }

    fun forget() {
        client.disconnect()
        lastHost?.let { prefs.edit().remove("key_$it").apply() }
        prefs.edit().remove("host").remove("name").remove("mac").apply()
        _tvName.value = null
        _mac.value = null
        scan()
    }

    fun setMac(value: String) {
        val v = value.trim().uppercase()
        if (WakeOnLan.parseMac(v) == null) {
            _messages.tryEmit("Adresse MAC invalide"); return
        }
        prefs.edit().putString("mac", v).apply()
        _mac.value = v
    }

    // ----- Commands -----

    fun button(name: String) = client.button(name)

    fun volumeUp() { client.send("ssap://audio/volumeUp"); refreshVolumeSoon() }
    fun volumeDown() { client.send("ssap://audio/volumeDown"); refreshVolumeSoon() }
    fun toggleMute() {
        val target = !_muted.value
        _muted.value = target
        client.send("ssap://audio/setMute", JSONObject().put("mute", target))
    }
    fun channelUp() = client.send("ssap://tv/channelUp")
    fun channelDown() = client.send("ssap://tv/channelDown")

    fun play() = client.send("ssap://media.controls/play")
    fun pause() = client.send("ssap://media.controls/pause")
    fun stop() = client.send("ssap://media.controls/stop")
    fun rewind() = client.send("ssap://media.controls/rewind")
    fun fastForward() = client.send("ssap://media.controls/fastForward")

    fun powerOff() = client.send("ssap://system/turnOff")

    fun powerOn() {
        val m = _mac.value
        if (m == null) {
            _messages.tryEmit("Adresse MAC inconnue : renseignez-la dans les réglages")
            return
        }
        viewModelScope.launch {
            WakeOnLan.wake(m, lastHost)
            _messages.tryEmit("Signal d'allumage envoyé…")
            // Give the TV time to boot its network stack, then reconnect.
            repeat(6) {
                delay(3000)
                if (state.value is ConnectionState.Connected) return@launch
                reconnect()
            }
        }
    }

    fun sendText(text: String) {
        if (text.isEmpty()) return
        client.send("ssap://com.webos.service.ime/insertText", JSONObject().put("text", text).put("replace", 0))
    }
    fun sendEnter() = client.send("ssap://com.webos.service.ime/sendEnterKey")
    fun deleteChars(count: Int = 1) =
        client.send("ssap://com.webos.service.ime/deleteCharacters", JSONObject().put("count", count))

    fun launch(app: TvApp) = client.send("ssap://system.launcher/launch", JSONObject().put("id", app.id))
    fun switchInput(input: TvInput) = client.send("ssap://tv/switchInput", JSONObject().put("inputId", input.id))

    fun loadApps() = viewModelScope.launch {
        val res = client.request("ssap://com.webos.applicationManager/listLaunchPoints") ?: return@launch
        val arr = res.optJSONArray("launchPoints") ?: return@launch
        _apps.value = (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            TvApp(
                id = o.optString("id"),
                title = o.optString("title"),
                icon = o.optString("largeIcon").ifBlank { o.optString("icon") }.ifBlank { null },
            )
        }.filter { it.id.isNotBlank() }
    }

    fun loadInputs() = viewModelScope.launch {
        val res = client.request("ssap://tv/getExternalInputList") ?: return@launch
        val arr = res.optJSONArray("devices") ?: return@launch
        _inputs.value = (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            TvInput(o.optString("id"), o.optString("label").ifBlank { o.optString("id") })
        }
    }

    fun toast(message: String) =
        client.send("ssap://system.notifications/createToast", JSONObject().put("message", message))

    private fun refreshVolumeSoon() = viewModelScope.launch { delay(250); refreshVolume() }

    private suspend fun refreshVolume() {
        val res = client.request("ssap://audio/getVolume") ?: return
        val status = res.optJSONObject("volumeStatus")
        val vol = status?.optInt("volume", -1) ?: res.optInt("volume", -1)
        if (vol >= 0) _volume.value = vol
        _muted.value = status?.optBoolean("muteStatus") ?: res.optBoolean("muted", _muted.value)
    }

    private suspend fun fetchMac() {
        if (_mac.value != null) return
        val macRegex = Regex("^([0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}$")
        val info = client.request("ssap://com.webos.service.connectionmanager/getinfo")
        val candidates = listOfNotNull(
            info?.optJSONObject("wiredInfo")?.optString("macAddress"),
            info?.optJSONObject("wifiInfo")?.optString("macAddress"),
            client.request("ssap://com.webos.service.update/getCurrentSWInformation")?.optString("device_id"),
        )
        candidates.firstOrNull { macRegex.matches(it) }?.let { setMac(it) }
    }

    override fun onCleared() {
        client.disconnect()
    }
}
