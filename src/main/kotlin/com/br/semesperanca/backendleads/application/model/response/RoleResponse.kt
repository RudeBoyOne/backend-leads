package com.br.semesperanca.backendleads.application.model.response

import java.time.LocalDateTime
import java.util.UUID

data class RoleResponse(
    val id: UUID,
    val name: String,
    val description: String?,
    val active: Boolean,
    val hierarchyLevel: Int,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
)
