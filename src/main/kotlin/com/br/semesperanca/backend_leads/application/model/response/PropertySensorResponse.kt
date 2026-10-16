package com.br.semesperanca.backendleads.application.model.response

import java.time.Instant
import java.util.UUID

data class PropertySensorResponse(
    val propertyId: UUID,
    val deviceId: String,
    val createdAt: Instant?,
)
