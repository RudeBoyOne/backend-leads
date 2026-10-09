package com.br.semesperanca.backendleads.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "branch_office")
class BranchOffice(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,
    @Column(name = "real_estate_agency_id", nullable = false)
    var realEstateAgencyId: UUID? = null,
    /**
     * mapsTo "branch_office.branch_owner_id: NOT NULL, UNIQUE". A associação é real
     * (e não uma coluna UUID crua) porque o escopo do branch owner precisa percorrer
     * a cadeia `filial -> responsavel` dentro de um subquery do Criteria, e a API
     * Criteria só atravessa association.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_owner_id", nullable = false)
    var branchOwner: BranchOwner? = null,
    @Column(name = "name", nullable = false)
    var name: String? = null,
    @Column(name = "address")
    var address: String? = null,
    @Column(name = "phone")
    var phone: String? = null,
    @Column(name = "active", nullable = false)
    var active: Boolean = true,
    @Column(name = "created_at", updatable = false)
    var createdAt: LocalDateTime? = null,
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
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
