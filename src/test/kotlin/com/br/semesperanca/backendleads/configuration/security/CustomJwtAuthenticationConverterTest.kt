package com.br.semesperanca.backendleads.configuration.security

import com.br.semesperanca.backendleads.domain.repository.user.UserRepository
import com.br.semesperanca.backendleads.support.TestFixtures
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.oauth2.jwt.Jwt
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class CustomJwtAuthenticationConverterTest {
    @Mock private lateinit var userRepository: UserRepository

    private val appIssuer = "http://backend-leads.local"
    private val googleIssuer = "https://accounts.google.com"
    private val userId: UUID = UUID.fromString("33333333-3333-3333-3333-333333333333")

    private fun converter() = CustomJwtAuthenticationConverter(appIssuer, userRepository)

    private fun jwt(
        issuer: String,
        subject: String = "sub",
        vararg claims: Pair<String, Any>,
    ): Jwt {
        val builder =
            Jwt
                .withTokenValue("token")
                .header("alg", "RS256")
                .subject(subject)
                .issuer(issuer)
        claims.forEach { (name, value) -> builder.claim(name, value) }
        return builder.build()
    }

    /** Token do Google: e-mail verificado, `sub` do Google (não é o nosso id). */
    private fun googleJwt(email: String? = "ana@example.com") =
        jwt(
            googleIssuer,
            "google-subject-123",
            *(listOfNotNull<Pair<String, Any>>(email?.let { "email" to it }, "email_verified" to true).toTypedArray()),
        )

    private fun authoritiesOf(token: Jwt): List<String> = converter().convert(token).authorities.map { it.authority }

    @Test
    fun `token local resolve o usuario pelo sub e devolve a role do banco`() {
        whenever(userRepository.findById(userId))
            .thenReturn(Optional.of(TestFixtures.bareUser(role = TestFixtures.role(name = "MANAGER")).apply { id = userId }))

        val authorities = authoritiesOf(jwt(appIssuer, subject = userId.toString(), "role" to "MANAGER"))

        assertThat(authorities).containsExactly("ROLE_MANAGER", "UID_$userId")
    }

    /**
     * A claim `role` do token local envelhece. Uma promoção vale no próximo request
     * porque a role é relida do banco — e uma demissão também, o que na versão
     * anterior não acontecia até o token expirar.
     */
    @Test
    fun `a claim role do token local e ignorada em favor da role atual`() {
        whenever(userRepository.findById(userId))
            .thenReturn(Optional.of(TestFixtures.bareUser(role = TestFixtures.role(name = "MANAGER")).apply { id = userId }))

        val authorities = authoritiesOf(jwt(appIssuer, subject = userId.toString(), "role" to "ADMIN"))

        assertThat(authorities).contains("ROLE_MANAGER")
    }

    @Test
    fun `token local sem role no banco fica sem autoridade nenhuma`() {
        whenever(userRepository.findById(userId))
            .thenReturn(Optional.of(TestFixtures.bareUser(role = null).apply { id = userId }))

        assertThat(authoritiesOf(jwt(appIssuer, subject = userId.toString()))).isEmpty()
    }

    @Test
    fun `token local com sub que nao e UUID e recusado`() {
        assertThatThrownBy { authoritiesOf(jwt(appIssuer, subject = "sub")) }
            .isInstanceOf(BadCredentialsException::class.java)
            .hasMessage("Token sem subject válido")
    }

    @Test
    fun `token local de usuario removido e recusado`() {
        whenever(userRepository.findById(userId)).thenReturn(Optional.empty())

        assertThatThrownBy { authoritiesOf(jwt(appIssuer, subject = userId.toString())) }
            .isInstanceOf(BadCredentialsException::class.java)
            .hasMessage("Usuário inexistente")
    }

    @Test
    fun `token do Google devolve a role do banco e o id do usuario`() {
        whenever(userRepository.findByEmail("ana@example.com"))
            .thenReturn(Optional.of(TestFixtures.bareUser(role = TestFixtures.role(name = "ADMIN")).apply { id = userId }))

        assertThat(authoritiesOf(googleJwt())).containsExactly("ROLE_ADMIN", "UID_$userId")
    }

    /**
     * `UID_` existe porque o `sub` do Google não é o nosso id: sem essa authority o
     * escopo não teria como saber de quem é a requisição sem consultar o banco de
     * novo, e a base do repositório não pode depender de nenhum bean.
     */
    @Test
    fun `o id do Google nunca vira o nosso id, e sim o do usuario encontrado`() {
        whenever(userRepository.findByEmail("ana@example.com"))
            .thenReturn(Optional.of(TestFixtures.bareUser(role = TestFixtures.role(name = "MANAGER")).apply { id = userId }))

        val authorities = authoritiesOf(googleJwt())

        assertThat(authorities).contains("UID_$userId").doesNotContain("UID_google-subject-123")
    }

    /**
     * Regressão do furo que motivou a mudança: um e-mail Google não cadastrado
     * virava corretor. Agora devolve token sem autoridade, e a camada de
     * autorização responde 403 — sem nunca conferir o banco.
     */
    @Test
    fun `email Google desconhecido nao vira corretor, e sim nega acesso`() {
        whenever(userRepository.findByEmail("novo@example.com")).thenReturn(Optional.empty())

        assertThat(authoritiesOf(googleJwt("novo@example.com"))).isEmpty()
    }

    @Test
    fun `usuario sem role no banco nao vira corretor`() {
        whenever(userRepository.findByEmail("ana@example.com"))
            .thenReturn(Optional.of(TestFixtures.bareUser(role = null).apply { id = userId }))

        assertThat(authoritiesOf(googleJwt())).isEmpty()
    }

    @Test
    fun `token do Google de usuario inativo e recusado com BadCredentialsException`() {
        whenever(userRepository.findByEmail("ana@example.com"))
            .thenReturn(Optional.of(TestFixtures.bareUser(isActive = false, role = TestFixtures.role()).apply { id = userId }))

        assertThatThrownBy { authoritiesOf(googleJwt()) }
            .isInstanceOf(BadCredentialsException::class.java)
            .hasMessage("Usuario inativo")
    }

    @Test
    fun `email nao verificado nem chega a consultar o banco`() {
        assertThat(
            authoritiesOf(
                jwt(googleIssuer, "google-subject-123", "email" to "ana@example.com", "email_verified" to false),
            ),
        ).isEmpty()
    }

    @Test
    fun `email_verified ausente vale como falso`() {
        assertThat(authoritiesOf(jwt(googleIssuer, "google-subject-123", "email" to "ana@example.com"))).isEmpty()
    }

    @Test
    fun `token do Google sem email nao concede acesso`() {
        assertThat(authoritiesOf(jwt(googleIssuer, "google-subject-123", "email_verified" to true))).isEmpty()
    }
}
