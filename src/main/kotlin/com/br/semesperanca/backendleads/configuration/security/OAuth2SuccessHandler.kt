package com.br.semesperanca.backendleads.configuration.security

import com.br.semesperanca.backendleads.domain.model.Role
import com.br.semesperanca.backendleads.domain.model.user.RealEstateAgent
import com.br.semesperanca.backendleads.domain.repository.RoleRepository
import com.br.semesperanca.backendleads.domain.repository.user.RealEstateAgentRepository
import com.br.semesperanca.backendleads.domain.repository.user.UserRepository
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class OAuth2SuccessHandler(
    private val authorizedClientService: OAuth2AuthorizedClientService,
    private val realEstateAgentRepository: RealEstateAgentRepository,
    private val roleRepository: RoleRepository,
    private val userRepository: UserRepository,
    @Value("\${app.frontend-url}")
    private val frontendUrl: String,
) : SimpleUrlAuthenticationSuccessHandler() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
    ) {
        val oauthToken = authentication as OAuth2AuthenticationToken

        val email: String? = oauthToken.principal.getAttribute<String>("email")
        val name: String? = oauthToken.principal.getAttribute<String>("name")
        val emailVerified: Boolean? = oauthToken.principal.getAttribute<Boolean>("email_verified")

        val tokenValue: String =
            if (oauthToken.principal is OidcUser) {
                (oauthToken.principal as OidcUser).idToken.tokenValue
            } else {
                val client =
                    authorizedClientService.loadAuthorizedClient<OAuth2AuthorizedClient>(
                        oauthToken.authorizedClientRegistrationId,
                        oauthToken.name,
                    )
                // Spring Security 7 declara loadAuthorizedClient como @Nullable:
                // ausencia de authorized client e um estado legitimo, nao um bug.
                client?.accessToken?.tokenValue
                    ?: throw IllegalStateException("Authorized client ausente para ${oauthToken.name}")
            }

        if (email != null && emailVerified == true && !userRepository.existsByEmail(email)) {
            val role: Role =
                roleRepository
                    .findByName("REAL_ESTATE_AGENT")
                    .orElseThrow { RuntimeException("Role nao encontrada") }

            val agent =
                RealEstateAgent(
                    creci =
                        "GOOGLE-" +
                            UUID
                                .randomUUID()
                                .toString()
                                .substring(0, 8)
                                .uppercase(),
                ).apply {
                    this.name = name
                    this.email = email
                    setPassword("")
                    this.role = role
                    isActive = true
                }

            realEstateAgentRepository.save(agent)
            log.info("Novo usuario criado via Google: $email")
        }

        val tokenType = if (oauthToken.principal is OidcUser) "ID Token" else "Access Token"
        log.info("Login Google com sucesso! Email: $email | Token type: $tokenType")

        val redirectUrl = "$frontendUrl/auth/callback?token=$tokenValue"
        redirectStrategy.sendRedirect(request, response, redirectUrl)
    }
}
