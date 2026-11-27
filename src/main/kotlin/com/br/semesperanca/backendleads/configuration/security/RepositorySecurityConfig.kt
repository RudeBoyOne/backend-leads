package com.br.semesperanca.backendleads.configuration.security

import com.br.semesperanca.backendleads.domain.repository.ScopedRepositoryImpl
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity

@Configuration
@EnableMethodSecurity
@EnableJpaRepositories(
    basePackages = ["com.br.semesperanca.backendleads"],
    repositoryBaseClass = ScopedRepositoryImpl::class,
)
class RepositorySecurityConfig
