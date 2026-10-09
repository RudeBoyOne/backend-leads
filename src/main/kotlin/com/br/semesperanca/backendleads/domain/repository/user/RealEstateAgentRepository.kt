package com.br.semesperanca.backendleads.domain.repository.user

import com.br.semesperanca.backendleads.domain.model.user.RealEstateAgent
import com.br.semesperanca.backendleads.domain.repository.ScopedRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface RealEstateAgentRepository : ScopedRepository<RealEstateAgent, UUID> {
    fun findByEmail(email: String): Optional<RealEstateAgent>

    fun existsByCreci(creci: String): Boolean

    fun findByBranchOfficeId(branchOfficeId: UUID): List<RealEstateAgent>
}
