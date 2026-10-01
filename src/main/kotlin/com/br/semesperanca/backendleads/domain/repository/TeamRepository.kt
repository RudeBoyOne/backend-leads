package com.br.semesperanca.backendleads.domain.repository

import com.br.semesperanca.backendleads.domain.model.Team
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TeamRepository : JpaRepository<Team, UUID> {
    fun findByBranchOfficeId(branchOfficeId: UUID): List<Team>

    fun findByManagerId(managerId: UUID): List<Team>
}
