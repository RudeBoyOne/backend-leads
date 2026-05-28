package com.br.semesperanca.backendleads.configuration.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class OAuth2LoginSecurityConfig {

  private final CustomJwtDecoder customJwtDecoder;
  private final CustomJwtAuthenticationConverter customJwtAuthenticationConverter;
  private final OAuth2SuccessHandler oAuth2SuccessHandler;
  private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
  private final CustomAccessDeniedHandler customAccessDeniedHandler;

  @Bean
  @Order(1)
  public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
    http.securityMatcher("/api/**", "/swagger-ui/**", "/v3/api-docs/**")
        .cors(Customizer.withDefaults())
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(HttpMethod.OPTIONS, "/api/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/login")
                    .permitAll()
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/register/full-admin")
                    .hasAnyRole("FULL_ADMIN")
                    .requestMatchers(HttpMethod.GET, "/api/real-estate-agents")
                    .hasAnyRole("FULL_ADMIN", "ADMIN")
                    .requestMatchers(HttpMethod.POST, "/api/real-estate-agents")
                    .hasAnyRole("FULL_ADMIN", "ADMIN")
                    .requestMatchers(HttpMethod.GET, "/api/real-estate-agents/{id}")
                    .hasAnyRole("FULL_ADMIN", "ADMIN", "REAL_ESTATE_AGENT")
                    .requestMatchers(HttpMethod.PUT, "/api/real-estate-agents/{id}")
                    .hasAnyRole("FULL_ADMIN", "ADMIN")
                    .requestMatchers(HttpMethod.PATCH, "/api/real-estate-agents/{id}/toggle-active")
                    .hasAnyRole("FULL_ADMIN", "ADMIN")
                    .requestMatchers(HttpMethod.GET, "/api/managers")
                    .hasAnyRole("FULL_ADMIN", "ADMIN")
                    .requestMatchers(HttpMethod.POST, "/api/managers")
                    .hasAnyRole("FULL_ADMIN", "ADMIN")
                    .requestMatchers(HttpMethod.GET, "/api/managers/{id}")
                    .hasAnyRole("FULL_ADMIN", "ADMIN", "MANAGER")
                    .requestMatchers(HttpMethod.PUT, "/api/managers/{id}")
                    .hasAnyRole("FULL_ADMIN", "ADMIN", "MANAGER")
                    .requestMatchers(HttpMethod.PATCH, "/api/managers/{id}/toggle-active")
                    .hasAnyRole("FULL_ADMIN", "ADMIN")
                    .requestMatchers(
                        HttpMethod.PATCH, "/api/real-estate-agents/{id}/reset-password")
                    .hasAnyRole("FULL_ADMIN", "ADMIN")
                    .requestMatchers(HttpMethod.PATCH, "/api/managers/{id}/reset-password")
                    .hasAnyRole("FULL_ADMIN", "ADMIN")
                    .requestMatchers(HttpMethod.PATCH, "/api/auth/change-password")
                    .hasAnyRole("FULL_ADMIN", "ADMIN", "REAL_ESTATE_AGENT", "MANAGER")
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            oauth2 ->
                oauth2
                    .jwt(
                        jwt ->
                            jwt.decoder(customJwtDecoder)
                                .jwtAuthenticationConverter(customJwtAuthenticationConverter))
                    .authenticationEntryPoint(customAuthenticationEntryPoint))
        .exceptionHandling(ex -> ex.accessDeniedHandler(customAccessDeniedHandler));

    return http.build();
  }

  @Bean
  @Order(2)
  public SecurityFilterChain oauth2FilterChain(HttpSecurity http) throws Exception {
    http.cors(Customizer.withDefaults())
        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
        .oauth2Login(oauth -> oauth.successHandler(oAuth2SuccessHandler));

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowCredentials(true);
    configuration.addAllowedHeader("*");
    configuration.addAllowedMethod("*");
    configuration.addAllowedOriginPattern("*");
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
