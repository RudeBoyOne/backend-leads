package com.br.semesperanca.backendleads.configuration.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.InsufficientAuthenticationException
import tools.jackson.databind.ObjectMapper

/**
 * Os dois handlers escrevem o corpo com o `ObjectMapper` cru e devolvem o
 * `LocalDateTime.now()` junto — entao o que se trava aqui e o status, o
 * content-type e os campos, nao a string exata.
 */
class SecurityErrorHandlersTest {
    private val request = MockHttpServletRequest("GET", "/properties")
    private val objectMapper = ObjectMapper()

    @Suppress("UNCHECKED_CAST")
    private fun body(response: MockHttpServletResponse): Map<String, Any> =
        objectMapper.readValue(response.contentAsString, Map::class.java) as Map<String, Any>

    @Test
    fun `entry point responde 401 com o JSON de token invalido`() {
        val response = MockHttpServletResponse()

        CustomAuthenticationEntryPoint().commence(request, response, InsufficientAuthenticationException("nope"))

        assertThat(response.status).isEqualTo(401)
        assertThat(response.contentType).isEqualTo("application/json")
        assertThat(body(response))
            .containsEntry("status", 401)
            .containsEntry("error", "Unauthorized")
            .containsEntry("message", "Token inválido ou expirado")
        assertThat(body(response)["timestamp"].toString()).isNotBlank()
    }

    @Test
    fun `access denied handler responde 403 com o JSON de acesso negado`() {
        val response = MockHttpServletResponse()

        CustomAccessDeniedHandler().handle(request, response, AccessDeniedException("nope"))

        assertThat(response.status).isEqualTo(403)
        assertThat(response.contentType).isEqualTo("application/json")
        assertThat(body(response))
            .containsEntry("status", 403)
            .containsEntry("error", "Forbidden")
            .containsEntry("message", "Acesso negado — você não tem permissão para acessar este recurso")
        assertThat(body(response)["timestamp"].toString()).isNotBlank()
    }
}
