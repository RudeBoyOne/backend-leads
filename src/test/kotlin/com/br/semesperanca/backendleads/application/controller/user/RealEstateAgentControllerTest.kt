package com.br.semesperanca.backendleads.application.controller.user

import com.br.semesperanca.backendleads.application.model.request.ResetPasswordRequest
import com.br.semesperanca.backendleads.application.model.request.UserFilterRequest
import com.br.semesperanca.backendleads.domain.service.security.AuthService
import com.br.semesperanca.backendleads.domain.service.user.RealEstateAgentService
import com.br.semesperanca.backendleads.support.AbstractControllerSlice
import com.br.semesperanca.backendleads.support.TestFixtures
import com.br.semesperanca.backendleads.support.jsonEmptyBody
import com.br.semesperanca.backendleads.support.jsonEquals
import com.br.semesperanca.backendleads.support.jsonNotEmpty
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
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

@WebMvcTest(RealEstateAgentController::class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class RealEstateAgentControllerTest : AbstractControllerSlice() {
    @MockitoBean private lateinit var service: RealEstateAgentService

    @MockitoBean private lateinit var authService: AuthService

    @Autowired private lateinit var mvc: MockMvcTester

    /**
     * Filial é `NOT NULL` e `@NotNull` no create e no update. O id não é validado
     * aqui: o slice não tem repositório, e a existência da filial é problema do
     * `BranchOfficeRepository` escopado, não do binding do JSON.
     */
    private val branchOfficeId = UUID.randomUUID()

    @Test
    fun `GET embrulha os query params em UserFilterRequest e repassa a pagina`() {
        val item = TestFixtures.realEstateAgentResponse()
        whenever(service.findAll(any(), any())).thenReturn(PageImpl(listOf(item)))

        mvc
            .get()
            .uri("/real-estate-agents?active=false&search=ana&page=2&size=5")
            .exchange()
            .assertThat()
            .hasStatus(200)
            .matches(jsonEquals("$.content[0].creci", "CRECI-123"))
            .matches(jsonEquals("$.totalElements", 1))

        verify(service).findAll(
            eq(UserFilterRequest(active = false, search = "ana")),
            eq(PageRequest.of(2, 5)),
        )
    }

    @Test
    fun `GET sem filtros usa filtro vazio e size 10 como padrao`() {
        whenever(service.findAll(any(), any())).thenReturn(PageImpl(emptyList()))

        mvc
            .get()
            .uri("/real-estate-agents")
            .exchange()
            .assertThat()
            .hasStatus(200)

        verify(service).findAll(
            eq(UserFilterRequest(active = null, search = null)),
            eq(PageRequest.of(0, 10)),
        )
    }

    @Test
    fun `POST cria o corretor e devolve 201`() {
        val created = TestFixtures.realEstateAgentResponse(name = "Ana Paula")
        whenever(service.create(any())).thenReturn(created)

        mvc
            .post()
            .uri("/real-estate-agents")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """{"name": "Ana Paula", "email": "ana@example.com", "password": "segredo123", "creci": "CRECI-123", "branchOfficeId": "$branchOfficeId"}""",
            ).exchange()
            .assertThat()
            .hasStatus(201)
            .matches(jsonEquals("$.id", created.id.toString()))
            .matches(jsonEquals("$.role", "REAL_ESTATE_AGENT"))
    }

    @Test
    fun `POST sem creci devolve 400 sem chamar o servico`() {
        mvc
            .post()
            .uri("/real-estate-agents")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"name": "Ana Paula", "email": "ana@example.com", "password": "segredo123", "creci": "", "branchOfficeId": "$branchOfficeId"}""")
            .exchange()
            .assertThat()
            .hasStatus(400)
            .matches(jsonNotEmpty("$.fields.creci"))

        verify(service, never()).create(any())
    }

    @Test
    fun `POST com nome de 2 caracteres devolve 400 pelo minimo de 3 do corretor`() {
        mvc
            .post()
            .uri("/real-estate-agents")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"name": "An", "email": "an@example.com", "password": "segredo123", "creci": "CRECI-9", "branchOfficeId": "$branchOfficeId"}""")
            .exchange()
            .assertThat()
            .hasStatus(400)
            .matches(jsonNotEmpty("$.fields.name"))
    }

    @Test
    fun `GET por id devolve o corretor`() {
        val id = UUID.randomUUID()
        whenever(service.findById(id)).thenReturn(TestFixtures.realEstateAgentResponse(id = id, name = "Pedro Alves"))

        mvc
            .get()
            .uri("/real-estate-agents/$id")
            .exchange()
            .assertThat()
            .hasStatus(200)
            .matches(jsonEquals("$.name", "Pedro Alves"))
    }

    @Test
    fun `GET por id invalido devolve 400 antes de chegar no servico`() {
        mvc
            .get()
            .uri("/real-estate-agents/abc")
            .exchange()
            .assertThat()
            .hasStatus(400)

        verify(service, never()).findById(any())
    }

    @Test
    fun `PUT atualiza e devolve 200`() {
        val id = UUID.randomUUID()
        whenever(service.update(eq(id), any()))
            .thenReturn(TestFixtures.realEstateAgentResponse(id = id, name = "Ana Beatriz"))

        mvc
            .put()
            .uri("/real-estate-agents/$id")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"name": "Ana Beatriz", "email": "ana@example.com", "creci": "CRECI-456", "branchOfficeId": "$branchOfficeId"}""")
            .exchange()
            .assertThat()
            .hasStatus(200)
            .matches(jsonEquals("$.name", "Ana Beatriz"))
    }

    @Test
    fun `PUT com email invalido devolve 400 e nao chama o servico`() {
        mvc
            .put()
            .uri("/real-estate-agents/${UUID.randomUUID()}")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"name": "Ana", "email": "sem-arroba", "creci": "CRECI-1", "branchOfficeId": "$branchOfficeId"}""")
            .exchange()
            .assertThat()
            .hasStatus(400)
            .matches(jsonNotEmpty("$.fields.email"))

        verify(service, never()).update(any(), any())
    }

    @Test
    fun `PATCH toggle-active devolve o corretor com o estado invertido`() {
        val id = UUID.randomUUID()
        whenever(service.toggleActive(id)).thenReturn(TestFixtures.realEstateAgentResponse(id = id, active = false))

        mvc
            .patch()
            .uri("/real-estate-agents/$id/toggle-active")
            .exchange()
            .assertThat()
            .hasStatus(200)
            .matches(jsonEquals("$.active", false))
    }

    @Test
    fun `DELETE devolve 204 sem corpo e delega ao servico`() {
        val id = UUID.randomUUID()

        mvc
            .delete()
            .uri("/real-estate-agents/$id")
            .exchange()
            .assertThat()
            .hasStatus(204)
            .matches(jsonEmptyBody())

        verify(service).delete(id)
    }

    @Test
    fun `PATCH reset-password devolve 204 e delega ao AuthService`() {
        val id = UUID.randomUUID()
        val request = ResetPasswordRequest(newPassword = "outra-senha", confirmPassword = "outra-senha")

        mvc
            .patch()
            .uri("/real-estate-agents/$id/reset-password")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"newPassword": "outra-senha", "confirmPassword": "outra-senha"}""")
            .exchange()
            .assertThat()
            .hasStatus(204)
            .matches(jsonEmptyBody())

        verify(authService).resetPassword(eq(id), eq(request))
    }

    /**
     * `confirmPassword` e `String` nao-nula, entao faltar no JSON quebra o binding
     * antes do `@Valid` — a resposta e 400 com mensagem de deserializacao, sem o
     * mapa `fields`.
     */
    @Test
    fun `PATCH reset-password sem confirmPassword devolve 400 e nao delega`() {
        mvc
            .patch()
            .uri("/real-estate-agents/${UUID.randomUUID()}/reset-password")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"newPassword": "outra-senha"}""")
            .exchange()
            .assertThat()
            .hasStatus(400)
            .matches(content().string(containsString("confirmPassword")))

        verify(authService, never()).resetPassword(any(), any())
    }
}
