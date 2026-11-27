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

    @Column(name = "title", nullable = false, length = 100)
    var title: String? = null,

    @Column(name = "description", length = 200)
    var description: String? = null,

    @Column(name = "price", precision = 12, scale = 2)
    var price: BigDecimal? = null,

    @Column(name = "property_type", length = 50)
    var propertyType: String? = null,

    @Column(name = "bedrooms")
    var bedrooms: Int? = null,

    @Column(name = "bathrooms")
    var bathrooms: Int? = null,

    @Column(name = "area_sqm", precision = 10, scale = 2)
    var areaSqm: BigDecimal? = null,

    @Column(name = "address", length = 200)
    var address: String? = null,

    @Column(name = "city", length = 100)
    var city: String? = null,

    @Column(name = "state", length = 50)
    var state: String? = null,

    @Column(name = "zip_code", length = 9)
    var zipCode: String? = null,

    @Column(name = "status", nullable = false, length = 20)
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