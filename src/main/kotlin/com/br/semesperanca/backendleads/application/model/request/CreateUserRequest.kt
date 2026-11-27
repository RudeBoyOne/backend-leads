package com.br.semesperanca.backendleads.application.model.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.util.UUID

data class CreateUserRequest(
    @field:NotBlank(message = "name é obrigatório")
    @field:Size(max = 150, message = "name deve ter no máximo 150 caracteres")
    val name: String,
    @field:NotBlank(message = "email é obrigatório")
    @field:Email(message = "email deve ser válido")
    val email: String,
    @field:NotBlank(message = "password é obrigatório")
    val password: String,
    @field:NotNull(message = "roleId é obrigatório")
    val roleId: UUID,
    @field:NotNull(message = "realEstateAgencyId é obrigatório")
    val realEstateAgencyId: UUID,
    val branchOfficeId: UUID? = null,
)
