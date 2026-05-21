package com.br.semesperanca.backendleads.configuration.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class OAuth2LoginSecurityConfig {

  private final CustomJwtDecoder customJwtDecoder;
  private final CustomJwtAuthenticationConverter customJwtAuthenticationConverter;
  private final OAuth2SuccessHandler oAuth2SuccessHandler;

  @Bean
  @Order(1)
  public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
    http.securityMatcher("/api/**")
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/api/auth/login").permitAll().anyRequest().authenticated())
        .oauth2ResourceServer(
            oauth2 ->
                oauth2.jwt(
                    jwt ->
                        jwt.decoder(customJwtDecoder)
                            .jwtAuthenticationConverter(customJwtAuthenticationConverter)));

    return http.build();
  }

  @Bean
  @Order(2)
  public SecurityFilterChain oauth2FilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
        .oauth2Login(oauth -> oauth.successHandler(oAuth2SuccessHandler));

    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
