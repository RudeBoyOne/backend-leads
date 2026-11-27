package com.br.semesperanca.backendleads.application.model.response

import java.time.Instant

data class SensorMetricResponse(
    val deviceId: String,
    val temperature: Double,
    val humidity: Double,
    val luminosity: Double,
    val batteryLevel: Int?,
    val isValid: Boolean,
    val recordedAt: Instant?,
)
