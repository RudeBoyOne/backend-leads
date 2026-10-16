package com.br.semesperanca.backendleads.application.model.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.util.UUID

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
    /**
     * Obrigatória pela mesma razão do gerente: a lotação define a agência (que é
     * `NOT NULL` em `users`) e define a qual subárvore o corretor pertence.
     */
    @field:NotNull(message = "branchOfficeId é obrigatório")
    val branchOfficeId: UUID,
)
