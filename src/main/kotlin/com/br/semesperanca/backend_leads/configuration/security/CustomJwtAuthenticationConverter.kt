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

/**
 * Converte o JWT em `Authentication` com as authorities que o escopo lê.
 *
 * Emite duas authorities:
 *
 * - `ROLE_<role>`, resolvida do banco e não do claim, para que uma promoção ou
 *   demissão valha no próximo request em vez de esperar o token expirar. O token
 *   local carrega `role` na claim, mas essa claim envelhece; confiar nela
 *   devolveria ao usuário o escopo que ele já perdeu.
 * - `UID_<id>`, o id do usuário **nosso**. O login Google usa o `sub` do Google,
 *   que não serve para nada aqui; sem esta authority o escopo não teria como
 *   saber de quem é a requisição sem bater no banco de novo.
 *
 * E-mail Google não cadastrado devolve token **sem** authority nenhuma, e a
 * camada de autorização responde 403. A versão anterior devolvia
 * `ROLE_REAL_ESTATE_AGENT` como padrão, o que transformava "conheço este e-mail"
 * em acesso de corretor para qualquer pessoa que respondesse por ele.
 */
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

    /**
     * `null` significa "e-mail não cadastrado", e `null` vira lista de
     * authorities vazia: a requisição morre em 403 em vez de virar corretor.
     */
    private fun googleUser(jwt: Jwt): User? {
        val email = jwt.getClaimAsString("email")
        // `email_verified` chega como booleano no id_token do Google; ler como
        // String devolveria null e rejeitaria todo login Google.
        val emailVerified = jwt.claims["email_verified"] as? Boolean ?: false
        if (email == null || !emailVerified) return null
        return userRepository.findByEmail(email).orElse(null)
    }
}
