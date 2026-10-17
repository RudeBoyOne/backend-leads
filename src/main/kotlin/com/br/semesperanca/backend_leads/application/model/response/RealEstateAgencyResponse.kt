package com.br.semesperanca.backendleads.application.model.response

import java.time.LocalDateTime
import java.util.UUID

data class RealEstateAgencyResponse(
    val id: UUID?,
    val name: String?,
    val cnpj: String?,
    val email: String?,
    val phone: String?,
    val active: Boolean,
    val createdAt: LocalDateTime?,
)
