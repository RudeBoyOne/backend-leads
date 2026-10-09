package com.br.semesperanca.backendleads.domain.repository

import com.br.semesperanca.backendleads.support.AbstractRepositoryTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.temporal.ChronoUnit

class SensorMetricRepositoryTest : AbstractRepositoryTest() {
    @Autowired
    private lateinit var repository: SensorMetricRepository

    @Test
    fun `upsert deve inserir nova metrica`() {
        // TIMESTAMPTZ guarda microssegundos; um Instant com nanos volta
        // truncado e a igualdade exata falharia.
        val now = Instant.now().truncatedTo(ChronoUnit.MICROS)

        repository.upsert("3DE62F3C", 23.9, 72.3, 4095.0, now)
        entityManager.flush()
        entityManager.clear()

        val found = repository.findById("3DE62F3C")

        assertThat(found).isPresent
        assertThat(found.get().temperature).isEqualTo(23.9)
        assertThat(found.get().humidity).isEqualTo(72.3)
        assertThat(found.get().luminosity).isEqualTo(4095.0)
        assertThat(found.get().recordedAt).isEqualTo(now)
    }

    @Test
    fun `upsert deve atualizar metrica existente`() {
        val now = Instant.now()

        repository.upsert("3DE62F3C", 23.9, 72.3, 4095.0, now)
        entityManager.flush()

        repository.upsert("3DE62F3C", 25.0, 70.0, 3000.0, now.plusSeconds(60))
        entityManager.flush()
        entityManager.clear()

        val found = repository.findById("3DE62F3C")

        assertThat(found).isPresent
        assertThat(found.get().temperature).isEqualTo(25.0)
        assertThat(found.get().humidity).isEqualTo(70.0)
        assertThat(found.get().luminosity).isEqualTo(3000.0)
    }

    @Test
    fun `findAllByDeviceIdIn deve retornar metricas dos devices informados`() {
        val now = Instant.now()
        repository.upsert("AAAA", 20.0, 50.0, 1000.0, now)
        repository.upsert("BBBB", 21.0, 51.0, 2000.0, now)
        repository.upsert("CCCC", 22.0, 52.0, 3000.0, now)
        entityManager.flush()
        entityManager.clear()

        val found = repository.findAllByDeviceIdIn(listOf("AAAA", "CCCC"))

        assertThat(found).hasSize(2)
        assertThat(found.map { it.deviceId }).containsExactlyInAnyOrder("AAAA", "CCCC")
    }

    @Test
    fun `findAllByDeviceIdIn deve retornar vazio quando nenhum device informado`() {
        val found = repository.findAllByDeviceIdIn(emptyList())

        assertThat(found).isEmpty()
    }
}
