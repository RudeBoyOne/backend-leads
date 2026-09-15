package com.br.semesperanca.backendleads.domain.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "branch_owner")
open class BranchOwner(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(name = "real_estate_agency_id", nullable = false)
    var realEstateAgencyId: UUID? = null,

    @Column(name = "name", nullable = false)
    var name: String? = null,

    @Column(name = "email")
    var email: String? = null,

    @Column(name = "phone")
    var phone: String? = null,

    @Column(name = "created_at", updatable = false)
    var createdAt: LocalDateTime? = null,

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null
)