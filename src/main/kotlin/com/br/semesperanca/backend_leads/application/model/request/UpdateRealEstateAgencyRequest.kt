package com.br.semesperanca.backendleads.application.model.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UpdateRealEstateAgencyRequest(
    @field:NotBlank(message = "Nome é obrigatório")
    @field:Size(min = 3, max = 150, message = "Nome deve ter entre 3 e 150 caracteres")
    val name: String,
    @field:NotBlank(message = "CNPJ é obrigatório")
    @field:Size(max = 18, message = "CNPJ deve ter no máximo 18 caracteres")
    val cnpj: String,
    @field:NotBlank(message = "Email é obrigatório")
    @field:Email(message = "Email inválido")
    @field:Size(max = 150, message = "Email deve ter no máximo 150 caracteres")
    val email: String,
    @field:Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
    val phone: String?,
    val active: Boolean,
)
