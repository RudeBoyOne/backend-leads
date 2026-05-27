package com.br.semesperanca.backendleads.domain.model.user;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "manager")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Manager extends User {

  @Column(name = "branch_office")
  private String branchOffice;
}
