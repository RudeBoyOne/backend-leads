package com.br.semesperanca.backendleads.configuration.security

import com.br.semesperanca.backendleads.domain.repository.ScopedRepositoryImpl
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity

/**
 * Registra [ScopedRepositoryImpl] como classe base de todo repositório JPA, e
 * liga a segurança de método.
 *
 * `repositoryBaseClass` é o gancho que faz o escopo ser estrutural: a partir
 * daqui todo `findAll`/`findById`/`count` de qualquer repositório já sai com a
 * subárvore da role do principal, sem depender de o autor do service ter
 * lembrado de escrevê-lo. Um repositório que **não** deva ser escopado declara
 * `JpaRepository` normalmente — é assim que `RoleRepository` e `UserRepository`
 * (usados na autenticação, antes de existir principal) ficam de fora.
 *
 * `@EnableMethodSecurity` cobre o `@PreAuthorize` das escritas. As duas camadas
 * não se sobrepõem: a anotação de método diz *quem* pode escrever, e o repositório
 * escopado diz *sobre o quê*. A camada de URL continua sendo a mais externa, e é
 * a que decide o que nem chega ao service.
 *
 * `basePackages` é explícito porque o padrão da anotação é o pacote da própria
 * classe de configuração — que aqui é `configuration.security`, e portanto
 * escanearia zero repositório e quebraria o contexto com `NoSuchBeanDefinition`
 * no primeiro service.
 */
@Configuration
@EnableMethodSecurity
@EnableJpaRepositories(
    basePackages = ["com.br.semesperanca.backendleads"],
    repositoryBaseClass = ScopedRepositoryImpl::class,
)
class RepositorySecurityConfig
