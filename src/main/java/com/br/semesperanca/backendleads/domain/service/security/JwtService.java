package com.br.semesperanca.backendleads.domain.service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  @Value("${app.jwt.secret}")
  private String secret;

  @Value("${app.jwt.expiration}")
  private Long expiration;

  public String generateToken(UUID id, String email, String role) {
    return Jwts.builder()
        .subject(id.toString())
        .claim("role", role)
        .claim("email", email)
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + expiration))
        .signWith(getSecretKey())
        .compact();
  }

  public String extractEmail(String token) {
    return getClaims(token).get("email", String.class);
  }

  public String extractRole(String token) {
    return getClaims(token).get("role", String.class);
  }

  public boolean isTokenValid(String token) {
    try {
      getClaims(token);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private Claims getClaims(String token) {
    return Jwts.parser().verifyWith(getSecretKey()).build().parseSignedClaims(token).getPayload();
  }

  private SecretKey getSecretKey() {
    byte[] keyBytes = Decoders.BASE64.decode(secret);
    return Keys.hmacShaKeyFor(keyBytes);
  }
}
