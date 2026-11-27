package com.br.semesperanca.backendleads.domain.model

import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "property_sensor")
class PropertySensor(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,
    @Column(name = "property_id", nullable = false)
    var propertyId: UUID? = null,
    @Column(name = "device_id", nullable = false)
    var deviceId: String? = null,
    @Column(name = "label", length = 100)
    var label: String? = null,
    @Column(name = "active", nullable = false)
    var active: Boolean = true,
    @Column(name = "sampling_interval_seconds")
    var samplingIntervalSeconds: Int? = null,
    @Column(name = "installed_at")
    var installedAt: LocalDateTime? = null,
    @Column(name = "created_at", updatable = false, nullable = false)
    var createdAt: Instant? = null,
) {
    @PrePersist
    fun prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now()
        }
        if (installedAt == null) {
            installedAt = LocalDateTime.now()
        }
    }
}
