package com.br.semesperanca.backendleads.application.model.request

import jakarta.validation.constraints.NotNull
import java.util.UUID

data class CreateTeamRequest(
    @field:NotNull(message = "Manager é obrigatório")
    val managerId: UUID,
    val branchOfficeId: UUID?,
)
