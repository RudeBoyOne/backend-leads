package com.br.semesperanca.backendleads.application.model.response

import java.time.LocalDateTime
import java.util.UUID

data class RealEstateAgentResponse(
    val id: UUID?,
    val name: String?,
    val email: String?,
    val creci: String?,
    val branchOfficeId: UUID?,
    val role: String?,
    val active: Boolean,
    val createdAt: LocalDateTime?,
)
