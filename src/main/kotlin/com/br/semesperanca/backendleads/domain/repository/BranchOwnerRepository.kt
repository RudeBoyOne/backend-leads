package com.br.semesperanca.backendleads.domain.repository

import com.br.semesperanca.backendleads.domain.model.BranchOwner
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BranchOwnerRepository : ScopedRepository<BranchOwner, UUID> {
    fun existsByRealEstateAgencyId(realEstateAgencyId: UUID): Boolean
}
