package com.br.semesperanca.backendleads.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "real_estate_agency")
class RealEstateAgency(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,
    @Column(name = "name", nullable = false, length = 150)
    var name: String? = null,
    @Column(name = "cnpj", nullable = false, unique = true, length = 18)
    var cnpj: String? = null,
    @Column(name = "email", nullable = false, unique = true, length = 150)
    var email: String? = null,
    @Column(name = "phone", length = 20)
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
