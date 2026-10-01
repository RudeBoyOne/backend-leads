package com.br.semesperanca.backendleads.domain.service.iot

import com.br.semesperanca.backendleads.application.model.request.BindPropertySensorRequest
import com.br.semesperanca.backendleads.application.model.response.HourlySensorMetricResponse
import com.br.semesperanca.backendleads.application.model.response.PropertySensorResponse
import com.br.semesperanca.backendleads.application.model.response.SensorPropertyMetricDetailResponse
import com.br.semesperanca.backendleads.application.model.response.SensorPropertyMetricResponse
import com.br.semesperanca.backendleads.application.model.response.SensorReadingResponse
import com.br.semesperanca.backendleads.domain.model.PropertySensor
import com.br.semesperanca.backendleads.domain.model.SensorMetric
import com.br.semesperanca.backendleads.domain.model.SensorMetricHourly
import com.br.semesperanca.backendleads.domain.repository.PropertyRepository
import com.br.semesperanca.backendleads.domain.repository.PropertySensorRepository
import com.br.semesperanca.backendleads.domain.repository.SensorMetricHourlyRepository
import com.br.semesperanca.backendleads.domain.repository.SensorMetricRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class SensorMetricService(
    private val propertySensorRepository: PropertySensorRepository,
    private val sensorMetricRepository: SensorMetricRepository,
    private val sensorMetricHourlyRepository: SensorMetricHourlyRepository,
    private val propertyRepository: PropertyRepository,
) {
    fun findAll(propertyId: UUID?): List<SensorPropertyMetricResponse> {
        val bindings =
            if (propertyId != null) {
                propertySensorRepository.findByPropertyId(propertyId).map { listOf(it) }.orElse(emptyList())
            } else {
                propertySensorRepository.findAllByOrderByPropertyIdAsc()
            }

        if (bindings.isEmpty()) return emptyList()

        val deviceIds = bindings.map { it.deviceId!! }
        val metrics = sensorMetricRepository.findAllByDeviceIdIn(deviceIds).associateBy { it.deviceId }

        return bindings.mapNotNull { binding ->
            val metric = metrics[binding.deviceId] ?: return@mapNotNull null
            val property = propertyRepository.findById(binding.propertyId!!).orElse(null)
            toResponse(metric, binding, property?.title)
        }
    }

    fun findByProperty(
        propertyId: UUID,
        hours: Int,
    ): SensorPropertyMetricDetailResponse {
        val binding =
            propertySensorRepository
                .findByPropertyId(propertyId)
                .orElseThrow { RuntimeException("Nenhum sensor vinculado a este imovel") }

        val deviceId = binding.deviceId!!
        val property =
            propertyRepository
                .findById(propertyId)
                .orElseThrow { RuntimeException("Imovel nao encontrado") }

        val metric = sensorMetricRepository.findById(deviceId).orElse(null)
        val latest =
            metric?.let {
                SensorReadingResponse(
                    deviceId = it.deviceId!!,
                    temperature = it.temperature,
                    humidity = it.humidity,
                    luminosity = it.luminosity,
                    recordedAt = it.recordedAt,
                )
            }

        val from = Instant.now().minus(hours.toLong(), ChronoUnit.HOURS).truncatedTo(ChronoUnit.HOURS)
        val hourly =
            sensorMetricHourlyRepository
                .findByDeviceIdAndBucketStartGreaterThanEqualOrderByBucketStartDesc(deviceId, from)
                .map { toHourlyResponse(it) }

        return SensorPropertyMetricDetailResponse(
            propertyId = propertyId,
            propertyTitle = property.title,
            deviceId = deviceId,
            latest = latest,
            history = hourly,
        )
    }

    @Transactional
    fun bind(request: BindPropertySensorRequest): PropertySensorResponse {
        val property =
            propertyRepository
                .findById(request.propertyId)
                .orElseThrow { RuntimeException("Imovel nao encontrado") }

        val existing = propertySensorRepository.findByDeviceId(request.deviceId)
        if (existing.isPresent && existing.get().propertyId == request.propertyId) {
            return toResponse(existing.get())
        }

        // Device ja ligado a outro imovel: revincula
        if (existing.isPresent) {
            propertySensorRepository.delete(existing.get())
        }

        val binding =
            PropertySensor(
                propertyId = request.propertyId,
                deviceId = request.deviceId.trim().uppercase(),
            )
        val saved = propertySensorRepository.save(binding)
        return toResponse(saved)
    }

    @Transactional
    fun unbind(propertyId: UUID) {
        val binding =
            propertySensorRepository
                .findByPropertyId(propertyId)
                .orElseThrow { RuntimeException("Nenhum sensor vinculado a este imovel") }
        propertySensorRepository.delete(binding)
    }

    private fun toResponse(
        metric: SensorMetric,
        binding: PropertySensor,
        propertyTitle: String?,
    ): SensorPropertyMetricResponse =
        SensorPropertyMetricResponse(
            propertyId = binding.propertyId!!,
            propertyTitle = propertyTitle,
            deviceId = metric.deviceId!!,
            temperature = metric.temperature,
            humidity = metric.humidity,
            luminosity = metric.luminosity,
            recordedAt = metric.recordedAt,
        )

    private fun toResponse(binding: PropertySensor): PropertySensorResponse =
        PropertySensorResponse(
            propertyId = binding.propertyId!!,
            deviceId = binding.deviceId!!,
            createdAt = binding.createdAt,
        )

    private fun toHourlyResponse(hourly: SensorMetricHourly): HourlySensorMetricResponse {
        val nowBucket = Instant.now().truncatedTo(ChronoUnit.HOURS)
        val isPartial = hourly.bucketStart == nowBucket

        return HourlySensorMetricResponse(
            bucketStart = hourly.bucketStart,
            sampleCount = hourly.sampleCount ?: 0,
            temperatureAvg = (hourly.temperatureSum ?: 0.0) / (hourly.sampleCount ?: 1),
            temperatureMin = hourly.temperatureMin,
            temperatureMax = hourly.temperatureMax,
            humidityAvg = (hourly.humiditySum ?: 0.0) / (hourly.sampleCount ?: 1),
            humidityMin = hourly.humidityMin,
            humidityMax = hourly.humidityMax,
            luminosityAvg = (hourly.luminositySum ?: 0.0) / (hourly.sampleCount ?: 1),
            luminosityMin = hourly.luminosityMin,
            luminosityMax = hourly.luminosityMax,
            partial = isPartial,
        )
    }
}
