package com.br.semesperanca.backendleads.configuration.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

@Component
public class CustomJwtDecoder implements JwtDecoder {

  @Value("${app.jwt.secret}")
  private String secret;

  private final JwtDecoder googleDecoder =
      NimbusJwtDecoder.withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs").build();

  @Override
  public Jwt decode(String token) throws JwtException {
    try {
      System.out.println("Tentando decodificar token local...");
      Jwt jwt = decodeLocalToken(token);
      System.out.println("Token local decodificado com sucesso!");
      return jwt;
    } catch (Exception e) {
      System.out.println(
          "Falha ao decodificar token local: " + e.getMessage()); // 👈 adicione o erro
      System.out.println("Tentando como token Google...");
      return googleDecoder.decode(token);
    }
  }

  private Jwt decodeLocalToken(String token) {
    SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));

    Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();

    return Jwt.withTokenValue(token)
        .header("alg", "HS256")
        .subject(claims.getSubject())
        .issuedAt(claims.getIssuedAt().toInstant()) // 👈 converte Date para Instant
        .expiresAt(claims.getExpiration().toInstant()) // 👈 converte Date para Instant
        .claims(
            c -> {
              claims.forEach(
                  (k, v) -> {
                    if (v instanceof Long) {
                      c.put(k, Instant.ofEpochSecond((Long) v)); // 👈 converte Long para Instant
                    } else {
                      c.put(k, v);
                    }
                  });
            })
        .build();
  }
}
