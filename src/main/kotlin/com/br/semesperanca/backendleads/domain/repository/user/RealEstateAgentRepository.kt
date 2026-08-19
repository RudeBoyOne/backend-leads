package com.br.semesperanca.backendleads.domain.repository.user

import com.br.semesperanca.backendleads.domain.model.user.RealEstateAgent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface RealEstateAgentRepository : JpaRepository<RealEstateAgent, UUID>, JpaSpecificationExecutor<RealEstateAgent> {
    fun findByEmail(email: String): Optional<RealEstateAgent>

    fun existsByCreci(creci: String): Boolean
}