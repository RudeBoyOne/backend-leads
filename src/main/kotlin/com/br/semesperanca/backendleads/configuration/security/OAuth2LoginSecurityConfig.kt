package com.br.semesperanca.backendleads.configuration.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class OAuth2LoginSecurityConfig(
    private val customJwtDecoder: CustomJwtDecoder,
    private val customJwtAuthenticationConverter: CustomJwtAuthenticationConverter,
    private val oAuth2SuccessHandler: OAuth2SuccessHandler,
    private val customAuthenticationEntryPoint: CustomAuthenticationEntryPoint,
    private val customAccessDeniedHandler: CustomAccessDeniedHandler
) {

    /**
     * Chain do fluxo OAuth2 (browser): apenas os endpoints do login social.
     * Matcher estreito — nao participa da protecao das rotas da aplicacao.
     */
    @Bean
    @Order(1)
    @Throws(Exception::class)
    fun oauth2FilterChain(http: HttpSecurity): SecurityFilterChain {
        http.securityMatcher("/oauth2/**", "/login/**", "/error")
            .authorizeHttpRequests { auth -> auth.anyRequest().permitAll() }
            .oauth2Login { oauth -> oauth.successHandler(oAuth2SuccessHandler) }

        return http.build()
    }

    /**
     * Chain da API: catch-all (sem securityMatcher) — toda rota nova
     * ja nasce protegida por JWT, sem manutencao por rota.
     */
    @Bean
    @Order(2)
    @Throws(Exception::class)
    fun apiFilterChain(http: HttpSecurity): SecurityFilterChain {
        http.cors(Customizer.withDefaults())
            .csrf { csrf -> csrf.disable() }
            .authorizeHttpRequests { auth ->
                auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                    .requestMatchers("/actuator/health/**").permitAll()
                    .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/error").permitAll()
                    .requestMatchers(HttpMethod.POST, "/auth/register/full-admin")
                    .hasAnyRole("FULL_ADMIN")
                    .requestMatchers(HttpMethod.GET, "/real-estate-agents")
                    .hasAnyRole("FULL_ADMIN", "ADMIN")
                    .requestMatchers(HttpMethod.POST, "/real-estate-agents")
                    .hasAnyRole("FULL_ADMIN", "ADMIN")
                    .requestMatchers(HttpMethod.GET, "/real-estate-agents/{id}")
                    .hasAnyRole("FULL_ADMIN", "ADMIN", "REAL_ESTATE_AGENT")
                    .requestMatchers(HttpMethod.PUT, "/real-estate-agents/{id}")
                    .hasAnyRole("FULL_ADMIN", "ADMIN")
                    .requestMatchers(HttpMethod.PATCH, "/real-estate-agents/{id}/toggle-active")
                    .hasAnyRole("FULL_ADMIN", "ADMIN")
                    .requestMatchers(HttpMethod.GET, "/managers")
                    .hasAnyRole("FULL_ADMIN", "ADMIN")
                    .requestMatchers(HttpMethod.POST, "/managers")
                    .hasAnyRole("FULL_ADMIN", "ADMIN")
                    .requestMatchers(HttpMethod.GET, "/managers/{id}")
                    .hasAnyRole("FULL_ADMIN", "ADMIN", "MANAGER")
                    .requestMatchers(HttpMethod.PUT, "/managers/{id}")
                    .hasAnyRole("FULL_ADMIN", "ADMIN", "MANAGER")
                    .requestMatchers(HttpMethod.PATCH, "/managers/{id}/toggle-active")
                    .hasAnyRole("FULL_ADMIN", "ADMIN")
                    .requestMatchers(HttpMethod.PATCH, "/real-estate-agents/{id}/reset-password")
                    .hasAnyRole("FULL_ADMIN", "ADMIN")
                    .requestMatchers(HttpMethod.PATCH, "/managers/{id}/reset-password")
                    .hasAnyRole("FULL_ADMIN", "ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/real-estate-agents/{id}")
                    .hasAnyRole("FULL_ADMIN", "ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/managers/{id}")
                    .hasAnyRole("FULL_ADMIN", "ADMIN")
                    .requestMatchers(HttpMethod.PATCH, "/auth/change-password")
                    .hasAnyRole("FULL_ADMIN", "ADMIN", "REAL_ESTATE_AGENT", "MANAGER")
                    .anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2
                    .jwt { jwt ->
                        jwt.decoder(customJwtDecoder)
                            .jwtAuthenticationConverter(customJwtAuthenticationConverter)
                    }
                    .authenticationEntryPoint(customAuthenticationEntryPoint)
            }
            .exceptionHandling { ex -> ex.accessDeniedHandler(customAccessDeniedHandler) }

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowCredentials = true
        configuration.addAllowedHeader("*")
        configuration.addAllowedMethod("*")
        configuration.addAllowedOriginPattern("*")
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}