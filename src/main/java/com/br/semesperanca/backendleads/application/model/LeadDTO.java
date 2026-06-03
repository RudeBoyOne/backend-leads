package com.br.semesperanca.backendleads.application.model;

import com.br.semesperanca.backendleads.domain.model.Lead;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record LeadDTO(
    @NotBlank(message = "Lead origin is required") String leadOrigin,
    @NotBlank(message = "Timestamp is required") String timestamp,
    @NotBlank(message = "Origin lead ID is required") String originLeadId,
    @NotBlank(message = "Origin listing ID is required") String originListingId,
    @NotBlank(message = "Client listing ID is required") String clientListingId,
    @NotBlank(message = "Name is required") String name,
    @NotBlank(message = "Email is required") @Email(message = "Email must be valid") String email,
    @NotBlank(message = "Phone is required")
        @Size(min = 8, max = 15, message = "Phone must be valid")
        String phone,
    @NotBlank(message = "Message is required") String message,
    @NotBlank(message = "Temperature is required") String temperature,
    @NotBlank(message = "Transaction type is required") String transactionType,
    UUID realEstateAgencyId,
    UUID realEstateAgentId,
    UUID portalId,
    UUID funnelStageId,
    String source,
    @NotBlank(message = "Status is required") String status,
    String insightId,
    String createdAt,
    String updatedAt) {

  public static LeadDTO fromLead(Lead lead) {
    return new LeadDTO(
        lead.getLeadOrigin(),
        lead.getTimestamp().toString(),
        lead.getOriginLeadId(),
        lead.getOriginListingId(),
        lead.getClientListingId(),
        lead.getName(),
        lead.getEmail(),
        lead.getPhoneNumber(),
        lead.getMessage(),
        lead.getTemperature(),
        lead.getTransactionType(),
        lead.getRealEstateAgencyId(),
        lead.getRealEstateAgentId(),
        lead.getPortalId(),
        lead.getFunnelStageId(),
        lead.getSource(),
        lead.getStatus(),
        lead.getInsightId(),
        lead.getCreatedAt() != null ? lead.getCreatedAt().toString() : null,
        lead.getUpdatedAt() != null ? lead.getUpdatedAt().toString() : null);
  }

  public static Lead toLead(LeadDTO leadDTO) {
    return Lead.builder()
        .leadOrigin(leadDTO.leadOrigin())
        .timestamp(Instant.parse(leadDTO.timestamp()))
        .originLeadId(leadDTO.originLeadId())
        .originListingId(leadDTO.originListingId())
        .clientListingId(leadDTO.clientListingId())
        .name(leadDTO.name())
        .email(leadDTO.email())
        .phoneNumber(leadDTO.phone())
        .message(leadDTO.message())
        .temperature(leadDTO.temperature())
        .transactionType(leadDTO.transactionType())
        .realEstateAgencyId(leadDTO.realEstateAgencyId())
        .realEstateAgentId(leadDTO.realEstateAgentId())
        .portalId(leadDTO.portalId())
        .funnelStageId(leadDTO.funnelStageId())
        .source(leadDTO.source())
        .status(leadDTO.status())
        .build();
  }
}
