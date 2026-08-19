package com.br.semesperanca.backendleads.application.model.response

import java.time.LocalDateTime
import java.util.UUID

data class ManagerResponse(
    val id: UUID?,
    val name: String?,
    val email: String?,
    val branchOffice: String?,
    val role: String?,
    val active: Boolean,
    val createdAt: LocalDateTime?
)