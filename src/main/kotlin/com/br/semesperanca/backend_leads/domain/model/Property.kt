package com.br.semesperanca.backendleads.domain.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID


@Entity
@Table(name = "property")
class Property(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(name = "real_estate_agency_id", nullable = false)
    var realEstateAgencyId: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_office_id", nullable = false)
    var branchOffice: BranchOffice? = null,

    @Column(name = "title", nullable = false)
    var title: String? = null,

    @Column(name = "description")
    var description: String? = null,

    @Column(name = "price", precision = 12, scale = 2)
    var price: BigDecimal? = null,

    @Column(name = "property_type")
    var propertyType: String? = null,

    @Column(name = "bedrooms")
    var bedrooms: Int? = null,

    @Column(name = "bathrooms")
    var bathrooms: Int? = null,

    @Column(name = "area_sqm", precision = 10, scale = 2)
    var areaSqm: BigDecimal? = null,

    @Column(name = "address")
    var address: String? = null,

    @Column(name = "city")
    var city: String? = null,

    @Column(name = "state")
    var state: String? = null,

    @Column(name = "zip_code")
    var zipCode: String? = null,

    @Column(name = "status", nullable = false)
    var status: String? = "available",

    @Column(name = "active", nullable = false)
    var active: Boolean? = true,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime? = null,

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null
) {
    @OneToMany(mappedBy = "property", fetch = FetchType.LAZY)
    var activities: MutableList<Activity> = mutableListOf()

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