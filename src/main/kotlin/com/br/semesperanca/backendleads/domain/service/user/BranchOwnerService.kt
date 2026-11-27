package com.br.semesperanca.backendleads.domain.service.user

import com.br.semesperanca.backendleads.application.model.request.CreateBranchOwnerRequest
import com.br.semesperanca.backendleads.application.model.request.UpdateBranchOwnerRequest
import com.br.semesperanca.backendleads.application.model.response.BranchOwnerResponse
import com.br.semesperanca.backendleads.configuration.exception.EmailAlreadyExistsException
import com.br.semesperanca.backendleads.configuration.exception.NotFoundException
import com.br.semesperanca.backendleads.domain.model.BranchOwner
import com.br.semesperanca.backendleads.domain.model.Role
import com.br.semesperanca.backendleads.domain.repository.BranchOfficeRepository
import com.br.semesperanca.backendleads.domain.repository.BranchOwnerRepository
import com.br.semesperanca.backendleads.domain.repository.RoleRepository
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
class BranchOwnerService(
    private val repository: BranchOwnerRepository,
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val branchOfficeRepository: BranchOfficeRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    @PreAuthorize("hasRole('FULL_ADMIN')")
    @Transactional
    fun create(request: CreateBranchOwnerRequest): BranchOwnerResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw EmailAlreadyExistsException(request.email)
        }

        val role: Role =
            roleRepository
                .findByName(Role.ADMIN)
                .orElseThrow { IllegalStateException("Role ADMIN não encontrada — a V002 semeia a tabela role") }

        val branchOwner =
            BranchOwner(phone = request.phone).apply {
                name = request.name
                email = request.email
                setPassword(passwordEncoder.encode(request.password))
                this.role = role
                isActive = true
                realEstateAgencyId = request.realEstateAgencyId
            }

        return toResponse(repository.save(branchOwner))
    }

    fun findById(id: UUID): BranchOwnerResponse = toResponse(findBranchOwnerById(id))

    fun findAll(
        realEstateAgencyId: UUID?,
        active: Boolean?,
        search: String?,
        pageable: Pageable,
    ): Page<BranchOwnerResponse> = repository.findAll(buildSpec(realEstateAgencyId, active, search), pageable).map { toResponse(it) }

    @PreAuthorize("hasRole('FULL_ADMIN') or hasRole('ADMIN')")
    @Transactional
    fun update(
        id: UUID,
        request: UpdateBranchOwnerRequest,
    ): BranchOwnerResponse {
        val branchOwner = findBranchOwnerById(id)
        branchOwner.name = request.name
        request.email?.let {
            if (it != branchOwner.email && userRepository.existsByEmail(it)) {
                throw EmailAlreadyExistsException(it)
            }
            branchOwner.email = it
        }
        branchOwner.phone = request.phone
        return toResponse(repository.save(branchOwner))
    }

    @PreAuthorize("hasRole('FULL_ADMIN') or hasRole('ADMIN')")
    @Transactional
    fun toggleActive(id: UUID): BranchOwnerResponse {
        val branchOwner = findBranchOwnerById(id)
        branchOwner.isActive = !branchOwner.isActive
        return toResponse(repository.save(branchOwner))
    }

    @PreAuthorize("hasRole('FULL_ADMIN')")
    @Transactional
    fun delete(id: UUID) {
        val branchOwner = findBranchOwnerById(id)
        if (branchOfficeRepository.existsByBranchOwnerId(id)) {
            throw IllegalStateException("Remova ou transfira as filiais sob responsabilidade antes de excluir o responsável")
        }
        repository.delete(branchOwner)
    }

    private fun findBranchOwnerById(id: UUID): BranchOwner =
        repository.findById(id).orElseThrow {
            NotFoundException("Responsável não encontrado")
        }

    private fun buildSpec(
        realEstateAgencyId: UUID?,
        active: Boolean?,
        search: String?,
    ): Specification<BranchOwner> =
        Specification { root, _, cb ->
            val predicates = mutableListOf<Predicate>()
            if (realEstateAgencyId != null) {
                predicates.add(cb.equal(root.get<Any>("realEstateAgencyId"), realEstateAgencyId))
            }
            if (active != null) {
                predicates.add(cb.equal(root.get<Boolean>("isActive"), active))
            }
            if (!search.isNullOrBlank()) {
                val pattern = "%" + search.lowercase() + "%"
                predicates.add(
                    cb.or(
                        cb.like(cb.lower(root.get<String>("name")), pattern),
                        cb.like(cb.lower(root.get<String>("email")), pattern),
                    ),
                )
            }
            cb.and(*predicates.toTypedArray())
        }

    private fun toResponse(branchOwner: BranchOwner): BranchOwnerResponse =
        BranchOwnerResponse(
            id = branchOwner.id!!,
            realEstateAgencyId = branchOwner.realEstateAgencyId!!,
            name = branchOwner.name!!,
            email = branchOwner.email,
            phone = branchOwner.phone,
            role = branchOwner.role?.name,
            active = branchOwner.isActive,
            createdAt = branchOwner.createdAt,
            updatedAt = branchOwner.updatedAt,
        )
}
