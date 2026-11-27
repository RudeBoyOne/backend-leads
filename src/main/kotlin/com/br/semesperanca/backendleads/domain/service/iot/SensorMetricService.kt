package com.br.semesperanca.backendleads.domain.service.iot

import com.br.semesperanca.backendleads.application.model.request.BindPropertySensorRequest
import com.br.semesperanca.backendleads.application.model.request.CreateSensorRequest
import com.br.semesperanca.backendleads.application.model.request.UpdatePropertySensorRequest
import com.br.semesperanca.backendleads.application.model.request.UpdateSensorRequest
import com.br.semesperanca.backendleads.application.model.response.HourlySensorMetricResponse
import com.br.semesperanca.backendleads.application.model.response.PropertySensorResponse
import com.br.semesperanca.backendleads.application.model.response.SensorMetricResponse
import com.br.semesperanca.backendleads.application.model.response.SensorPropertyMetricDetailResponse
import com.br.semesperanca.backendleads.application.model.response.SensorPropertyMetricResponse
import com.br.semesperanca.backendleads.application.model.response.SensorReadingResponse
import com.br.semesperanca.backendleads.configuration.exception.NotFoundException
import com.br.semesperanca.backendleads.domain.model.PropertySensor
import com.br.semesperanca.backendleads.domain.model.SensorMetric
import com.br.semesperanca.backendleads.domain.model.SensorMetricHourly
import com.br.semesperanca.backendleads.domain.repository.PropertyRepository
import com.br.semesperanca.backendleads.domain.repository.PropertySensorRepository
import com.br.semesperanca.backendleads.domain.repository.SensorMetricHourlyRepository
import com.br.semesperanca.backendleads.domain.repository.SensorMetricRepository
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
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

        val visibleTitles = visiblePropertyTitles(bindings.mapNotNull { it.propertyId })
        if (visibleTitles.isEmpty()) return emptyList()

        val deviceIds = bindings.map { it.deviceId!! }
        val metrics = sensorMetricRepository.findAllByDeviceIdIn(deviceIds).associateBy { it.deviceId }

        return bindings.mapNotNull { binding ->
            val title = visibleTitles[binding.propertyId] ?: return@mapNotNull null
            val metric = metrics[binding.deviceId] ?: return@mapNotNull null
            toResponse(metric, binding, title)
        }
    }

    private fun visiblePropertyTitles(propertyIds: List<UUID>): Map<UUID, String> =
        if (propertyIds.isEmpty()) {
            emptyMap()
        } else {
            propertyRepository
                .findAllById(propertyIds.toSet())
                .mapNotNull { property -> property.id?.let { it to property.title!! } }
                .toMap()
        }

    fun findByProperty(
        propertyId: UUID,
        hours: Int,
    ): SensorPropertyMetricDetailResponse {
        val binding =
            propertySensorRepository
                .findByPropertyId(propertyId)
                .orElseThrow { NotFoundException("Nenhum sensor vinculado a este imovel") }

        val deviceId = binding.deviceId!!
        val property =
            propertyRepository
                .findById(propertyId)
                .orElseThrow { NotFoundException("Imovel nao encontrado") }

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

    @PreAuthorize("hasRole('FULL_ADMIN') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Transactional
    fun bind(request: BindPropertySensorRequest): PropertySensorResponse {
        val property =
            propertyRepository
                .findById(request.propertyId)
                .orElseThrow { NotFoundException("Imovel nao encontrado") }

        val existing = propertySensorRepository.findByDeviceId(request.deviceId)
        if (existing.isPresent && existing.get().propertyId == request.propertyId) {
            return toResponse(existing.get())
        }

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

    @PreAuthorize("hasRole('FULL_ADMIN') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Transactional
    fun unbind(propertyId: UUID) {
        propertyRepository.findById(propertyId).orElseThrow { NotFoundException("Imovel nao encontrado") }

        val binding =
            propertySensorRepository
                .findByPropertyId(propertyId)
                .orElseThrow { NotFoundException("Nenhum sensor vinculado a este imovel") }
        propertySensorRepository.delete(binding)
    }

    fun findPropertySensorById(id: UUID): PropertySensorResponse = toResponse(findPropertySensor(id))

    @PreAuthorize("hasRole('FULL_ADMIN') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Transactional
    fun updatePropertySensor(
        id: UUID,
        request: UpdatePropertySensorRequest,
    ): PropertySensorResponse {
        val binding = findPropertySensor(id)
        request.label?.let { binding.label = it }
        request.active?.let { binding.active = it }
        request.samplingIntervalSeconds?.let { binding.samplingIntervalSeconds = it }
        request.installedAt?.let { binding.installedAt = it }
        return toResponse(propertySensorRepository.save(binding))
    }

    @PreAuthorize("hasRole('FULL_ADMIN') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Transactional
    fun deletePropertySensor(id: UUID) {
        propertySensorRepository.delete(findPropertySensor(id))
    }

    @PreAuthorize("hasRole('FULL_ADMIN') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Transactional
    fun createSensor(request: CreateSensorRequest): SensorMetricResponse {
        val deviceId = request.deviceId.trim().uppercase()
        if (sensorMetricRepository.existsById(deviceId)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "deviceId já cadastrado")
        }

        val metric =
            SensorMetric(
                deviceId = deviceId,
                temperature = request.temperature,
                humidity = request.humidity,
                luminosity = request.luminosity,
                batteryLevel = request.batteryLevel,
                isValid = request.isValid,
                recordedAt = request.recordedAt ?: Instant.now(),
            )
        return toSensorResponse(sensorMetricRepository.save(metric))
    }

    fun findSensorById(deviceId: String): SensorMetricResponse = toSensorResponse(findSensor(deviceId))

    fun findAllSensors(): List<SensorMetricResponse> = sensorMetricRepository.findAll().map(::toSensorResponse)

    @PreAuthorize("hasRole('FULL_ADMIN') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Transactional
    fun updateSensor(
        deviceId: String,
        request: UpdateSensorRequest,
    ): SensorMetricResponse {
        val metric = findSensor(deviceId)
        request.temperature?.let { metric.temperature = it }
        request.humidity?.let { metric.humidity = it }
        request.luminosity?.let { metric.luminosity = it }
        request.batteryLevel?.let { metric.batteryLevel = it }
        request.isValid?.let { metric.isValid = it }
        request.recordedAt?.let { metric.recordedAt = it }
        return toSensorResponse(sensorMetricRepository.save(metric))
    }

    @PreAuthorize("hasRole('FULL_ADMIN') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Transactional
    fun deleteSensor(deviceId: String) {
        sensorMetricRepository.delete(findSensor(deviceId))
    }

    private fun findPropertySensor(id: UUID): PropertySensor =
        propertySensorRepository.findById(id).orElseThrow { NotFoundException("Sensor vinculado não encontrado") }

    private fun findSensor(deviceId: String): SensorMetric =
        sensorMetricRepository.findById(deviceId).orElseThrow { NotFoundException("Leitura não encontrada") }

    private fun toSensorResponse(metric: SensorMetric): SensorMetricResponse =
        SensorMetricResponse(
            deviceId = metric.deviceId!!,
            temperature = metric.temperature!!,
            humidity = metric.humidity!!,
            luminosity = metric.luminosity!!,
            batteryLevel = metric.batteryLevel,
            isValid = metric.isValid,
            recordedAt = metric.recordedAt,
        )

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
            id = binding.id,
            propertyId = binding.propertyId!!,
            deviceId = binding.deviceId!!,
            label = binding.label,
            active = binding.active,
            samplingIntervalSeconds = binding.samplingIntervalSeconds,
            installedAt = binding.installedAt,
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
