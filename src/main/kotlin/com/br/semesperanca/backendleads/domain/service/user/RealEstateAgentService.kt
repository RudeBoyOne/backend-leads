package com.br.semesperanca.backendleads.domain.service.user

import com.br.semesperanca.backendleads.application.model.request.CreateRealEstateAgentRequest
import com.br.semesperanca.backendleads.application.model.request.UpdateRealEstateAgentRequest
import com.br.semesperanca.backendleads.application.model.request.UserFilterRequest
import com.br.semesperanca.backendleads.application.model.response.RealEstateAgentResponse
import com.br.semesperanca.backendleads.configuration.exception.EmailAlreadyExistsException
import com.br.semesperanca.backendleads.domain.model.Role
import com.br.semesperanca.backendleads.domain.model.user.RealEstateAgent
import com.br.semesperanca.backendleads.domain.repository.BranchOfficeRepository
import com.br.semesperanca.backendleads.domain.repository.RoleRepository
import com.br.semesperanca.backendleads.domain.repository.user.RealEstateAgentRepository
import com.br.semesperanca.backendleads.domain.repository.user.UserRepository
import jakarta.persistence.criteria.Predicate
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class RealEstateAgentService(
    private val repository: RealEstateAgentRepository,
    private val roleRepository: RoleRepository,
    private val passwordEncoder: PasswordEncoder,
    private val userRepository: UserRepository,
    private val branchOfficeRepository: BranchOfficeRepository,
) {
    @Transactional
    fun create(request: CreateRealEstateAgentRequest): RealEstateAgentResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw EmailAlreadyExistsException(request.email)
        }
        if (repository.existsByCreci(request.creci)) {
            throw RuntimeException("CRECI já cadastrado")
        }
        val role: Role =
            roleRepository
                .findByName("REAL_ESTATE_AGENT")
                .orElseThrow { RuntimeException("Role não encontrada") }

        val agent =
            RealEstateAgent(creci = request.creci).apply {
                name = request.name
                email = request.email
                setPassword(passwordEncoder.encode(request.password))
                this.role = role
                isActive = true
                this.branchOffice =
                    request.branchOfficeId?.let {
                        branchOfficeRepository.findById(it).orElseThrow { RuntimeException("Filial não encontrada") }
                    }
            }

        return toResponse(repository.save(agent))
    }

    fun findById(id: UUID): RealEstateAgentResponse = toResponse(findAgentById(id))

    @Transactional
    fun update(
        id: UUID,
        request: UpdateRealEstateAgentRequest,
    ): RealEstateAgentResponse {
        val agent = findAgentById(id)
        agent.name = request.name
        agent.email = request.email
        agent.creci = request.creci
        agent.branchOffice =
            request.branchOfficeId?.let {
                branchOfficeRepository.findById(it).orElseThrow { RuntimeException("Filial não encontrada") }
            }
        return toResponse(repository.save(agent))
    }

    @Transactional
    fun toggleActive(id: UUID): RealEstateAgentResponse {
        val agent = findAgentById(id)
        agent.isActive = !agent.isActive
        return toResponse(repository.save(agent))
    }

    @Transactional
    fun delete(id: UUID) {
        val agent = findAgentById(id)
        repository.delete(agent)
    }

    private fun findAgentById(id: UUID): RealEstateAgent =
        repository.findById(id).orElseThrow { RuntimeException("Corretor não encontrado") }

    private fun toResponse(agent: RealEstateAgent): RealEstateAgentResponse =
        RealEstateAgentResponse(
            id = agent.id,
            name = agent.name,
            email = agent.email,
            creci = agent.creci,
            branchOfficeId = agent.branchOffice?.id,
            role = agent.role?.name,
            active = agent.isActive,
            createdAt = agent.createdAt,
        )

    fun findAll(
        filter: UserFilterRequest,
        pageable: Pageable,
    ): Page<RealEstateAgentResponse> =
        repository.findAll(buildSpec(filter), pageable).map {
            toResponse(it)
        }

    private fun buildSpec(filter: UserFilterRequest): Specification<RealEstateAgent> =
        Specification { root, _, cb ->
            val predicates = mutableListOf<Predicate>()
            if (filter.active != null) {
                predicates.add(cb.equal(root.get<String>("active"), filter.active))
            }
            if (!filter.search.isNullOrBlank()) {
                val pattern = "%" + filter.search!!.lowercase() + "%"
                predicates.add(
                    cb.or(
                        cb.like(cb.lower(root.get<String>("name")), pattern),
                        cb.like(cb.lower(root.get<String>("email")), pattern),
                    ),
                )
            }
            cb.and(*predicates.toTypedArray())
        }
}
