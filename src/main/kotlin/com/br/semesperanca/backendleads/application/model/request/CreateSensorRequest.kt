package com.br.semesperanca.backendleads.application.model.request

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.Instant

data class CreateSensorRequest(
    @field:NotBlank(message = "deviceId é obrigatório")
    @field:Size(max = 64, message = "deviceId deve ter no máximo 64 caracteres")
    @field:Pattern(
        regexp = "^[A-Za-z0-9_-]+$",
        message = "deviceId deve conter apenas letras, numeros, hifen e underscore",
    )
    val deviceId: String,
    @field:NotNull(message = "temperature é obrigatório")
    val temperature: Double,
    @field:NotNull(message = "humidity é obrigatório")
    @field:DecimalMin(value = "0.0", message = "humidity não pode ser negativo")
    @field:DecimalMax(value = "100.0", message = "humidity não pode passar de 100")
    val humidity: Double,
    @field:NotNull(message = "luminosity é obrigatório")
    @field:DecimalMin(value = "0.0", message = "luminosity não pode ser negativo")
    val luminosity: Double,
    val batteryLevel: Int? = null,
    val isValid: Boolean = true,
    val recordedAt: Instant? = null,
)
