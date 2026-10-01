package com.br.semesperanca.backendleads.domain.repository

import com.br.semesperanca.backendleads.domain.model.BranchOffice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BranchOfficeRepository : JpaRepository<BranchOffice, UUID> {
    fun findByRealEstateAgencyIdAndName(
        realEstateAgencyId: UUID,
        name: String,
    ): BranchOffice?

    fun findByRealEstateAgencyId(realEstateAgencyId: UUID): List<BranchOffice>

    fun existsByRealEstateAgencyIdAndName(
        realEstateAgencyId: UUID,
        name: String,
    ): Boolean
}
