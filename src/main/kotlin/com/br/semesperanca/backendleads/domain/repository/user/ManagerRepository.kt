package com.br.semesperanca.backendleads.domain.repository.user

import com.br.semesperanca.backendleads.domain.model.user.Manager
import com.br.semesperanca.backendleads.domain.repository.ScopedRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface ManagerRepository : ScopedRepository<Manager, UUID> {
    fun findByEmail(email: String): Optional<Manager>

    fun findByBranchOfficeId(branchOfficeId: UUID): List<Manager>
}
