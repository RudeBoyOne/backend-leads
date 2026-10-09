package com.br.semesperanca.backendleads.application.model.response

import java.time.Instant

data class SensorReadingResponse(
    val deviceId: String,
    val temperature: Double?,
    val humidity: Double?,
    val luminosity: Double?,
    val recordedAt: Instant?,
)
