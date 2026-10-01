package com.br.semesperanca.backendleads.application.model.response

import java.time.LocalDateTime
import java.util.UUID

data class BranchOfficeResponse(
    val id: UUID?,
    val realEstateAgencyId: UUID?,
    val name: String?,
    val address: String?,
    val phone: String?,
    val active: Boolean,
    val createdAt: LocalDateTime?,
)
