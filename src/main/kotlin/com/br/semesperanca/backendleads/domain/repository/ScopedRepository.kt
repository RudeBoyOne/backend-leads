package com.br.semesperanca.backendleads.domain.repository

import com.br.semesperanca.backendleads.domain.security.DataScope
import jakarta.persistence.EntityManager
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.support.JpaEntityInformation
import org.springframework.data.jpa.repository.support.SimpleJpaRepository
import org.springframework.data.repository.NoRepositoryBean
import java.util.Optional

/**
 * Repositório que aplica o escopo da role **dentro da query**, e não no service.
 *
 * A propriedade é o ponto: um service novo, escrito amanhã, já nasce escopado
 * porque consulta este tipo. Não existe linha de código para lembrar de escrever
 * — que era exatamente a origem dos três vãos que o escopo veio fechar
 * (`TeamService.findAll()` na tabela inteira, `LeadService` partindo de
 * `cb.conjunction()`, `ManagerService.findAll` listando tudo).
 *
 * `findById` também é escopado: um id de filial alheia **não resolve**, e a
 * resposta é 404, e não 403, porque 403 confirmaria ao atacante que a filial
 * existe.
 */
@NoRepositoryBean
interface ScopedRepository<T : Any, ID : Any> :
    JpaRepository<T, ID>,
    JpaSpecificationExecutor<T>

/**
 * Base genérica de todo repositório escopado, registrada em
 * [com.br.semesperanca.backendleads.configuration.security.RepositorySecurityConfig]
 * via `baseClass`.
 *
 * O filtro do cliente se **soma** ao de escopo em toda sobrecarga que recebe
 * `Specification`: um service pode passar o próprio filtro, nunca remover a
 * subárvore da role. É a diferença entre um filtro e uma brecha — `and` é
 * obrigatório, e a soma é o que faz a paginação continuar correta, porque a
 * mesma `Specification` alimenta o `COUNT`.
 *
 * O construtor tem exatamente `(JpaEntityInformation, EntityManager)` porque
 * esse é o único que o Spring Data resolve por reflexão. Por isso o escopo não
 * pode depender de nenhum bean: [DataScope] lê o `SecurityContextHolder`.
 *
 * ### Limite conhecido e deliberado
 * Só os métodos herdados de `SimpleJpaRepository` são reescritos. Métodos
 * derivados declarados na interface (`findByManagerId`, `existsByCreci`,
 * `findByOriginLeadId`) são montados pelo interceptor de query do Spring Data e
 * **não** passam por aqui — eles não carregam escopo. Por isso os services que
 * os usam de ponta exposta validam antes o id do recurso via `findById`, que é
 * escopado. A lista desses métodos é curta e está auditada; introduzir um
 * método derivado novo em entidade escopada exige passar por `findAll(spec)`.
 *
 * A classe é concreta e `open`, não `abstract`: `repositoryBaseClass` instancia a
 * base por reflexão, e uma classe abstrata faria o primeiro repositório falhar
 * com `Is it an abstract class?`.
 */
open class ScopedRepositoryImpl<T : Any, ID : Any>(
    entityInformation: JpaEntityInformation<T, ID>,
    entityManager: EntityManager,
) : SimpleJpaRepository<T, ID>(entityInformation, entityManager) {
    private val domainType: Class<T> = entityInformation.javaType

    override fun findById(id: ID): Optional<T> = findOne(scoped().and(whereIdEquals(id)))

    override fun findAll(): List<T> = super.findAll(scoped())

    override fun findAll(sort: Sort): List<T> = super.findAll(scoped(), sort)

    override fun findAll(pageable: Pageable): Page<T> = super.findAll(scoped(), pageable)

    override fun findAll(spec: Specification<T>): List<T> = super.findAll(scoped().and(spec))

    override fun findAll(
        spec: Specification<T>,
        sort: Sort,
    ): List<T> = super.findAll(scoped().and(spec), sort)

    override fun findAll(
        spec: Specification<T>,
        pageable: Pageable,
    ): Page<T> = super.findAll(scoped().and(spec), pageable)

    /**
     * `findAllById` do `SimpleJpaRepository` monta um `IN` direto e ignoraria o
     * escopo, então é reescrito pelo mesmo caminho dos demais. Sem isto,
     * `SensorMetricService` resolveria o título de um imóvel de outra agência.
     */
    override fun findAllById(ids: Iterable<ID>): List<T> = if (ids.none()) emptyList() else super.findAll(scoped().and(whereIdIn(ids)))

    override fun findOne(spec: Specification<T>): Optional<T> = super.findOne(scoped().and(spec))

    override fun existsById(id: ID): Boolean = findById(id).isPresent

    override fun count(): Long = super.count(scoped())

    override fun count(spec: Specification<T>): Long = super.count(scoped().and(spec))

    /**
     * Remoção em massa é recusada: sem id para aplicar escopo, `deleteAll()` é
     * exatamente a operação que um branch owner não pode fazer na tabela
     * inteira. Para apagar, use `deleteById`, que passa por `findById` escopado.
     */
    override fun deleteAll() = throw UnsupportedOperationException("Remoção em massa não respeita escopo de role")

    private fun scoped(): Specification<T> = DataScope.forType(domainType)

    private fun whereIdEquals(id: ID): Specification<T> =
        Specification { root, _, cb ->
            @Suppress("UNCHECKED_CAST")
            cb.equal(root.get<Any>("id"), id as Any)
        }

    private fun whereIdIn(ids: Iterable<ID>): Specification<T> =
        Specification { root, _, cb ->
            @Suppress("UNCHECKED_CAST")
            root.get<Any>("id").`in`(ids as Collection<Any>)
        }
}
