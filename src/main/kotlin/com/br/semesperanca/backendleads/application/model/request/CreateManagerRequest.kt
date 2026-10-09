package com.br.semesperanca.backendleads.application.model.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.util.UUID

data class CreateManagerRequest(
    @field:NotBlank(message = "Nome é obrigatório")
    @field:Size(min = 2, max = 150, message = "Nome deve ter entre 2 e 150 caracteres")
    val name: String,
    @field:NotBlank(message = "Email é obrigatório")
    @field:Email(message = "Email inválido")
    val email: String,
    @field:NotBlank(message = "Senha é obrigatória")
    @field:Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
    val password: String,
    /**
     * Obrigatória, e não opcional: um gerente sem filial não pertence a nenhuma
     * subárvore do escopo, e `users.real_estate_agency_id` é `NOT NULL` — a agência
     * é derivada da filial, o que elimina a possibilidade de um gerente lotado na
     * filial de uma agência e vinculado a outra.
     */
    @field:NotNull(message = "branchOfficeId é obrigatório")
    val branchOfficeId: UUID,
)
