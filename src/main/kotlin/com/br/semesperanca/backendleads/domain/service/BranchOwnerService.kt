package com.br.semesperanca.backendleads.domain.service

import com.br.semesperanca.backendleads.application.model.request.CreateBranchOwnerRequest
import com.br.semesperanca.backendleads.application.model.request.UpdateBranchOwnerRequest
import com.br.semesperanca.backendleads.application.model.response.BranchOwnerResponse
import com.br.semesperanca.backendleads.domain.model.BranchOwner
import com.br.semesperanca.backendleads.domain.repository.BranchOwnerRepository
import jakarta.persistence.criteria.Predicate
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class BranchOwnerService(
    private val repository: BranchOwnerRepository
) {
    @Transactional
    fun create(request: CreateBranchOwnerRequest): BranchOwnerResponse {
        val branchOwner = BranchOwner(
            realEstateAgencyId = request.realEstateAgencyId,
            name = request.name,
            email = request.email,
            phone = request.phone,
            active = true
        )

        return toResponse(repository.save(branchOwner))
    }

    fun findById(id: UUID): BranchOwnerResponse = toResponse(findBranchOwnerById(id))

    fun findAll(
        realEstateAgencyId: UUID?,
        active: Boolean?,
        search: String?,
        pageable: Pageable
    ): Page<BranchOwnerResponse> {
        return repository
            .findAll(buildSpec(realEstateAgencyId, active, search), pageable)
            .map { toResponse(it) }
    }

    @Transactional
    fun update(id: UUID, request: UpdateBranchOwnerRequest): BranchOwnerResponse {
        val branchOwner = findBranchOwnerById(id)
        branchOwner.name = request.name
        branchOwner.email = request.email
        branchOwner.phone = request.phone

        return toResponse(repository.save(branchOwner))
    }

    @Transactional
    fun toggleActive(id: UUID): BranchOwnerResponse {
        val branchOwner = findBranchOwnerById(id)
        branchOwner.active = !branchOwner.active
        return toResponse(repository.save(branchOwner))
    }

    @Transactional
    fun delete(id: UUID) {
        repository.delete(findBranchOwnerById(id))
    }

    private fun findBranchOwnerById(id: UUID): BranchOwner {
        return repository.findById(id).orElseThrow { RuntimeException("Responsável de filial não encontrado") }
    }

    private fun buildSpec(
        realEstateAgencyId: UUID?,
        active: Boolean?,
        search: String?
    ): Specification<BranchOwner> {
        return Specification { root, _, cb ->
            val predicates = mutableListOf<Predicate>()
            if (realEstateAgencyId != null) {
                predicates.add(cb.equal(root.get<Any>("realEstateAgencyId"), realEstateAgencyId))
            }
            if (active != null) {
                predicates.add(cb.equal(root.get<Boolean>("active"), active))
            }
            if (!search.isNullOrBlank()) {
                val pattern = "%" + search.lowercase() + "%"
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

    private fun toResponse(branchOwner: BranchOwner): BranchOwnerResponse {
        return BranchOwnerResponse(
            id = branchOwner.id!!,
            realEstateAgencyId = branchOwner.realEstateAgencyId!!,
            name = branchOwner.name!!,
            email = branchOwner.email,
            phone = branchOwner.phone,
            active = branchOwner.active,
            createdAt = branchOwner.createdAt,
            updatedAt = branchOwner.updatedAt
        )
    }
}