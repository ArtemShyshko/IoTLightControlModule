package com.example.lightsapp.ui.gasControl

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lightsapp.ui.gasControl.GasControlViewModel.Companion.maxCO2
import com.example.lightsapp.ui.gasControl.GasControlViewModel.Companion.maxHumidity
import com.example.lightsapp.ui.gasControl.GasControlViewModel.Companion.maxIAQ
import com.example.lightsapp.ui.gasControl.GasControlViewModel.Companion.maxPressure
import com.example.lightsapp.ui.gasControl.GasControlViewModel.Companion.maxTemperature
import com.example.lightsapp.ui.gasControl.GasControlViewModel.Companion.maxVOC
import com.example.lightsapp.ui.mainScreen.ConnectionFailedWindow
import com.example.lightsapp.ui.theme.LightsAppTheme

@Composable
fun GasControlScreen() {
    val viewModel: GasControlViewModel = viewModel()
    val gasParameters = viewModel.gasParametersState.value

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
        Text(
            text = "Air parameters",
            style = MaterialTheme.typography.h4
        )

        Row {
            CircularIndicator("Temperature, *C", gasParameters.temperature, maxTemperature)
            CircularIndicator("Pressure, hPa", gasParameters.pressure, maxPressure)
        }

        Row {
            CircularIndicator("Humidity, %", gasParameters.humidity, maxHumidity)
            CircularIndicator("IAQ, PPM", gasParameters.iaq, maxIAQ)
        }

        Row {
            CircularIndicator("CO2 Equivalent, PPM", gasParameters.co2, maxCO2)
            CircularIndicator("Breath VOC Equivalent, PPM", gasParameters.voc, maxVOC)
        }

        Button(
            modifier = Modifier.padding(bottom = 20.dp),
            onClick = { viewModel.toggleAlarm() }
        ) {
            Text(text = "Turn alarm on/off")
        }
    }
}

@Composable
fun CircularIndicator(
    parameterName: String,
    currentValue: Float,
    maxValue: Float
) {
    val percentage: Float
    val indicatorColor: Color

    if (currentValue > maxValue) {
        indicatorColor = Color.Magenta
        percentage = 1f
    } else {
        indicatorColor = Color(android.graphics.Color.parseColor("#4DB6AC"))
        percentage = currentValue / maxValue
    }

    val animatedSweepAngle: Float by animateFloatAsState(targetValue = percentage * 260f)

    Column() {

        Text(
            text = parameterName,
            style = MaterialTheme.typography.caption
        )

        Box(
            modifier = Modifier.padding(start = 16.dp)
        ) {
            Canvas(modifier = Modifier
                .size(150.dp)
                .padding(10.dp)
            ) {
                drawArc(
                    color = Color(android.graphics.Color.parseColor("#90A4AE")),
                    startAngle = 140f,
                    sweepAngle = 260f,
                    useCenter = false,
                    style = Stroke(10.dp.toPx(), cap = StrokeCap.Round),
                    size = Size(size.width, size.height)
                )

                drawArc(
                    color = indicatorColor,
                    startAngle = 140f,
                    sweepAngle = animatedSweepAngle,
                    useCenter = false,
                    style = Stroke(10.dp.toPx(), cap = StrokeCap.Round),
                    size = Size(size.width, size.height)
                )
            }

            Text(
                text = "$currentValue",
                style = MaterialTheme.typography.body2,
                modifier = Modifier.align(Alignment.Center)
            )

            Text(
                text = "0",
                style = MaterialTheme.typography.caption,
                modifier = Modifier.align(Alignment.BottomStart)
            )

            Text(
                text = maxValue.toInt().toString(),
                style = MaterialTheme.typography.caption,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GasControlPreview() {
    LightsAppTheme {
        GasControlScreen()
    }
}