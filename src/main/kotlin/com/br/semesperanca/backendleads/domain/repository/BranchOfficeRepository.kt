package com.br.semesperanca.backendleads.domain.repository

import com.br.semesperanca.backendleads.domain.model.BranchOffice
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BranchOfficeRepository : ScopedRepository<BranchOffice, UUID> {
    fun findByRealEstateAgencyIdAndName(
        realEstateAgencyId: UUID,
        name: String,
    ): BranchOffice?

    fun findByRealEstateAgencyId(realEstateAgencyId: UUID): List<BranchOffice>

    fun findByBranchOwnerId(branchOwnerId: UUID): List<BranchOffice>

    fun existsByBranchOwnerId(branchOwnerId: UUID): Boolean

    /**
     * Conflito de nome **na mesma agência**, e não no mesmo dono: `branch_office`
     * tem `UNIQUE (real_estate_agency_id, name)`, e é esse índice que impede dois
     * homônimos na rua. Checar por dono + nome aceitaria o mesmo nome duas vezes
     * na mesma agência, para deixar o banco recusar com um 500 genérico.
     */
    fun existsByRealEstateAgencyIdAndName(
        realEstateAgencyId: UUID,
        name: String,
    ): Boolean
}
