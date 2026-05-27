package com.br.semesperanca.backendleads.domain.model.user;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "real_estate_agent")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class RealEstateAgent extends User {

  @Column(name = "creci", nullable = false, unique = true)
  private String creci;
}
