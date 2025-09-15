package com.br.semesperanca.backendleads.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "leads")
public class Lead {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private UUID id;

  private String leadOrigin;

  private Instant timestamp;

  private String originLeadId;

  private String originListingId;

  private String clientListingId;

  private String name;

  private String email;

  private String phone;

  private String message;

  private String temperature;

  private String transactionType;
}
