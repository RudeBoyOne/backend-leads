package com.br.semesperanca.backendleads.domain.service.user

import com.br.semesperanca.backendleads.application.model.request.CreateManagerRequest
import com.br.semesperanca.backendleads.application.model.request.UpdateManagerRequest
import com.br.semesperanca.backendleads.application.model.request.UserFilterRequest
import com.br.semesperanca.backendleads.application.model.response.ManagerResponse
import com.br.semesperanca.backendleads.configuration.exception.EmailAlreadyExistsException
import com.br.semesperanca.backendleads.domain.model.Role
import com.br.semesperanca.backendleads.domain.model.user.Manager
import com.br.semesperanca.backendleads.domain.repository.BranchOfficeRepository
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
    private val userRepository: UserRepository,
    private val branchOfficeRepository: BranchOfficeRepository,
) {
    @Transactional
    fun create(request: CreateManagerRequest): ManagerResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw EmailAlreadyExistsException(request.email)
        }
        val role: Role =
            roleRepository
                .findByName("MANAGER")
                .orElseThrow { RuntimeException("Role não encontrada") }

        val branchOffice =
            request.branchOfficeId?.let {
                branchOfficeRepository.findById(it).orElseThrow { RuntimeException("Filial não encontrada") }
            }

        val manager =
            Manager().apply {
                name = request.name
                email = request.email
                setPassword(passwordEncoder.encode(request.password))
                this.role = role
                isActive = true
                this.branchOffice = branchOffice
            }

        return toResponse(repository.save(manager))
    }

    fun findById(id: UUID): ManagerResponse = toResponse(findManagerById(id))

    fun findAll(
        filter: UserFilterRequest,
        pageable: Pageable,
    ): Page<ManagerResponse> =
        repository.findAll(buildSpec(filter), pageable).map {
            toResponse(it)
        }

    private fun buildSpec(filter: UserFilterRequest): Specification<Manager> =
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

    @Transactional
    fun update(
        id: UUID,
        request: UpdateManagerRequest,
    ): ManagerResponse {
        val manager = findManagerById(id)
        manager.name = request.name
        manager.email = request.email
        manager.branchOffice =
            request.branchOfficeId?.let {
                branchOfficeRepository.findById(it).orElseThrow { RuntimeException("Filial não encontrada") }
            }
        return toResponse(repository.save(manager))
    }

    @Transactional
    fun toggleActive(id: UUID): ManagerResponse {
        val manager = findManagerById(id)
        manager.isActive = !manager.isActive
        return toResponse(repository.save(manager))
    }

    @Transactional
    fun delete(id: UUID) {
        val manager = findManagerById(id)
        repository.delete(manager)
    }

    private fun findManagerById(id: UUID): Manager = repository.findById(id).orElseThrow { RuntimeException("Gerente não encontrado") }

    private fun toResponse(manager: Manager): ManagerResponse =
        ManagerResponse(
            id = manager.id,
            name = manager.name,
            email = manager.email,
            branchOfficeId = manager.branchOffice?.id,
            role = manager.role?.name,
            active = manager.isActive,
            createdAt = manager.createdAt,
        )
}
