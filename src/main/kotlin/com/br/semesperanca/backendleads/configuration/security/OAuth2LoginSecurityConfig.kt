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
    /**
     * Chain do fluxo OAuth2 (browser): apenas os endpoints do login social.
     * Matcher estreito — nao participa da protecao das rotas da aplicacao.
     */
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

    /**
     * Chain da API: catch-all (sem securityMatcher) — toda rota nova
     * ja nasce protegida por JWT, sem manutencao por rota.
     */
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
                    // ---------------- raiz ----------------
                    // FULL_ADMIN é externo ao negócio e não é escopado; ADMIN é o
                    // branch owner e enxerga a própria rede. Nenhuma outra role
                    // cria ou remove um responsável.
                    .requestMatchers(HttpMethod.POST, "/auth/register/full-admin")
                    .hasRole("FULL_ADMIN")
                    .requestMatchers("/branch-owners/**")
                    .hasAnyRole("FULL_ADMIN", "ADMIN")
                    .requestMatchers(HttpMethod.PATCH, "/auth/change-password")
                    .authenticated()
                    // ---------------- leitura ----------------
                    // Leitura é recortada no repositório, não na URL: o papel aqui
                    // é só o piso, e quem decide *quais* registros é o escopo da
                    // role. Um corretor entra no seu próprio registro e recebe 404
                    // no do colega — nunca 403, que confirmaria a existência.
                    .requestMatchers(HttpMethod.GET, "/branch-offices/**")
                    .authenticated()
                    .requestMatchers(HttpMethod.GET, "/real-estate-agents/**")
                    .hasAnyRole("FULL_ADMIN", "ADMIN", "MANAGER", "REAL_ESTATE_AGENT")
                    .requestMatchers(HttpMethod.GET, "/managers/**")
                    .hasAnyRole("FULL_ADMIN", "ADMIN", "MANAGER")
                    .requestMatchers(HttpMethod.GET, "/teams/**", "/leads/**", "/properties/**", "/sensor-property-metrics/**")
                    .hasAnyRole("FULL_ADMIN", "ADMIN", "MANAGER", "REAL_ESTATE_AGENT")
                    // ---------------- escrita ----------------
                    // Escrita é `FULL_ADMIN` ou `ADMIN`, e o `@PreAuthorize` do
                    // service repete a regra: a camada de URL rejeita cedo, a do
                    // service protege quem chama o método por fora do HTTP.
                    .requestMatchers(HttpMethod.POST, "/branch-offices", "/real-estate-agents", "/managers")
                    .hasAnyRole("FULL_ADMIN", "ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/branch-offices/**", "/real-estate-agents/**")
                    .hasAnyRole("FULL_ADMIN", "ADMIN")
                    .requestMatchers(HttpMethod.PATCH, "/branch-offices/**", "/real-estate-agents/**", "/managers/**")
                    .hasAnyRole("FULL_ADMIN", "ADMIN")
                    // Transferência de gerente e de lotação mudam a subárvore de
                    // alguém, então o gerente age apenas sobre si.
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
                    // Remoção é só do root: `ON DELETE CASCADE` leva junto gerente,
                    // corretor, imóvel, sensor e lead do subtree, e não há como
                    // reverter isso.
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
