package com.br.semesperanca.backendleads.configuration.security

import com.br.semesperanca.backendleads.domain.model.user.User
import com.br.semesperanca.backendleads.domain.repository.user.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class CustomJwtAuthenticationConverter(
    @Value("\${app.jwt.issuer}")
    private val appIssuer: String,
    private val userRepository: UserRepository,
) : Converter<Jwt, AbstractAuthenticationToken> {
    override fun convert(jwt: Jwt): AbstractAuthenticationToken = JwtAuthenticationToken(jwt, authoritiesOf(jwt))

    private fun authoritiesOf(jwt: Jwt): List<SimpleGrantedAuthority> {
        val user =
            if (jwt.issuer?.toString() == appIssuer) {
                val id =
                    runCatching { UUID.fromString(jwt.subject) }.getOrNull()
                        ?: throw BadCredentialsException("Token sem subject válido")
                userRepository.findById(id).orElseThrow { BadCredentialsException("Usuário inexistente") }
            } else {
                googleUser(jwt)
            } ?: return emptyList()

        if (!user.isActive) {
            throw BadCredentialsException("Usuario inativo")
        }

        val role = user.role?.name ?: return emptyList()
        val id = user.id ?: return emptyList()
        return listOf(
            SimpleGrantedAuthority("ROLE_$role"),
            SimpleGrantedAuthority("UID_$id"),
        )
    }

    private fun googleUser(jwt: Jwt): User? {
        val email = jwt.getClaimAsString("email")
        val emailVerified = jwt.claims["email_verified"] as? Boolean ?: false
        if (email == null || !emailVerified) return null
        return userRepository.findByEmail(email).orElse(null)
    }
}
