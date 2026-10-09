package com.br.semesperanca.backendleads.domain.service

import com.br.semesperanca.backendleads.application.model.request.CreateActivityRequest
import com.br.semesperanca.backendleads.application.model.request.UpdateActivityRequest
import com.br.semesperanca.backendleads.domain.model.Activity
import com.br.semesperanca.backendleads.domain.repository.ActivityRepository
import com.br.semesperanca.backendleads.domain.repository.LeadRepository
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
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class ActivityServiceTest {
    @Mock private lateinit var activityRepository: ActivityRepository

    @Mock private lateinit var leadRepository: LeadRepository

    @Mock private lateinit var propertyRepository: PropertyRepository

    private lateinit var service: ActivityService

    private val pageable: Pageable = PageRequest.of(0, 20, Sort.by("activityDate"))
    private val leadId: UUID = UUID.fromString("44444444-4444-4444-4444-444444444444")
    private val activityDate: LocalDateTime = LocalDateTime.of(2026, 1, 20, 14, 0)

    @BeforeEach
    fun setUp() {
        service = ActivityService(activityRepository, leadRepository, propertyRepository)
    }

    private fun saveGeneratingId() =
        whenever(activityRepository.save(any<Activity>())).doAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            (invocation.getArgument<Activity>(0)).apply { if (id == null) id = UUID.randomUUID() }
        }

    private fun createRequest(propertyId: UUID? = null) =
        CreateActivityRequest(
            leadId = leadId,
            propertyId = propertyId,
            title = "Visita ao imovel",
            description = "Agendada para sexta as 14h",
            activityDate = activityDate,
        )

    @Nested
    inner class Create {
        @Test
        fun `persiste a atividade com todos os campos do request`() {
            val lead = TestFixtures.lead(id = leadId)
            whenever(leadRepository.findById(leadId)).thenReturn(Optional.of(lead))
            saveGeneratingId()

            val response = service.create(createRequest())

            val captor = argumentCaptor<Activity>()
            verify(activityRepository).save(captor.capture())
            assertThat(captor.firstValue.lead).isSameAs(lead)
            assertThat(captor.firstValue.property).isNull()
            assertThat(captor.firstValue.title).isEqualTo("Visita ao imovel")
            assertThat(captor.firstValue.description).isEqualTo("Agendada para sexta as 14h")
            assertThat(captor.firstValue.activityDate).isEqualTo(activityDate)
            assertThat(response.id).isNotNull()
        }

        @Test
        fun `vincula o imovel quando o request traz propertyId`() {
            val lead = TestFixtures.lead(id = leadId)
            val property = TestFixtures.property()
            whenever(leadRepository.findById(leadId)).thenReturn(Optional.of(lead))
            whenever(propertyRepository.findById(property.id!!)).thenReturn(Optional.of(property))
            saveGeneratingId()

            val response = service.create(createRequest(propertyId = property.id))

            assertThat(response.propertyId).isEqualTo(property.id)
        }

        @Test
        fun `lead inexistente aborta antes de gravar`() {
            whenever(leadRepository.findById(leadId)).thenReturn(Optional.empty())

            assertThatThrownBy { service.create(createRequest()) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Lead não encontrado")

            verify(activityRepository, never()).save(any())
            verify(propertyRepository, never()).findById(any())
        }

        @Test
        fun `imovel inexistente aborta antes de gravar`() {
            val lead = TestFixtures.lead(id = leadId)
            val propertyId = UUID.randomUUID()
            whenever(leadRepository.findById(leadId)).thenReturn(Optional.of(lead))
            whenever(propertyRepository.findById(propertyId)).thenReturn(Optional.empty())

            assertThatThrownBy { service.create(createRequest(propertyId = propertyId)) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Imóvel não encontrado")

            verify(activityRepository, never()).save(any())
        }
    }

    @Nested
    inner class FindById {
        @Test
        fun `mapeia a entidade para o response, com os ids das relacoes`() {
            val lead = TestFixtures.lead(id = leadId)
            val property = TestFixtures.property()
            val activity = TestFixtures.activity(lead = lead, property = property)
            whenever(activityRepository.findById(activity.id!!)).thenReturn(Optional.of(activity))

            val response = service.findById(activity.id!!)

            assertThat(response.id).isEqualTo(activity.id)
            assertThat(response.leadId).isEqualTo(leadId)
            assertThat(response.propertyId).isEqualTo(property.id)
            assertThat(response.title).isEqualTo("Visita ao imovel")
            assertThat(response.description).isEqualTo("Agendada para sexta as 14h")
            assertThat(response.activityDate).isEqualTo(activityDate)
            assertThat(response.createdAt).isEqualTo(activity.createdAt)
        }

        @Test
        fun `atividade sem imovel devolve propertyId nulo`() {
            val lead = TestFixtures.lead(id = leadId)
            val activity = TestFixtures.activity(lead = lead, property = null)
            whenever(activityRepository.findById(activity.id!!)).thenReturn(Optional.of(activity))

            assertThat(service.findById(activity.id!!).propertyId).isNull()
        }

        @Test
        fun `id inexistente vira RuntimeException com mensagem de dominio`() {
            val id = UUID.randomUUID()
            whenever(activityRepository.findById(id)).thenReturn(Optional.empty())

            assertThatThrownBy { service.findById(id) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Atividade não encontrada")
        }
    }

    @Nested
    inner class FindAll {
        private fun traceOf(
            leadId: UUID? = null,
            propertyId: UUID? = null,
            search: String? = null,
        ): SpecProbe.Trace {
            whenever(activityRepository.findAll(any<Specification<Activity>>(), eq(pageable)))
                .thenReturn(PageImpl(emptyList()))

            service.findAll(leadId, propertyId, search, pageable)

            val specCaptor = argumentCaptor<Specification<Activity>>()
            verify(activityRepository, atLeastOnce()).findAll(specCaptor.capture(), eq(pageable))
            return SpecProbe.traceOf(specCaptor.allValues.last())
        }

        @Test
        fun `devolve a pagina ja mapeada para response`() {
            whenever(activityRepository.findAll(any<Specification<Activity>>(), eq(pageable)))
                .thenReturn(PageImpl(listOf(TestFixtures.activity(lead = TestFixtures.lead(id = leadId)))))

            val page = service.findAll(null, null, null, pageable)

            assertThat(page.content).hasSize(1)
            assertThat(page.content.first().leadId).isEqualTo(leadId)
        }

        @Test
        fun `repassa o Pageable recebido sem reordenar`() {
            whenever(activityRepository.findAll(any<Specification<Activity>>(), eq(pageable)))
                .thenReturn(PageImpl(emptyList()))

            service.findAll(null, null, null, pageable)

            verify(activityRepository).findAll(any<Specification<Activity>>(), eq(pageable))
        }

        @Test
        fun `sem nenhum filtro a Specification nao restringe nada`() {
            assertThat(traceOf().andValues.flatten()).isEmpty()
        }

        @Test
        fun `search vazio ou em branco nao vira filtro de like`() {
            assertThat(traceOf(search = "").andValues.flatten()).isEmpty()
            assertThat(traceOf(search = " ").andValues.flatten()).isEmpty()
        }

        @Test
        fun `filtra por lead e por imovel`() {
            val propertyId = UUID.randomUUID()
            val trace = traceOf(leadId = leadId, propertyId = propertyId)

            assertThat(trace.operations)
                .contains("equal($leadId)", "equal($propertyId)")
        }

        @Test
        fun `search vira um like com curinga sobre titulo e descricao`() {
            val trace = traceOf(search = "VISITA")

            assertThat(trace.operations.filter { it.startsWith("like(") })
                .containsExactly("like(%visita%)", "like(%visita%)")
        }

        @Test
        fun `todos os filtros juntos viram tres restricoes`() {
            val trace = traceOf(leadId = leadId, propertyId = UUID.randomUUID(), search = "visita")

            assertThat(trace.andValues.flatten()).hasSize(3)
        }
    }

    @Nested
    inner class Update {
        private val updateRequest =
            UpdateActivityRequest(
                propertyId = null,
                title = "Visitaremark remodelada",
                description = "Confirmada para sabado",
                activityDate = LocalDateTime.of(2026, 2, 1, 10, 0),
            )

        @Test
        fun `sobrescreve titulo, descricao e data`() {
            val lead = TestFixtures.lead(id = leadId)
            val activity = TestFixtures.activity(lead = lead)
            whenever(activityRepository.findById(activity.id!!)).thenReturn(Optional.of(activity))
            saveGeneratingId()

            val response = service.update(activity.id!!, updateRequest)

            assertThat(response.title).isEqualTo("Visitaremark remodelada")
            assertThat(response.description).isEqualTo("Confirmada para sabado")
            assertThat(response.activityDate).isEqualTo(LocalDateTime.of(2026, 2, 1, 10, 0))
        }

        @Test
        fun `update nao mexe no lead da atividade`() {
            val lead = TestFixtures.lead(id = leadId)
            val activity = TestFixtures.activity(lead = lead)
            whenever(activityRepository.findById(activity.id!!)).thenReturn(Optional.of(activity))
            saveGeneratingId()

            val response = service.update(activity.id!!, updateRequest)

            assertThat(response.leadId).isEqualTo(leadId)
        }

        @Test
        fun `propertyId nulo desvincula o imovel`() {
            val lead = TestFixtures.lead(id = leadId)
            val activity = TestFixtures.activity(lead = lead, property = TestFixtures.property())
            whenever(activityRepository.findById(activity.id!!)).thenReturn(Optional.of(activity))
            saveGeneratingId()

            val response = service.update(activity.id!!, updateRequest)

            assertThat(response.propertyId).isNull()
        }

        @Test
        fun `propertyId informado troca o imovel vinculado`() {
            val lead = TestFixtures.lead(id = leadId)
            val novo = TestFixtures.property()
            val activity = TestFixtures.activity(lead = lead)
            whenever(activityRepository.findById(activity.id!!)).thenReturn(Optional.of(activity))
            whenever(propertyRepository.findById(novo.id!!)).thenReturn(Optional.of(novo))
            saveGeneratingId()

            val response = service.update(activity.id!!, updateRequest.copy(propertyId = novo.id))

            assertThat(response.propertyId).isEqualTo(novo.id)
        }

        @Test
        fun `imovel inexistente no update aborta antes de gravar`() {
            val lead = TestFixtures.lead(id = leadId)
            val activity = TestFixtures.activity(lead = lead)
            val propertyId = UUID.randomUUID()
            whenever(activityRepository.findById(activity.id!!)).thenReturn(Optional.of(activity))
            whenever(propertyRepository.findById(propertyId)).thenReturn(Optional.empty())

            assertThatThrownBy { service.update(activity.id!!, updateRequest.copy(propertyId = propertyId)) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Imóvel não encontrado")

            verify(activityRepository, never()).save(any())
        }

        @Test
        fun `atividade inexistente nao salva nada`() {
            val id = UUID.randomUUID()
            whenever(activityRepository.findById(id)).thenReturn(Optional.empty())

            assertThatThrownBy { service.update(id, updateRequest) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Atividade não encontrada")

            verify(activityRepository, never()).save(any())
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `remove a atividade encontrada`() {
            val activity = TestFixtures.activity(lead = TestFixtures.lead(id = leadId))
            whenever(activityRepository.findById(activity.id!!)).thenReturn(Optional.of(activity))

            service.delete(activity.id!!)

            verify(activityRepository).delete(activity)
        }

        @Test
        fun `id inexistente nao deleta nada`() {
            val id = UUID.randomUUID()
            whenever(activityRepository.findById(id)).thenReturn(Optional.empty())

            assertThatThrownBy { service.delete(id) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Atividade não encontrada")

            verify(activityRepository, never()).delete(any<Activity>())
        }
    }
}
