package com.br.semesperanca.backendleads.application.model.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Size
import java.util.UUID

data class UpdateUserRequest(
    @field:Size(max = 150, message = "name deve ter no máximo 150 caracteres")
    val name: String? = null,
    @field:Email(message = "email deve ser válido")
    val email: String? = null,
    val roleId: UUID? = null,
    val realEstateAgencyId: UUID? = null,
    val branchOfficeId: UUID? = null,
    val active: Boolean? = null,
)
