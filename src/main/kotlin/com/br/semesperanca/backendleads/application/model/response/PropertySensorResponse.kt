package com.br.semesperanca.backendleads.application.model.response

import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

data class PropertySensorResponse(
    val id: UUID? = null,
    val propertyId: UUID,
    val deviceId: String,
    val label: String? = null,
    val active: Boolean = true,
    val samplingIntervalSeconds: Int? = null,
    val installedAt: LocalDateTime? = null,
    val createdAt: Instant? = null,
)
