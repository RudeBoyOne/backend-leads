package com.br.semesperanca.backendleads.application.model.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.util.UUID

data class CreateLeadRequest(
    @field:NotNull(message = "realEstateAgencyId é obrigatório")
    val realEstateAgencyId: UUID,
    val realEstateAgentId: UUID? = null,
    val portalId: UUID? = null,
    val funnelStageId: UUID? = null,
    val originLeadId: String? = null,
    val originListingId: String? = null,
    val leadOrigin: String? = null,
    val clientListingId: String? = null,
    @field:NotBlank(message = "name é obrigatório")
    @field:Size(max = 150, message = "name deve ter no máximo 150 caracteres")
    val name: String,
    val email: String? = null,
    val phoneNumber: Long? = null,
    val message: String? = null,
    val temperature: String? = null,
    val transactionType: String? = null,
    val source: String? = null,
    val status: String? = "new",
)
