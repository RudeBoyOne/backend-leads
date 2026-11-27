package com.br.semesperanca.backendleads.application.model.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDateTime
import java.util.UUID

data class UpdateActivityRequest(
    val propertyId: UUID? = null,

    @field:NotBlank(message = "title é obrigatório")
    @field:Size(max = 150, message = "title deve ter no máximo 150 caracteres")
    val title: String,

    @field:NotBlank(message = "description é obrigatória")
    @field:Size(max = 1000, message = "description deve ter no máximo 1000 caracteres")
    val description: String,

    @field:NotNull(message = "activityDate é obrigatório")
    val activityDate: LocalDateTime,

    val completed: Boolean = false,
)
