package com.br.semesperanca.backendleads.application.model.response

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class PropertyResponse(
    val id: UUID,
    val realEstateAgencyId: UUID,
    val title: String,
    val description: String?,
    val price: BigDecimal?,
    val propertyType: String?,
    val bedrooms: Int?,
    val bathrooms: Int?,
    val areaSqm: BigDecimal?,
    val address: String?,
    val city: String?,
    val state: String?,
    val zipCode: String?,
    val status: String?,
    val active: Boolean,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
)