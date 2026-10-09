package com.br.semesperanca.backendleads.domain.service.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.Base64
import java.util.Date
import java.util.UUID

class JwtServiceTest {
    private val userId: UUID = UUID.fromString("22222222-2222-2222-2222-222222222222")

    /** 32 bytes em Base64 — o minimo que o HS256 aceita. */
    private val secret: String = Base64.getEncoder().encodeToString(ByteArray(32) { it.toByte() })

    private fun newService(
        secret: String = this.secret,
        expiration: Long = 86_400_000L,
        issuer: String = ISSUER,
    ) = JwtService(secret, expiration, issuer)

    private fun parse(token: String) =
        Jwts
            .parser()
            .verifyWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret)))
            .build()
            .parseSignedClaims(token)

    @Test
    fun `generateToken assina com HS256 e carrega os claims de negocio`() {
        val token = newService().generateToken(userId, "maria@example.com", "MANAGER")

        val claims = parse(token).payload
        assertThat(claims.issuer).isEqualTo(ISSUER)
        assertThat(claims.subject).isEqualTo(userId.toString())
        assertThat(claims.get("role", String::class.java)).isEqualTo("MANAGER")
        assertThat(claims.get("email", String::class.java)).isEqualTo("maria@example.com")
        assertThat(claims.issuedAt).isNotNull()
        assertThat(claims.expiration).isNotNull()
    }

    @Test
    fun `o header do token declara o algoritmo HS256`() {
        val token = newService().generateToken(userId, "a@b.com", null)

        assertThat(parse(token).header.algorithm).isEqualTo("HS256")
    }

    @Test
    fun `expira depois do prazo configurado`() {
        val token = newService(expiration = 60_000L).generateToken(userId, "a@b.com", "MANAGER")

        val expiration = parse(token).payload.expiration.time

        val expected = Date(System.currentTimeMillis() + 60_000L).time
        // folga de 10s cobre o tempo entre gerar o token e ler o claim
        assertThat(expiration).isBetween(expected - 10_000L, expected + 10_000L)
    }

    @Test
    fun `generateToken aceita e-mail e role nulos`() {
        val service = newService()

        val token = service.generateToken(userId, null, null)

        assertThat(service.extractEmail(token)).isNull()
        assertThat(service.extractRole(token)).isNull()
    }

    @Test
    fun `extractEmail devolve o claim email`() {
        val service = newService()

        val token = service.generateToken(userId, "ana@example.com", null)

        assertThat(service.extractEmail(token)).isEqualTo("ana@example.com")
    }

    @Test
    fun `extractRole devolve o claim role`() {
        val service = newService()

        val token = service.generateToken(userId, null, "FULL_ADMIN")

        assertThat(service.extractRole(token)).isEqualTo("FULL_ADMIN")
    }

    @Test
    fun `isTokenValid aceita um token recem-gerado`() {
        val service = newService()

        val token = service.generateToken(userId, "a@b.com", "MANAGER")

        assertThat(service.isTokenValid(token)).isTrue()
    }

    @Test
    fun `isTokenValid rejeita token assinado com outra chave`() {
        val otherSecret = Base64.getEncoder().encodeToString(ByteArray(32) { 7 })
        val token = newService(secret = otherSecret).generateToken(userId, "a@b.com", "MANAGER")

        assertThat(newService().isTokenValid(token)).isFalse()
    }

    @Test
    fun `isTokenValid rejeita token expirado`() {
        val expired =
            Jwts
                .builder()
                .issuer(ISSUER)
                .subject(userId.toString())
                .issuedAt(Date(System.currentTimeMillis() - 7_200_000L))
                .expiration(Date(System.currentTimeMillis() - 3_600_000L))
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret)), Jwts.SIG.HS256)
                .compact()

        assertThat(newService().isTokenValid(expired)).isFalse()
    }

    @Test
    fun `isTokenValid rejeita texto que nao e JWT`() {
        assertThat(newService().isTokenValid("nao-e-um-jwt")).isFalse()
    }

    @Test
    fun `isTokenValid rejeita token vazio`() {
        assertThat(newService().isTokenValid("")).isFalse()
    }

    @Test
    fun `isTokenValid rejeita quando o secret configurado nao e Base64 valido`() {
        val token = newService().generateToken(userId, "a@b.com", "MANAGER")

        assertThat(newService(secret = "%%%-nao-base64%%").isTokenValid(token)).isFalse()
    }

    private companion object {
        const val ISSUER = "http://backend-leads.local"
    }
}
