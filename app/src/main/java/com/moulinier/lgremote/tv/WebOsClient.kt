package com.moulinier.lgremote.tv

import android.annotation.SuppressLint
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data class Connecting(val host: String) : ConnectionState
    /** The TV is showing the "allow this device?" prompt. */
    data class WaitingForPairing(val host: String) : ConnectionState
    data class Connected(val host: String) : ConnectionState
    data class Failed(val host: String, val message: String) : ConnectionState
    /** We turned the TV off ourselves: no point trying to reconnect until it is powered on again. */
    data class PoweredOff(val host: String) : ConnectionState
}

/**
 * Minimal client for the LG webOS SSAP protocol (WebSocket on port 3001/wss or 3000/ws).
 */
class WebOsClient(private val scope: CoroutineScope) {

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    /** Called whenever the TV hands us a (new) client key after pairing. */
    var onClientKey: (host: String, key: String) -> Unit = { _, _ -> }
    /** Called once registration succeeds. */
    var onRegistered: () -> Unit = {}

    private val http: OkHttpClient = insecureClient()

    private var socket: WebSocket? = null
    private var pointer: WebSocket? = null
    private var generation = 0
    private val ids = AtomicInteger(1)
    private val pending = ConcurrentHashMap<String, CompletableDeferred<JSONObject>>()

    fun connect(host: String, clientKey: String?) {
        disconnect()
        val gen = ++generation
        _state.value = ConnectionState.Connecting(host)
        open(gen, host, clientKey, secure = true)
    }

    /** Turns the TV off, then drops the connection so the UI leaves the remote screen. */
    suspend fun turnOff() {
        val host = (_state.value as? ConnectionState.Connected)?.host ?: return
        request("ssap://system/turnOff", timeoutMs = 2000)
        disconnect()
        _state.value = ConnectionState.PoweredOff(host)
    }

    fun disconnect() {
        generation++
        socket?.close(1000, null); socket = null
        pointer?.close(1000, null); pointer = null
        pending.values.forEach { it.cancel() }
        pending.clear()
        _state.value = ConnectionState.Disconnected
    }

    private fun open(gen: Int, host: String, clientKey: String?, secure: Boolean) {
        val url = if (secure) "wss://$host:3001" else "ws://$host:3000"
        var opened = false
        val request = Request.Builder().url(url).build()
        socket = http.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (gen != generation) return
                opened = true
                val msg = JSONObject()
                    .put("type", "register")
                    .put("id", "register_0")
                    .put("payload", Registration.payload(clientKey))
                webSocket.send(msg.toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (gen != generation) return
                handleMessage(gen, host, text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (gen != generation) return
                Log.w(TAG, "socket failure on $url", t)
                if (!opened && secure) {
                    // Older TVs only expose the plain websocket on port 3000.
                    open(gen, host, clientKey, secure = false)
                } else {
                    socket = null
                    _state.value = ConnectionState.Failed(host, t.message ?: "Connexion perdue")
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (gen != generation) return
                socket = null
                _state.value = ConnectionState.Failed(host, "Connexion fermée par la TV")
            }
        })
    }

    private fun handleMessage(gen: Int, host: String, text: String) {
        val json = runCatching { JSONObject(text) }.getOrNull() ?: return
        val id = json.optString("id")
        val type = json.optString("type")
        val payload = json.optJSONObject("payload") ?: JSONObject()

        if (id == "register_0") {
            when (type) {
                "response" -> if (payload.optString("pairingType") == "PROMPT") {
                    _state.value = ConnectionState.WaitingForPairing(host)
                }
                "registered" -> {
                    payload.optString("client-key").takeIf { it.isNotBlank() }?.let { onClientKey(host, it) }
                    _state.value = ConnectionState.Connected(host)
                    scope.launch { openPointerSocket(gen) }
                    onRegistered()
                }
                "error" -> _state.value = ConnectionState.Failed(host, json.optString("error", "Appairage refusé"))
            }
            return
        }
        pending.remove(id)?.complete(json)
    }

    /** Sends a request and waits for its response (or null on timeout / not connected). */
    suspend fun request(uri: String, payload: JSONObject? = null, timeoutMs: Long = 5000): JSONObject? {
        val ws = socket ?: return null
        if (_state.value !is ConnectionState.Connected) return null
        val id = "req_${ids.getAndIncrement()}"
        val deferred = CompletableDeferred<JSONObject>()
        pending[id] = deferred
        val msg = JSONObject().put("type", "request").put("id", id).put("uri", uri)
        if (payload != null) msg.put("payload", payload)
        if (!ws.send(msg.toString())) {
            pending.remove(id); return null
        }
        val res = withTimeoutOrNull(timeoutMs) { deferred.await() }
        pending.remove(id)
        return res?.optJSONObject("payload")
    }

    /** Fire-and-forget request. */
    fun send(uri: String, payload: JSONObject? = null) {
        scope.launch { request(uri, payload) }
    }

    // ---- Pointer / button input socket ----

    private suspend fun openPointerSocket(gen: Int) {
        val res = request("ssap://com.webos.service.networkinput/getPointerInputSocket") ?: return
        val path = res.optString("socketPath").takeIf { it.isNotBlank() } ?: return
        if (gen != generation) return
        pointer = http.newWebSocket(Request.Builder().url(path).build(), object : WebSocketListener() {
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (pointer === webSocket) pointer = null
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (pointer === webSocket) pointer = null
            }
        })
    }

    private fun sendPointer(frame: String) {
        val p = pointer
        if (p != null) {
            p.send(frame)
        } else if (_state.value is ConnectionState.Connected) {
            val gen = generation
            scope.launch {
                openPointerSocket(gen)
                pointer?.send(frame)
            }
        }
    }

    /** Remote button: UP, DOWN, LEFT, RIGHT, ENTER, BACK, HOME, MENU, EXIT, INFO, RED, GREEN, YELLOW, BLUE, 0-9, ... */
    fun button(name: String) = sendPointer("type:button\nname:$name\n\n")
    fun move(dx: Int, dy: Int) = sendPointer("type:move\ndx:$dx\ndy:$dy\ndown:0\n\n")
    fun click() = sendPointer("type:click\n\n")
    fun scroll(dx: Int, dy: Int) = sendPointer("type:scroll\ndx:$dx\ndy:$dy\n\n")

    companion object {
        private const val TAG = "WebOsClient"

        /** TVs use a self-signed certificate on the local network, so we accept it. */
        @SuppressLint("CustomX509TrustManager", "TrustAllX509TrustManager")
        private fun insecureClient(): OkHttpClient {
            val trustAll = object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            }
            val ssl = SSLContext.getInstance("TLS").apply { init(null, arrayOf<TrustManager>(trustAll), SecureRandom()) }
            return OkHttpClient.Builder()
                .sslSocketFactory(ssl.socketFactory, trustAll)
                .hostnameVerifier { _, _ -> true }
                .connectTimeout(4, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)
                .pingInterval(20, TimeUnit.SECONDS)
                .build()
        }

        val sharedHttp: OkHttpClient by lazy { insecureClient() }
    }
}
