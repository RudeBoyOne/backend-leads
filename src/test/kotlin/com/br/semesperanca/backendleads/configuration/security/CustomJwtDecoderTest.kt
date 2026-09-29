package com.br.semesperanca.backendleads.configuration.security

import io.jsonwebtoken.Jwts
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.jwt.BadJwtException
import org.springframework.security.oauth2.jwt.Jwt
import java.lang.reflect.Method
import java.util.Date

/**
 * `CustomJwtDecoder` escolhe o decoder pelo `iss` do token, entao os testes
 * fabricam JWTs reais com o jjwt em vez de mockar `NimbusJwtDecoder`.
 *
 * O ramo `googleDecoder` chama a JWKS da Google e portanto exige rede — fica de
 * fora. Em compensacao a `googleAudienceValidator`, que e a logica_decisoria de
 * verdade desse ramo, e exercitada por reflexao no fim do arquivo.
 */
class CustomJwtDecoderTest {
    private val appIssuer = "http://backend-leads.local"
    private val googleIssuer = "https://accounts.google.com"
    private val clientId = "test-client-id.apps.googleusercontent.com"

    private fun decoder() =
        CustomJwtDecoder(
            secret = SECRET,
            appIssuer = appIssuer,
            googleIssuer = googleIssuer,
            googleClientId = clientId,
        )

    private fun localToken(
        issuer: String = appIssuer,
        subject: String = "11111111-1111-1111-1111-111111111111",
    ): String =
        Jwts
            .builder()
            .issuer(issuer)
            .subject(subject)
            .claim("role", "ADMIN")
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + 60_000))
            .signWith(hmacKey(), Jwts.SIG.HS256)
            .compact()

    @Test
    fun `decodifica token emitido pela propria aplicacao`() {
        val token = localToken(subject = "22222222-2222-2222-2222-222222222222")

        val jwt = decoder().decode(token)

        assertThat(jwt.subject).isEqualTo("22222222-2222-2222-2222-222222222222")
        assertThat(jwt.getClaimAsString("role")).isEqualTo("ADMIN")
        assertThat(jwt.issuer.toString()).isEqualTo(appIssuer)
    }

    /** O validador de timestamp tolera 60s de clock skew, entao o token tem de estar bem vencido. */
    @Test
    fun `recusa token expirado com BadJwtException`() {
        val expired =
            Jwts
                .builder()
                .issuer(appIssuer)
                .subject("expirado")
                .issuedAt(Date(System.currentTimeMillis() - 180_000))
                .expiration(Date(System.currentTimeMillis() - 120_000))
                .signWith(hmacKey(), Jwts.SIG.HS256)
                .compact()

        assertThatThrownBy { decoder().decode(expired) }
            .isInstanceOf(BadJwtException::class.java)
    }

    @Test
    fun `recusa token de emissor desconhecido`() {
        val token = localToken(issuer = "https://evil.example")

        assertThatThrownBy { decoder().decode(token) }
            .isInstanceOf(BadJwtException::class.java)
            .hasMessageContaining("Emissor JWT nao confiavel")
            .hasMessageContaining("https://evil.example")
    }

    @Test
    fun `recusa token sem issuer e cita a causa ausente malformado`() {
        val token =
            Jwts
                .builder()
                .subject("sem-issuer")
                .issuedAt(Date())
                .expiration(Date(System.currentTimeMillis() + 60_000))
                .signWith(hmacKey(), Jwts.SIG.HS256)
                .compact()

        assertThatThrownBy { decoder().decode(token) }
            .isInstanceOf(BadJwtException::class.java)
            .hasMessageContaining("ausente/malformado")
    }

    @Test
    fun `token que nao e um JWT cai no mesmo caminho de emissor nao confiavel`() {
        assertThatThrownBy { decoder().decode("isto-nao-e-um-jwt") }
            .isInstanceOf(BadJwtException::class.java)
            .hasMessageContaining("ausente/malformado")
    }

    /**
     * `googleAudienceValidator` e `private` e so alcancavel pelo `googleDecoder`,
     * que buscaria a JWKS da Google na rede. O que o teste quer travar e a tabela
     * de decisao: `aud` como texto unico, como lista, ou ausente.
     */
    @Test
    fun `audience validator aceita aud como texto unico igual ao client id`() {
        val result = googleAudienceValidator().validate(jwtWithAudience(clientId))

        assertThat(result.hasErrors()).isFalse()
    }

    @Test
    fun `audience validator aceita aud como lista que contem o client id`() {
        val result = googleAudienceValidator().validate(jwtWithAudience(listOf("outro", clientId)))

        assertThat(result.hasErrors()).isFalse()
    }

    @Test
    fun `audience validator recusa aud que nao contem o client id`() {
        val result = googleAudienceValidator().validate(jwtWithAudience("client-id-de-outro"))

        assertThat(result.hasErrors()).isTrue()
        assertThat(result.errors).singleElement().satisfies({ error ->
            assertThat(error.errorCode).isEqualTo("invalid_token")
            assertThat(error.description).isEqualTo("Audiencia invalida")
        })
    }

    @Test
    fun `audience validator recusa token sem claim aud`() {
        val result = googleAudienceValidator().validate(jwtWithAudience(null))

        assertThat(result.hasErrors()).isTrue()
    }

    @Suppress("UNCHECKED_CAST")
    private fun googleAudienceValidator(): OAuth2TokenValidator<Jwt> {
        val method: Method = CustomJwtDecoder::class.java.getDeclaredMethod("googleAudienceValidator")
        method.isAccessible = true
        return method.invoke(decoder()) as OAuth2TokenValidator<Jwt>
    }

    private fun jwtWithAudience(audience: Any?): Jwt {
        val builder =
            Jwt
                .withTokenValue("token")
                .header("alg", "RS256")
                .subject("sub")
        if (audience is Collection<*>) {
            builder.audience(audience.filterIsInstance<String>())
        } else if (audience != null) {
            builder.audience(listOf(audience.toString()))
        }
        return builder.build()
    }

    private companion object {
        /** 32 bytes em Base64 — o mesmo valor do `application-test.yml`. */
        const val SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="

        fun hmacKey() =
            io.jsonwebtoken.security.Keys
                .hmacShaKeyFor(
                    io.jsonwebtoken.io.Decoders.BASE64
                        .decode(SECRET),
                )
    }
}
