package com.br.semesperanca.backendleads.configuration.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.stereotype.Component
import java.time.Instant
import javax.crypto.SecretKey

@Component
class CustomJwtDecoder(
    @Value("\${app.jwt.secret}")
    private val secret: String
) : JwtDecoder {

    private val googleDecoder: JwtDecoder = NimbusJwtDecoder
        .withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
        .build()

    @Throws(JwtException::class)
    override fun decode(token: String): Jwt {
        return try {
            println("Tentando decodificar token local...")
            val jwt = decodeLocalToken(token)
            println("Token local decodificado com sucesso!")
            jwt
        } catch (e: Exception) {
            println("Falha ao decodificar token local: ${e.message}")
            println("Tentando como token Google...")
            googleDecoder.decode(token)
        }
    }

    private fun decodeLocalToken(token: String): Jwt {
        val key: SecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))

        val claims: Claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload

        return Jwt.withTokenValue(token)
            .header("alg", "HS256")
            .subject(claims.subject)
            .issuedAt(claims.issuedAt.toInstant())
            .expiresAt(claims.expiration.toInstant())
            .claims { c ->
                claims.forEach { (k, v) ->
                    if (v is Long) {
                        c[k] = Instant.ofEpochSecond(v)
                    } else {
                        c[k] = v
                    }
                }
            }
            .build()
    }
}