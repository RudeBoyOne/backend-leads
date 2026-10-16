package com.br.semesperanca.backendleads.application.model.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.util.UUID

data class CreateTeamRequest(
    @field:NotNull(message = "managerId é obrigatório")
    val managerId: UUID,

    @field:NotBlank(message = "name é obrigatório")
    @field:Size(max = 100, message = "name deve ter no máximo 100 caracteres")
    val name: String,

    @field:Size(max = 255, message = "description deve ter no máximo 255 caracteres")
    val description: String? = null,

    @field:Positive(message = "maxAgents deve ser maior que zero")
    val maxAgents: Int? = null,

    @field:Positive(message = "goalAmount deve ser maior que zero")
    val goalAmount: Double? = null,
)
