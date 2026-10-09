package com.br.semesperanca.backendleads.domain.service.iot

import com.br.semesperanca.backendleads.application.model.request.BindPropertySensorRequest
import com.br.semesperanca.backendleads.configuration.exception.NotFoundException
import com.br.semesperanca.backendleads.application.model.response.HourlySensorMetricResponse
import com.br.semesperanca.backendleads.application.model.response.SensorPropertyMetricDetailResponse
import com.br.semesperanca.backendleads.application.model.response.SensorPropertyMetricResponse
import com.br.semesperanca.backendleads.domain.model.Property
import com.br.semesperanca.backendleads.domain.model.PropertySensor
import com.br.semesperanca.backendleads.domain.model.SensorMetric
import com.br.semesperanca.backendleads.domain.model.SensorMetricHourly
import com.br.semesperanca.backendleads.domain.repository.PropertyRepository
import com.br.semesperanca.backendleads.domain.repository.PropertySensorRepository
import com.br.semesperanca.backendleads.domain.repository.SensorMetricHourlyRepository
import com.br.semesperanca.backendleads.domain.repository.SensorMetricRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Optional
import java.util.UUID

class SensorMetricServiceTest {
    private val propertySensorRepository = org.mockito.kotlin.mock<PropertySensorRepository>()
    private val sensorMetricRepository = org.mockito.kotlin.mock<SensorMetricRepository>()
    private val sensorMetricHourlyRepository = org.mockito.kotlin.mock<SensorMetricHourlyRepository>()
    private val propertyRepository = org.mockito.kotlin.mock<PropertyRepository>()

    private val service =
        SensorMetricService(
            propertySensorRepository,
            sensorMetricRepository,
            sensorMetricHourlyRepository,
            propertyRepository,
        )

    private val propertyId = UUID.randomUUID()
    private val deviceId = "3DE62F3C"

    /**
     * `findAll` resolve os títulos por `findAllById`, que é o método escopado do
     * `PropertyRepository`. O id precisa estar na própria entidade: é a chave do
     * mapa que liga o binding ao título.
     */
    private fun visibleProperty(title: String, id: UUID = propertyId) =
        Property(title = title).apply { this.id = id }

    @Test
    fun `findAll deve retornar lista vazia quando nao ha bindings`() {
        whenever(propertySensorRepository.findAllByOrderByPropertyIdAsc()).thenReturn(emptyList())

        val result = service.findAll(null)

        assertThat(result).isEmpty()
    }

    @Test
    fun `findAll deve retornar metricas dos devices vinculados`() {
        val binding = PropertySensor(propertyId = propertyId, deviceId = deviceId)
        val metric =
            SensorMetric(
                deviceId = deviceId,
                temperature = 23.9,
                humidity = 72.3,
                luminosity = 4095.0,
                recordedAt = Instant.now(),
            )
        whenever(propertySensorRepository.findAllByOrderByPropertyIdAsc()).thenReturn(listOf(binding))
        whenever(sensorMetricRepository.findAllByDeviceIdIn(listOf(deviceId))).thenReturn(listOf(metric))
        whenever(propertyRepository.findAllById(setOf(propertyId))).thenReturn(listOf(visibleProperty("Apartamento no centro")))

        val result = service.findAll(null)

        assertThat(result).hasSize(1)
        assertThat(result[0].deviceId).isEqualTo(deviceId)
        assertThat(result[0].propertyId).isEqualTo(propertyId)
        assertThat(result[0].propertyTitle).isEqualTo("Apartamento no centro")
        assertThat(result[0].temperature).isEqualTo(23.9)
    }

    @Test
    fun `findAll deve filtrar por propertyId quando informado`() {
        val binding = PropertySensor(propertyId = propertyId, deviceId = deviceId)
        val metric =
            SensorMetric(
                deviceId = deviceId,
                temperature = 23.9,
                humidity = 72.3,
                luminosity = 4095.0,
                recordedAt = Instant.now(),
            )

        whenever(propertySensorRepository.findByPropertyId(propertyId)).thenReturn(Optional.of(binding))
        whenever(sensorMetricRepository.findAllByDeviceIdIn(listOf(deviceId))).thenReturn(listOf(metric))
        whenever(propertyRepository.findAllById(setOf(propertyId))).thenReturn(listOf(visibleProperty("Imovel")))

        val result = service.findAll(propertyId)

        assertThat(result).hasSize(1)
        assertThat(result[0].propertyId).isEqualTo(propertyId)
    }

    /**
     * `property_sensor` não é entidade escopada — o vínculo herda o escopo do
     * imóvel. Como o `findAllById` consulto devolve só o que o principal enxerga,
     * um binding cujo imóvel está fora do escopo desaparece da lista, e o serviço
     * nem chega a ler as métricas dele. Era este o furo: sem o corte, um branch
     * owner veria temperatura e umidade de imóveis de outra agência.
     */
    @Test
    fun `binding de imovel fora do escopo nao aparece, ainda que o device tenha metricas`() {
        val binding = PropertySensor(propertyId = propertyId, deviceId = deviceId)
        val metric =
            SensorMetric(
                deviceId = deviceId,
                temperature = 23.9,
                humidity = 72.3,
                luminosity = 4095.0,
                recordedAt = Instant.now(),
            )

        whenever(propertySensorRepository.findAllByOrderByPropertyIdAsc()).thenReturn(listOf(binding))
        whenever(propertyRepository.findAllById(setOf(propertyId))).thenReturn(emptyList())

        assertThat(service.findAll(null)).isEmpty()

        verify(sensorMetricRepository, never()).findAllByDeviceIdIn(any())
    }

    @Test
    fun `findByProperty deve lancar excecao quando imovel nao tem sensor vinculado`() {
        whenever(propertySensorRepository.findByPropertyId(propertyId)).thenReturn(Optional.empty())

        assertThrows<NotFoundException> {
            service.findByProperty(propertyId, 24)
        }
    }

    @Test
    fun `findByProperty deve retornar detalhe com latest e history`() {
        val binding = PropertySensor(propertyId = propertyId, deviceId = deviceId)
        val metric =
            SensorMetric(
                deviceId = deviceId,
                temperature = 23.9,
                humidity = 72.3,
                luminosity = 4095.0,
                recordedAt = Instant.now(),
            )
        val now = Instant.now()
        val bucketStart = now.truncatedTo(ChronoUnit.HOURS)
        val hourly =
            SensorMetricHourly(
                deviceId = deviceId,
                bucketStart = bucketStart,
                sampleCount = 10,
                temperatureSum = 237.0,
                temperatureMin = 23.0,
                temperatureMax = 24.5,
                humiditySum = 720.0,
                humidityMin = 70.0,
                humidityMax = 75.0,
                luminositySum = 40000.0,
                luminosityMin = 4000.0,
                luminosityMax = 4095.0,
            )

        whenever(propertySensorRepository.findByPropertyId(propertyId)).thenReturn(Optional.of(binding))
        whenever(propertyRepository.findById(propertyId)).thenReturn(Optional.of(Property(title = "Imovel")))
        whenever(sensorMetricRepository.findById(deviceId)).thenReturn(Optional.of(metric))
        whenever(sensorMetricHourlyRepository.findByDeviceIdAndBucketStartGreaterThanEqualOrderByBucketStartDesc(eq(deviceId), any()))
            .thenReturn(listOf(hourly))

        val result = service.findByProperty(propertyId, 24)

        assertThat(result.deviceId).isEqualTo(deviceId)
        assertThat(result.latest).isNotNull
        assertThat(result.latest!!.temperature).isEqualTo(23.9)
        assertThat(result.history).hasSize(1)
        assertThat(result.history[0].sampleCount).isEqualTo(10)
        assertThat(result.history[0].temperatureAvg).isEqualTo(23.7)
        assertThat(result.history[0].partial).isTrue()
    }

    @Test
    fun `findByProperty deve marcar bucket anterior como nao parcial`() {
        val binding = PropertySensor(propertyId = propertyId, deviceId = deviceId)
        val now = Instant.now()
        val previousBucket = now.truncatedTo(ChronoUnit.HOURS).minus(1, ChronoUnit.HOURS)
        val hourly =
            SensorMetricHourly(
                deviceId = deviceId,
                bucketStart = previousBucket,
                sampleCount = 60,
                temperatureSum = 1400.0,
                temperatureMin = 22.0,
                temperatureMax = 25.0,
                humiditySum = 4200.0,
                humidityMin = 68.0,
                humidityMax = 76.0,
                luminositySum = 240000.0,
                luminosityMin = 3900.0,
                luminosityMax = 4095.0,
            )

        whenever(propertySensorRepository.findByPropertyId(propertyId)).thenReturn(Optional.of(binding))
        whenever(propertyRepository.findById(propertyId)).thenReturn(Optional.of(Property(title = "Imovel")))
        whenever(sensorMetricRepository.findById(deviceId)).thenReturn(Optional.empty())
        whenever(sensorMetricHourlyRepository.findByDeviceIdAndBucketStartGreaterThanEqualOrderByBucketStartDesc(eq(deviceId), any()))
            .thenReturn(listOf(hourly))

        val result = service.findByProperty(propertyId, 24)

        assertThat(result.latest).isNull()
        assertThat(result.history[0].partial).isFalse()
    }

    @Test
    fun `bind deve criar vinculo quando device nao esta vinculado`() {
        val request = BindPropertySensorRequest(propertyId = propertyId, deviceId = deviceId)
        val saved = PropertySensor(propertyId = propertyId, deviceId = deviceId)

        whenever(propertyRepository.findById(propertyId)).thenReturn(Optional.of(Property(title = "Imovel")))
        whenever(propertySensorRepository.findByDeviceId(deviceId)).thenReturn(Optional.empty())
        whenever(propertySensorRepository.save(any())).thenReturn(saved)

        val result = service.bind(request)

        assertThat(result.propertyId).isEqualTo(propertyId)
        assertThat(result.deviceId).isEqualTo(deviceId)
        verify(propertySensorRepository).save(any())
    }

    @Test
    fun `bind deve ser idempotente quando device ja esta vinculado ao mesmo imovel`() {
        val request = BindPropertySensorRequest(propertyId = propertyId, deviceId = deviceId)
        val existing = PropertySensor(propertyId = propertyId, deviceId = deviceId)

        whenever(propertyRepository.findById(propertyId)).thenReturn(Optional.of(Property(title = "Imovel")))
        whenever(propertySensorRepository.findByDeviceId(deviceId)).thenReturn(Optional.of(existing))

        val result = service.bind(request)

        assertThat(result.propertyId).isEqualTo(propertyId)
        verify(propertySensorRepository, never()).save(any())
        verify(propertySensorRepository, never()).delete(any())
    }

    @Test
    fun `bind deve revincular device quando esta em outro imovel`() {
        val outroPropertyId = UUID.randomUUID()
        val request = BindPropertySensorRequest(propertyId = propertyId, deviceId = deviceId)
        val existing = PropertySensor(propertyId = outroPropertyId, deviceId = deviceId)
        val saved = PropertySensor(propertyId = propertyId, deviceId = deviceId)

        whenever(propertyRepository.findById(propertyId)).thenReturn(Optional.of(Property(title = "Imovel")))
        whenever(propertySensorRepository.findByDeviceId(deviceId)).thenReturn(Optional.of(existing))
        whenever(propertySensorRepository.save(any())).thenReturn(saved)

        val result = service.bind(request)

        assertThat(result.propertyId).isEqualTo(propertyId)
        verify(propertySensorRepository).delete(existing)
        verify(propertySensorRepository).save(any())
    }

    @Test
    fun `bind deve lancar excecao quando imovel nao existe`() {
        val request = BindPropertySensorRequest(propertyId = propertyId, deviceId = deviceId)

        whenever(propertyRepository.findById(propertyId)).thenReturn(Optional.empty())

        assertThrows<NotFoundException> {
            service.bind(request)
        }
    }

    @Test
    fun `unbind deve remover vinculo existente`() {
        val binding = PropertySensor(propertyId = propertyId, deviceId = deviceId)

        whenever(propertyRepository.findById(propertyId)).thenReturn(Optional.of(visibleProperty("Imovel")))
        whenever(propertySensorRepository.findByPropertyId(propertyId)).thenReturn(Optional.of(binding))

        service.unbind(propertyId)

        verify(propertySensorRepository).delete(binding)
    }

    @Test
    fun `unbind deve lancar excecao quando imovel nao tem sensor vinculado`() {
        whenever(propertyRepository.findById(propertyId)).thenReturn(Optional.of(visibleProperty("Imovel")))
        whenever(propertySensorRepository.findByPropertyId(propertyId)).thenReturn(Optional.empty())

        assertThrows<NotFoundException> {
            service.unbind(propertyId)
        }
    }

    /**
     * `unbind` valida o imóvel pelo `PropertyRepository` escopado antes de
     * desvincular. Sem essa linha, o desvinculo funcionaria para o imóvel de
     * qualquer agência, porque `property_sensor` não carrega escopo.
     */
    @Test
    fun `unbind de imovel fora do escopo aborta sem tocar no vinculo`() {
        whenever(propertyRepository.findById(propertyId)).thenReturn(Optional.empty())

        assertThrows<NotFoundException> {
            service.unbind(propertyId)
        }

        verify(propertySensorRepository, never()).delete(any())
        verify(propertySensorRepository, never()).findByPropertyId(any())
    }
}
