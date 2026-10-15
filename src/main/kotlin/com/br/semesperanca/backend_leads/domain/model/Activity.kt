package com.br.semesperanca.backend_leads.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "activity")
class Activity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lead_id", nullable = false)
    var lead: Lead? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id")
    var property: Property? = null,

    @Column(name = "title", nullable = false, length = 150)
    var title: String? = null,

    @Column(name = "description", nullable = false)
    var description: String? = null,

    @Column(name = "activity_date", nullable = false)
    var activityDate: LocalDateTime? = null,

    @Column(name = "completed", nullable = false)
    var completed: Boolean = false,

    @Column(name = "created_at", updatable = false, nullable = false)
    var createdAt: LocalDateTime? = null,
) {
    @PrePersist
    fun prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now()
    }
}
