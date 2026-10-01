package com.br.semesperanca.backendleads.application.model.response

import java.time.Instant
import java.util.UUID

data class SensorPropertyMetricResponse(
    val propertyId: UUID,
    val propertyTitle: String?,
    val deviceId: String,
    val temperature: Double?,
    val humidity: Double?,
    val luminosity: Double?,
    val recordedAt: Instant?,
)
