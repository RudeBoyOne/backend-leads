package com.br.semesperanca.backendleads.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "lead")
public class Lead {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private UUID id;

  private UUID realEstateAgencyId;

  private UUID realEstateAgentId;

  private UUID portalId;

  private UUID funnelStageId;

  private String originLeadId;

  private String originListingId;

  private String leadOrigin;

  private String clientListingId;

  private String name;

  private String email;

  private String phoneNumber;

  private String message;

  private String temperature;

  private String transactionType;

  private String source;

  private String status;

  private String insightId;

  private Instant timestamp;

  private Instant createdAt;

  private Instant updatedAt;
}
