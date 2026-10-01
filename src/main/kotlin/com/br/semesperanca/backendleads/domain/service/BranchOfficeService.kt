package com.br.semesperanca.backendleads.domain.service

import com.br.semesperanca.backendleads.application.model.request.CreateBranchOfficeRequest
import com.br.semesperanca.backendleads.application.model.request.UpdateBranchOfficeRequest
import com.br.semesperanca.backendleads.application.model.response.BranchOfficeResponse
import com.br.semesperanca.backendleads.domain.model.BranchOffice
import com.br.semesperanca.backendleads.domain.repository.BranchOfficeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class BranchOfficeService(
    private val repository: BranchOfficeRepository,
) {
    @Transactional
    fun create(request: CreateBranchOfficeRequest): BranchOfficeResponse {
        if (repository.existsByRealEstateAgencyIdAndName(request.realEstateAgencyId, request.name)) {
            throw RuntimeException("Filial já cadastrada nesta agência")
        }

        val branchOffice =
            BranchOffice(
                realEstateAgencyId = request.realEstateAgencyId,
                name = request.name,
                address = request.address,
                phone = request.phone,
                active = true,
            )

        return toResponse(repository.save(branchOffice))
    }

    fun findById(id: UUID): BranchOfficeResponse = toResponse(findBranchOfficeById(id))

    fun findAll(): List<BranchOfficeResponse> = repository.findAll().map { toResponse(it) }

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

    @Transactional
    fun toggleActive(id: UUID): BranchOfficeResponse {
        val branchOffice = findBranchOfficeById(id)
        branchOffice.active = !branchOffice.active
        return toResponse(repository.save(branchOffice))
    }

    @Transactional
    fun delete(id: UUID) {
        val branchOffice = findBranchOfficeById(id)
        repository.delete(branchOffice)
    }

    private fun findBranchOfficeById(id: UUID): BranchOffice =
        repository.findById(id).orElseThrow { RuntimeException("Filial não encontrada") }

    private fun toResponse(branchOffice: BranchOffice): BranchOfficeResponse =
        BranchOfficeResponse(
            id = branchOffice.id,
            realEstateAgencyId = branchOffice.realEstateAgencyId,
            name = branchOffice.name,
            address = branchOffice.address,
            phone = branchOffice.phone,
            active = branchOffice.active,
            createdAt = branchOffice.createdAt,
        )
}
