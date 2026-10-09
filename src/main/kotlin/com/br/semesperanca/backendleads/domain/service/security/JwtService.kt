package com.br.semesperanca.backendleads.domain.service.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value("\${app.jwt.secret}")
    private val secret: String,

    @Value("\${app.jwt.expiration}")
    private val expiration: Long,

    @Value("\${app.jwt.issuer}")
    private val issuer: String
) {
    fun generateToken(id: UUID, email: String?, role: String?): String {
        return Jwts.builder()
            .issuer(issuer)
            .subject(id.toString())
            .claim("role", role)
            .claim("email", email)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + expiration))
            .signWith(getSecretKey(), Jwts.SIG.HS256)
            .compact()
    }

    fun extractEmail(token: String): String? = getClaims(token).get("email", String::class.java)

    fun extractRole(token: String): String? = getClaims(token).get("role", String::class.java)

    fun isTokenValid(token: String): Boolean {
        return try {
            getClaims(token)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun getClaims(token: String): Claims {
        return Jwts.parser().verifyWith(getSecretKey()).build().parseSignedClaims(token).payload
    }

    private fun getSecretKey(): SecretKey {
        val keyBytes = Decoders.BASE64.decode(secret)
        return Keys.hmacShaKeyFor(keyBytes)
    }
}