package com.br.semesperanca.backendleads.application.model.request

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import java.time.Instant

data class UpdateSensorRequest(
    val temperature: Double? = null,
    @field:DecimalMin(value = "0.0", message = "humidity não pode ser negativo")
    @field:DecimalMax(value = "100.0", message = "humidity não pode passar de 100")
    val humidity: Double? = null,
    @field:DecimalMin(value = "0.0", message = "luminosity não pode ser negativo")
    val luminosity: Double? = null,
    val batteryLevel: Int? = null,
    val isValid: Boolean? = null,
    val recordedAt: Instant? = null,
)
