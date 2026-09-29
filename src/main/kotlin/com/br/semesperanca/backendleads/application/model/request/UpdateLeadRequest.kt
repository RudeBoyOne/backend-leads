package com.br.semesperanca.backendleads.application.model.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.util.UUID

data class UpdateLeadRequest(
    @field:Size(max = 150, message = "name deve ter no máximo 150 caracteres")
    val name: String?,
    @field:Email(message = "email deve ser válido")
    @field:Size(max = 150, message = "email deve ter no máximo 150 caracteres")
    val email: String?,
    @field:Size(min = 8, max = 20, message = "phoneNumber deve ter entre 8 e 20 caracteres")
    val phoneNumber: String?,
    val message: String?,
    @field:Size(max = 20, message = "temperature deve ter no máximo 20 caracteres")
    val temperature: String?,
    @field:Size(max = 50, message = "transactionType deve ter no máximo 50 caracteres")
    val transactionType: String?,
    @field:Size(max = 100, message = "source deve ter no máximo 100 caracteres")
    val source: String?,
    @field:Pattern(
        regexp = "new|in_progress|converted|lost|archived",
        message = "status deve ser one of: new, in_progress, converted, lost, archived",
    )
    val status: String?,
    val realEstateAgentId: UUID?,
    val funnelStageId: UUID?,
)
