package com.br.semesperanca.backendleads.application.model.request

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateRoleRequest(
    @field:NotBlank(message = "name é obrigatório")
    @field:Size(max = 50, message = "name deve ter no máximo 50 caracteres")
    val name: String,
    val description: String? = null,
    @field:Min(value = 0, message = "hierarchyLevel não pode ser negativo")
    val hierarchyLevel: Int = 0,
)
