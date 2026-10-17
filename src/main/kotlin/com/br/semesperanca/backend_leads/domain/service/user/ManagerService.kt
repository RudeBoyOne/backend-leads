package com.br.semesperanca.backendleads.domain.service.user

import com.br.semesperanca.backendleads.application.model.request.CreateManagerRequest
import com.br.semesperanca.backendleads.application.model.request.UpdateManagerRequest
import com.br.semesperanca.backendleads.application.model.request.UserFilterRequest
import com.br.semesperanca.backendleads.application.model.response.ManagerResponse
import com.br.semesperanca.backendleads.configuration.exception.EmailAlreadyExistsException
import com.br.semesperanca.backendleads.configuration.exception.NotFoundException
import com.br.semesperanca.backendleads.domain.model.BranchOffice
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
import org.springframework.security.access.prepost.PreAuthorize
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
    /**
     * A filial vem do `BranchOfficeRepository` escopado, então a filial de outra
     * agência não resolve e a criação sai em 404. É a mesma consulta que autoriza
     * e que fornece a agência — não há comparação de papel nem id em lista.
     */
    @PreAuthorize("hasRole('FULL_ADMIN') or hasRole('ADMIN')")
    @Transactional
    fun create(request: CreateManagerRequest): ManagerResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw EmailAlreadyExistsException(request.email)
        }
        val role: Role =
            roleRepository
                .findByName(Role.MANAGER)
                .orElseThrow { IllegalStateException("Role MANAGER não encontrada — a V002 semeia a tabela role") }

        val branchOffice = findBranchOfficeById(request.branchOfficeId)

        val manager =
            Manager().apply {
                name = request.name
                email = request.email
                setPassword(passwordEncoder.encode(request.password))
                this.role = role
                isActive = true
                this.branchOffice = branchOffice
                // Derivada da filial, e não vinda do corpo: `users` tem
                // `real_estate_agency_id NOT NULL`, e um gerente com filial de uma
                // agência e agência de outra não pertence a nenhuma subárvore
                // coerente.
                realEstateAgencyId = branchOffice.realEstateAgency?.id
            }

        return toResponse(repository.save(manager))
    }

    fun findById(id: UUID): ManagerResponse = toResponse(findManagerById(id))

    fun findAll(
        filter: UserFilterRequest,
        pageable: Pageable,
    ): Page<ManagerResponse> = repository.findAll(buildSpec(filter), pageable).map { toResponse(it) }

    private fun buildSpec(filter: UserFilterRequest): Specification<Manager> =
        Specification { root, _, cb ->
            val predicates = mutableListOf<Predicate>()
            // `isActive` é o nome do atributo JPA; `active` é o nome da coluna. A
            // distinção importa: `root.get("active")` estoura em runtime, porque o
            // Criteria resolve por atributo, não por coluna.
            if (filter.active != null) {
                predicates.add(cb.equal(root.get<Boolean>("isActive"), filter.active))
            }
            if (!filter.search.isNullOrBlank()) {
                val pattern = "%" + filter.search.lowercase() + "%"
                predicates.add(
                    cb.or(
                        cb.like(cb.lower(root.get<String>("name")), pattern),
                        cb.like(cb.lower(root.get<String>("email")), pattern),
                    ),
                )
            }
            cb.and(*predicates.toTypedArray())
        }

    @PreAuthorize("hasRole('FULL_ADMIN') or hasRole('ADMIN')")
    @Transactional
    fun update(
        id: UUID,
        request: UpdateManagerRequest,
    ): ManagerResponse {
        val manager = findManagerById(id)
        val branchOffice = findBranchOfficeById(request.branchOfficeId)
        manager.name = request.name
        manager.email = request.email
        manager.branchOffice = branchOffice
        manager.realEstateAgencyId = branchOffice.realEstateAgency?.id
        return toResponse(repository.save(manager))
    }

    @PreAuthorize("hasRole('FULL_ADMIN') or hasRole('ADMIN')")
    @Transactional
    fun toggleActive(id: UUID): ManagerResponse {
        val manager = findManagerById(id)
        manager.isActive = !manager.isActive
        return toResponse(repository.save(manager))
    }

    @PreAuthorize("hasRole('FULL_ADMIN')")
    @Transactional
    fun delete(id: UUID) {
        val manager = findManagerById(id)
        repository.delete(manager)
    }

    private fun findManagerById(id: UUID): Manager = repository.findById(id).orElseThrow { NotFoundException("Gerente não encontrado") }

    private fun findBranchOfficeById(id: UUID): BranchOffice =
        branchOfficeRepository.findById(id).orElseThrow { NotFoundException("Filial não encontrada") }

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
