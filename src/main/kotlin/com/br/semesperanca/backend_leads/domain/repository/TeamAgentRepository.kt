package com.br.semesperanca.backendleads.domain.repository

import com.br.semesperanca.backendleads.domain.model.TeamAgent
import com.br.semesperanca.backendleads.domain.model.TeamAgentId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TeamAgentRepository : JpaRepository<TeamAgent, TeamAgentId> {

    fun findByTeamId(teamId: UUID): List<TeamAgent>

    fun findByRealEstateAgentId(realEstateAgentId: UUID): List<TeamAgent>

    fun existsByTeamIdAndRealEstateAgentId(
        teamId: UUID,
        realEstateAgentId: UUID,
    ): Boolean
}
