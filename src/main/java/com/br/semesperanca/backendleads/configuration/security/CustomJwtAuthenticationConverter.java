package com.br.semesperanca.backendleads.configuration.security;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class CustomJwtAuthenticationConverter
    implements Converter<Jwt, AbstractAuthenticationToken> {

  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    Collection<SimpleGrantedAuthority> authorities = extractAuthorities(jwt);
    return new JwtAuthenticationToken(jwt, authorities);
  }

  private Collection<SimpleGrantedAuthority> extractAuthorities(Jwt jwt) {
    String issuer = jwt.getClaimAsString("iss");
    if (issuer != null && issuer.contains("accounts.google.com")) {
      return List.of(new SimpleGrantedAuthority("ROLE_REAL_ESTATE_AGENT"));
    }

    String role = jwt.getClaimAsString("role");
    if (role != null) {
      return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    String email = jwt.getClaimAsString("email");
    if (email != null) {
      return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    return Collections.emptyList();
  }
}
