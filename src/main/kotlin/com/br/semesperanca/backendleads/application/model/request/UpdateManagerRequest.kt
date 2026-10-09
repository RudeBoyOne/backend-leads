package com.br.semesperanca.backendleads.application.model.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.util.UUID

data class UpdateManagerRequest(
    @field:NotBlank(message = "Nome é obrigatório")
    @field:Size(min = 2, max = 150, message = "Nome deve ter entre 2 e 150 caracteres")
    val name: String,
    @field:NotBlank(message = "Email é obrigatório")
    @field:Email(message = "Email inválido")
    val email: String,
    /**
     * Transferência de filial é sempre explícita. Sem o campo obrigatório, um
     * `branchOfficeId` ausente desatribuiria o gerente e o deixaria sem escopo
     * algum — invisível para o próprio branch owner e para o gerente.
     */
    @field:NotNull(message = "branchOfficeId é obrigatório")
    val branchOfficeId: UUID,
)
