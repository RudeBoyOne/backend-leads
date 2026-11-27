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

class RoleScope(
    private val principal: Principal,
) {

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

                    principal.isManager -> {
                        cb.or(
                            root.get<Any>("branchOffice").get<UUID>("id").`in`(ownOffice(parent, cb)),
                            root.get<UUID>("id").`in`(myAgents(parent, cb)),
                        )
                    }

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


    fun forLead(): Specification<Lead> =
        if (principal.isFullAdmin) {
            unrestricted()
        } else {
            Specification { root, parent, cb ->
                when {
                    principal.isAgent -> {
                        cb.equal(root.get<Lead>("realEstateAgent").get<Any>("id"), principal.id)
                    }

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


    private fun ownedOffices(
        parent: AbstractQuery<*>,
        cb: CriteriaBuilder,
    ): Subquery<UUID> =
        parent.subquery(UUID::class.javaObjectType).apply {
            val office: Root<BranchOffice> = from(BranchOffice::class.java)
            select(office.get("id"))
                .where(cb.equal(office.get<Any>("branchOwner").get<Any>("id"), principal.id))
        }

    private fun ownOffice(
        parent: AbstractQuery<*>,
        cb: CriteriaBuilder,
    ): Subquery<UUID> =
        parent.subquery(UUID::class.javaObjectType).apply {
            val user: Root<User> = from(User::class.java)
            select(user.get<Any>("branchOffice").get("id"))
                .where(cb.equal(user.get<UUID>("id"), principal.id))
        }

    private fun agencyOfPrincipal(
        parent: AbstractQuery<*>,
        cb: CriteriaBuilder,
    ): Subquery<UUID> =
        parent.subquery(UUID::class.javaObjectType).apply {
            val user: Root<User> = from(User::class.java)
            select(user.get<UUID>("realEstateAgencyId"))
                .where(cb.equal(user.get<UUID>("id"), principal.id))
        }

    private fun myAgents(
        parent: AbstractQuery<*>,
        cb: CriteriaBuilder,
    ): Subquery<UUID> =
        parent.subquery(UUID::class.javaObjectType).apply {
            val membership: Root<TeamAgent> = from(TeamAgent::class.java)
            select(membership.get<Any>("realEstateAgent").get("id"))
                .where(cb.equal(membership.get<Any>("team").get<Any>("manager").get<Any>("id"), principal.id))
        }

    private fun myTeams(
        parent: AbstractQuery<*>,
        cb: CriteriaBuilder,
    ): Subquery<UUID> =
        parent.subquery(UUID::class.javaObjectType).apply {
            val membership: Root<TeamAgent> = from(TeamAgent::class.java)
            select(membership.get<Any>("team").get("id"))
                .where(cb.equal(membership.get<Any>("realEstateAgent").get<Any>("id"), principal.id))
        }

    private fun managersOfOwnedOffices(
        parent: AbstractQuery<*>,
        cb: CriteriaBuilder,
    ): Subquery<UUID> =
        parent.subquery(UUID::class.javaObjectType).apply {
            val manager: Root<Manager> = from(Manager::class.java)
            select(manager.get("id"))
                .where(manager.get<Any>("branchOffice").get<UUID>("id").`in`(ownedOffices(this, cb)))
        }

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
