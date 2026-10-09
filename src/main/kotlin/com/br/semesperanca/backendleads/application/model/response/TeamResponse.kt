package com.br.semesperanca.backendleads.application.model.response

import java.time.LocalDateTime
import java.util.UUID

data class TeamResponse(
    val id: UUID?,
    val managerId: UUID?,
    val branchOfficeId: UUID?,
    val createdAt: LocalDateTime?,
)
