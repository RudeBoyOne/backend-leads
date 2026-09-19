package com.br.semesperanca.backendleads.configuration.security

import com.nimbusds.jwt.SignedJWT
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.BadJwtException
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.stereotype.Component
import javax.crypto.SecretKey

@Component
class CustomJwtDecoder(
    @Value("\${app.jwt.secret}")
    private val secret: String,

    @Value("\${app.jwt.issuer}")
    private val appIssuer: String,

    @Value("\${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private val googleIssuer: String,

    @Value("\${spring.security.oauth2.client.registration.google.client-id}")
    private val googleClientId: String
) : JwtDecoder {

    private val localDecoder: NimbusJwtDecoder = NimbusJwtDecoder
        .withSecretKey(hmacKey())
        .macAlgorithm(MacAlgorithm.HS256)
        .build()
        .also { it.setJwtValidator(JwtValidators.createDefaultWithIssuer(appIssuer)) }

    private val googleDecoder: NimbusJwtDecoder = NimbusJwtDecoder
        .withJwkSetUri(GOOGLE_JWKS_URI)
        .build()
        .also {
            it.setJwtValidator(
                DelegatingOAuth2TokenValidator(
                    JwtValidators.createDefaultWithIssuer(googleIssuer),
                    googleAudienceValidator()
                )
            )
        }

    override fun decode(token: String): Jwt {
        val issuer = extractIssuer(token)
        return when (issuer) {
            appIssuer -> localDecoder.decode(token)
            googleIssuer -> googleDecoder.decode(token)
            else -> throw BadJwtException("Emissor JWT nao confiavel: ${issuer ?: "ausente/malformado"}")
        }
    }

    private fun hmacKey(): SecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))

    private fun extractIssuer(token: String): String? = runCatching {
        SignedJWT.parse(token).jwtClaimsSet.issuer
    }.getOrNull()

    private fun googleAudienceValidator(): OAuth2TokenValidator<Jwt> =
        OAuth2TokenValidator { jwt ->
            val valid = when (val aud = jwt.getClaim<Any>("aud")) {
                is String -> aud == googleClientId
                is Collection<*> -> aud.contains(googleClientId)
                else -> false
            }
            if (valid) {
                OAuth2TokenValidatorResult.success()
            } else {
                OAuth2TokenValidatorResult.failure(
                    OAuth2Error("invalid_token", "Audiencia invalida", null)
                )
            }
        }

    companion object {
        private const val GOOGLE_JWKS_URI = "https://www.googleapis.com/oauth2/v3/certs"
    }
}