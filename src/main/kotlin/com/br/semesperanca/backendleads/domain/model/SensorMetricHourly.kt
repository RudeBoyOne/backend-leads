package com.br.semesperanca.backendleads.domain.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "sensor_metric_hourly")
@IdClass(SensorMetricHourlyId::class)
class SensorMetricHourly(
    @Id
    @Column(name = "device_id", nullable = false)
    var deviceId: String? = null,
    @Id
    @Column(name = "bucket_start", nullable = false)
    var bucketStart: Instant? = null,
    @Column(name = "sample_count", nullable = false)
    var sampleCount: Int? = 0,
    @Column(name = "temperature_sum", nullable = false)
    var temperatureSum: Double? = 0.0,
    @Column(name = "temperature_min", nullable = false)
    var temperatureMin: Double? = null,
    @Column(name = "temperature_max", nullable = false)
    var temperatureMax: Double? = null,
    @Column(name = "humidity_sum", nullable = false)
    var humiditySum: Double? = 0.0,
    @Column(name = "humidity_min", nullable = false)
    var humidityMin: Double? = null,
    @Column(name = "humidity_max", nullable = false)
    var humidityMax: Double? = null,
    @Column(name = "luminosity_sum", nullable = false)
    var luminositySum: Double? = 0.0,
    @Column(name = "luminosity_min", nullable = false)
    var luminosityMin: Double? = null,
    @Column(name = "luminosity_max", nullable = false)
    var luminosityMax: Double? = null,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null,
) {
    @PrePersist
    fun prePersist() {
        if (updatedAt == null) {
            updatedAt = Instant.now()
        }
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = Instant.now()
    }
}
