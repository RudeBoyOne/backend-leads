package com.br.semesperanca.backendleads.application.controller

import com.br.semesperanca.backendleads.application.model.LeadReceivedNotificationDTO
import com.br.semesperanca.backendleads.application.model.request.UpdateLeadRequest
import com.br.semesperanca.backendleads.domain.model.Lead
import com.br.semesperanca.backendleads.domain.service.LeadService
import com.br.semesperanca.backendleads.support.AbstractControllerSlice
import com.br.semesperanca.backendleads.support.TestFixtures
import com.br.semesperanca.backendleads.support.jsonEmptyBody
import com.br.semesperanca.backendleads.support.jsonEquals
import com.br.semesperanca.backendleads.support.jsonNotEmpty
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.assertj.MockMvcTester
import java.time.Instant
import java.util.UUID

@WebMvcTest(LeadController::class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class LeadControllerTest : AbstractControllerSlice() {
    @MockitoBean private lateinit var leadService: LeadService

    @Autowired private lateinit var mvc: MockMvcTester

    @Test
    fun `POST receive devolve 200 com corpo vazio e repassa o DTO`() {
        val dto = LeadReceivedNotificationDTO(leadIdDb = "9", originLeadId = "OL-9", leadOrigin = "zap")

        mvc
            .post()
            .uri("/leads/receive")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"leadIdDb": "9", "originLeadId": "OL-9", "leadOrigin": "zap"}""")
            .exchange()
            .assertThat()
            .hasStatus(200)
            .matches(jsonEmptyBody())

        verify(leadService).receiveLead(eq(dto))
    }

    @Test
    fun `POST receive sem originLeadId devolve 400 e nao delega`() {
        mvc
            .post()
            .uri("/leads/receive")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"leadIdDb": "9", "leadOrigin": "zap"}""")
            .exchange()
            .assertThat()
            .hasStatus(400)

        verify(leadService, never()).receiveLead(any())
    }

    @Test
    fun `GET search repassa os 4 filtros e o Pageable resolvido pelo Spring`() {
        val lead =
            Lead(
                id = UUID.fromString("33333333-3333-3333-3333-333333333333"),
                name = "Joana",
                status = "converted",
                timestamp = Instant.parse("2026-01-15T10:00:00Z"),
            )
        whenever(leadService.findLeads(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), any()))
            .thenReturn(PageImpl(listOf(lead)))

        mvc
            .get()
            .uri("/leads/search?status=converted&corretorId=42&origin=zap&search=joana&page=1&size=30&sort=name,asc")
            .exchange()
            .assertThat()
            .hasStatus(200)
            .matches(jsonEquals("$.content[0].id", lead.id.toString()))
            .matches(jsonEquals("$.totalElements", 1))

        verify(leadService).findLeads(
            eq("converted"),
            eq(42L),
            eq("zap"),
            eq("joana"),
            eq(PageRequest.of(1, 30, Sort.by(Sort.Direction.ASC, "name"))),
        )
    }

    @Test
    fun `GET search sem filtros usa Pageable sem sort e pagina 0 de 20`() {
        whenever(leadService.findLeads(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), any()))
            .thenReturn(PageImpl(emptyList()))

        mvc
            .get()
            .uri("/leads/search")
            .exchange()
            .assertThat()
            .hasStatus(200)

        verify(leadService).findLeads(
            eq(null),
            eq(null),
            eq(null),
            eq(null),
            eq(PageRequest.of(0, 20)),
        )
    }

    @Test
    fun `GET search com corretorId nao numerico devolve 400 antes de chegar no servico`() {
        mvc
            .get()
            .uri("/leads/search?corretorId=abc")
            .exchange()
            .assertThat()
            .hasStatus(400)

        verify(leadService, never()).findLeads(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), any())
    }

    @Test
    fun `GET search por id devolve o LeadDTO`() {
        val id = UUID.randomUUID()
        whenever(leadService.getById(id)).thenReturn(TestFixtures.leadDTO(originLeadId = "OL-77"))

        mvc
            .get()
            .uri("/leads/search/$id")
            .exchange()
            .assertThat()
            .hasStatus(200)
            .matches(jsonEquals("$.originLeadId", "OL-77"))
            .matches(jsonEquals("$.phone", "11988887777"))
    }

    @Test
    fun `GET search por id inexistente transforma o erro do dominio em 400`() {
        val id = UUID.randomUUID()
        whenever(leadService.getById(id)).thenThrow(RuntimeException("Lead não encontrado"))

        mvc
            .get()
            .uri("/leads/search/$id")
            .exchange()
            .assertThat()
            .hasStatus(400)
            .matches(jsonEquals("$.message", "Lead não encontrado"))
    }

    @Test
    fun `PUT atualiza e devolve o LeadDTO do servico`() {
        val id = UUID.randomUUID()
        whenever(leadService.update(eq(id), any())).thenReturn(TestFixtures.leadDTO(status = "converted"))

        mvc
            .put()
            .uri("/leads/$id")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"name": "Joana Ribeiro", "status": "converted"}""")
            .exchange()
            .assertThat()
            .hasStatus(200)
            .matches(jsonEquals("$.status", "converted"))
    }

    @Test
    fun `PUT com status fora do enum devolve 400 e nao delega`() {
        mvc
            .put()
            .uri("/leads/${UUID.randomUUID()}")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"status": "nao-existe"}""")
            .exchange()
            .assertThat()
            .hasStatus(400)
            .matches(jsonNotEmpty("$.fields.status"))

        verify(leadService, never()).update(any(), any())
    }

    @Test
    fun `PUT com telefone curto demais devolve 400`() {
        mvc
            .put()
            .uri("/leads/${UUID.randomUUID()}")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"phoneNumber": "1234"}""")
            .exchange()
            .assertThat()
            .hasStatus(400)
            .matches(jsonNotEmpty("$.fields.phoneNumber"))
    }

    @Test
    fun `PUT com email invalido devolve 400`() {
        mvc
            .put()
            .uri("/leads/${UUID.randomUUID()}")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"email": "sem-arroba"}""")
            .exchange()
            .assertThat()
            .hasStatus(400)
            .matches(jsonNotEmpty("$.fields.email"))
    }

    @Test
    fun `DELETE devolve 204 sem corpo e delega ao servico`() {
        val id = UUID.randomUUID()

        mvc
            .delete()
            .uri("/leads/$id")
            .exchange()
            .assertThat()
            .hasStatus(204)
            .matches(jsonEmptyBody())

        verify(leadService).delete(id)
    }
}
