package com.br.semesperanca.backendleads.application.model.response

import java.time.LocalDateTime
import java.util.UUID

data class ActivityResponse(
    val id: UUID,
    val leadId: UUID,
    val propertyId: UUID?,
    val title: String,
    val description: String,
    val activityDate: LocalDateTime,
    val createdAt: LocalDateTime?
)