package com.br.semesperanca.backendleads.domain.repository

import com.br.semesperanca.backendleads.domain.model.Team
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TeamRepository : ScopedRepository<Team, UUID> {
    fun findByManagerId(managerId: UUID): List<Team>

    fun findByManagerIdIn(managerIds: Collection<UUID>): List<Team>
}
