package com.br.semesperanca.backendleads.support

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.test.web.servlet.request.RequestPostProcessor

/**
 * Autentica a requisição com um principal `Jwt` sem ligar a cadeia de filtros.
 *
 * O caminho idiomático seria `SecurityMockMvcRequestPostProcessors.authentication(...)`,
 * mas ele grava o contexto num `SecurityContextRepository` de request — quem o
 * lê de volta é o `SecurityContextHolderFilter`. Nos slices de controller os
 * filtros ficam desligados (`addFilters = false`), então o
 * `@AuthenticationPrincipal Jwt` chegaria nulo e o controller estouraria.
 *
 * Aqui o `SecurityContextHolder` é preenchido direto. O vazamento para a thread
 * do JUnit é inevitável, então quem usar deve limpar com `clearSecurityContext()`.
 */
fun authenticatedAsJwt(
    email: String? = null,
    vararg extraClaims: Pair<String, Any>,
): RequestPostProcessor {
    val claims =
        buildList {
            add("sub" to "1234567890")
            if (email != null) add("email" to email)
            addAll(extraClaims)
        }

    val jwt =
        Jwt
            .withTokenValue("header.payload.signature")
            .header("alg", "RS256")
            .claims { map -> claims.forEach { (name, value) -> map[name] = value } }
            .build()

    return RequestPostProcessor { request ->
        val context = SecurityContextHolder.createEmptyContext()
        context.authentication = JwtAuthenticationToken(jwt)
        SecurityContextHolder.setContext(context)
        request
    }
}

fun clearSecurityContext() = SecurityContextHolder.clearContext()
