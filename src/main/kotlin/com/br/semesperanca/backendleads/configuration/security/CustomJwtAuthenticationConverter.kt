package com.br.semesperanca.backendleads.configuration.security

import com.br.semesperanca.backendleads.domain.repository.user.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component

@Component
class CustomJwtAuthenticationConverter(
    @Value("\${app.jwt.issuer}")
    private val appIssuer: String,

    private val userRepository: UserRepository
) : Converter<Jwt, AbstractAuthenticationToken> {

    override fun convert(jwt: Jwt): AbstractAuthenticationToken =
        JwtAuthenticationToken(jwt, resolveAuthorities(jwt))

    private fun resolveAuthorities(jwt: Jwt): List<SimpleGrantedAuthority> {
        if (jwt.issuer?.toString() == appIssuer) {
            val role = jwt.getClaimAsString("role")
            return role?.let { listOf(SimpleGrantedAuthority("ROLE_$it")) } ?: emptyList()
        }
        return googleAuthorities(jwt)
    }

    private fun googleAuthorities(jwt: Jwt): List<SimpleGrantedAuthority> {
        val email = jwt.getClaimAsString("email")
        val emailVerified = jwt.getClaimAsBoolean("email_verified") ?: false

        if (email != null && emailVerified) {
            val user = userRepository.findByEmail(email).orElse(null)
            if (user != null) {
                if (!user.isActive) {
                    throw BadCredentialsException("Usuario inativo")
                }
                user.role?.name?.let { return listOf(SimpleGrantedAuthority("ROLE_$it")) }
            }
        }

        return listOf(SimpleGrantedAuthority("ROLE_REAL_ESTATE_AGENT"))
    }
}