package com.example.lightsapp.ui.lightControl

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lightsapp.model.api.CHANGE_BRIGHTNESS
import com.example.lightsapp.model.api.CURRENT_BRIGHTNESS
import com.example.lightsapp.model.api.ConnectionStateListener
import com.example.lightsapp.model.api.LIGHTS_OFF
import com.example.lightsapp.model.api.LIGHTS_ON
import com.example.lightsapp.model.api.LightsWebService
import com.example.lightsapp.model.api.TOGGLE_AUTO_MODE
import com.example.lightsapp.model.api.WebSocketListenerDelegate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okio.ByteString
import java.net.ConnectException
import java.net.SocketTimeoutException
import kotlin.concurrent.fixedRateTimer

class LightControlViewModel(
    private val webService: LightsWebService = LightsWebService
) : ViewModel(), ConnectionStateListener, WebSocketListenerDelegate {
    val connectState: MutableState<Boolean> = mutableStateOf(false)
    val lightState: MutableState<Int> = mutableStateOf(0)
    val turnOffState: MutableState<Boolean> = mutableStateOf(false)
    val autoModeState: MutableState<Boolean> = mutableStateOf(false)
    val brightnessValueState: MutableState<Float> = mutableStateOf(0f)

    init {
        webService.addConnectionListener(this)
        webService.addWebSocketListener(this)

        fixedRateTimer("lightTimer", initialDelay = 0L, period = 350L) {
            viewModelScope.launch {
                lightState.value = webService.getLightInLux().lux
            }
        }
    }

    fun toggleAutoMode() {
        autoModeState.value = !autoModeState.value
        webService.sendWebSocketCommand(TOGGLE_AUTO_MODE)
        if (!autoModeState.value) {
            webService.sendWebSocketCommand(CURRENT_BRIGHTNESS)
        }
    }

    fun changeBrightness(newValue: Float) {
        brightnessValueState.value = newValue
        webService.sendWebSocketCommand(CHANGE_BRIGHTNESS)
        webService.sendWebSocketMessage(newValue.toString())
    }

    fun turnLightsOff() {
        webService.sendWebSocketCommand(LIGHTS_OFF)
        turnOffState.value = true
    }

    fun turnLightsOn() {
        webService.sendWebSocketCommand(LIGHTS_ON)
        turnOffState.value = false
    }

    override fun onConnFailed() {
        connectState.value = true
    }

    override fun onMessageReceived(message: String) {
        println("LightControl Message: " + message)
        brightnessValueState.value = message.toFloat()
    }

    override fun onCommandReceived(command: ByteString) {
        println("LightControl Command: " + command.getByte(0))
    }
}