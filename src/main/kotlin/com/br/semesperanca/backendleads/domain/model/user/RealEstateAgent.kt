package com.br.semesperanca.backendleads.domain.model.user

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "real_estate_agent")
class RealEstateAgent(
    @Column(name = "creci", unique = true)
    var creci: String? = null,
) : User()
