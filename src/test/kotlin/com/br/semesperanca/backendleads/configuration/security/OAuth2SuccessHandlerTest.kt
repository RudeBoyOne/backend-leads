package com.br.semesperanca.backendleads.configuration.security

import com.br.semesperanca.backendleads.domain.repository.RoleRepository
import com.br.semesperanca.backendleads.domain.repository.user.RealEstateAgentRepository
import com.br.semesperanca.backendleads.domain.repository.user.UserRepository
import com.br.semesperanca.backendleads.support.TestFixtures
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.oauth2.core.user.OAuth2User
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class OAuth2SuccessHandlerTest {
    @Mock private lateinit var authorizedClientService: OAuth2AuthorizedClientService

    @Mock private lateinit var realEstateAgentRepository: RealEstateAgentRepository

    @Mock private lateinit var roleRepository: RoleRepository

    @Mock private lateinit var userRepository: UserRepository

    private val frontendUrl = "http://localhost:3000"
    private val request = MockHttpServletRequest("GET", "/oauth2/authorization/google")
    private val response = MockHttpServletResponse()

    private fun handler() =
        OAuth2SuccessHandler(
            authorizedClientService,
            realEstateAgentRepository,
            roleRepository,
            userRepository,
            frontendUrl,
        )

    private fun oauthUser(
        email: String? = "ana@example.com",
        name: String? = "Ana Paula",
        emailVerified: Boolean? = true,
    ): OAuth2User {
        val attributes = mutableMapOf<String, Any>("sub" to "1234")
        if (email != null) attributes["email"] = email
        if (name != null) attributes["name"] = name
        if (emailVerified != null) attributes["email_verified"] = emailVerified
        return DefaultOAuth2User(listOf(SimpleGrantedAuthority("ROLE_USER")), attributes, "sub")
    }

    private fun authenticationToken(principal: OAuth2User) = OAuth2AuthenticationToken(principal, principal.authorities, "google")

    private fun authorizedClient(accessToken: String): OAuth2AuthorizedClient {
        val registration =
            ClientRegistration
                .withRegistrationId("google")
                .clientId("client-id")
                .clientSecret("client-secret")
                .authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope(listOf("email"))
                .authorizationUri("https://accounts.google.com/o/oauth2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .userNameAttributeName("sub")
                .build()

        return OAuth2AuthorizedClient(
            registration,
            "ana@example.com",
            OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                accessToken,
                java.time.Instant.now(),
                java.time.Instant
                    .now()
                    .plusSeconds(3600),
            ),
        )
    }

    private fun oidcToken(idTokenValue: String): OidcIdToken =
        OidcIdToken
            .withTokenValue(idTokenValue)
            .claim("sub", "1234")
            .claim("email", "ana@example.com")
            .claim("email_verified", true)
            .build()

    @Test
    fun `login via OAuth2 com email novo cria o corretor e redireciona com o access token`() {
        whenever(userRepository.existsByEmail("ana@example.com")).thenReturn(false)
        whenever(roleRepository.findByName("REAL_ESTATE_AGENT"))
            .thenReturn(Optional.of(TestFixtures.role(name = "REAL_ESTATE_AGENT")))
        whenever(authorizedClientService.loadAuthorizedClient<OAuth2AuthorizedClient>("google", "1234"))
            .thenReturn(authorizedClient("access-token-abc"))

        handler().onAuthenticationSuccess(request, response, authenticationToken(oauthUser()))

        val captor = ArgumentCaptor.forClass(com.br.semesperanca.backendleads.domain.model.user.RealEstateAgent::class.java)
        verify(realEstateAgentRepository).save(captor.capture())
        val agent = captor.value
        assertThat(agent.email).isEqualTo("ana@example.com")
        assertThat(agent.name).isEqualTo("Ana Paula")
        assertThat(agent.isActive).isTrue()
        assertThat(agent.role?.name).isEqualTo("REAL_ESTATE_AGENT")
        assertThat(agent.creci).startsWith("GOOGLE-").hasSize(15)
        assertThat(response.redirectedUrl).isEqualTo("$frontendUrl/auth/callback?token=access-token-abc")
    }

    @Test
    fun `login com email ja cadastrado nao cria usuario`() {
        whenever(userRepository.existsByEmail("ana@example.com")).thenReturn(true)
        whenever(authorizedClientService.loadAuthorizedClient<OAuth2AuthorizedClient>("google", "1234"))
            .thenReturn(authorizedClient("access-token-abc"))

        handler().onAuthenticationSuccess(request, response, authenticationToken(oauthUser()))

        verify(realEstateAgentRepository, never()).save(any())
        assertThat(response.redirectedUrl).endsWith("token=access-token-abc")
    }

    @Test
    fun `email nao verificado nao cria usuario`() {
        whenever(authorizedClientService.loadAuthorizedClient<OAuth2AuthorizedClient>("google", "1234"))
            .thenReturn(authorizedClient("access-token-abc"))

        handler().onAuthenticationSuccess(request, response, authenticationToken(oauthUser(emailVerified = false)))

        verify(userRepository, never()).existsByEmail(any())
        verify(realEstateAgentRepository, never()).save(any())
    }

    @Test
    fun `role REAL_ESTATE_AGENT ausente interrompe o login com RuntimeException`() {
        whenever(userRepository.existsByEmail("ana@example.com")).thenReturn(false)
        whenever(authorizedClientService.loadAuthorizedClient<OAuth2AuthorizedClient>("google", "1234"))
            .thenReturn(authorizedClient("access-token-abc"))
        whenever(roleRepository.findByName("REAL_ESTATE_AGENT")).thenReturn(Optional.empty())

        assertThatThrownBy {
            handler().onAuthenticationSuccess(request, response, authenticationToken(oauthUser()))
        }.isInstanceOf(RuntimeException::class.java)
            .hasMessage("Role nao encontrada")
    }

    @Test
    fun `principal Oidc usa o id token e o rotula como ID Token`() {
        whenever(userRepository.existsByEmail("ana@example.com")).thenReturn(true)
        val oidcUser = DefaultOidcUser(listOf(SimpleGrantedAuthority("ROLE_USER")), oidcToken("id-token-xyz"))

        handler().onAuthenticationSuccess(request, response, authenticationToken(oidcUser))

        assertThat(response.redirectedUrl).isEqualTo("$frontendUrl/auth/callback?token=id-token-xyz")
        verify(authorizedClientService, never()).loadAuthorizedClient<OAuth2AuthorizedClient>(any(), any())
    }

    @Test
    fun `authorized client ausente e erro explicito e nao um token silenciosamente vazio`() {
        whenever(authorizedClientService.loadAuthorizedClient<OAuth2AuthorizedClient>("google", "1234"))
            .thenReturn(null)

        assertThatThrownBy {
            handler().onAuthenticationSuccess(request, response, authenticationToken(oauthUser()))
        }.isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("Authorized client ausente")
            .hasMessageContaining("1234")
    }
}
