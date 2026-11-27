package com.br.semesperanca.backendleads.application.model.request

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size

data class UpdateRoleRequest(
    @field:Size(max = 50, message = "name deve ter no máximo 50 caracteres")
    val name: String? = null,
    @field:Size(max = 150, message = "description deve ter no máximo 150 caracteres")
    val description: String? = null,
    val active: Boolean? = null,
    @field:Min(value = 0, message = "hierarchyLevel não pode ser negativo")
    val hierarchyLevel: Int? = null,
)
