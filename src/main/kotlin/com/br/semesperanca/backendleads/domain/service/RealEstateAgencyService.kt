package com.br.semesperanca.backendleads.domain.service

import com.br.semesperanca.backendleads.application.model.request.CreateRealEstateAgencyRequest
import com.br.semesperanca.backendleads.application.model.request.UpdateRealEstateAgencyRequest
import com.br.semesperanca.backendleads.application.model.response.RealEstateAgencyResponse
import com.br.semesperanca.backendleads.configuration.exception.EmailAlreadyExistsException
import com.br.semesperanca.backendleads.configuration.exception.NotFoundException
import com.br.semesperanca.backendleads.domain.model.RealEstateAgency
import com.br.semesperanca.backendleads.domain.repository.RealEstateAgencyRepository
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class RealEstateAgencyService(
    private val repository: RealEstateAgencyRepository,
) {
    @PreAuthorize("hasRole('FULL_ADMIN')")
    @Transactional
    fun create(request: CreateRealEstateAgencyRequest): RealEstateAgencyResponse {
        if (repository.existsByEmail(request.email)) {
            throw EmailAlreadyExistsException(request.email)
        }
        if (repository.existsByCnpj(request.cnpj)) {
            throw IllegalStateException("CNPJ já cadastrado")
        }

        val agency =
            RealEstateAgency(
                name = request.name,
                cnpj = request.cnpj,
                email = request.email,
                phone = request.phone,
                active = true,
            )

        return toResponse(repository.save(agency))
    }

    fun findById(id: UUID): RealEstateAgencyResponse = toResponse(findAgencyById(id))

    fun findAll(): List<RealEstateAgencyResponse> = repository.findAll().map { toResponse(it) }

    fun searchByName(name: String): List<RealEstateAgencyResponse> = repository.searchByName(name).map { toResponse(it) }

    @PreAuthorize("hasRole('FULL_ADMIN')")
    @Transactional
    fun update(
        id: UUID,
        request: UpdateRealEstateAgencyRequest,
    ): RealEstateAgencyResponse {
        val agency = findAgencyById(id)

        if (agency.email != request.email && repository.existsByEmail(request.email)) {
            throw EmailAlreadyExistsException(request.email)
        }
        if (agency.cnpj != request.cnpj && repository.existsByCnpj(request.cnpj)) {
            throw IllegalStateException("CNPJ já cadastrado")
        }

        agency.name = request.name
        agency.cnpj = request.cnpj
        agency.email = request.email
        agency.phone = request.phone
        agency.active = request.active

        return toResponse(repository.save(agency))
    }

    @PreAuthorize("hasRole('FULL_ADMIN')")
    @Transactional
    fun delete(id: UUID) {
        repository.delete(findAgencyById(id))
    }

    private fun findAgencyById(id: UUID): RealEstateAgency =
        repository.findById(id).orElseThrow {
            NotFoundException("Agência imobiliária não encontrada")
        }

    private fun toResponse(agency: RealEstateAgency): RealEstateAgencyResponse =
        RealEstateAgencyResponse(
            id = agency.id,
            name = agency.name,
            cnpj = agency.cnpj,
            email = agency.email,
            phone = agency.phone,
            active = agency.active,
            createdAt = agency.createdAt,
        )
}
