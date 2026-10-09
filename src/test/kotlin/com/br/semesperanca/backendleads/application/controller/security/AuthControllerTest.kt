package com.br.semesperanca.backendleads.application.controller.security

import com.br.semesperanca.backendleads.application.model.request.ChangePasswordRequest
import com.br.semesperanca.backendleads.domain.service.security.AuthService
import com.br.semesperanca.backendleads.support.AbstractControllerSlice
import com.br.semesperanca.backendleads.support.TestFixtures
import com.br.semesperanca.backendleads.support.authenticatedAsJwt
import com.br.semesperanca.backendleads.support.clearSecurityContext
import com.br.semesperanca.backendleads.support.jsonEmptyBody
import com.br.semesperanca.backendleads.support.jsonEquals
import com.br.semesperanca.backendleads.support.jsonNotEmpty
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.assertj.MockMvcTester

@WebMvcTest(AuthController::class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AuthControllerTest : AbstractControllerSlice() {
    @MockitoBean private lateinit var authService: AuthService

    @Autowired private lateinit var mvc: MockMvcTester

    private val changePasswordJson =
        """{"currentPassword": "senha-antiga", "newPassword": "senha-nova", "confirmPassword": "senha-nova"}"""

    @AfterEach
    fun `limpa o SecurityContext que o post-processor deixou na thread`() = clearSecurityContext()

    @Test
    fun `POST login devolve 200 com o token`() {
        whenever(authService.login(any())).thenReturn(TestFixtures.loginResponse("jwt-xyz"))

        mvc
            .post()
            .uri("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"email": "ana@example.com", "password": "segredo123"}""")
            .exchange()
            .assertThat()
            .hasStatus(200)
            .matches(jsonEquals("$.token", "jwt-xyz"))
    }

    @Test
    fun `POST login com credenciais invalidas transforma o erro do dominio em 400`() {
        whenever(authService.login(any())).thenThrow(RuntimeException("Email ou senha inválidos"))

        mvc
            .post()
            .uri("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"email": "ana@example.com", "password": "errada123"}""")
            .exchange()
            .assertThat()
            .hasStatus(400)
            .matches(jsonEquals("$.message", "Email ou senha inválidos"))
    }

    /**
     * `login` e `createFullAdmin` sao os dois endpoints sem `@Valid` no
     * `@RequestBody` — as constraints de `LoginRequest` e `CreateFullAdminRequest`
     * existem mas nunca sao avaliadas. Aqui o payload invalido chega inteiro no
     * `AuthService` e a resposta e 200. O teste existe para deixar o buraco
     * visivel: se alguem adicionar o `@Valid`, ele quebra.
     */
    @Test
    fun `POST login rejeita email invalido com 400 e devolve mapa de erros de validacao`() {
        mvc
            .post()
            .uri("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"email": "nao-e-email", "password": "123"}""")
            .exchange()
            .assertThat()
            .hasStatus(400)
            .matches(jsonEquals("$.message", "Erro de validação"))
            .matches(jsonNotEmpty("$.fields.email"))

        verify(authService, never()).login(any())
    }

    @Test
    fun `POST register full-admin devolve 201`() {
        val created = TestFixtures.realEstateAgentResponse(name = "Super Admin", active = true)
        whenever(authService.createFullAdmin(any())).thenReturn(created)

        mvc
            .post()
            .uri("/auth/register/full-admin")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"name": "Super Admin", "email": "root@example.com", "password": "segredo123"}""")
            .exchange()
            .assertThat()
            .hasStatus(201)
            .matches(jsonEquals("$.id", created.id.toString()))
    }

    @Test
    fun `POST register full-admin rejeita senha curta com 400 e devolve mapa de erros`() {
        mvc
            .post()
            .uri("/auth/register/full-admin")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"name": "A", "email": "invalido", "password": "123"}""")
            .exchange()
            .assertThat()
            .hasStatus(400)
            .matches(jsonNotEmpty("$.fields.password"))

        verify(authService, never()).createFullAdmin(any())
    }

    @Test
    fun `PATCH change-password usa o claim email do JWT e devolve 204`() {
        val request =
            ChangePasswordRequest(
                currentPassword = "senha-antiga",
                newPassword = "senha-nova",
                confirmPassword = "senha-nova",
            )

        mvc
            .patch()
            .uri("/auth/change-password")
            .contentType(MediaType.APPLICATION_JSON)
            .content(changePasswordJson)
            .with(authenticatedAsJwt(email = "ana@example.com"))
            .exchange()
            .assertThat()
            .hasStatus(204)
            .matches(jsonEmptyBody())

        verify(authService).changePassword(eq(request), eq("ana@example.com"))
    }

    /**
     * Sem o claim `email` o controller nao quebra: repassa string vazia e deixa o
     * `AuthService` recusar. Fica travado aqui para uma mudanca futura nao virar NPE.
     */
    @Test
    fun `PATCH change-password sem o claim email repassa string vazia em vez de estourar`() {
        mvc
            .patch()
            .uri("/auth/change-password")
            .contentType(MediaType.APPLICATION_JSON)
            .content(changePasswordJson)
            .with(authenticatedAsJwt())
            .exchange()
            .assertThat()
            .hasStatus(204)

        verify(authService).changePassword(any(), eq(""))
    }

    @Test
    fun `PATCH change-password com senha nova curta devolve 400 e nao delega`() {
        mvc
            .patch()
            .uri("/auth/change-password")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"currentPassword": "senha-antiga", "newPassword": "123", "confirmPassword": "123"}""")
            .with(authenticatedAsJwt(email = "ana@example.com"))
            .exchange()
            .assertThat()
            .hasStatus(400)
            .matches(jsonNotEmpty("$.fields.newPassword"))

        verify(authService, never()).changePassword(any(), any())
    }

    @Test
    fun `PATCH change-password sem confirmPassword devolve 400 por erro de binding`() {
        mvc
            .patch()
            .uri("/auth/change-password")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"currentPassword": "senha-antiga", "newPassword": "senha-nova"}""")
            .with(authenticatedAsJwt(email = "ana@example.com"))
            .exchange()
            .assertThat()
            .hasStatus(400)
            .matches(jsonNotEmpty("$.message"))

        verify(authService, never()).changePassword(any(), any())
    }
}
