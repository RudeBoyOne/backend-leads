package com.br.semesperanca.backendleads.support

import com.br.semesperanca.backendleads.domain.repository.user.UserRepository
import org.springframework.test.context.bean.override.mockito.MockitoBean

/**
 * Base dos slices de controller.
 *
 * Os testes usam, juntos e **diretamente na classe**:
 *
 * ```
 * @WebMvcTest(AlgumController::class)
 * @AutoConfigureMockMvc(addFilters = false)
 * @ActiveProfiles("test")
 * class AlgumControllerTest : AbstractControllerSlice()
 * ```
 *
 * Duas armadilhas explicam por que nada disso pode virar meta-anotacao:
 *
 * 1. `@WebMvcTest` ja e meta-anotado com `@AutoConfigureMockMvc`. O bootstrapper
 *    converte os atributos da anotacao em *inlined properties*, e inlined property
 *    vence `@TestPropertySource` e `application*.yml` — pedir `false` por qualquer
 *    um dos outros dois caminhos continua dando 302 para o login do Google.
 * 2. `@ImportAutoConfiguration(exclude = ...)` nao remove a auto-configuracao de
 *    seguranca do recorte: o `@WebMvcTest` injeta o que precisa por
 *    `ImportsContextCustomizer`, cujo `ImportSelector` ignora exclusoes. Quem
 *    atrapalha no recorte e desligado por propriedade, no `application-test.yml`.
 *
 * `@ActiveProfiles("test")` e o que faz o `application-test.yml` existir; ele
 * resolve os placeholders (`${DB_URL}`, `${JWT_SECRET}`, ...) que o
 * `application.yml` principal interpola, sem nenhuma variavel de ambiente.
 *
 * Sobre `CustomJwtAuthenticationConverter`: ele implementa `Converter<Jwt, ...>`,
 * e `Converter` esta entre os includes padrao do `@WebMvcTest` — o bean entra no
 * recorte mesmo com os filtros desligados e exige um `UserRepository`. Como o
 * `MockitoBean` nao roda em classe-base anotada, o mock fica aqui, onde
 * `BeanOverride` acha por heranca.
 */
abstract class AbstractControllerSlice {
    @MockitoBean
    @Suppress("unused", "ktlint")
    lateinit var userRepository: UserRepository
}
