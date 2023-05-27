package com.example.lightsapp.ui.gasControl

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lightsapp.model.api.ConnectionStateListener
import com.example.lightsapp.model.api.LightsWebService
import com.example.lightsapp.model.api.TOGGLE_ALARM
import com.example.lightsapp.model.api.WebSocketListenerDelegate
import com.example.lightsapp.model.responses.GasParametersResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okio.ByteString
import java.net.ConnectException
import java.net.SocketTimeoutException
import kotlin.concurrent.fixedRateTimer

class GasControlViewModel(
    private val webService: LightsWebService = LightsWebService
) : ViewModel(), ConnectionStateListener, WebSocketListenerDelegate {

    val connectState: MutableState<Boolean> = mutableStateOf(false)
    val gasParametersState: MutableState<GasParametersResponse> =
        mutableStateOf(GasParametersResponse())

    init {
        webService.addConnectionListener(this)
        webService.addWebSocketListener(this)

        fixedRateTimer("gasParametersTimer", initialDelay = 0L, period = 1500L) {
            viewModelScope.launch {
                gasParametersState.value = webService.getGasParameters()
            }
        }
    }

    fun toggleAlarm() {
        webService.sendWebSocketCommand(TOGGLE_ALARM)
    }

    override fun onConnFailed() {
        connectState.value = true
    }

    override fun onMessageReceived(message: String) {
        println("GasControl Message: " + message)
    }

    override fun onCommandReceived(command: ByteString) {
        println("GasControl Command: " + command.getByte(0))
    }

    companion object {
        const val maxTemperature: Float = 50f
        const val maxPressure: Float = 1200f
        const val maxHumidity: Float = 100f
        const val maxIAQ: Float = 500f
        const val maxCO2: Float = 2000f
        const val maxVOC: Float = 5f
    }
}