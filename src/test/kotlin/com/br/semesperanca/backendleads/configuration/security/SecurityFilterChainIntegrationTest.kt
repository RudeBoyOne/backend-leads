package com.br.semesperanca.backendleads.configuration.security

import com.br.semesperanca.backendleads.domain.service.EmailService
import com.br.semesperanca.backendleads.domain.service.InsightClient
import com.br.semesperanca.backendleads.support.AbstractIntegrationTest
import com.br.semesperanca.backendleads.support.TestJwt
import com.br.semesperanca.backendleads.support.jsonEquals
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Duration
import java.util.UUID

/**
 * A cadeia de segurança inteira, com os filtros **ligados** — o oposto dos
 * slices de controller, que rodam com `addFilters = false`.
 *
 * É o único lugar onde as duas `SecurityFilterChain` são exercitadas de verdade:
 * o `BearerTokenAuthenticationFilter` decodifica o token, o
 * `CustomJwtAuthenticationConverter` monta as autoridades, e o
 * `AuthorizationFilter` decide. O token é assinado de verdade pelo [TestJwt]
 * (mesmo segredo HS256, mesmo `iss` do `app.jwt.issuer`), então o
 * `CustomJwtDecoder` cai no decoder local e valida emissor e expiração — não é
 * atalho de autenticação.
 *
 * O que a matriz fixa é o contrato de acesso da API: quem entra sem token, quem
 * toma 401, quem toma 403, e — o ponto que nenhum teste de controller alcança —
 * que uma rota nova nasce protegida, porque a chain da API é catch-all e não
 * lista rotas liberadas.
 *
 * Por que `MockMvcRequestBuilders.request(HttpMethod, uri)` e não `mvc.get(...)`:
 * o Kotlin importa método estático como função de topo, então `mvc.get("/x")`
 * não resolve. `request(...)` passa o método explicitamente e evita ter cinco
 * imports estáticos disputando o mesmo nome.
 */
@AutoConfigureMockMvc
class SecurityFilterChainIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var mvc: MockMvc

    /** `POST /leads` chega no serviço real e dispararia SMTP e HTTP de verdade. */
    @MockitoBean
    private lateinit var emailService: EmailService

    @MockitoBean
    private lateinit var insightClient: InsightClient

    private val id: UUID = UUID.randomUUID()

    // ------------------------------------------------------------- público

    @Test
    fun `o health do actuator é público`() {
        assertThat(codigo(HttpMethod.GET, "/actuator/health")).isEqualTo(200)
    }

    /**
     * Prova de que a rota é pública sem depender de senha: corpo vazio é 400 de
     * desserialização, e 400 só acontece se a requisição chegou ao controller.
     * Se a rota estivesse protegida, viraria 401 do entry point.
     */
    @Test
    fun `o login é público - corpo inválido dá 400 e não 401`() {
        assertThat(
            codigo(
                HttpMethod.POST,
                "/auth/login",
                body = "{}",
            ),
        ).isEqualTo(400)
    }

    /**
     * O `admin@semesperanca.com.br` do `V007` existe de verdade, com hash
     * BCrypt; a senha aqui é outra qualquer. O `AuthService` estoura
     * `RuntimeException("Senha inválida")` e o `GlobalExceptionHandler`
     * traduz `RuntimeException` em 400 — o que importa é que **não** é 401:
     * se fosse, o filtro teria comido a requisição e o serviço nunca rodaria.
     */
    @Test
    fun `login com senha errada falha no serviço e não no filtro`() {
        val resposta =
            mvc
                .perform(
                    MockMvcRequestBuilders
                        .request(HttpMethod.POST, "/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"email":"admin@semesperanca.com.br","password":"senha-errada"}"""),
                ).andReturn()
                .response

        assertThat(resposta.status).isEqualTo(400)
        assertThat(resposta.contentAsString).contains("Senha inválida")
    }

    @Test
    fun `a documentação da API é pública`() {
        assertThat(codigo(HttpMethod.GET, "/v3/api-docs")).isIn(200, 302)
    }

    @Test
    fun `o preflight OPTIONS de qualquer rota é liberado sem token`() {
        val resposta =
            mvc
                .perform(
                    MockMvcRequestBuilders
                        .request(HttpMethod.OPTIONS, "/real-estate-agents")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"),
                ).andReturn()
                .response

        assertThat(resposta.status).isIn(200, 204)
        assertThat(resposta.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("http://localhost:3000")
    }

    // ---------------------------------------------------------- sem token

    @Test
    fun `rota protegida sem token responde 401 com o corpo do entry point customizado`() {
        mvc
            .perform(MockMvcRequestBuilders.request(HttpMethod.GET, "/real-estate-agents"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonEquals("$.status", "401"))
            .andExpect(jsonEquals("$.error", "Unauthorized"))
            .andExpect(jsonEquals("$.message", "Token inválido ou expirado"))
    }

    @Test
    fun `token expirado dá 401 e não 403`() {
        assertThat(codigo(HttpMethod.GET, "/real-estate-agents", TestJwt.bearer("FULL_ADMIN", expiresIn = Duration.ofHours(-1))))
            .isEqualTo(401)
    }

    @Test
    fun `token assinado com a chave errada dá 401`() {
        assertThat(codigo(HttpMethod.GET, "/real-estate-agents", TestJwt.bearer("FULL_ADMIN", key = TestJwt.outraChave)))
            .isEqualTo(401)
    }

    @Test
    fun `token de emissor desconhecido dá 401 antes de qualquer validação`() {
        val token = TestJwt.bearer("FULL_ADMIN", issuer = "https://emissor-falsificado.example")

        assertThat(codigo(HttpMethod.GET, "/real-estate-agents", token)).isEqualTo(401)
    }

    @Test
    fun `cabeçalho Authorization malformado dá 401 e não 500`() {
        assertThat(codigo(HttpMethod.GET, "/real-estate-agents", "Basic YW5hOnNlbmhh")).isEqualTo(401)
    }

    @Test
    fun `token que não é JWT dá 401`() {
        assertThat(codigo(HttpMethod.GET, "/real-estate-agents", "Bearer nao-e-um-jwt")).isEqualTo(401)
    }

    // --------------------------------------------------------------- 403

    @Test
    fun `token válido mas sem o papel dá 403 com o corpo do access denied handler`() {
        mvc
            .perform(
                MockMvcRequestBuilders
                    .request(HttpMethod.GET, "/real-estate-agents")
                    .header(HttpHeaders.AUTHORIZATION, TestJwt.bearer("MANAGER")),
            ).andExpect(status().isForbidden)
            .andExpect(jsonEquals("$.status", "403"))
            .andExpect(jsonEquals("$.error", "Forbidden"))
            .andExpect(jsonEquals("$.message", "Acesso negado — você não tem permissão para acessar este recurso"))
    }

    @Test
    fun `token sem claim role dá 403 e não 401 - a autenticação passou`() {
        assertThat(codigo(HttpMethod.GET, "/real-estate-agents", TestJwt.bearer(null))).isEqualTo(403)
    }

    @Test
    fun `a autoridade vem da claim role e não do subject do token`() {
        // Mesmo subject nos dois tokens: se o conversor lesse o subject, dariam
        // a mesma resposta.
        val subject = "11111111-1111-1111-1111-111111111111"
        val comoGerente = TestJwt.token("MANAGER", subject = subject)
        val comoAdmin = TestJwt.token("FULL_ADMIN", subject = subject)

        assertThat(codigo(HttpMethod.GET, "/real-estate-agents", "Bearer $comoGerente")).isEqualTo(403)
        assertThat(codigo(HttpMethod.GET, "/real-estate-agents", "Bearer $comoAdmin")).isNotEqualTo(403)
    }

    // ------------------------------------------------- matriz de papéis

    @Test
    fun `GET real-estate-agents é liberada para FULL_ADMIN e ADMIN`() {
        assertPassa(HttpMethod.GET, "/real-estate-agents", "FULL_ADMIN")
        assertPassa(HttpMethod.GET, "/real-estate-agents", "ADMIN")
    }

    @Test
    fun `GET real-estate-agents barra MANAGER, REAL_ESTATE_AGENT e TEAM_AGENT`() {
        listOf("MANAGER", "REAL_ESTATE_AGENT", "TEAM_AGENT").forEach { papel ->
            assertBarra(HttpMethod.GET, "/real-estate-agents", papel)
        }
    }

    @Test
    fun `GET real-estate-agents por id abre um papel que a listagem não abre`() {
        assertPassa(HttpMethod.GET, "/real-estate-agents/$id", "REAL_ESTATE_AGENT")
        assertBarra(HttpMethod.GET, "/real-estate-agents/$id", "MANAGER")
    }

    @Test
    fun `PUT de lead aceita corretor e gerente mas barra o TEAM_AGENT`() {
        assertPassa(HttpMethod.PUT, "/leads/$id", "REAL_ESTATE_AGENT")
        assertPassa(HttpMethod.PUT, "/leads/$id", "MANAGER")
        assertBarra(HttpMethod.PUT, "/leads/$id", "TEAM_AGENT")
    }

    @Test
    fun `managers por id é aberta ao gerente e fechada para o corretor`() {
        assertPassa(HttpMethod.GET, "/managers/$id", "MANAGER")
        assertBarra(HttpMethod.GET, "/managers/$id", "REAL_ESTATE_AGENT")
    }

    @Test
    fun `criar FULL_ADMIN exige exatamente FULL_ADMIN - nem ADMIN passa`() {
        assertBarra(HttpMethod.POST, "/auth/register/full-admin", "ADMIN")
        assertBarra(HttpMethod.POST, "/auth/register/full-admin", "MANAGER")
        assertPassa(HttpMethod.POST, "/auth/register/full-admin", "FULL_ADMIN")
    }

    @Test
    fun `trocar senha aceita os quatro papeis de pessoa e barra o TEAM_AGENT`() {
        listOf("FULL_ADMIN", "ADMIN", "MANAGER", "REAL_ESTATE_AGENT").forEach { papel ->
            assertPassa(HttpMethod.PATCH, "/auth/change-password", papel)
        }
        assertBarra(HttpMethod.PATCH, "/auth/change-password", "TEAM_AGENT")
    }

    @Test
    fun `deletar corretor é restrito a FULL_ADMIN e ADMIN`() {
        assertPassa(HttpMethod.DELETE, "/real-estate-agents/$id", "ADMIN")
        assertBarra(HttpMethod.DELETE, "/real-estate-agents/$id", "MANAGER")
    }

    @Test
    fun `o reset de senha é restrito a FULL_ADMIN e ADMIN`() {
        assertPassa(HttpMethod.PATCH, "/managers/$id/reset-password", "ADMIN")
        assertBarra(HttpMethod.PATCH, "/managers/$id/reset-password", "MANAGER")
        assertPassa(HttpMethod.PATCH, "/real-estate-agents/$id/reset-password", "FULL_ADMIN")
    }

    @Test
    fun `o toggle de ativo é restrito a FULL_ADMIN e ADMIN`() {
        assertPassa(HttpMethod.PATCH, "/managers/$id/toggle-active", "FULL_ADMIN")
        assertBarra(HttpMethod.PATCH, "/managers/$id/toggle-active", "MANAGER")
    }

    // -------------------------------------------------- catch-all e OAuth2

    @Test
    fun `rota fora da lista explicita sem token cai no anyRequest e da 401`() {
        assertThat(codigo(HttpMethod.GET, "/rota-que-nao-existe-mesmo")).isEqualTo(401)
    }

    /**
     * Bug de handler, não de teste: rota inexistente deveria dar 404, mas o
     * `GlobalExceptionHandler` não estende `ResponseEntityExceptionHandler` e
     * tem um `@ExceptionHandler(Exception::class)` que engole o
     * `NoResourceFoundException` e devolve 500. Um `GET` em URL errada — typo
     * de front, endpoint removido, robô de varredura — aparece nos logs como
     * erro interno. Fixado aqui, não corrigido.
     */
    @Test
    fun `rota inexistente com token válido dá 500 e não 404 porque o handler engole NoResourceFoundException`() {
        val resposta =
            mvc
                .perform(
                    MockMvcRequestBuilders
                        .request(HttpMethod.GET, "/rota-que-nao-existe-mesmo")
                        .header(HttpHeaders.AUTHORIZATION, TestJwt.bearer("TEAM_AGENT")),
                ).andReturn()
                .response

        assertThat(resposta.status).isEqualTo(500)
        assertThat(resposta.contentAsString).contains("Erro interno do servidor")
    }

    @Test
    fun `a chain do OAuth2 atende as rotas de login social sem token`() {
        assertThat(codigo(HttpMethod.GET, "/login/oauth2/code/google")).isIn(200, 302)
    }

    /**
     * `/error` está liberada nas duas chains, mas sem atributo de erro no request
     * ela cai no mesmo `Exception` genérico e devolve 500.
     */
    @Test
    fun `a rota de erro é pública mas devolve 500 quando não há erro para reportar`() {
        assertThat(codigo(HttpMethod.GET, "/error")).isEqualTo(500)
    }

    // ----------------------------------------------------------------- utils

    /**
     * "Passa" significa: passou do filtro de autorização. A resposta pode ser
     * 404 (recurso inexistente), 400 (corpo faltando) ou 200 — o que não pode
     * é 401/403.
     */
    private fun assertPassa(
        metodo: HttpMethod,
        uri: String,
        papel: String,
    ) {
        val codigo = codigo(metodo, uri, TestJwt.bearer(papel))
        assertThat(codigo).describedAs("%s %s com papel %s", metodo, uri, papel).isNotIn(401, 403)
    }

    private fun assertBarra(
        metodo: HttpMethod,
        uri: String,
        papel: String,
    ) {
        val codigo = codigo(metodo, uri, TestJwt.bearer(papel))
        assertThat(codigo).describedAs("%s %s com papel %s", metodo, uri, papel).isEqualTo(403)
    }

    private fun codigo(
        metodo: HttpMethod,
        uri: String,
        authorization: String? = null,
        body: String? = null,
    ): Int {
        val request = MockMvcRequestBuilders.request(metodo, uri)
        if (authorization != null) {
            request.header(HttpHeaders.AUTHORIZATION, authorization)
        }
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(body)
        }
        return mvc
            .perform(request)
            .andReturn()
            .response.status
    }
}
