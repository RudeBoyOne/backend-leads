package com.br.semesperanca.backendleads.domain.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "sensor_metric")
class SensorMetric(
    @Id
    @Column(name = "device_id", nullable = false)
    var deviceId: String? = null,
    @Column(name = "temperature", nullable = false)
    var temperature: Double? = null,
    @Column(name = "humidity", nullable = false)
    var humidity: Double? = null,
    @Column(name = "luminosity", nullable = false)
    var luminosity: Double? = null,
    @Column(name = "recorded_at", nullable = false)
    var recordedAt: Instant? = null,
)
