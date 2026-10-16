package com.br.semesperanca.backendleads.domain.repository

import com.br.semesperanca.backendleads.domain.model.SensorMetricHourly
import com.br.semesperanca.backendleads.domain.model.SensorMetricHourlyId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface SensorMetricHourlyRepository : JpaRepository<SensorMetricHourly, SensorMetricHourlyId> {
    fun findByDeviceIdAndBucketStartGreaterThanEqualOrderByBucketStartDesc(
        deviceId: String,
        from: Instant,
    ): List<SensorMetricHourly>

    /**
     * Acumula o bucket horario: soma, min e max. A media e derivada de
     * sum/sample_count na leitura, entao o bucket e acumulavel sem recalcular
     * a partir do historico.
     */
    @Modifying
    @Query(
        value = """
            INSERT INTO sensor_metric_hourly (
                device_id, bucket_start, sample_count,
                temperature_sum, temperature_min, temperature_max,
                humidity_sum, humidity_min, humidity_max,
                luminosity_sum, luminosity_min, luminosity_max,
                updated_at
            ) VALUES (
                :deviceId, :bucketStart, 1,
                :temperature, :temperature, :temperature,
                :humidity, :humidity, :humidity,
                :luminosity, :luminosity, :luminosity,
                NOW()
            )
            ON CONFLICT (device_id, bucket_start) DO UPDATE SET
                sample_count    = sensor_metric_hourly.sample_count + 1,
                temperature_sum = sensor_metric_hourly.temperature_sum + EXCLUDED.temperature_sum,
                temperature_min = LEAST(sensor_metric_hourly.temperature_min, EXCLUDED.temperature_min),
                temperature_max = GREATEST(sensor_metric_hourly.temperature_max, EXCLUDED.temperature_max),
                humidity_sum    = sensor_metric_hourly.humidity_sum + EXCLUDED.humidity_sum,
                humidity_min    = LEAST(sensor_metric_hourly.humidity_min, EXCLUDED.humidity_min),
                humidity_max    = GREATEST(sensor_metric_hourly.humidity_max, EXCLUDED.humidity_max),
                luminosity_sum  = sensor_metric_hourly.luminosity_sum + EXCLUDED.luminosity_sum,
                luminosity_min  = LEAST(sensor_metric_hourly.luminosity_min, EXCLUDED.luminosity_min),
                luminosity_max  = GREATEST(sensor_metric_hourly.luminosity_max, EXCLUDED.luminosity_max),
                updated_at      = NOW()
        """,
        nativeQuery = true,
    )
    fun upsertAccumulate(
        @Param("deviceId") deviceId: String,
        @Param("bucketStart") bucketStart: Instant,
        @Param("temperature") temperature: Double,
        @Param("humidity") humidity: Double,
        @Param("luminosity") luminosity: Double,
    )
}
