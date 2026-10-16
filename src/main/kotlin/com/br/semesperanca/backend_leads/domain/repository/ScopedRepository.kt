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
