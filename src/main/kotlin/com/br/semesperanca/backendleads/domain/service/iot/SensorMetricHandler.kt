package com.br.semesperanca.backendleads.domain.service.iot

import com.br.semesperanca.backendleads.application.model.iot.SensorReadingPayload
import com.br.semesperanca.backendleads.domain.repository.SensorMetricHourlyRepository
import com.br.semesperanca.backendleads.domain.repository.SensorMetricRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

@Component
class SensorMetricHandler(
    private val sensorMetricRepository: SensorMetricRepository,
    private val sensorMetricHourlyRepository: SensorMetricHourlyRepository,
) {
    @Transactional
    fun record(
        deviceId: String,
        reading: SensorReadingPayload,
    ) {
        val now = Instant.now()
        val bucketStart = now.truncatedTo(ChronoUnit.HOURS)

        sensorMetricRepository.upsert(
            deviceId = deviceId,
            temperature = reading.temperature!!,
            humidity = reading.humidity!!,
            luminosity = reading.luminosity!!,
            recordedAt = now,
        )

        sensorMetricHourlyRepository.upsertAccumulate(
            deviceId = deviceId,
            bucketStart = bucketStart,
            temperature = reading.temperature,
            humidity = reading.humidity,
            luminosity = reading.luminosity,
        )
    }
}
