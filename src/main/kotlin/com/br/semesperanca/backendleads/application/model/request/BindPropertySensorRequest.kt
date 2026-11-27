package com.br.semesperanca.backendleads.application.model.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.util.UUID

data class BindPropertySensorRequest(
    @field:NotNull(message = "propertyId e obrigatorio")
    val propertyId: UUID,
    @field:NotBlank(message = "deviceId e obrigatorio")
    @field:Size(max = 64, message = "deviceId deve ter no maximo 64 caracteres")
    @field:Pattern(
        regexp = "^[A-Za-z0-9_-]+$",
        message = "deviceId deve conter apenas letras, numeros, hifen e underscore",
    )
    val deviceId: String,
)
