package com.br.semesperanca.backend_leads.application.model.response

import java.time.LocalDateTime
import java.util.UUID

data class TeamResponse(
    val id: UUID?,
    val managerId: UUID?,
    val branchOfficeId: UUID?,
    val name: String?,
    val description: String?,
    val active: Boolean,
    val maxAgents: Int?,
    val goalAmount: Double?,
    val createdAt: LocalDateTime?,
)
