package com.br.semesperanca.backendleads.domain.security

import com.br.semesperanca.backendleads.domain.model.Role
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.oauth2.jwt.Jwt
import java.util.UUID

object PrincipalResolver {
    private const val UID_PREFIX = "UID_"
    private const val ROLE_PREFIX = "ROLE_"

    fun currentOrNull(): Principal? {
        val authentication: Authentication =
            SecurityContextHolder.getContext().authentication
                ?: return null

        if (!authentication.isAuthenticated) return null

        val id = uidOf(authentication) ?: return null
        val role =
            authentication.authorities
                .firstOrNull { it.authority.startsWith(ROLE_PREFIX) }
                ?.authority
                ?.removePrefix(ROLE_PREFIX)
                ?: return null

        return Principal(id = id, role = role)
    }

    fun current(): Principal = currentOrNull() ?: throw NotAuthenticatedException()

    private fun uidOf(authentication: Authentication): UUID? {
        authentication.authorities
            .firstOrNull { it.authority.startsWith(UID_PREFIX) }
            ?.authority
            ?.removePrefix(UID_PREFIX)
            ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?.let { return it }

        val subject =
            when (val principal = authentication.principal) {
                is Jwt -> principal.subject
                is OidcUser -> null
                is String -> principal
                else -> null
            } ?: return null

        return runCatching { UUID.fromString(subject) }.getOrNull()
    }
}

data class Principal(
    val id: UUID,
    val role: String,
) {
    val isFullAdmin: Boolean get() = role == Role.FULL_ADMIN
    val isBranchOwner: Boolean get() = role == Role.ADMIN
    val isManager: Boolean get() = role == Role.MANAGER

    val isAgent: Boolean get() = role == Role.REAL_ESTATE_AGENT || role == "TEAM_AGENT"
}

class NotAuthenticatedException : RuntimeException("Não autenticado")
