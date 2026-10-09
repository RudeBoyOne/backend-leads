package com.br.semesperanca.backendleads.domain.security

import com.br.semesperanca.backendleads.domain.model.Role
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.oauth2.jwt.Jwt
import java.util.UUID

/**
 * Quem é o principal autenticado, e a que role ele tem.
 *
 * **Não tem nenhuma dependência injetada, de propósito.** O
 * [com.br.semesperanca.backendleads.domain.repository.ScopedRepositoryImpl] é a
 * classe base de todos os repositórios e o Spring Data só consegue construí-la
 * com `(JpaEntityInformation, EntityManager)`. Se este objeto precisasse de
 * `UserRepository` para descobrir quem é o usuário, a base deixaria de ser
 * instanciável e o escopo inteiro cairia junto.
 *
 * A informação vem da authority `UID_<id>`, que o
 * [com.br.semesperanca.backendleads.configuration.security.CustomJwtAuthenticationConverter]
 * emite — ele já carrega o `User` para resolver a role, então o id sai de graça.
 * Não há claim novo, coluna discriminadora nem lookup extra por requisição.
 */
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

    /**
     * Falha fechada. Um repositório chamado sem autenticação não pode cair para
     * "sem escopo", porque aí a ausência de identity vira tabela inteira — o
     * oposto do que o escopo existe para impedir.
     */
    fun current(): Principal = currentOrNull() ?: throw NotAuthenticatedException()

    /**
     * Ordem: `UID_` primeiro, porque o login Google usa o `sub` do Google, que não
     * é o nosso id. Só o login local tem `sub` = id do usuário, porque o
     * `JwtService` grava `subject(id)` ao emitir o token.
     */
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

/**
 * O usuário autenticado, reduzido ao que o escopo precisa.
 *
 * A lotação (`branchOfficeId`) **não** mora aqui de propósito: cacheá-la no
 * principal a tornaria desatualizada (o gerente pode ser transferido de filial
 * com a sessão aberta) e, pior, obrigaria a base do repositório a consultar o
 * banco. O escopo resolve a lotação em subquery, no mesmo instante da consulta.
 */
data class Principal(
    val id: UUID,
    val role: String,
) {
    val isFullAdmin: Boolean get() = role == Role.FULL_ADMIN
    val isBranchOwner: Boolean get() = role == Role.ADMIN
    val isManager: Boolean get() = role == Role.MANAGER

    /**
     * Corretor. `TEAM_AGENT` é a mesma pessoa vista pelo time, então compartilha
     * o escopo de corretor em vez de ganhar um terceiro quase idêntico.
     */
    val isAgent: Boolean get() = role == Role.REAL_ESTATE_AGENT || role == "TEAM_AGENT"
}

class NotAuthenticatedException : RuntimeException("Não autenticado")
