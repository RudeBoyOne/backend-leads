package com.br.semesperanca.backendleads.configuration.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class OAuth2LoginSecurityConfig {

  @Bean
  @Order(1)
  public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
    http.securityMatcher("/api/**")
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/api/auth/**")
                    .permitAll()
                    .requestMatchers("/api/real-estate-agents/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated());

    return http.build();
  }

  @Bean
  @Order(2)
  public SecurityFilterChain oauth2FilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
        .oauth2Login(Customizer.withDefaults());

    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
