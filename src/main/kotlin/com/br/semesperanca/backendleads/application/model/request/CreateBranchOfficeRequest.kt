package com.br.semesperanca.backendleads.application.model.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.util.UUID

data class CreateBranchOfficeRequest(
    @field:NotBlank(message = "Nome é obrigatório")
    @field:Size(min = 2, max = 150, message = "Nome deve ter entre 2 e 150 caracteres")
    val name: String,
    val address: String?,
    val phone: String?,
    /**
     * Toda filial tem exatamente um responsável, e `branch_office.branch_owner_id` é
     * `NOT NULL` com `UNIQUE`: o dono não é opcional nem compartilhado.
     *
     * O id passa pelo `BranchOwnerRepository` escopado, então um branch owner que
     * envie o id de outro recebe 404 — a autorização vem da query, não de uma
     * comparação no código.
     */
    @field:NotNull(message = "branchOwnerId é obrigatório")
    val branchOwnerId: UUID,
)
