package com.br.semesperanca.backendleads.domain.service

import com.br.semesperanca.backendleads.application.model.LeadDTO
import com.br.semesperanca.backendleads.application.model.LeadReceivedNotificationDTO
import com.br.semesperanca.backendleads.application.model.request.UpdateLeadRequest
import com.br.semesperanca.backendleads.configuration.MailProperties
import com.br.semesperanca.backendleads.configuration.exception.NotFoundException
import com.br.semesperanca.backendleads.domain.model.Lead
import com.br.semesperanca.backendleads.domain.repository.LeadRepository
import com.br.semesperanca.backendleads.domain.repository.user.RealEstateAgentRepository
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
import org.mockito.Mockito.lenient
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
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
import java.time.Instant
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class LeadServiceTest {
    @Mock private lateinit var leadRepository: LeadRepository

    @Mock private lateinit var emailService: EmailService

    @Mock private lateinit var insightClient: InsightClient

    @Mock private lateinit var realEstateAgentRepository: RealEstateAgentRepository

    private val mailProperties = MailProperties()
    private lateinit var service: LeadService

    private val pageable: Pageable = PageRequest.of(0, 20, Sort.by("createdAt"))
    private val leadId: UUID = UUID.fromString("55555555-5555-5555-5555-555555555555")

    @BeforeEach
    fun setUp() {
        service = LeadService(leadRepository, emailService, mailProperties, insightClient, realEstateAgentRepository)
    }

    @Nested
    inner class ReceiveLead {
        private val notification = LeadReceivedNotificationDTO("99999999-9999-9999-9999-999999999999", "origin-1", "Zapimoveis")

        private val lead = TestFixtures.lead(originLeadId = "origin-1")

        /**
         * `lenient` porque o caso do lead fora do escopo substitui o stub por
         * `Optional.empty()`; sem isso o `MockitoExtension` acusaria o stub do
         * `@BeforeEach` como não usado naquele teste.
         */
        @BeforeEach
        fun stubLeadNoEscopo() {
            lenient()
                .`when`(leadRepository.findOne(any<Specification<Lead>>()))
                .thenReturn(Optional.of(lead))
        }

        @Test
        fun `notifica o e-mail e despacha o lead carregado pelo repositorio escopado`() {
            mailProperties.notifyTo = "corretores@imobiliaria.com.br"

            service.receiveLead(notification)

            verify(emailService).sendLeadReceived(notification, "corretores@imobiliaria.com.br")
            verify(insightClient).createInsight(lead)
        }

        @Test
        fun `notifyTo ausente vira destinatario vazio, sem NPE`() {
            mailProperties.notifyTo = null

            service.receiveLead(notification)

            verify(emailService).sendLeadReceived(notification, "")
        }

        /**
         * O `originLeadId` vem do corpo do webhook, então qualquer usuário
         * autenticado pode nomear o lead de outra agência. A resolução é por
         * `findOne(spec)` — a sobrecarga que recebe `Specification` e por isso
         * recebe também a subárvore da role — e `findOne` vazio é o mesmo
         * "não existe" e "não é seu". Nenhum dado alheio sai daqui.
         *
         * OInsightClient é `@Async` e a thread dele não tem `SecurityContext`;
         * resolver dentro dele seria uma consulta sem principal, e método
         * derivado não passa pela base escopada.
         */
        @Test
        fun `lead fora do escopo aborta com NotFoundException e nada e enviado`() {
            whenever(leadRepository.findOne(any<Specification<Lead>>())).thenReturn(Optional.empty())

            assertThatThrownBy { service.receiveLead(notification) }
                .isInstanceOf(NotFoundException::class.java)
                .hasMessage("Lead não encontrado")

            verify(insightClient, never()).createInsight(any())
        }

        /**
         * A busca é uma `Specification` sobre `originLeadId`, e não um método
         * derivado: método derivado não passa pela [ScopedRepositoryImpl] e a
         * chamada sairia sem escopo.
         */
        @Test
        fun `a busca do lead e uma Specification, e nao um metodo derivado`() {
            service.receiveLead(notification)

            val captor = argumentCaptor<Specification<Lead>>()
            verify(leadRepository).findOne(captor.capture())
            assertThat(captor.firstValue).isNotNull()
        }
    }

    @Nested
    inner class FindLeads {
        private fun traceOf(
            status: String? = null,
            corretorId: Long? = null,
            origin: String? = null,
            search: String? = null,
        ): SpecProbe.Trace {
            whenever(leadRepository.findAll(any<Specification<Lead>>(), eq(pageable))).thenReturn(PageImpl(emptyList()))

            service.findLeads(status, corretorId, origin, search, pageable)

            val specCaptor = argumentCaptor<Specification<Lead>>()
            verify(leadRepository, atLeastOnce()).findAll(specCaptor.capture(), eq(pageable))
            return SpecProbe.traceOf(specCaptor.allValues.last())
        }

        @Test
        fun `repassa o Pageable recebido sem reordenar`() {
            whenever(leadRepository.findAll(any<Specification<Lead>>(), eq(pageable))).thenReturn(PageImpl(emptyList()))

            service.findLeads(null, null, null, null, pageable)

            verify(leadRepository).findAll(any<Specification<Lead>>(), eq(pageable))
        }

        @Test
        fun `devolve as entidades Lead cruas, sem passar pelo DTO`() {
            val lead = TestFixtures.lead(id = leadId)
            whenever(leadRepository.findAll(any<Specification<Lead>>(), eq(pageable)))
                .thenReturn(PageImpl(listOf(lead)))

            val page = service.findLeads(null, null, null, null, pageable)

            assertThat(page.content).containsExactly(lead)
        }

        @Test
        fun `sem nenhum filtro a Specification nao restringe nada`() {
            assertThat(traceOf().andValues.flatten()).isEmpty()
        }

        @Test
        fun `filtra por status e por origem`() {
            val trace = traceOf(status = "converted", origin = "Zapimoveis")

            assertThat(trace.operations).contains("equal(converted)", "equal(Zapimoveis)")
        }

        @Test
        fun `corretorId vira comparacao textual contra o clientListingId`() {
            val trace = traceOf(corretorId = 4711L)

            assertThat(trace.operations).contains("equal(4711)")
        }

        @Test
        fun `search vira um like com curinga sobre nome e e-mail`() {
            val trace = traceOf(search = "MARIA")

            assertThat(trace.operations.filter { it.startsWith("like(") })
                .containsExactly("like(%maria%)", "like(%maria%)")
        }

        @Test
        fun `search vazio ainda vira filtro, diferente dos demais servicos`() {
            val trace = traceOf(search = "")

            assertThat(trace.operations.filter { it.startsWith("like(") })
                .containsExactly("like(%%)", "like(%%)")
        }

        /**
         * O que importa aqui é a quantidade de filtros de folha, não o
         * aninhamento: a Specification começa em `Specification.unrestricted()`
         * e cada filtro entra por `spec.and { }`, então a quantidade de `cb.and`
         * depende de como a composição é aninhada — detalhe de implementação do
         * Spring Data, não do serviço.
         */
        @Test
        fun `todos os filtros juntos viram quatro filtros de folha`() {
            val trace = traceOf(status = "new", corretorId = 1L, origin = "Zapimoveis", search = "maria")

            assertThat(trace.operations)
                .containsSubsequence("equal(new)", "equal(1)", "equal(Zapimoveis)")
                .containsOnlyOnce("equal(new)")
            assertThat(trace.operations.filter { it.startsWith("like(") })
                .containsExactly("like(%maria%)", "like(%maria%)")
            assertThat(trace.operations.count { it.startsWith("equal(") }).isEqualTo(3)
        }
    }

    @Nested
    inner class GetById {
        @Test
        fun `converte a entidade no DTO de saida`() {
            val lead = TestFixtures.lead(id = leadId)
            whenever(leadRepository.findById(leadId)).thenReturn(Optional.of(lead))

            val dto = service.getById(leadId)

            assertThat(dto.leadOrigin).isEqualTo("Zapimoveis")
            assertThat(dto.originLeadId).isEqualTo("origin-1")
            assertThat(dto.originListingId).isEqualTo("OL-1")
            assertThat(dto.clientListingId).isEqualTo("LST-1")
            assertThat(dto.name).isEqualTo("Maria Silva")
            assertThat(dto.email).isEqualTo("maria@example.com")
            assertThat(dto.phone).isEqualTo("11987654321")
            assertThat(dto.message).isEqualTo("Quero comprar um apartamento")
            assertThat(dto.temperature).isEqualTo("hot")
            assertThat(dto.transactionType).isEqualTo("venda")
            assertThat(dto.status).isEqualTo("new")
            assertThat(dto.realEstateAgencyId).isEqualTo(TestFixtures.AGENCY_ID)
        }

        /**
         * 404 e não 400: `findById` é escopado, e um lead de outra agência sai
         * daqui como `Optional.empty()`. Um id inexistente e um id fora do escopo
         * precisam responder igual — 403 confirmaria a existência do registro.
         */
        @Test
        fun `id fora do escopo vira NotFoundException`() {
            whenever(leadRepository.findById(leadId)).thenReturn(Optional.empty())

            assertThatThrownBy { service.getById(leadId) }
                .isInstanceOf(NotFoundException::class.java)
                .hasMessage("Lead não encontrado")
        }
    }

    @Nested
    inner class Update {
        @Test
        fun `aplica todos os campos enviados`() {
            val lead = TestFixtures.lead(id = leadId)
            val agentId = UUID.randomUUID()
            val funnelId = UUID.randomUUID()
            whenever(leadRepository.findById(leadId)).thenReturn(Optional.of(lead))
            whenever(leadRepository.save(any<Lead>())).doAnswer { invocation ->
                @Suppress("UNCHECKED_CAST")
                (invocation.getArgument<Lead>(0))
            }
            whenever(realEstateAgentRepository.findById(agentId))
                .thenReturn(Optional.of(TestFixtures.realEstateAgent().apply { id = agentId }))

            val dto =
                service.update(
                    leadId,
                    UpdateLeadRequest(
                        name = "Maria Souza",
                        email = "maria.souza@example.com",
                        phoneNumber = "11998887777",
                        message = "Quero alugar",
                        temperature = "warm",
                        transactionType = "locacao",
                        source = "site",
                        status = "in_progress",
                        realEstateAgentId = agentId,
                        funnelStageId = funnelId,
                    ),
                )

            assertThat(dto.name).isEqualTo("Maria Souza")
            assertThat(dto.email).isEqualTo("maria.souza@example.com")
            assertThat(dto.phone).isEqualTo("11998887777")
            assertThat(dto.message).isEqualTo("Quero alugar")
            assertThat(dto.temperature).isEqualTo("warm")
            assertThat(dto.transactionType).isEqualTo("locacao")
            assertThat(dto.status).isEqualTo("in_progress")
            assertThat(dto.source).isEqualTo("site")
            assertThat(dto.realEstateAgentId).isEqualTo(agentId)
            assertThat(dto.funnelStageId).isEqualTo(funnelId)
        }

        @Test
        fun `campos ausentes mantem o valor atual, e o lead e persistido uma unica vez`() {
            val lead = TestFixtures.lead(id = leadId)
            whenever(leadRepository.findById(leadId)).thenReturn(Optional.of(lead))
            whenever(leadRepository.save(any<Lead>())).doAnswer { invocation ->
                @Suppress("UNCHECKED_CAST")
                (invocation.getArgument<Lead>(0))
            }

            val dto = service.update(leadId, UpdateLeadRequest(name = "So o nome", null, null, null, null, null, null, null, null, null))

            assertThat(dto.name).isEqualTo("So o nome")
            assertThat(dto.email).isEqualTo("maria@example.com")
            assertThat(dto.phone).isEqualTo("11987654321")
            assertThat(dto.message).isEqualTo("Quero comprar um apartamento")
            assertThat(dto.temperature).isEqualTo("hot")
            assertThat(dto.transactionType).isEqualTo("venda")
            assertThat(dto.status).isEqualTo("new")
            verify(leadRepository).save(any())
        }

        @Test
        fun `atualiza o updatedAt, que a trigger do banco nao cobre para lead`() {
            val lead = TestFixtures.lead(id = leadId, createdAt = Instant.parse("2020-01-01T00:00:00Z"))
            lead.updatedAt = null
            whenever(leadRepository.findById(leadId)).thenReturn(Optional.of(lead))
            whenever(leadRepository.save(any<Lead>())).doAnswer { invocation ->
                @Suppress("UNCHECKED_CAST")
                (invocation.getArgument<Lead>(0))
            }

            val dto = service.update(leadId, UpdateLeadRequest("x", null, null, null, null, null, null, null, null, null))

            assertThat(dto.updatedAt).isNotNull()
            assertThat(dto.createdAt).isEqualTo("2020-01-01T00:00:00Z")
        }

        @Test
        fun `id fora do escopo nao salva nada`() {
            whenever(leadRepository.findById(leadId)).thenReturn(Optional.empty())

            assertThatThrownBy {
                service.update(leadId, UpdateLeadRequest("x", null, null, null, null, null, null, null, null, null))
            }.isInstanceOf(NotFoundException::class.java)
                .hasMessage("Lead não encontrado")

            verify(leadRepository, never()).save(any())
        }

        /**
         * O corretor do lead passa pelo `RealEstateAgentRepository` escopado: um
         * corretor de outra agência não é encontrado, e `orElse(null)` deixa o
         * lead sem dono em vez de_null no banco. É o que impede desviar um lead
         * para o corretor de outra agência.
         */
        @Test
        fun `corretor fora do escopo deixa o lead sem dono, em vez de null no banco`() {
            val lead = TestFixtures.lead(id = leadId)
            val agentId = UUID.randomUUID()
            whenever(leadRepository.findById(leadId)).thenReturn(Optional.of(lead))
            whenever(leadRepository.save(any<Lead>())).doAnswer { invocation ->
                @Suppress("UNCHECKED_CAST")
                (invocation.getArgument<Lead>(0))
            }
            whenever(realEstateAgentRepository.findById(agentId)).thenReturn(Optional.empty())

            val dto =
                service.update(
                    leadId,
                    UpdateLeadRequest("x", null, null, null, null, null, null, null, agentId, null),
                )

            assertThat(dto.realEstateAgentId).isNull()
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `remove o lead encontrado`() {
            val lead = TestFixtures.lead(id = leadId)
            whenever(leadRepository.findById(leadId)).thenReturn(Optional.of(lead))

            service.delete(leadId)

            verify(leadRepository).delete(lead)
        }

        @Test
        fun `id inexistente nao deleta nada`() {
            whenever(leadRepository.findById(leadId)).thenReturn(Optional.empty())

            assertThatThrownBy { service.delete(leadId) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Lead não encontrado")

            verify(leadRepository, never()).delete(any<Lead>())
        }
    }
}
