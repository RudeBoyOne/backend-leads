package com.br.semesperanca.backendleads.domain.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "activity")
open class Activity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    open var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lead_id", nullable = false)
    open var lead: Lead,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id")
    open var property: Property? = null,

    @Column(name = "title", nullable = false)
    open var title: String? = null,

    @Column(name = "description", nullable = false)
    open var description: String? = null,

    @Column(name = "activity_date", nullable = false)
    open var activityDate: LocalDateTime? = null,

    @Column(name = "created_at", updatable = false, nullable = false)
    open var createdAt: LocalDateTime? = null
) {
    @PrePersist
    fun prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now()
        }
    }
}