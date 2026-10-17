package com.br.semesperanca.backendleads.domain.security

import com.br.semesperanca.backendleads.domain.model.BranchOffice
import com.br.semesperanca.backendleads.domain.model.BranchOwner
import com.br.semesperanca.backendleads.domain.model.Lead
import com.br.semesperanca.backendleads.domain.model.Property
import com.br.semesperanca.backendleads.domain.model.RealEstateAgency
import com.br.semesperanca.backendleads.domain.model.Team
import com.br.semesperanca.backendleads.domain.model.TeamAgent
import com.br.semesperanca.backendleads.domain.model.user.Manager
import com.br.semesperanca.backendleads.domain.model.user.RealEstateAgent
import com.br.semesperanca.backendleads.domain.model.user.User
import jakarta.persistence.criteria.AbstractQuery
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import jakarta.persistence.criteria.Subquery
import org.springframework.data.jpa.domain.Specification
import java.util.UUID

/**
 * A subárvore que cada role pode tocar. Uma role enxerga exatamente o que
 * possui; `FULL_ADMIN` é root e não é escopado.
 *
 * ```
 * FULL_ADMIN          root do software, externo ao negócio
 *   └─ ADMIN          branch owner  -> suas filiais
 *        └─ MANAGER   -> seus times -> seus corretores
 *             └─ REAL_ESTATE_AGENT  -> seus leads
 * ```
 *
 * A travessia é feita por `Subquery`, nunca por `IN` de lista de ids
 * materializada. A diferença que importa não é estética: com a lista, o número
 * de filiais e de corretores sob um branch owner aparece no SQL e no plano de
 * execução, a contagem da paginação carrega a lista, e o teto de parâmetros do
 * Postgres (65535) vira bug em produção. Com subquery o banco faz o caminho, e a
 * `Specification` do cliente compõe com a de escopo sem custo duplicado.
 *
 * Toda subquery é criada a partir do `AbstractQuery` que a contém
 * ([parent]), porque `CriteriaBuilder` não sabe gerar `Subquery` — ele só
 * conhece a raiz. Passar o pai explicitamente também impede o erro sutil de
 * anexar a subquery à query errada quando a predicate é reusada.
 */
class RoleScope(
    private val principal: Principal,
) {
    // ------------------------------------------------------------------ dono

    /**
     * A agência é o tenant: não tem dono acima, então o escopo é a própria
     * agência do usuário (`users.real_estate_agency_id`), resolvida na consulta
     * como os demais — um FULL_ADMIN vê todas, e os demais veem a sua.
     */
    fun forRealEstateAgency(): Specification<RealEstateAgency> =
        if (principal.isFullAdmin) {
            unrestricted()
        } else {
            Specification { root, parent, cb ->
                root.get<UUID>("id").`in`(agencyOfPrincipal(parent, cb))
            }
        }

    fun forBranchOwner(): Specification<BranchOwner> =
        if (principal.isFullAdmin) {
            unrestricted()
        } else {
            Specification { root, _, cb ->
                // O escopo do responsável é o próprio login: o branch owner
                // enxerga a si mesmo, e mais nada.
                if (principal.isBranchOwner) cb.equal(root.get<UUID>("id"), principal.id) else cb.disjunction()
            }
        }

    fun forBranchOffice(): Specification<BranchOffice> =
        if (principal.isFullAdmin) {
            unrestricted()
        } else {
            Specification { root, parent, cb ->
                if (principal.isBranchOwner) {
                    cb.equal(root.get<Any>("branchOwner").get<Any>("id"), principal.id)
                } else {
                    // Gerente e corretor leem a filial em que estão lotados, e
                    // nenhuma outra.
                    root.get<UUID>("id").`in`(ownOffice(parent, cb))
                }
            }
        }

    fun forManager(): Specification<Manager> =
        if (principal.isFullAdmin) {
            unrestricted()
        } else {
            Specification { root, parent, cb ->
                when {
                    principal.isBranchOwner -> {
                        root.get<Any>("branchOffice").get<UUID>("id").`in`(ownedOffices(parent, cb))
                    }

                    // O gerente enxerga a si mesmo; um corretor não enxerga
                    // gerente nenhum, nem a si próprio como gerente.
                    principal.isManager -> {
                        cb.equal(root.get<UUID>("id"), principal.id)
                    }

                    else -> {
                        cb.disjunction()
                    }
                }
            }
        }

    fun forRealEstateAgent(): Specification<RealEstateAgent> =
        if (principal.isFullAdmin) {
            unrestricted()
        } else {
            Specification { root, parent, cb ->
                when {
                    principal.isBranchOwner -> {
                        root.get<Any>("branchOffice").get<UUID>("id").`in`(ownedOffices(parent, cb))
                    }

                    // O gerente enxerga os corretores da sua filial **e** os
                    // inscritos nos seus times. Sem o primeiro ramo, um corretor
                    // lotado na filial do gerente e sem time nenhum sumiria da
                    // listagem — e, pela mesma regra, os leads dele também, o que
                    // deixaria o gerente com imóveis da filial (visíveis por
                    // [forProperty]) e sem os corretores que os produziram.
                    principal.isManager -> {
                        cb.or(
                            root.get<Any>("branchOffice").get<UUID>("id").`in`(ownOffice(parent, cb)),
                            root.get<UUID>("id").`in`(myAgents(parent, cb)),
                        )
                    }

                    // O corretor enxerga a si mesmo, e só a si mesmo.
                    else -> {
                        cb.equal(root.get<UUID>("id"), principal.id)
                    }
                }
            }
        }

    fun forTeam(): Specification<Team> =
        if (principal.isFullAdmin) {
            unrestricted()
        } else {
            Specification { root, parent, cb ->
                when {
                    principal.isBranchOwner -> {
                        // A filial do time é derivada do gerente:
                        // `team.branch_office_id` saiu do schema e o caminho é
                        // time -> gerente -> filial.
                        root.get<Any>("manager").get<UUID>("id").`in`(managersOfOwnedOffices(parent, cb))
                    }

                    principal.isManager -> {
                        cb.equal(root.get<Any>("manager").get<Any>("id"), principal.id)
                    }

                    else -> {
                        root.get<UUID>("id").`in`(myTeams(parent, cb))
                    }
                }
            }
        }

    // ------------------------------------------------------------------ negócio

    fun forLead(): Specification<Lead> =
        if (principal.isFullAdmin) {
            unrestricted()
        } else {
            Specification { root, parent, cb ->
                when {
                    // O corretor tem o caminho mais curto possível: o lead é dele.
                    principal.isAgent -> {
                        cb.equal(root.get<Lead>("realEstateAgent").get<Any>("id"), principal.id)
                    }

                    // Branch owner e gerente precisam do caminho inteiro
                    // `lead -> corretor -> filial -> responsável`. A API Criteria
                    // não faz subquery aninhado, então o caminho precisa caber num
                    // join só — daí o `exists` com um único `from`.
                    else -> {
                        val sub = parent.subquery(Long::class.javaObjectType)
                        val agent: Root<RealEstateAgent> = sub.from(RealEstateAgent::class.java)
                        sub.select(cb.literal(1L))
                        sub.where(
                            cb.equal(agent.get<UUID>("id"), root.get<Lead>("realEstateAgent").get<Any>("id")),
                            agentUnderManagement(sub, cb, agent),
                        )
                        cb.exists(sub)
                    }
                }
            }
        }

    fun forProperty(): Specification<Property> =
        if (principal.isFullAdmin) {
            unrestricted()
        } else {
            Specification { root, parent, cb ->
                if (principal.isBranchOwner) {
                    root.get<Any>("branchOffice").get<UUID>("id").`in`(ownedOffices(parent, cb))
                } else {
                    root.get<Any>("branchOffice").get<UUID>("id").`in`(ownOffice(parent, cb))
                }
            }
        }

    // ------------------------------------------------------------------ subqueries

    /** Filiais cujo responsável é o branch owner autenticado. */
    private fun ownedOffices(
        parent: AbstractQuery<*>,
        cb: CriteriaBuilder,
    ): Subquery<UUID> =
        parent.subquery(UUID::class.javaObjectType).apply {
            val office: Root<BranchOffice> = from(BranchOffice::class.java)
            select(office.get("id"))
                .where(cb.equal(office.get<Any>("branchOwner").get<Any>("id"), principal.id))
        }

    /**
     * Filial em que o gerente ou o corretor está lotado, resolvida no instante da
     * consulta. Ler `users.branch_office_id` por subquery, e não de um valor
     * guardado no principal, é o que impede o escopo de servir uma transferência
     * antiga durante uma sessão já aberta.
     */
    private fun ownOffice(
        parent: AbstractQuery<*>,
        cb: CriteriaBuilder,
    ): Subquery<UUID> =
        parent.subquery(UUID::class.javaObjectType).apply {
            val user: Root<User> = from(User::class.java)
            select(user.get<Any>("branchOffice").get("id"))
                .where(cb.equal(user.get<UUID>("id"), principal.id))
        }

    /** A agência dona do usuário autenticado, de `users.real_estate_agency_id`. */
    private fun agencyOfPrincipal(
        parent: AbstractQuery<*>,
        cb: CriteriaBuilder,
    ): Subquery<UUID> =
        parent.subquery(UUID::class.javaObjectType).apply {
            val user: Root<User> = from(User::class.java)
            select(user.get<UUID>("realEstateAgencyId"))
                .where(cb.equal(user.get<UUID>("id"), principal.id))
        }

    /** Corretores que estão em algum time gerenciado por este gerente. */
    private fun myAgents(
        parent: AbstractQuery<*>,
        cb: CriteriaBuilder,
    ): Subquery<UUID> =
        parent.subquery(UUID::class.javaObjectType).apply {
            val membership: Root<TeamAgent> = from(TeamAgent::class.java)
            select(membership.get<Any>("realEstateAgent").get("id"))
                .where(cb.equal(membership.get<Any>("team").get<Any>("manager").get<Any>("id"), principal.id))
        }

    /** Times em que este corretor está inscrito. */
    private fun myTeams(
        parent: AbstractQuery<*>,
        cb: CriteriaBuilder,
    ): Subquery<UUID> =
        parent.subquery(UUID::class.javaObjectType).apply {
            val membership: Root<TeamAgent> = from(TeamAgent::class.java)
            select(membership.get<Any>("team").get("id"))
                .where(cb.equal(membership.get<Any>("realEstateAgent").get<Any>("id"), principal.id))
        }

    /** Gerentes lotados nas filiais do branch owner autenticado. */
    private fun managersOfOwnedOffices(
        parent: AbstractQuery<*>,
        cb: CriteriaBuilder,
    ): Subquery<UUID> =
        parent.subquery(UUID::class.javaObjectType).apply {
            val manager: Root<Manager> = from(Manager::class.java)
            select(manager.get("id"))
                .where(manager.get<Any>("branchOffice").get<UUID>("id").`in`(ownedOffices(this, cb)))
        }

    /**
     * Um corretor está sob gestão do branch owner por estar lotado em uma das
     * filiais dele; sob gestão do gerente, por estar lotado na filial em que o
     * gerente está ou inscrito em um time dele. São os mesmos dois caminhos dos
     * dois níveis, com a raiz trocada — e o par é o mesmo em [forRealEstateAgent],
     * para que a listagem de corretores e a de leads nunca discordem sobre quem
     * está sob gestão de quem.
     *
     * A disjunção é explícita porque "corretor da minha rede" é uma regra de
     * negócio definida por duas condições, e não um detalhe que se possa achatar
     * num `if` sem que alguém note a perda de um dos lados. Um `if` que
     * escolhesse entre "lotação" e "time" resolveria o tipo errado de problema:
     * `ownedOffices` casa `branch_office.branch_owner_id` com o principal, e o
     * principal de um gerente não é branch owner de nada — o ramo viraria
     * predicado falso e sumiria da disjunção sem erro nenhum.
     */
    private fun agentUnderManagement(
        parent: AbstractQuery<*>,
        cb: CriteriaBuilder,
        agent: Root<RealEstateAgent>,
    ): Predicate {
        if (principal.isBranchOwner) {
            return agent.get<Any>("branchOffice").get<UUID>("id").`in`(ownedOffices(parent, cb))
        }
        return cb.or(
            agent.get<Any>("branchOffice").get<UUID>("id").`in`(ownOffice(parent, cb)),
            agent.get<UUID>("id").`in`(myAgents(parent, cb)),
        )
    }

    private fun <T> unrestricted(): Specification<T> = Specification.unrestricted()
}
