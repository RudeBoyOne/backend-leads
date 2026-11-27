package com.br.semesperanca.backendleads.domain.repository

import com.br.semesperanca.backendleads.domain.model.SensorMetric
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface SensorMetricRepository : JpaRepository<SensorMetric, String> {
    fun findAllByDeviceIdIn(deviceIds: List<String>): List<SensorMetric>

    @Modifying
    @Query(
        value = """
            INSERT INTO sensor_metric (device_id, temperature, humidity, luminosity, recorded_at)
            VALUES (:deviceId, :temperature, :humidity, :luminosity, :recordedAt)
            ON CONFLICT (device_id) DO UPDATE SET
                temperature = EXCLUDED.temperature,
                humidity    = EXCLUDED.humidity,
                luminosity  = EXCLUDED.luminosity,
                recorded_at = EXCLUDED.recorded_at
        """,
        nativeQuery = true,
    )
    fun upsert(
        @Param("deviceId") deviceId: String,
        @Param("temperature") temperature: Double,
        @Param("humidity") humidity: Double,
        @Param("luminosity") luminosity: Double,
        @Param("recordedAt") recordedAt: Instant,
    )
}
