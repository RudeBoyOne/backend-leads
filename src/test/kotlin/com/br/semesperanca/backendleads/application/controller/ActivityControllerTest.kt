package com.br.semesperanca.backendleads.application.controller

import com.br.semesperanca.backendleads.domain.service.ActivityService
import com.br.semesperanca.backendleads.support.AbstractControllerSlice
import com.br.semesperanca.backendleads.support.TestFixtures
import com.br.semesperanca.backendleads.support.jsonEmptyBody
import com.br.semesperanca.backendleads.support.jsonEquals
import com.br.semesperanca.backendleads.support.jsonNotEmpty
import org.hamcrest.Matchers.containsString
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
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.assertj.MockMvcTester
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import java.util.UUID

private val LEAD_ID: UUID = UUID.fromString("22222222-2222-2222-2222-222222222222")

private val validActivity: String =
    """
    {
      "leadId": "$LEAD_ID",
      "propertyId": null,
      "title": "Visita ao imovel",
      "description": "Agendada para sexta as 14h",
      "activityDate": "2026-01-20T14:00:00"
    }
    """.trimIndent()

@WebMvcTest(ActivityController::class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ActivityControllerTest : AbstractControllerSlice() {
    @MockitoBean private lateinit var service: ActivityService

    @Autowired private lateinit var mvc: MockMvcTester

    @Test
    fun `POST cria a atividade e devolve 201 com o JSON do servico`() {
        val created = TestFixtures.activityResponse(leadId = LEAD_ID)
        whenever(service.create(any())).thenReturn(created)

        mvc
            .post()
            .uri("/activities")
            .contentType(MediaType.APPLICATION_JSON)
            .content(validActivity)
            .exchange()
            .assertThat()
            .hasStatus(201)
            .matches(jsonEquals("$.id", created.id.toString()))
            .matches(jsonEquals("$.leadId", LEAD_ID.toString()))
            .matches(jsonEquals("$.title", "Visita ao imovel"))
    }

    @Test
    fun `POST com titulo em branco devolve 400 sem chamar o servico`() {
        mvc
            .post()
            .uri("/activities")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"leadId": "$LEAD_ID", "title": "  ", "description": "x", "activityDate": "2026-01-20T14:00:00"}""")
            .exchange()
            .assertThat()
            .hasStatus(400)
            .matches(jsonNotEmpty("$.fields.title"))

        verify(service, never()).create(any())
    }

    @Test
    fun `POST com descricao em branco devolve 400`() {
        mvc
            .post()
            .uri("/activities")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"leadId": "$LEAD_ID", "title": "Visita", "description": "", "activityDate": "2026-01-20T14:00:00"}""")
            .exchange()
            .assertThat()
            .hasStatus(400)
            .matches(jsonNotEmpty("$.fields.description"))
    }

    /**
     * `leadId` e `activityDate` sao nao-nulos no construtor, entao a falha e de
     * binding, antes do `@Valid` — e o handler de `RuntimeException` devolve 400.
     */
    @Test
    fun `POST sem leadId devolve 400 com o erro de deserializacao`() {
        mvc
            .post()
            .uri("/activities")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"title": "Visita", "description": "x", "activityDate": "2026-01-20T14:00:00"}""")
            .exchange()
            .assertThat()
            .hasStatus(400)
            .matches(content().string(containsString("leadId")))
    }

    @Test
    fun `GET lista com os filtros da query`() {
        val item = TestFixtures.activityResponse()
        whenever(service.findAll(anyOrNull(), anyOrNull(), anyOrNull(), any()))
            .thenReturn(PageImpl(listOf(item)))

        mvc
            .get()
            .uri("/activities?leadId=$LEAD_ID&search=visita&page=1&size=5")
            .exchange()
            .assertThat()
            .hasStatus(200)
            .matches(jsonEquals("$.content[0].id", item.id.toString()))
            .matches(jsonEquals("$.totalElements", 1))

        verify(service).findAll(
            eq(LEAD_ID),
            eq(null),
            eq("visita"),
            eq(PageRequest.of(1, 5)),
        )
    }

    @Test
    fun `GET sem filtros usa page 0 e size 20`() {
        whenever(service.findAll(anyOrNull(), anyOrNull(), anyOrNull(), any()))
            .thenReturn(PageImpl(emptyList()))

        mvc
            .get()
            .uri("/activities")
            .exchange()
            .assertThat()
            .hasStatus(200)

        verify(service).findAll(eq(null), eq(null), eq(null), eq(PageRequest.of(0, 20)))
    }

    @Test
    fun `GET por id devolve a atividade`() {
        val id = UUID.randomUUID()
        whenever(service.findById(id)).thenReturn(TestFixtures.activityResponse(id = id, title = "Assinatura"))

        mvc
            .get()
            .uri("/activities/$id")
            .exchange()
            .assertThat()
            .hasStatus(200)
            .matches(jsonEquals("$.title", "Assinatura"))
    }

    @Test
    fun `GET por id invalido devolve 400 antes de chegar no servico`() {
        mvc
            .get()
            .uri("/activities/xyz")
            .exchange()
            .assertThat()
            .hasStatus(400)

        verify(service, never()).findById(any())
    }

    @Test
    fun `GET por id inexistente transforma o erro do dominio em 400`() {
        val id = UUID.randomUUID()
        whenever(service.findById(id)).thenThrow(RuntimeException("Atividade não encontrada"))

        mvc
            .get()
            .uri("/activities/$id")
            .exchange()
            .assertThat()
            .hasStatus(400)
            .matches(jsonEquals("$.message", "Atividade não encontrada"))
    }

    @Test
    fun `PUT atualiza e devolve 200`() {
        val id = UUID.randomUUID()
        whenever(service.update(eq(id), any())).thenReturn(TestFixtures.activityResponse(id = id, title = "Remarcada"))

        mvc
            .put()
            .uri("/activities/$id")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"title": "Remarcada", "description": "Nova data", "activityDate": "2026-02-01T09:00:00"}""")
            .exchange()
            .assertThat()
            .hasStatus(200)
            .matches(jsonEquals("$.title", "Remarcada"))
    }

    @Test
    fun `PUT com activityDate ausente devolve 400 e nao chama o servico`() {
        mvc
            .put()
            .uri("/activities/${UUID.randomUUID()}")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"title": "Remarcada", "description": "Nova data"}""")
            .exchange()
            .assertThat()
            .hasStatus(400)

        verify(service, never()).update(any(), any())
    }

    @Test
    fun `DELETE devolve 204 sem corpo e delega ao servico`() {
        val id = UUID.randomUUID()

        mvc
            .delete()
            .uri("/activities/$id")
            .exchange()
            .assertThat()
            .hasStatus(204)
            .matches(jsonEmptyBody())

        verify(service).delete(id)
    }
}
