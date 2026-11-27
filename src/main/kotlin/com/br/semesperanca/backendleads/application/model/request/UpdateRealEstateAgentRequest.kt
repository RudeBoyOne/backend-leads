package com.br.semesperanca.backendleads.application.model.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.util.UUID

data class UpdateRealEstateAgentRequest(
    @field:NotBlank(message = "Nome é obrigatório")
    @field:Size(min = 2, max = 150, message = "Nome deve ter entre 2 e 150 caracteres")
    val name: String,
    @field:NotBlank(message = "Email é obrigatório")
    @field:Email(message = "Email inválido")
    val email: String,
    @field:NotBlank(message = "CRECI é obrigatório")
    val creci: String,
    @field:NotNull(message = "branchOfficeId é obrigatório")
    val branchOfficeId: UUID,
)
