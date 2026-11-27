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
    private val customAccessDeniedHandler: CustomAccessDeniedHandler,
) {
    @Bean
    @Order(1)
    @Throws(Exception::class)
    fun oauth2FilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .securityMatcher("/oauth2/**", "/login/**", "/error")
            .authorizeHttpRequests { auth -> auth.anyRequest().permitAll() }
            .oauth2Login { oauth -> oauth.successHandler(oAuth2SuccessHandler) }

        return http.build()
    }

    @Bean
    @Order(2)
    @Throws(Exception::class)
    fun apiFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors(Customizer.withDefaults())
            .csrf { csrf -> csrf.disable() }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/auth/login")
                    .permitAll()
                    .requestMatchers("/actuator/health/**")
                    .permitAll()
                    .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/error")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/auth/register/full-admin")
                    .hasRole("FULL_ADMIN")
                    .requestMatchers("/branch-owners/**")
                    .hasAnyRole("FULL_ADMIN", "ADMIN")
                    .requestMatchers(HttpMethod.PATCH, "/auth/change-password")
                    .authenticated()
                    .requestMatchers(HttpMethod.GET, "/real-estate-agencies/**")
                    .hasAnyRole("FULL_ADMIN", "ADMIN")
                    .requestMatchers(HttpMethod.GET, "/branch-offices/**")
                    .authenticated()
                    .requestMatchers(HttpMethod.GET, "/real-estate-agents/**")
                    .hasAnyRole("FULL_ADMIN", "ADMIN", "MANAGER", "REAL_ESTATE_AGENT")
                    .requestMatchers(HttpMethod.GET, "/managers/**")
                    .hasAnyRole("FULL_ADMIN", "ADMIN", "MANAGER")
                    .requestMatchers(HttpMethod.GET, "/teams/**", "/leads/**", "/properties/**", "/sensor-property-metrics/**")
                    .hasAnyRole("FULL_ADMIN", "ADMIN", "MANAGER", "REAL_ESTATE_AGENT")
                    .requestMatchers(HttpMethod.POST, "/real-estate-agencies")
                    .hasRole("FULL_ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/real-estate-agencies/**")
                    .hasRole("FULL_ADMIN")
                    .requestMatchers(HttpMethod.POST, "/branch-offices", "/real-estate-agents", "/managers")
                    .hasAnyRole("FULL_ADMIN", "ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/branch-offices/**", "/real-estate-agents/**")
                    .hasAnyRole("FULL_ADMIN", "ADMIN")
                    .requestMatchers(HttpMethod.PATCH, "/branch-offices/**", "/real-estate-agents/**", "/managers/**")
                    .hasAnyRole("FULL_ADMIN", "ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/managers/**")
                    .hasAnyRole("FULL_ADMIN", "ADMIN", "MANAGER")
                    .requestMatchers(HttpMethod.POST, "/teams/**", "/sensor-property-metrics")
                    .hasAnyRole("FULL_ADMIN", "ADMIN", "MANAGER")
                    .requestMatchers(HttpMethod.PUT, "/teams/**", "/properties/**")
                    .hasAnyRole("FULL_ADMIN", "ADMIN", "MANAGER")
                    .requestMatchers(HttpMethod.PATCH, "/properties/**")
                    .hasAnyRole("FULL_ADMIN", "ADMIN", "MANAGER")
                    .requestMatchers(HttpMethod.PUT, "/leads/{id}")
                    .hasAnyRole("FULL_ADMIN", "ADMIN", "MANAGER", "REAL_ESTATE_AGENT")
                    .requestMatchers(HttpMethod.DELETE, "/**")
                    .hasRole("FULL_ADMIN")
                    .anyRequest()
                    .authenticated()
            }.oauth2ResourceServer { oauth2 ->
                oauth2
                    .jwt { jwt ->
                        jwt
                            .decoder(customJwtDecoder)
                            .jwtAuthenticationConverter(customJwtAuthenticationConverter)
                    }.authenticationEntryPoint(customAuthenticationEntryPoint)
            }.exceptionHandling { ex -> ex.accessDeniedHandler(customAccessDeniedHandler) }

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
