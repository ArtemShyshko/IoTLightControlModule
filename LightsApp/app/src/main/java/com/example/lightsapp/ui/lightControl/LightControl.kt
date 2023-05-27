package com.example.lightsapp.ui.lightControl

import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lightsapp.ui.mainScreen.ConnectionFailedWindow
import com.example.lightsapp.ui.theme.LightsAppTheme

@Composable
fun LightControlScreen() {
    val viewModel: LightControlViewModel = viewModel()

    if (viewModel.connectState.value) {
        ConnectionFailedWindow {
            viewModel.connectState.value = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LuxLayout(viewModel)
            AutoModeSwitch(viewModel)
            BrightnessSlider(viewModel)
        }

        OnOffButtons(viewModel)

    }
}

@Composable
fun LuxLayout(viewModel: LightControlViewModel) {
    val lightInLux = viewModel.lightState.value

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            modifier = Modifier.padding(start = 40.dp, bottom = 16.dp),
            text = "Light control",
            style = MaterialTheme.typography.h4
        )
        Text(
            modifier = Modifier.padding(start = 40.dp),
            text = "Current value is ${lightInLux} lux",
            style = MaterialTheme.typography.body2
        )
    }
}

@Composable
fun AutoModeSwitch(viewModel: LightControlViewModel) {
    val autoMode = viewModel.autoModeState.value

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.padding(start = 40.dp),
            text = "Auto mode",
            style = MaterialTheme.typography.body2
        )
        Switch(
            modifier = Modifier.padding(end = 40.dp),
            checked = autoMode,
            onCheckedChange = { viewModel.toggleAutoMode() }
        )
    }
}

@Composable
fun BrightnessSlider(viewModel: LightControlViewModel) {
    val brightnessValue = viewModel.brightnessValueState.value
    val isSliderVisible = !viewModel.autoModeState.value && !viewModel.turnOffState.value

    val offsetY = animateOffsetAsState(
        targetValue = if (isSliderVisible)
            Offset(0f, 0f) else Offset(0f, -24f),
        animationSpec = tween(durationMillis = 250)
    )

    if (isSliderVisible) {
        Slider(
            value = brightnessValue,
            onValueChange = { newValue ->
                viewModel.changeBrightness(newValue)
            },
            valueRange = 0f..100f,
            steps = 100,
            modifier = Modifier
                .padding(start = 40.dp, end = 40.dp)
                .offset(y = offsetY.value.y.dp)
        )
    }

}

@Composable
fun OnOffButtons(viewModel: LightControlViewModel) {
    Row() {
        Button(
            modifier = Modifier.padding(20.dp),
            onClick = { viewModel.turnLightsOn() }
        ) {
            Text(text = "Lights on")
        }
        Button(
            modifier = Modifier.padding(20.dp),
            onClick = { viewModel.turnLightsOff() }
        ) {
            Text(text = "Lights off")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GasControlPreview() {
    LightsAppTheme {
        LightControlScreen()
    }
}