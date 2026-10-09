package com.br.semesperanca.backendleads.application.controller.user

import com.br.semesperanca.backendleads.application.model.request.ResetPasswordRequest
import com.br.semesperanca.backendleads.application.model.request.UserFilterRequest
import com.br.semesperanca.backendleads.domain.service.security.AuthService
import com.br.semesperanca.backendleads.domain.service.user.ManagerService
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
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.assertj.MockMvcTester
import java.util.UUID

@WebMvcTest(ManagerController::class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ManagerControllerTest : AbstractControllerSlice() {
    @MockitoBean private lateinit var service: ManagerService

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
        val item = TestFixtures.managerResponse()
        whenever(service.findAll(any(), any())).thenReturn(PageImpl(listOf(item)))

        mvc
            .get()
            .uri("/managers?active=true&search=bruno&page=1&size=10")
            .exchange()
            .assertThat()
            .hasStatus(200)
            .matches(jsonEquals("$.content[0].id", item.id.toString()))
            .matches(jsonEquals("$.totalElements", 1))

        verify(service).findAll(
            eq(UserFilterRequest(active = true, search = "bruno")),
            eq(PageRequest.of(1, 10)),
        )
    }

    @Test
    fun `GET sem filtros usa filtro vazio e size 10 como padrao`() {
        whenever(service.findAll(any(), any())).thenReturn(PageImpl(emptyList()))

        mvc
            .get()
            .uri("/managers")
            .exchange()
            .assertThat()
            .hasStatus(200)

        verify(service).findAll(
            eq(UserFilterRequest(active = null, search = null)),
            eq(PageRequest.of(0, 10)),
        )
    }

    @Test
    fun `POST cria o gerente e devolve 201`() {
        val created = TestFixtures.managerResponse(name = "Bruno Lima")
        whenever(service.create(any())).thenReturn(created)

        mvc
            .post()
            .uri("/managers")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """{"name": "Bruno Lima", "email": "bruno@example.com", "password": "segredo123", "branchOfficeId": "$branchOfficeId"}""",
            ).exchange()
            .assertThat()
            .hasStatus(201)
            .matches(jsonEquals("$.id", created.id.toString()))
            .matches(jsonEquals("$.role", "MANAGER"))
    }

    @Test
    fun `POST com senha curta devolve 400 sem chamar o servico`() {
        mvc
            .post()
            .uri("/managers")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"name": "Bruno", "email": "bruno@example.com", "password": "123", "branchOfficeId": "$branchOfficeId"}""")
            .exchange()
            .assertThat()
            .hasStatus(400)
            .matches(jsonNotEmpty("$.fields.password"))

        verify(service, never()).create(any())
    }

    @Test
    fun `POST com nome de 1 caractere devolve 400`() {
        mvc
            .post()
            .uri("/managers")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"name": "B", "email": "b@example.com", "password": "segredo123", "branchOfficeId": "$branchOfficeId"}""")
            .exchange()
            .assertThat()
            .hasStatus(400)
            .matches(jsonNotEmpty("$.fields.name"))
    }

    @Test
    fun `GET por id devolve o gerente`() {
        val id = UUID.randomUUID()
        whenever(service.findById(id)).thenReturn(TestFixtures.managerResponse(id = id, name = "Patricia Alves"))

        mvc
            .get()
            .uri("/managers/$id")
            .exchange()
            .assertThat()
            .hasStatus(200)
            .matches(jsonEquals("$.name", "Patricia Alves"))
    }

    @Test
    fun `GET por id inexistente transforma o erro do dominio em 400`() {
        val id = UUID.randomUUID()
        whenever(service.findById(id)).thenThrow(RuntimeException("Gerente não encontrado"))

        mvc
            .get()
            .uri("/managers/$id")
            .exchange()
            .assertThat()
            .hasStatus(400)
            .matches(jsonEquals("$.message", "Gerente não encontrado"))
    }

    @Test
    fun `PUT atualiza e devolve 200`() {
        val id = UUID.randomUUID()
        whenever(service.update(eq(id), any())).thenReturn(TestFixtures.managerResponse(id = id, name = "Bruno Costa"))

        mvc
            .put()
            .uri("/managers/$id")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"name": "Bruno Costa", "email": "bruno@example.com", "branchOfficeId": "$branchOfficeId"}""")
            .exchange()
            .assertThat()
            .hasStatus(200)
            .matches(jsonEquals("$.name", "Bruno Costa"))
    }

    @Test
    fun `PUT com email invalido devolve 400 e nao chama o servico`() {
        mvc
            .put()
            .uri("/managers/${UUID.randomUUID()}")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"name": "Bruno", "email": "bruno@@example", "branchOfficeId": "$branchOfficeId"}""")
            .exchange()
            .assertThat()
            .hasStatus(400)
            .matches(jsonNotEmpty("$.fields.email"))

        verify(service, never()).update(any(), any())
    }

    @Test
    fun `PATCH toggle-active devolve o gerente com o estado invertido`() {
        val id = UUID.randomUUID()
        whenever(service.toggleActive(id)).thenReturn(TestFixtures.managerResponse(id = id, active = false))

        mvc
            .patch()
            .uri("/managers/$id/toggle-active")
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
            .uri("/managers/$id")
            .exchange()
            .assertThat()
            .hasStatus(204)
            .matches(jsonEmptyBody())

        verify(service).delete(id)
    }

    @Test
    fun `PATCH reset-password devolve 204 e delega ao AuthService, nao ao ManagerService`() {
        val id = UUID.randomUUID()
        val request = ResetPasswordRequest(newPassword = "nova-senha", confirmPassword = "nova-senha")

        mvc
            .patch()
            .uri("/managers/$id/reset-password")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"newPassword": "nova-senha", "confirmPassword": "nova-senha"}""")
            .exchange()
            .assertThat()
            .hasStatus(204)
            .matches(jsonEmptyBody())

        verify(authService).resetPassword(eq(id), eq(request))
        verify(service, never()).update(any(), any())
    }

    @Test
    fun `PATCH reset-password com senha curta devolve 400 e nao delega`() {
        mvc
            .patch()
            .uri("/managers/${UUID.randomUUID()}/reset-password")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"newPassword": "123", "confirmPassword": "123"}""")
            .exchange()
            .assertThat()
            .hasStatus(400)
            .matches(jsonNotEmpty("$.fields.newPassword"))

        verify(authService, never()).resetPassword(any(), any())
    }
}
