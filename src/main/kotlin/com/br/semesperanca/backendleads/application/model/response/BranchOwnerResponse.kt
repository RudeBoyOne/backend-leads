package com.br.semesperanca.backendleads.application.model.response

import java.time.LocalDateTime
import java.util.UUID

data class BranchOwnerResponse(
    val id: UUID,
    val realEstateAgencyId: UUID,
    val name: String,
    val email: String?,
    val phone: String?,
    val role: String?,
    val active: Boolean,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
)
