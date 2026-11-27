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
@Table(name = "role")
class Role(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,
    @Column(name = "name", nullable = false, unique = true)
    var name: String? = null,
    @Column(name = "description", length = 150)
    var description: String? = null,
    @Column(name = "active", nullable = false)
    var active: Boolean = true,
    @Column(name = "hierarchy_level", nullable = false)
    var hierarchyLevel: Int = 0,
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

    companion object {
        const val FULL_ADMIN = "FULL_ADMIN"
        const val ADMIN = "ADMIN"
        const val MANAGER = "MANAGER"
        const val REAL_ESTATE_AGENT = "REAL_ESTATE_AGENT"
    }
}
