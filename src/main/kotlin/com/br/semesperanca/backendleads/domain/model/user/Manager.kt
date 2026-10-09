package com.br.semesperanca.backendleads.domain.model.user

import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "manager")
class Manager : User()
