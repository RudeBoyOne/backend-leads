package com.br.semesperanca.backendleads.application.model.response

import java.util.UUID

data class SensorPropertyMetricDetailResponse(
    val propertyId: UUID,
    val propertyTitle: String?,
    val deviceId: String,
    val latest: SensorReadingResponse?,
    val history: List<HourlySensorMetricResponse>,
)
