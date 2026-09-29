package com.br.semesperanca.backendleads.domain.service

import com.br.semesperanca.backendleads.application.model.request.CreatePropertyRequest
import com.br.semesperanca.backendleads.application.model.request.UpdatePropertyRequest
import com.br.semesperanca.backendleads.domain.model.Property
import com.br.semesperanca.backendleads.domain.repository.PropertyRepository
import com.br.semesperanca.backendleads.support.SpecProbe
import com.br.semesperanca.backendleads.support.TestFixtures
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import java.math.BigDecimal
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class PropertyServiceTest {
    @Mock private lateinit var repository: PropertyRepository

    private lateinit var service: PropertyService

    private val pageable: Pageable = PageRequest.of(0, 20, Sort.by("title"))

    /**
     * No banco o id de `Property` e gerado por `@GeneratedValue(UUID)` no
     * INSERT, entao chega preenchido no `toResponse`. O mock precisa fazer o
     * mesmo, senao `property.id!!` estoura NPE num cenario que nunca acontece
     * em producao.
     */
    private fun saveGeneratingId() =
        whenever(repository.save(any<Property>())).doAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            (invocation.getArgument<Property>(0)).apply { if (id == null) id = UUID.randomUUID() }
        }

    @BeforeEach
    fun setUp() {
        service = PropertyService(repository)
    }

    private fun createRequest(status: String? = null) =
        CreatePropertyRequest(
            realEstateAgencyId = TestFixtures.AGENCY_ID,
            title = "Apartamento no centro",
            description = "Sala, cozinha e dois quartos",
            price = BigDecimal("350000.00"),
            propertyType = "apartamento",
            bedrooms = 2,
            bathrooms = 1,
            areaSqm = BigDecimal("65.00"),
            address = "Rua das Flores, 100",
            city = "Sao Paulo",
            state = "SP",
            zipCode = "01001000",
            status = status,
        )

    @Nested
    inner class Create {
        @Test
        fun `persiste o imovel com todos os campos do request`() {
            saveGeneratingId()

            val response = service.create(createRequest())

            val captor = argumentCaptor<Property>()
            verify(repository).save(captor.capture())
            val saved = captor.firstValue
            assertThat(saved.realEstateAgencyId).isEqualTo(TestFixtures.AGENCY_ID)
            assertThat(saved.title).isEqualTo("Apartamento no centro")
            assertThat(saved.description).isEqualTo("Sala, cozinha e dois quartos")
            assertThat(saved.price).isEqualByComparingTo("350000.00")
            assertThat(saved.propertyType).isEqualTo("apartamento")
            assertThat(saved.bedrooms).isEqualTo(2)
            assertThat(saved.bathrooms).isEqualTo(1)
            assertThat(saved.areaSqm).isEqualByComparingTo("65.00")
            assertThat(saved.address).isEqualTo("Rua das Flores, 100")
            assertThat(saved.city).isEqualTo("Sao Paulo")
            assertThat(saved.state).isEqualTo("SP")
            assertThat(saved.zipCode).isEqualTo("01001000")
            assertThat(response.id).isNotNull()
        }

        @Test
        fun `imovel nasce ativo, independente do request`() {
            saveGeneratingId()

            val response = service.create(createRequest())

            assertThat(response.active).isTrue()
        }

        @Test
        fun `status ausente assume available`() {
            saveGeneratingId()

            val response = service.create(createRequest(status = null))

            assertThat(response.status).isEqualTo("available")
        }

        @Test
        fun `status informado e respeitado`() {
            saveGeneratingId()

            val response = service.create(createRequest(status = "reserved"))

            assertThat(response.status).isEqualTo("reserved")
        }
    }

    @Nested
    inner class FindById {
        @Test
        fun `mapeia a entidade para o response`() {
            val property = TestFixtures.property()
            whenever(repository.findById(property.id!!)).thenReturn(Optional.of(property))

            val response = service.findById(property.id!!)

            assertThat(response.id).isEqualTo(property.id)
            assertThat(response.realEstateAgencyId).isEqualTo(TestFixtures.AGENCY_ID)
            assertThat(response.title).isEqualTo("Apartamento no centro")
            assertThat(response.price).isEqualByComparingTo("350000.00")
            assertThat(response.bedrooms).isEqualTo(2)
            assertThat(response.status).isEqualTo("available")
            assertThat(response.active).isTrue()
            assertThat(response.createdAt).isEqualTo(property.createdAt)
            assertThat(response.updatedAt).isEqualTo(property.updatedAt)
        }

        @Test
        fun `id inexistente vira RuntimeException com mensagem de dominio`() {
            val id = UUID.randomUUID()
            whenever(repository.findById(id)).thenReturn(Optional.empty())

            assertThatThrownBy { service.findById(id) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Imóvel não encontrado")
        }
    }

    @Nested
    inner class FindAll {
        /** Executa findAll e devolve o rastro de filtros montado pelo servico. */
        private fun traceOf(
            realEstateAgencyId: UUID? = null,
            active: Boolean? = null,
            status: String? = null,
            propertyType: String? = null,
            search: String? = null,
            minPrice: BigDecimal? = null,
            maxPrice: BigDecimal? = null,
        ): SpecProbe.Trace {
            whenever(repository.findAll(any<Specification<Property>>(), eq(pageable)))
                .thenReturn(PageImpl(emptyList()))

            service.findAll(realEstateAgencyId, active, status, propertyType, search, minPrice, maxPrice, pageable)

            val specCaptor = argumentCaptor<Specification<Property>>()
            verify(repository, org.mockito.kotlin.atLeastOnce()).findAll(specCaptor.capture(), eq(pageable))
            return SpecProbe.traceOf(specCaptor.allValues.last())
        }

        @Test
        fun `devolve a pagina ja mapeada para response`() {
            val property = TestFixtures.property()
            whenever(repository.findAll(any<Specification<Property>>(), eq(pageable)))
                .thenReturn(PageImpl(listOf(property)))

            val page = service.findAll(null, null, null, null, null, null, null, pageable)

            assertThat(page.content).hasSize(1)
            assertThat(page.content.first().title).isEqualTo("Apartamento no centro")
        }

        @Test
        fun `repassa o Pageable recebido sem reordenar`() {
            whenever(repository.findAll(any<Specification<Property>>(), eq(pageable))).thenReturn(PageImpl(emptyList()))

            service.findAll(null, null, null, null, null, null, null, pageable)

            verify(repository).findAll(any<Specification<Property>>(), eq(pageable))
        }

        @Test
        fun `sem nenhum filtro a Specification nao restringe nada`() {
            assertThat(traceOf().andValues.flatten()).isEmpty()
        }

        @Test
        fun `filtros nulos sao ignorados um a um`() {
            assertThat(traceOf(realEstateAgencyId = null, active = null, status = null).andValues.flatten()).isEmpty()
        }

        @Test
        fun `search vazio ou em branco nao vira filtro de like`() {
            assertThat(traceOf(search = "").andValues.flatten()).isEmpty()
            assertThat(traceOf(search = "   ").andValues.flatten()).isEmpty()
        }

        @Test
        fun `filtra por agencia`() {
            assertThat(traceOf(realEstateAgencyId = TestFixtures.AGENCY_ID).operations)
                .contains("equal(${TestFixtures.AGENCY_ID})")
        }

        @Test
        fun `filtra por active, incluindo o false explicito`() {
            assertThat(traceOf(active = true).operations).contains("equal(true)")
            assertThat(traceOf(active = false).operations).contains("equal(false)")
        }

        @Test
        fun `filtra por status e por tipo, que sao comparacoes exatas`() {
            val trace = traceOf(status = "sold", propertyType = "apartamento")

            assertThat(trace.operations).contains("equal(sold)", "equal(apartamento)")
        }

        @Test
        fun `filtra por faixa de preco com limites inclusivos`() {
            val trace = traceOf(minPrice = BigDecimal("100000.00"), maxPrice = BigDecimal("500000.00"))

            assertThat(trace.operations).contains("greaterThanOrEqualTo(100000.00)", "lessThanOrEqualTo(500000.00)")
        }

        @Test
        fun `minPrice e maxPrice sao filtros independentes`() {
            assertThat(traceOf(minPrice = BigDecimal("1.00")).operations)
                .contains("greaterThanOrEqualTo(1.00)")
                .doesNotContain("lessThanOrEqualTo(1.00)")
            assertThat(traceOf(maxPrice = BigDecimal("9.00")).operations)
                .contains("lessThanOrEqualTo(9.00)")
                .doesNotContain("greaterThanOrEqualTo(9.00)")
        }

        @Test
        fun `search vira um like com curinga aplicado a titulo, endereco e cidade`() {
            val trace = traceOf(search = "JARDIM")

            // o servico faz lowercase() no padrao e envolve em "%", em 3 colunas
            assertThat(trace.operations.filter { it.startsWith("like(") })
                .containsExactly("like(%jardim%)", "like(%jardim%)", "like(%jardim%)")
        }

        @Test
        fun `todos os filtros podem ser enviados juntos, virando sete restricoes`() {
            val trace =
                traceOf(
                    realEstateAgencyId = TestFixtures.AGENCY_ID,
                    active = false,
                    status = "sold",
                    propertyType = "casa",
                    search = "jardim",
                    minPrice = BigDecimal("1.00"),
                    maxPrice = BigDecimal("9.00"),
                )

            assertThat(trace.andValues.flatten()).hasSize(7)
        }
    }

    @Nested
    inner class Update {
        private val updateRequest =
            UpdatePropertyRequest(
                title = "Cobertura duplex",
                description = "Vista para o parque",
                price = BigDecimal("1250000.00"),
                propertyType = "cobertura",
                bedrooms = 4,
                bathrooms = 3,
                areaSqm = BigDecimal("220.00"),
                address = "Av. Paulista, 1000",
                city = "Sao Paulo",
                state = "SP",
                zipCode = "01310-100",
                status = "sold",
            )

        @Test
        fun `sobrescreve todos os campos editaveis`() {
            val property = TestFixtures.property()
            whenever(repository.findById(property.id!!)).thenReturn(Optional.of(property))
            saveGeneratingId()

            val response = service.update(property.id!!, updateRequest)

            assertThat(response.title).isEqualTo("Cobertura duplex")
            assertThat(response.description).isEqualTo("Vista para o parque")
            assertThat(response.price).isEqualByComparingTo("1250000.00")
            assertThat(response.propertyType).isEqualTo("cobertura")
            assertThat(response.bedrooms).isEqualTo(4)
            assertThat(response.bathrooms).isEqualTo(3)
            assertThat(response.areaSqm).isEqualByComparingTo("220.00")
            assertThat(response.address).isEqualTo("Av. Paulista, 1000")
            assertThat(response.zipCode).isEqualTo("01310-100")
            assertThat(response.status).isEqualTo("sold")
        }

        @Test
        fun `update nao mexe em active nem em realEstateAgencyId`() {
            val property = TestFixtures.property(active = false)
            whenever(repository.findById(property.id!!)).thenReturn(Optional.of(property))
            saveGeneratingId()

            val response = service.update(property.id!!, updateRequest)

            assertThat(response.active).isFalse()
            assertThat(response.realEstateAgencyId).isEqualTo(TestFixtures.AGENCY_ID)
        }

        @Test
        fun `status nulo no update volta para available`() {
            val property = TestFixtures.property()
            whenever(repository.findById(property.id!!)).thenReturn(Optional.of(property))
            saveGeneratingId()

            val response = service.update(property.id!!, updateRequest.copy(status = null))

            assertThat(response.status).isEqualTo("available")
        }

        @Test
        fun `id inexistente nao salva nada`() {
            val id = UUID.randomUUID()
            whenever(repository.findById(id)).thenReturn(Optional.empty())

            assertThatThrownBy { service.update(id, updateRequest) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Imóvel não encontrado")

            verify(repository, never()).save(any())
        }
    }

    @Nested
    inner class ToggleActive {
        @Test
        fun `desativa um imovel ativo`() {
            val property = TestFixtures.property(active = true)
            whenever(repository.findById(property.id!!)).thenReturn(Optional.of(property))
            saveGeneratingId()

            val response = service.toggleActive(property.id!!)

            assertThat(response.active).isFalse()
        }

        @Test
        fun `reativa um imovel inativo, invertendo o estado atual`() {
            val property = TestFixtures.property(active = false)
            whenever(repository.findById(property.id!!)).thenReturn(Optional.of(property))
            saveGeneratingId()

            val response = service.toggleActive(property.id!!)

            assertThat(response.active).isTrue()
        }

        @Test
        fun `toggle duas vezes devolve o imovel ao estado inicial`() {
            val property = TestFixtures.property(active = true)
            whenever(repository.findById(property.id!!)).thenReturn(Optional.of(property))
            saveGeneratingId()

            service.toggleActive(property.id!!)
            val response = service.toggleActive(property.id!!)

            assertThat(response.active).isTrue()
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `remove o imovel encontrado`() {
            val property = TestFixtures.property()
            whenever(repository.findById(property.id!!)).thenReturn(Optional.of(property))

            service.delete(property.id!!)

            verify(repository).delete(property)
        }

        @Test
        fun `id inexistente nao deleta nada`() {
            val id = UUID.randomUUID()
            whenever(repository.findById(id)).thenReturn(Optional.empty())

            assertThatThrownBy { service.delete(id) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Imóvel não encontrado")

            verify(repository, never()).delete(any<Property>())
        }
    }
}
