package com.br.semesperanca.backendleads.domain.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "property")
open class Property(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    open var id: UUID? = null,

    @Column(name = "real_estate_agency_id", nullable = false)
    open var realEstateAgencyId: UUID? = null,

    @Column(name = "title", nullable = false)
    open var title: String? = null,

    @Column(name = "description")
    open var description: String? = null,

    @Column(name = "price", precision = 12, scale = 2)
    open var price: BigDecimal? = null,

    @Column(name = "property_type")
    open var propertyType: String? = null,

    @Column(name = "bedrooms")
    open var bedrooms: Int? = null,

    @Column(name = "bathrooms")
    open var bathrooms: Int? = null,

    @Column(name = "area_sqm", precision = 10, scale = 2)
    open var areaSqm: BigDecimal? = null,

    @Column(name = "address")
    open var address: String? = null,

    @Column(name = "city")
    open var city: String? = null,

    @Column(name = "state")
    open var state: String? = null,

    @Column(name = "zip_code")
    open var zipCode: String? = null,

    @Column(name = "status", nullable = false)
    open var status: String? = "available",

    @Column(name = "active", nullable = false)
    open var active: Boolean = true,

    @Column(name = "created_at", updatable = false)
    open var createdAt: LocalDateTime? = null,

    @Column(name = "updated_at")
    open var updatedAt: LocalDateTime? = null
) {
    @OneToMany(mappedBy = "property", fetch = FetchType.LAZY)
    open var activities: MutableList<Activity> = mutableListOf()

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