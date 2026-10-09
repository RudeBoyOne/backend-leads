package com.br.semesperanca.backendleads.domain.service.iot

import com.br.semesperanca.backendleads.application.model.iot.SensorReadingPayload
import com.br.semesperanca.backendleads.domain.model.SensorMetricHourlyId
import com.br.semesperanca.backendleads.domain.repository.SensorMetricHourlyRepository
import com.br.semesperanca.backendleads.domain.repository.SensorMetricRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import java.time.Instant
import java.time.temporal.ChronoUnit

@ExtendWith(MockitoExtension::class)
class SensorMetricHandlerTest {
    @Mock private lateinit var metricRepository: SensorMetricRepository

    @Mock private lateinit var hourlyRepository: SensorMetricHourlyRepository

    private lateinit var handler: SensorMetricHandler

    @BeforeEach
    fun setUp() {
        handler = SensorMetricHandler(metricRepository, hourlyRepository)
    }

    private val reading =
        SensorReadingPayload(
            temperature = 23.9,
            humidity = 72.3,
            luminosity = 4095.0,
        )

    @Test
    fun `record grava a ultima leitura com os tres campos`() {
        handler.record("3DE62F3C", reading)

        verify(metricRepository).upsert(
            deviceId = eq("3DE62F3C"),
            temperature = eq(23.9),
            humidity = eq(72.3),
            luminosity = eq(4095.0),
            recordedAt = any<Instant>(),
        )
    }

    @Test
    fun `record acumula no bucket da hora corrente, truncado em hora UTC`() {
        val antes = Instant.now().truncatedTo(ChronoUnit.HOURS)

        handler.record("3DE62F3C", reading)

        val captor = argumentCaptor<Instant>()
        verify(hourlyRepository).upsertAccumulate(
            deviceId = eq("3DE62F3C"),
            bucketStart = captor.capture(),
            temperature = eq(23.9),
            humidity = eq(72.3),
            luminosity = eq(4095.0),
        )

        val bucket = captor.firstValue
        assertThat(bucket.truncatedTo(ChronoUnit.HOURS)).isEqualTo(bucket)
        assertThat(bucket).isAfterOrEqualTo(antes)
    }

    @Test
    fun `as duas escritas acontecem para o mesmo deviceId`() {
        handler.record("3DE62F3C", reading)

        verify(metricRepository).upsert(eq("3DE62F3C"), any(), any(), any(), any())
        verify(hourlyRepository).upsertAccumulate(eq("3DE62F3C"), any(), any(), any(), any())
    }

    @Test
    fun `SensorMetricHourlyId compara e hasheia device e bucket`() {
        val bucket = Instant.parse("2026-01-15T10:00:00Z")
        val id = SensorMetricHourlyId("3DE62F3C", bucket)

        assertThat(id).isEqualTo(SensorMetricHourlyId("3DE62F3C", bucket)).hasSameHashCodeAs(SensorMetricHourlyId("3DE62F3C", bucket))
        assertThat(id).isNotEqualTo(SensorMetricHourlyId("OUTRO", bucket))
        assertThat(id).isNotEqualTo(SensorMetricHourlyId("3DE62F3C", bucket.plusSeconds(3600)))
        assertThat(id).isEqualTo(id)
        assertThat(id).isNotEqualTo("outro tipo")
        assertThat(id.hashCode()).isEqualTo(31 * "3DE62F3C".hashCode() + bucket.hashCode())
    }
}
