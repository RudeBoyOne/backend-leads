package com.br.semesperanca.backendleads.application.model.request

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class UpdatePropertySensorRequest(
    @field:Size(max = 100, message = "label deve ter no máximo 100 caracteres")
    val label: String? = null,
    val active: Boolean? = null,
    @field:Min(value = 1, message = "samplingIntervalSeconds deve ser positivo")
    val samplingIntervalSeconds: Int? = null,
    val installedAt: LocalDateTime? = null,
)
