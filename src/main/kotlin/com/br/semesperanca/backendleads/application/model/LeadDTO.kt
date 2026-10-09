package com.br.semesperanca.backendleads.application.model

import com.br.semesperanca.backendleads.domain.model.Lead
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class LeadDTO(
    @field:NotBlank(message = "Lead origin is required")
    val leadOrigin: String,
    @field:NotBlank(message = "Timestamp is required")
    val timestamp: String,
    @field:NotBlank(message = "Origin lead ID is required")
    val originLeadId: String,
    @field:NotBlank(message = "Origin listing ID is required")
    val originListingId: String,
    @field:NotBlank(message = "Client listing ID is required")
    val clientListingId: String,
    @field:NotBlank(message = "Name is required")
    val name: String,
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email must be valid")
    val email: String,
    @field:NotBlank(message = "Phone is required")
    @field:Size(min = 8, max = 15, message = "Phone must be valid")
    val phone: String,
    @field:NotBlank(message = "Message is required")
    val message: String,
    @field:NotBlank(message = "Temperature is required")
    val temperature: String,
    @field:NotBlank(message = "Transaction type is required")
    val transactionType: String,
    val realEstateAgencyId: UUID?,
    val realEstateAgentId: UUID?,
    val portalId: UUID?,
    val funnelStageId: UUID?,
    val source: String?,
    @field:NotBlank(message = "Status is required")
    val status: String,
    val insightId: String?,
    val createdAt: String?,
    val updatedAt: String?,
) {
    companion object {
        @JvmStatic
        fun fromLead(lead: Lead): LeadDTO =
            LeadDTO(
                leadOrigin = lead.leadOrigin!!,
                timestamp = lead.timestamp.toString(),
                originLeadId = lead.originLeadId!!,
                originListingId = lead.originListingId!!,
                clientListingId = lead.clientListingId!!,
                name = lead.name!!,
                email = lead.email!!,
                phone = lead.phoneNumber!!,
                message = lead.message!!,
                temperature = lead.temperature!!,
                transactionType = lead.transactionType!!,
                realEstateAgencyId = lead.realEstateAgencyId,
                realEstateAgentId = lead.realEstateAgent?.id,
                portalId = lead.portalId,
                funnelStageId = lead.funnelStageId,
                source = lead.source,
                status = lead.status!!,
                insightId = lead.insightId,
                createdAt = lead.createdAt?.toString(),
                updatedAt = lead.updatedAt?.toString(),
            )

        @JvmStatic
        fun toLead(dto: LeadDTO): Lead =
            Lead(
                leadOrigin = dto.leadOrigin,
                timestamp = Instant.parse(dto.timestamp),
                originLeadId = dto.originLeadId,
                originListingId = dto.originListingId,
                clientListingId = dto.clientListingId,
                name = dto.name,
                email = dto.email,
                phoneNumber = dto.phone,
                message = dto.message,
                temperature = dto.temperature,
                transactionType = dto.transactionType,
                realEstateAgencyId = dto.realEstateAgencyId,
                // A association `realEstateAgent` fica de fora: este mapper é
                // estático e não tem repositório para resolver o id em entidade.
                // Quem cria lead resolve o corretor e passa a association.
                portalId = dto.portalId,
                funnelStageId = dto.funnelStageId,
                source = dto.source,
                status = dto.status,
            )
    }
}
