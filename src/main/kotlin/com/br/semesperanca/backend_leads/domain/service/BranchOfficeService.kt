package com.br.semesperanca.backendleads.domain.service

import com.br.semesperanca.backendleads.application.model.request.CreateBranchOfficeRequest
import com.br.semesperanca.backendleads.application.model.request.UpdateBranchOfficeRequest
import com.br.semesperanca.backendleads.application.model.response.BranchOfficeResponse
import com.br.semesperanca.backendleads.configuration.exception.NotFoundException
import com.br.semesperanca.backendleads.domain.model.BranchOffice
import com.br.semesperanca.backendleads.domain.repository.BranchOfficeRepository
import com.br.semesperanca.backendleads.domain.repository.BranchOwnerRepository
import com.br.semesperanca.backendleads.domain.repository.RealEstateAgencyRepository
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class BranchOfficeService(
    private val repository: BranchOfficeRepository,
    private val branchOwnerRepository: BranchOwnerRepository,
    private val realEstateAgencyRepository: RealEstateAgencyRepository,
) {
    @PreAuthorize("hasRole('FULL_ADMIN') or hasRole('ADMIN')")
    @Transactional
    fun create(request: CreateBranchOfficeRequest): BranchOfficeResponse {
        val branchOwner =
            branchOwnerRepository.findById(request.branchOwnerId).orElseThrow {
                NotFoundException("Responsável não encontrado")
            }

        // A agência é a do responsável: filial e dono pertencem à mesma agência,
        // e derivar evita o par de ids que divergiria. O id vem de `users`, mas a
        // coluna da filial é associação — então a entidade é resolvida aqui.
        val realEstateAgency =
            realEstateAgencyRepository
                .findById(branchOwner.realEstateAgencyId!!)
                .orElseThrow { NotFoundException("Agência imobiliária não encontrada") }

        if (repository.existsByRealEstateAgencyIdAndName(branchOwner.realEstateAgencyId!!, request.name)) {
            throw IllegalStateException("Já existe uma filial com este nome nesta agência")
        }

        val branchOffice =
            BranchOffice(
                realEstateAgency = realEstateAgency,
                branchOwner = branchOwner,
                name = request.name,
                address = request.address,
                phone = request.phone,
                active = true,
            )

        return toResponse(repository.save(branchOffice))
    }

    fun findById(id: UUID): BranchOfficeResponse = toResponse(findBranchOfficeById(id))

    fun findAll(): List<BranchOfficeResponse> = repository.findAll().map { toResponse(it) }

    fun searchByName(name: String): List<BranchOfficeResponse> = repository.searchByName(name).map { toResponse(it) }

    @PreAuthorize("hasRole('FULL_ADMIN') or hasRole('ADMIN')")
    @Transactional
    fun update(
        id: UUID,
        request: UpdateBranchOfficeRequest,
    ): BranchOfficeResponse {
        val branchOffice = findBranchOfficeById(id)
        branchOffice.name = request.name
        branchOffice.address = request.address
        branchOffice.phone = request.phone
        return toResponse(repository.save(branchOffice))
    }

    @PreAuthorize("hasRole('FULL_ADMIN') or hasRole('ADMIN')")
    @Transactional
    fun toggleActive(id: UUID): BranchOfficeResponse {
        val branchOffice = findBranchOfficeById(id)
        branchOffice.active = !branchOffice.active
        return toResponse(repository.save(branchOffice))
    }

    @PreAuthorize("hasRole('FULL_ADMIN')")
    @Transactional
    fun delete(id: UUID) {
        val branchOffice = findBranchOfficeById(id)
        repository.delete(branchOffice)
    }

    private fun findBranchOfficeById(id: UUID): BranchOffice =
        repository.findById(id).orElseThrow {
            NotFoundException("Filial não encontrada")
        }

    private fun toResponse(branchOffice: BranchOffice): BranchOfficeResponse =
        BranchOfficeResponse(
            id = branchOffice.id,
            realEstateAgencyId = branchOffice.realEstateAgency?.id,
            branchOwnerId = branchOffice.branchOwner?.id,
            name = branchOffice.name,
            address = branchOffice.address,
            phone = branchOffice.phone,
            active = branchOffice.active,
            createdAt = branchOffice.createdAt,
        )
}
