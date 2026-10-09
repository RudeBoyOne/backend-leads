package com.br.semesperanca.backendleads.configuration.security

import com.br.semesperanca.backendleads.domain.model.user.User
import com.br.semesperanca.backendleads.domain.repository.RoleRepository
import com.br.semesperanca.backendleads.domain.repository.user.UserRepository
import com.br.semesperanca.backendleads.domain.service.EmailService
import com.br.semesperanca.backendleads.domain.service.InsightClient
import com.br.semesperanca.backendleads.support.AbstractIntegrationTest
import com.br.semesperanca.backendleads.support.TestJwt
import com.br.semesperanca.backendleads.support.jsonEquals
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
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
 * Por isso os papéis aqui são **usuários de verdade em `users`**, e o `sub` do
 * token é o id do usuário semeado: o conversor relê a role do banco em vez de
 * confiar na claim, e um `sub` que não corresponde a ninguém é recusado com 401.
 * A versão anterior deste arquivo confiava na claim `role` e usava um `sub`
 * fixo que não existia no banco — ou seja, ela não testava o caminho de
 * autenticação, só o de autorização.
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

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var roleRepository: RoleRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    /** Id que não existe em nenhuma tabela: o escopo o devolve vazio, e a rota 404. */
    private val id: UUID = UUID.randomUUID()

    /**
     * Um usuário real por papel, semeado uma vez por JVM. `@SpringBootTest` não
     * dá rollback, e `users.email` é `UNIQUE`: semear a cada método colidiria com
     * a linha do método anterior. O `id` fica num `companion` porque o JUnit cria
     * uma instância da classe por método.
     */
    @BeforeEach
    fun semeiaUsuariosUmaVez() {
        if (idsPorPapel.isNotEmpty()) return
        val agencia =
            entityManager
                .createNativeQuery("SELECT id FROM real_estate_agency LIMIT 1")
                .singleResult as UUID
        idsPorPapel =
            PAPEIS.associateWith { papel ->
                userRepository.save(
                    User(realEstateAgencyId = agencia, role = roleRepository.findByName(papel).orElseThrow()).apply {
                        name = "Teste $papel"
                        // E-mail único por papel e por seed: `users.email` é UNIQUE
                        // e a linha é permanente, então um e-mail fixo só funcionaria
                        // enquanto houvesse um único banco por JVM.
                        email = "chain-$papel-${UUID.randomUUID()}@example.com"
                        isActive = true
                        setPassword("{noop}senha")
                    },
                ).id!!
            }
        entityManager.clear()
    }

    /**
     * Cabeçalho `Authorization` de um papel: token assinado de verdade com o
     * `sub` do usuário semeado daquele papel.
     *
     * A claim `role` é [claim] e não [papel] de propósito — é assim que os testes
     * provam que ela é ignorada. O padrão é a própria claim, para os testes de
     * matriz ficarem legíveis como `bearerDe("ADMIN")`.
     */
    private fun bearerDe(
        papel: String,
        claim: String? = papel,
    ): String = "Bearer " + TestJwt.token(claim, subject = idsPorPapel.getValue(papel).toString())

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

    /**
     * Token de gente autenticada, sem permissão na rota: 403 com o corpo do
     * handler, e não 401 — a autenticação passou, a autorização é que não.
     */
    @Test
    fun `token valido mas sem permissao nesta rota da 403 com o corpo do access denied handler`() {
        mvc
            .perform(
                MockMvcRequestBuilders
                    .request(HttpMethod.GET, "/real-estate-agents")
                    .header(HttpHeaders.AUTHORIZATION, bearerDe("TEAM_AGENT")),
            ).andExpect(status().isForbidden)
            .andExpect(jsonEquals("$.status", "403"))
            .andExpect(jsonEquals("$.error", "Forbidden"))
            .andExpect(jsonEquals("$.message", "Acesso negado — você não tem permissão para acessar este recurso"))
    }

    /**
     * A claim `role` é **ignorada**. O `sub` identifica o usuário e o conversor
     * relê a role no banco, então um token sem claim e um token que se declara
     * `FULL_ADMIN` dão exatamente a mesma resposta quando o usuário do `sub` é
     * gerente: 403 nos dois casos. Confiar na claim devolveria a quem o escopo
     * já tirou, e só quando o token expirasse é que voltaria ao lugar certo.
     */
    @Test
    fun `a claim role do token e ignorada - a role vem do banco`() {
        val semClaim = codigo(HttpMethod.GET, "/real-estate-agents", bearerDe("TEAM_AGENT", claim = null))
        val declarada = codigo(HttpMethod.GET, "/real-estate-agents", bearerDe("TEAM_AGENT", claim = "FULL_ADMIN"))

        assertThat(semClaim).isEqualTo(403)
        assertThat(declarada).isEqualTo(403)
    }

    /**
     * Promoção não vem de token: o mesmo `sub` com claims diferentes continua
     * sendo o mesmo usuário, com a mesma role. A autoridade acompanha o banco,
     * e não o que o token afirma.
     */
    @Test
    fun `o mesmo subject com claims diferentes continua sendo o mesmo usuario`() {
        val comoGerente = codigo(HttpMethod.GET, "/managers", bearerDe("MANAGER", claim = "MANAGER"))
        val comoAdmin = codigo(HttpMethod.GET, "/managers", bearerDe("MANAGER", claim = "FULL_ADMIN"))
        val semClaim = codigo(HttpMethod.GET, "/managers", bearerDe("MANAGER", claim = null))

        assertThat(comoGerente).isEqualTo(comoAdmin).isEqualTo(semClaim).isNotEqualTo(403)
    }

    /**
     * `sub` que não é UUID é recusado antes do banco; `sub` que é UUID mas não
     * está em `users` é recusado depois. Os dois são 401: token que não aponta
     * para uma pessoa real não é token de ninguém.
     */
    @Test
    fun `sub que nao corresponde a ninguem da 401, e nao 403`() {
        assertThat(codigo(HttpMethod.GET, "/real-estate-agents", TestJwt.bearer("FULL_ADMIN", subject = TestJwt.NOT_A_UUID_SUBJECT)))
            .isEqualTo(401)
        assertThat(codigo(HttpMethod.GET, "/real-estate-agents", TestJwt.bearer("FULL_ADMIN", subject = TestJwt.UNKNOWN_UUID_SUBJECT)))
            .isEqualTo(401)
    }

    // ------------------------------------------------- matriz de papéis

    @Test
    fun `GET real-estate-agents é liberada para os quatro papeis de pessoa`() {
        listOf("FULL_ADMIN", "ADMIN", "MANAGER", "REAL_ESTATE_AGENT").forEach { papel ->
            assertPassa(HttpMethod.GET, "/real-estate-agents", papel)
        }
    }

    @Test
    fun `GET real-estate-agents barra o TEAM_AGENT`() {
        assertBarra(HttpMethod.GET, "/real-estate-agents", "TEAM_AGENT")
    }

    /**
     * Listagem e registro por id têm o mesmo piso de role na URL. A diferença
     * entre "não tem permissão" e "não existe" é do escopo, não do filtro: um
     * corretor que peça o id de um colega passa do filtro e toma 404 do
     * `findById` escopado. 403 aqui confirmaria que o registro existe.
     */
    @Test
    fun `a listagem e o registro por id dividem o mesmo piso de role`() {
        listOf("FULL_ADMIN", "ADMIN", "MANAGER", "REAL_ESTATE_AGENT").forEach { papel ->
            assertPassa(HttpMethod.GET, "/real-estate-agents/$id", papel)
        }
        assertBarra(HttpMethod.GET, "/real-estate-agents/$id", "TEAM_AGENT")
    }

    /**
     * O id é aleatório e não existe, então o corretor sai com 404 mesmo passando
     * do filtro: é o `findById` escopado que responde, e ele não distingue
     * "inexistente" de "de outra filial" — os dois são vazio.
     */
    @Test
    fun `quem passa do filtro leva 404 do escopo, e nunca 403`() {
        assertThat(codigo(HttpMethod.GET, "/real-estate-agents/$id", bearerDe("REAL_ESTATE_AGENT"))).isEqualTo(404)
        assertThat(codigo(HttpMethod.GET, "/real-estate-agents/$id", bearerDe("ADMIN"))).isEqualTo(404)
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

    /**
     * `PATCH /auth/change-password` é `.authenticated()` e não
     * `hasAnyRole(...)`: trocar a própria senha não é privilégio de hierarquia.
     * Quem não tem o papel também tem senha para trocar, e excluir o
     * `TEAM_AGENT` daqui só o obrigaria a um e-mail novo quando esquecesse a
     * senha. Sem corpo a resposta é 400, e 400 é o que "passou do filtro" quer
     * dizer aqui.
     */
    @Test
    fun `trocar a propria senha e liberada para qualquer autenticado, ate o TEAM_AGENT`() {
        PAPEIS.forEach { papel ->
            assertPassa(HttpMethod.PATCH, "/auth/change-password", papel)
        }
        assertThat(codigo(HttpMethod.PATCH, "/auth/change-password")).isEqualTo(401)
    }

    /**
     * Remoção é só do root: `ON DELETE CASCADE` leva junto gerente, corretor,
     * imóvel, sensor e lead da subárvore, e não há como reverter isso — por isso
     * nem o branch owner, que enxerga tudo o que é dele, deleta.
     */
    @Test
    fun `deletar corretor e restrito ao FULL_ADMIN, nem o branch owner passa`() {
        assertPassa(HttpMethod.DELETE, "/real-estate-agents/$id", "FULL_ADMIN")
        listOf("ADMIN", "MANAGER", "REAL_ESTATE_AGENT", "TEAM_AGENT").forEach { papel ->
            assertBarra(HttpMethod.DELETE, "/real-estate-agents/$id", papel)
        }
    }

    @Test
    fun `o reset de senha é restrito a FULL_ADMIN e ADMIN`() {
        assertPassa(HttpMethod.PATCH, "/managers/$id/reset-password", "ADMIN")
        assertBarra(HttpMethod.PATCH, "/managers/$id/reset-password", "MANAGER")
        assertPassa(HttpMethod.PATCH, "/real-estate-agents/$id/reset-password", "FULL_ADMIN")
        assertBarra(HttpMethod.PATCH, "/real-estate-agents/$id/reset-password", "REAL_ESTATE_AGENT")
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
     * `GlobalExceptionHandler` tem um `@ExceptionHandler(Exception::class)` que
     * engole o `NoResourceFoundException` do Spring 7 e devolve 500 — um `GET`
     * em URL errada (typo de front, endpoint removido, robô de varredura)
     * virava erro interno no log. O handler específico de
     * `NoResourceFoundException` ganha por tipo, sem precisar estender
     * `ResponseEntityExceptionHandler` (que trocaria o corpo de todos os erros
     * MVC por `ProblemDetail`).
     */
    @Test
    fun `rota inexistente com token válido dá 404 e não 500`() {
        val resposta =
            mvc
                .perform(
                    MockMvcRequestBuilders
                        .request(HttpMethod.GET, "/rota-que-nao-existe-mesmo")
                        .header(HttpHeaders.AUTHORIZATION, bearerDe("TEAM_AGENT")),
                ).andReturn()
                .response

        assertThat(resposta.status).isEqualTo(404)
        assertThat(resposta.contentAsString).contains("Recurso não encontrado")
    }

    @Test
    fun `a chain do OAuth2 atende as rotas de login social sem token`() {
        assertThat(codigo(HttpMethod.GET, "/login/oauth2/code/google")).isIn(200, 302)
    }

    /**
     * `/error` está liberada nas duas chains e é atendida pelo
     * `BasicErrorController`, não pelo `GlobalExceptionHandler`. Sem atributo de
     * erro no request, o controller cai no 500 padrão dele — o handler de
     * `NoResourceFoundException` não muda esse caminho.
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
        val codigo = codigo(metodo, uri, bearerDe(papel))
        assertThat(codigo).describedAs("%s %s com papel %s", metodo, uri, papel).isNotIn(401, 403)
    }

    private fun assertBarra(
        metodo: HttpMethod,
        uri: String,
        papel: String,
    ) {
        val codigo = codigo(metodo, uri, bearerDe(papel))
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

    private companion object {
        /** Os papéis que a matriz de autorização exercita. */
        val PAPEIS = listOf("FULL_ADMIN", "ADMIN", "MANAGER", "REAL_ESTATE_AGENT", "TEAM_AGENT")

        /** `papel -> id em users`. Vazio até o primeiro teste semear. */
        var idsPorPapel: Map<String, UUID> = emptyMap()
    }
}
