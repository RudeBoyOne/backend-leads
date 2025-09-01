package com.br.semesperanca.backendleads.model;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

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
  private Long id;

  private String leadOrigin;

  private Instant timestamp;

  private String originLeadId;

  private String originListingId;

  private String clientListingId;

  private String name;

  private String email;

  private String ddd;

  private String phone;

  private String message;

  private String temperature;

  private String transactionType;
}
