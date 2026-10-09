package com.br.semesperanca.backendleads.application.model.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UpdateBranchOwnerRequest(
    @field:NotBlank(message = "name é obrigatório")
    @field:Size(max = 150, message = "name deve ter no máximo 150 caracteres")
    val name: String,
    @field:Email(message = "email deve ser válido")
    @field:Size(max = 150, message = "email deve ter no máximo 150 caracteres")
    val email: String?,
    @field:Size(max = 20, message = "phone deve ter no máximo 20 caracteres")
    val phone: String?,
)
