package com.br.semesperanca.backendleads.configuration

import io.swagger.v3.oas.models.security.SecurityScheme
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SwaggerConfigTest {
    private val openApi = SwaggerConfig().openAPI()

    @Test
    fun `o documento se identifica com titulo, descricao e versao`() {
        assertThat(openApi.info.title).isEqualTo("Backend Leads API")
        assertThat(openApi.info.description).isEqualTo("API para gerenciamento de leads imobiliários")
        assertThat(openApi.info.version).isEqualTo("1.0.0")
    }

    @Test
    fun `o unico requisito de seguranca e o bearer`() {
        assertThat(openApi.security).hasSize(1)
        assertThat(openApi.security.first().containsKey("Bearer Authentication")).isTrue()
    }

    @Test
    fun `o esquema de seguranca e HTTP bearer com formato JWT`() {
        val scheme = requireNotNull(openApi.components.securitySchemes["Bearer Authentication"])

        assertThat(scheme.type).isEqualTo(SecurityScheme.Type.HTTP)
        assertThat(scheme.scheme).isEqualTo("bearer")
        assertThat(scheme.bearerFormat).isEqualTo("JWT")
        assertThat(scheme.description)
            .isEqualTo("Insira o token JWT obtido via /auth/login ou Google OAuth2")
    }
}
