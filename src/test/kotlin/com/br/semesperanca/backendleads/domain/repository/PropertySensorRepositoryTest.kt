package com.br.semesperanca.backendleads.domain.repository

import com.br.semesperanca.backendleads.domain.model.PropertySensor
import com.br.semesperanca.backendleads.support.AbstractRepositoryTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class PropertySensorRepositoryTest : AbstractRepositoryTest() {
    @Autowired
    private lateinit var repository: PropertySensorRepository

    @Autowired
    private lateinit var propertyRepository: PropertyRepository

    // `property.real_estate_agency_id` e NOT NULL REFERENCES: a agencia real
    // precisa existir, e o insert so pode rodar depois que o Spring injetou o
    // EntityManager — por isso o init no @BeforeEach e nao no construtor.
    private lateinit var agencyId: UUID

    @BeforeEach
    fun setUp() {
        agencyId = insertAgency()
    }

    @Test
    fun `deve salvar e buscar binding por propertyId`() {
        val propertyId = insertProperty()
        val binding = PropertySensor(propertyId = propertyId, deviceId = "3DE62F3C")
        repository.saveAndFlush(binding)

        val found = repository.findByPropertyId(propertyId)

        assertThat(found).isPresent
        assertThat(found.get().deviceId).isEqualTo("3DE62F3C")
    }

    @Test
    fun `deve buscar binding por deviceId`() {
        val propertyId = insertProperty()
        val binding = PropertySensor(propertyId = propertyId, deviceId = "3DE62F3C")
        repository.saveAndFlush(binding)

        val found = repository.findByDeviceId("3DE62F3C")

        assertThat(found).isPresent
        assertThat(found.get().propertyId).isEqualTo(propertyId)
    }

    @Test
    fun `deve retornar vazio quando binding nao existe`() {
        val found = repository.findByPropertyId(UUID.randomUUID())

        assertThat(found).isEmpty
    }

    @Test
    fun `deve listar todos os bindings ordenados por propertyId`() {
        val propertyId1 = insertProperty()
        val propertyId2 = insertProperty()
        repository.saveAndFlush(PropertySensor(propertyId = propertyId1, deviceId = "AAAA"))
        repository.saveAndFlush(PropertySensor(propertyId = propertyId2, deviceId = "BBBB"))

        val all = repository.findAllByOrderByPropertyIdAsc()

        assertThat(all).hasSize(2)
    }

    @Test
    fun `deve impedir device duplicado`() {
        val propertyId = insertProperty()
        repository.saveAndFlush(PropertySensor(propertyId = propertyId, deviceId = "3DE62F3C"))

        val duplicate = PropertySensor(propertyId = UUID.randomUUID(), deviceId = "3DE62F3C")

        org.junit.jupiter.api.assertThrows<Exception> {
            repository.saveAndFlush(duplicate)
        }
    }

    @Test
    fun `deve impedir property duplicado`() {
        val propertyId = insertProperty()
        repository.saveAndFlush(PropertySensor(propertyId = propertyId, deviceId = "AAAA"))

        val duplicate = PropertySensor(propertyId = propertyId, deviceId = "BBBB")

        org.junit.jupiter.api.assertThrows<Exception> {
            repository.saveAndFlush(duplicate)
        }
    }

    private fun insertProperty(): UUID {
        val id = UUID.randomUUID()
        entityManager
            .createNativeQuery(
                """
                INSERT INTO property (id, real_estate_agency_id, title, status, active)
                VALUES (:id, :agencyId, 'Imovel Teste', 'available', TRUE)
                """.trimIndent(),
            ).setParameter("id", id)
            .setParameter("agencyId", agencyId)
            .executeUpdate()
        entityManager.clear()
        return id
    }
}
