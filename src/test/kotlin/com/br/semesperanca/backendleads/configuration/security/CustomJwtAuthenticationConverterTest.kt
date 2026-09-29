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

@ExtendWith(MockitoExtension::class)
class CustomJwtAuthenticationConverterTest {
    @Mock private lateinit var userRepository: UserRepository

    private val appIssuer = "http://backend-leads.local"
    private val googleIssuer = "https://accounts.google.com"

    private fun converter() = CustomJwtAuthenticationConverter(appIssuer, userRepository)

    private fun jwt(
        issuer: String,
        vararg claims: Pair<String, Any>,
    ): Jwt {
        val builder =
            Jwt
                .withTokenValue("token")
                .header("alg", "RS256")
                .subject("sub")
                .issuer(issuer)
        claims.forEach { (name, value) -> builder.claim(name, value) }
        return builder.build()
    }

    @Test
    fun `token local vira ROLE a partir do claim role`() {
        val token = converter().convert(jwt(appIssuer, "role" to "MANAGER"))

        assertThat(token.authorities.map { it.authority }).containsExactly("ROLE_MANAGER")
    }

    @Test
    fun `token local sem claim role fica sem autoridade nenhuma`() {
        val token = converter().convert(jwt(appIssuer))

        assertThat(token.authorities).isEmpty()
    }

    @Test
    fun `token do Google de usuario ativo com role devolve a role do banco`() {
        whenever(userRepository.findByEmail("ana@example.com"))
            .thenReturn(Optional.of(TestFixtures.bareUser(role = TestFixtures.role(name = "ADMIN"))))

        val token = converter().convert(jwt(googleIssuer, "email" to "ana@example.com", "email_verified" to true))

        assertThat(token.authorities.map { it.authority }).containsExactly("ROLE_ADMIN")
    }

    @Test
    fun `token do Google de usuario ativo sem role cai no papel padrao de corretor`() {
        whenever(userRepository.findByEmail("ana@example.com"))
            .thenReturn(Optional.of(TestFixtures.bareUser(role = null)))

        val token = converter().convert(jwt(googleIssuer, "email" to "ana@example.com", "email_verified" to true))

        assertThat(token.authorities.map { it.authority }).containsExactly("ROLE_REAL_ESTATE_AGENT")
    }

    @Test
    fun `token do Google de usuario inativo e recusado com BadCredentialsException`() {
        whenever(userRepository.findByEmail("ana@example.com"))
            .thenReturn(Optional.of(TestFixtures.bareUser(isActive = false, role = TestFixtures.role())))

        assertThatThrownBy {
            converter().convert(jwt(googleIssuer, "email" to "ana@example.com", "email_verified" to true))
        }.isInstanceOf(BadCredentialsException::class.java)
            .hasMessage("Usuario inativo")
    }

    @Test
    fun `token do Google de email desconhecido cai no papel padrao de corretor`() {
        whenever(userRepository.findByEmail("novo@example.com")).thenReturn(Optional.empty())

        val token = converter().convert(jwt(googleIssuer, "email" to "novo@example.com", "email_verified" to true))

        assertThat(token.authorities.map { it.authority }).containsExactly("ROLE_REAL_ESTATE_AGENT")
    }

    @Test
    fun `email nao verificado nem chega a consultar o banco e cai no papel padrao`() {
        val token = converter().convert(jwt(googleIssuer, "email" to "ana@example.com", "email_verified" to false))

        assertThat(token.authorities.map { it.authority }).containsExactly("ROLE_REAL_ESTATE_AGENT")
    }

    @Test
    fun `email_verified ausente vale como falso`() {
        val token = converter().convert(jwt(googleIssuer, "email" to "ana@example.com"))

        assertThat(token.authorities.map { it.authority }).containsExactly("ROLE_REAL_ESTATE_AGENT")
    }

    @Test
    fun `token do Google sem email cai no papel padrao de corretor`() {
        val token = converter().convert(jwt(googleIssuer, "email_verified" to true))

        assertThat(token.authorities.map { it.authority }).containsExactly("ROLE_REAL_ESTATE_AGENT")
    }
}
