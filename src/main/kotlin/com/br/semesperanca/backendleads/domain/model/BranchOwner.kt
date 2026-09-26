package com.br.semesperanca.backendleads.domain.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "branch_owner")
open class BranchOwner(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    open var id: UUID? = null,

    @Column(name = "real_estate_agency_id", nullable = false)
    open var realEstateAgencyId: UUID? = null,

    @Column(name = "name", nullable = false)
    open var name: String? = null,

    @Column(name = "email")
    open var email: String? = null,

    @Column(name = "phone")
    open var phone: String? = null,

    @Column(name = "active", nullable = false)
    open var active: Boolean = true,

    @Column(name = "created_at", updatable = false)
    open var createdAt: LocalDateTime? = null,

    @Column(name = "updated_at")
    open var updatedAt: LocalDateTime? = null
) {
    @PrePersist
    fun prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now()
        }
        updatedAt = LocalDateTime.now()
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = LocalDateTime.now()
    }
}