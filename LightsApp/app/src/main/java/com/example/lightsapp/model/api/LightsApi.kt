package com.example.lightsapp.model.api

import com.example.lightsapp.model.responses.GasParametersResponse
import com.example.lightsapp.model.responses.LightResponse
import okhttp3.*
import okio.ByteString
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

const val LIGHTS_OFF: Int = 1
const val LIGHTS_ON: Int = 2
const val CHANGE_BRIGHTNESS: Int = 3
const val CURRENT_BRIGHTNESS: Int = 4
const val TOGGLE_ALARM: Int = 5
const val TOGGLE_AUTO_MODE: Int = 6

const val serverIP: String = "192.168.0.200"

object LightsWebService {
    private val api: LightsApi
    private val webSocketManager = WebSocketManager()
    private val listeners: MutableList<ConnectionStateListener> = mutableListOf()

    init {

        val retrofit = Retrofit.Builder()
            .baseUrl("http://$serverIP")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(LightsApi::class.java)

        try {
            webSocketManager.connect()
        } catch (e: ConnectException) {
            println("Not connected to WebSocket")
        }

    }

    fun addConnectionListener(listener: ConnectionStateListener) {
        listeners.add(listener)
    }

    fun addWebSocketListener(listenerDelegate: WebSocketListenerDelegate) {
        webSocketManager.addDelegate(listenerDelegate)
    }

    fun sendWebSocketMessage(msg: String) {
        webSocketManager.sendMessage(msg)
    }

    fun sendWebSocketCommand(intValue: Int) {
        val cmd = ByteString.of(intValue.toByte())
        webSocketManager.sendCommand(cmd)
    }

    suspend fun getGasParameters(): GasParametersResponse {
        try {
            return api.getGasParameters()
        } catch (e: Exception) {
            for (listener in listeners) {
                listener.onConnFailed()
            }
            return GasParametersResponse()
        }
    }

    suspend fun getLightInLux(): LightResponse {
        try {
            return api.getLightInLux()
        } catch (e: Exception) {
            for (listener in listeners) {
                listener.onConnFailed()
            }
            return LightResponse()
        }
    }

    interface LightsApi {
        @GET("gasParam")
        suspend fun getGasParameters(): GasParametersResponse

        @GET("light")
        suspend fun getLightInLux(): LightResponse
    }
}

interface ConnectionStateListener {
    fun onConnFailed()
}

interface WebSocketListenerDelegate {
    fun onMessageReceived(message: String)
    fun onCommandReceived(command: ByteString)
}

class WebSocketManager() {
    private var webSocket: WebSocket? = null
    private val delegates: MutableList<WebSocketListenerDelegate> = mutableListOf()

    fun addDelegate(listenerDelegate: WebSocketListenerDelegate) {
        delegates.add(listenerDelegate)
    }

    val client = OkHttpClient.Builder()
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    val request = Request.Builder()
        .url("ws://$serverIP:8080")
        .build()

    fun connect() {
        webSocket = client.newWebSocket(request, createSocketListener())
    }

    fun disconnect() {
        webSocket?.close(1000, "Disconnected")
    }

    fun sendMessage(message: String) {
        webSocket?.send(message)
    }

    fun sendCommand(command: ByteString) {
        webSocket?.send(command)
    }

    private fun createSocketListener(): WebSocketListener {
        return object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                println("Connected successfully")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                for (delegate in delegates) {
                    delegate.onMessageReceived(text)
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                for (delegate in delegates) {
                    delegate.onCommandReceived(bytes)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                println("Connection failure: ${t.message}")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                println("Connection closed")
            }
        }
    }
}