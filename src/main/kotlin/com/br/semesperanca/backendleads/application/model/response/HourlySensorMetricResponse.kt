package com.br.semesperanca.backendleads.application.model.response

import java.time.Instant

data class HourlySensorMetricResponse(
    val bucketStart: Instant?,
    val sampleCount: Int,
    val temperatureAvg: Double?,
    val temperatureMin: Double?,
    val temperatureMax: Double?,
    val humidityAvg: Double?,
    val humidityMin: Double?,
    val humidityMax: Double?,
    val luminosityAvg: Double?,
    val luminosityMin: Double?,
    val luminosityMax: Double?,
    val partial: Boolean,
)
