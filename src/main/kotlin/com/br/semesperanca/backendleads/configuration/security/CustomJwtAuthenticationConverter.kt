package com.br.semesperanca.backendleads.configuration.security

import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component

@Component
class CustomJwtAuthenticationConverter : Converter<Jwt, AbstractAuthenticationToken> {

    override fun convert(jwt: Jwt): AbstractAuthenticationToken {
        val authorities = extractAuthorities(jwt)
        return JwtAuthenticationToken(jwt, authorities)
    }

    private fun extractAuthorities(jwt: Jwt): Collection<SimpleGrantedAuthority> {
        val issuer = jwt.getClaimAsString("iss")
        if (issuer != null && issuer.contains("accounts.google.com")) {
            return listOf(SimpleGrantedAuthority("ROLE_REAL_ESTATE_AGENT"))
        }

        val role = jwt.getClaimAsString("role")
        if (role != null) {
            return listOf(SimpleGrantedAuthority("ROLE_$role"))
        }

        val email = jwt.getClaimAsString("email")
        if (email != null) {
            return listOf(SimpleGrantedAuthority("ROLE_USER"))
        }

        return emptyList()
    }
}