package com.br.semesperanca.backendleads.support

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import java.time.Duration
import java.time.Instant
import java.util.Date
import javax.crypto.SecretKey

/**
 * Emissor de JWT para os testes que sobem a cadeia de segurança **real** — os
 * slices de controller rodam com `addFilters = false` e usam
 * [authenticatedAsJwt], que não passa pelo [com.br.semesperanca.backendleads.configuration.security.CustomJwtDecoder].
 *
 * Aqui é o contrário: o token é assinado de verdade com o mesmo segredo HS256
 * do `application-test.yml` e com o mesmo `iss` do `app.jwt.issuer`, então
 * `CustomJwtDecoder` cai no `localDecoder` (o do app), o
 * `JwtValidator` de issuer passa, e o
 * `CustomJwtAuthenticationConverter` lê a claim `role`. O que o teste exercita
 * de fato é a cadeia, não um atalho de autenticação.
 */
object TestJwt {
    /** `app.jwt.issuer` do `application.yml`: `${JWT_ISSUER:http://backend-leads.local}`. */
    const val ISSUER: String = "http://backend-leads.local"

    /** `JWT_SECRET` do `application-test.yml` — 32 bytes em Base64, o mínimo do HS256. */
    const val SECRET_BASE64: String = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="

    private val defaultKey: SecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET_BASE64))

    /** Uma chave de 32 bytes diferente — serve para testar assinatura errada. */
    val outraChave: SecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode("QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVowMTIzNDU="))

    /**
     * @param role claim `role`; `null` gera um token sem papel, que o
     *   `CustomJwtAuthenticationConverter` traduz em lista de autoridades vazia.
     * @param issuer `iss`; um valor fora de [ISSUER] e de
     *   `OAUTH2_ISSUER_URI` faz o `CustomJwtDecoder` estourar
     *   `BadJwtException` antes de qualquer validação.
     */
    fun token(
        role: String?,
        subject: String = "3f8c1b62-9d0e-4a51-b0f2-6c1a5d7e9b34",
        email: String = "user@example.com",
        issuer: String = ISSUER,
        expiresIn: Duration = Duration.ofHours(1),
        key: SecretKey = defaultKey,
    ): String {
        val agora = Instant.now()
        val builder =
            Jwts
                .builder()
                .subject(subject)
                .issuer(issuer)
                .claim("email", email)
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(expiresIn)))
                .signWith(key)

        if (role != null) {
            builder.claim("role", role)
        }
        return builder.compact()
    }

    fun bearer(
        role: String?,
        email: String = "user@example.com",
        issuer: String = ISSUER,
        expiresIn: Duration = Duration.ofHours(1),
        key: SecretKey = defaultKey,
    ): String = "Bearer " + token(role, email = email, issuer = issuer, expiresIn = expiresIn, key = key)
}
