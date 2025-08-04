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

  @Column(nullable = false)
  private String leadOrigin;

  @Column(nullable = false)
  private Instant timestamp;

  @Column(nullable = false)
  private String originLeadId;

  @Column(nullable = false)
  private String originListingId;

  @Column(nullable = false)
  private String clientListingId;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String email;

  @Column(nullable = false)
  private String ddd;

  @Column(nullable = false)
  private String phone;

  @Column(nullable = false, length = 1000)
  private String message;

  @Column(nullable = false)
  private String temperature;

  @Column(nullable = false)
  private String transactionType;
}
