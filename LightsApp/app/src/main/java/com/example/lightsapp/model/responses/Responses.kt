package com.example.lightsapp.model.responses

data class GasParametersResponse(
    val temperature: Float = 0f,
    val pressure: Float = 0f,
    val humidity: Float = 0f,
    val iaq: Float = 0f,
    val co2: Float = 0f,
    val voc: Float = 0f
)

data class LightResponse(
    val lux: Int = 0
)