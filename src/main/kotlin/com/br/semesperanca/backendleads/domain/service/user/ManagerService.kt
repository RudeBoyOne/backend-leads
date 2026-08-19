package com.br.semesperanca.backendleads.domain.service.user

import com.br.semesperanca.backendleads.application.model.request.CreateManagerRequest
import com.br.semesperanca.backendleads.application.model.request.UpdateManagerRequest
import com.br.semesperanca.backendleads.application.model.request.UserFilterRequest
import com.br.semesperanca.backendleads.application.model.response.ManagerResponse
import com.br.semesperanca.backendleads.configuration.exception.EmailAlreadyExistsException
import com.br.semesperanca.backendleads.domain.model.Role
import com.br.semesperanca.backendleads.domain.model.user.Manager
import com.br.semesperanca.backendleads.domain.repository.RoleRepository
import com.br.semesperanca.backendleads.domain.repository.user.ManagerRepository
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
class ManagerService(
    private val repository: ManagerRepository,
    private val roleRepository: RoleRepository,
    private val passwordEncoder: PasswordEncoder,
    private val userRepository: UserRepository
) {
    @Transactional
    fun create(request: CreateManagerRequest): ManagerResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw EmailAlreadyExistsException(request.email)
        }
        val role: Role = roleRepository
            .findByName("MANAGER")
            .orElseThrow { RuntimeException("Role não encontrada") }

        val manager = Manager(branchOffice = request.branchOffice).apply {
            name = request.name
            email = request.email
            setPassword(passwordEncoder.encode(request.password))
            this.role = role
            isActive = true
        }

        return toResponse(repository.save(manager))
    }

    fun findById(id: UUID): ManagerResponse = toResponse(findManagerById(id))

    fun findAll(filter: UserFilterRequest, pageable: Pageable): Page<ManagerResponse> {
        return repository.findAll(buildSpec(filter), pageable).map { toResponse(it) }
    }

    private fun buildSpec(filter: UserFilterRequest): Specification<Manager> {
        return Specification { root, _, cb ->
            val predicates = mutableListOf<Predicate>()
            if (filter.active != null) {
                predicates.add(cb.equal(root.get<String>("active"), filter.active))
            }
            if (!filter.search.isNullOrBlank()) {
                val pattern = "%" + filter.search!!.lowercase() + "%"
                predicates.add(
                    cb.or(
                        cb.like(cb.lower(root.get<String>("name")), pattern),
                        cb.like(cb.lower(root.get<String>("email")), pattern)
                    )
                )
            }
            cb.and(*predicates.toTypedArray())
        }
    }

    @Transactional
    fun update(id: UUID, request: UpdateManagerRequest): ManagerResponse {
        val manager = findManagerById(id)
        manager.name = request.name
        manager.email = request.email
        manager.branchOffice = request.branchOffice
        return toResponse(repository.save(manager))
    }

    @Transactional
    fun toggleActive(id: UUID): ManagerResponse {
        val manager = findManagerById(id)
        manager.isActive = !manager.isActive
        return toResponse(repository.save(manager))
    }

    private fun findManagerById(id: UUID): Manager {
        return repository.findById(id).orElseThrow { RuntimeException("Gerente não encontrado") }
    }

    private fun toResponse(manager: Manager): ManagerResponse {
        return ManagerResponse(
            id = manager.id,
            name = manager.name,
            email = manager.email,
            branchOffice = manager.branchOffice,
            role = manager.role?.name,
            active = manager.isActive,
            createdAt = manager.createdAt
        )
    }
}