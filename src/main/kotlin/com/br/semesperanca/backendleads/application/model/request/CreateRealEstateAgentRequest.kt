package com.br.semesperanca.backendleads.application.model.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateRealEstateAgentRequest(
    @field:NotBlank(message = "Nome é obrigatório")
    @field:Size(min = 3, max = 150, message = "Nome deve ter entre 3 e 150 caracteres")
    val name: String,
    @field:NotBlank(message = "Email é obrigatório")
    @field:Email(message = "Email inválido")
    val email: String,
    @field:NotBlank(message = "Senha é obrigatória")
    @field:Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
    val password: String,
    @field:NotBlank(message = "CRECI é obrigatório")
    val creci: String,
    val branchOfficeId: java.util.UUID?,
)
