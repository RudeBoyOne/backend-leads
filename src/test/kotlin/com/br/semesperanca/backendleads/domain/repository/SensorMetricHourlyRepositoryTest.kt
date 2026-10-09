package com.br.semesperanca.backendleads.domain.repository

import com.br.semesperanca.backendleads.domain.model.SensorMetricHourlyId
import com.br.semesperanca.backendleads.support.AbstractRepositoryTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.temporal.ChronoUnit

class SensorMetricHourlyRepositoryTest : AbstractRepositoryTest() {
    @Autowired
    private lateinit var repository: SensorMetricHourlyRepository

    private fun id(
        deviceId: String,
        bucketStart: Instant,
    ) = SensorMetricHourlyId(deviceId = deviceId, bucketStart = bucketStart)

    @Test
    fun `upsertAccumulate deve inserir novo bucket`() {
        val bucketStart = Instant.now().truncatedTo(ChronoUnit.HOURS)

        repository.upsertAccumulate("3DE62F3C", bucketStart, 23.9, 72.3, 4095.0)
        entityManager.flush()
        entityManager.clear()

        val found = repository.findById(id("3DE62F3C", bucketStart))

        assertThat(found).isPresent
        assertThat(found.get().sampleCount).isEqualTo(1)
        assertThat(found.get().temperatureSum).isEqualTo(23.9)
        assertThat(found.get().temperatureMin).isEqualTo(23.9)
        assertThat(found.get().temperatureMax).isEqualTo(23.9)
    }

    @Test
    fun `upsertAccumulate deve acumular no bucket existente`() {
        val bucketStart = Instant.now().truncatedTo(ChronoUnit.HOURS)

        repository.upsertAccumulate("3DE62F3C", bucketStart, 20.0, 50.0, 1000.0)
        entityManager.flush()

        repository.upsertAccumulate("3DE62F3C", bucketStart, 30.0, 80.0, 5000.0)
        entityManager.flush()
        entityManager.clear()

        val found = repository.findById(id("3DE62F3C", bucketStart))

        assertThat(found).isPresent
        assertThat(found.get().sampleCount).isEqualTo(2)
        assertThat(found.get().temperatureSum).isEqualTo(50.0)
        assertThat(found.get().temperatureMin).isEqualTo(20.0)
        assertThat(found.get().temperatureMax).isEqualTo(30.0)
        assertThat(found.get().humiditySum).isEqualTo(130.0)
        assertThat(found.get().humidityMin).isEqualTo(50.0)
        assertThat(found.get().humidityMax).isEqualTo(80.0)
        assertThat(found.get().luminositySum).isEqualTo(6000.0)
        assertThat(found.get().luminosityMin).isEqualTo(1000.0)
        assertThat(found.get().luminosityMax).isEqualTo(5000.0)
    }

    @Test
    fun `upsertAccumulate deve criar buckets separados para horas diferentes`() {
        val now = Instant.now()
        val bucket1 = now.truncatedTo(ChronoUnit.HOURS)
        val bucket2 = bucket1.plus(1, ChronoUnit.HOURS)

        repository.upsertAccumulate("3DE62F3C", bucket1, 20.0, 50.0, 1000.0)
        repository.upsertAccumulate("3DE62F3C", bucket2, 25.0, 60.0, 2000.0)
        entityManager.flush()
        entityManager.clear()

        val found =
            repository.findByDeviceIdAndBucketStartGreaterThanEqualOrderByBucketStartDesc(
                "3DE62F3C",
                bucket1,
            )

        assertThat(found).hasSize(2)
        assertThat(found[0].bucketStart).isEqualTo(bucket2)
        assertThat(found[1].bucketStart).isEqualTo(bucket1)
    }

    @Test
    fun `findByDeviceIdAndBucketStartGreaterThanEqual deve filtrar por data`() {
        val now = Instant.now()
        val bucket1 = now.truncatedTo(ChronoUnit.HOURS).minus(2, ChronoUnit.HOURS)
        val bucket2 = now.truncatedTo(ChronoUnit.HOURS).minus(1, ChronoUnit.HOURS)
        val bucket3 = now.truncatedTo(ChronoUnit.HOURS)

        repository.upsertAccumulate("3DE62F3C", bucket1, 20.0, 50.0, 1000.0)
        repository.upsertAccumulate("3DE62F3C", bucket2, 21.0, 51.0, 1100.0)
        repository.upsertAccumulate("3DE62F3C", bucket3, 22.0, 52.0, 1200.0)
        entityManager.flush()
        entityManager.clear()

        val found =
            repository.findByDeviceIdAndBucketStartGreaterThanEqualOrderByBucketStartDesc(
                "3DE62F3C",
                bucket2,
            )

        assertThat(found).hasSize(2)
        assertThat(found.map { it.bucketStart }).containsExactly(bucket3, bucket2)
    }

    @Test
    fun `findByDeviceIdAndBucketStartGreaterThanEqual deve retornar vazio quando nao ha dados`() {
        val found =
            repository.findByDeviceIdAndBucketStartGreaterThanEqualOrderByBucketStartDesc(
                "INEXISTENTE",
                Instant.now(),
            )

        assertThat(found).isEmpty()
    }
}
